use strict;
use warnings;
use RDF::Query::Client;
use LWP::Simple;


#  NOTE THAT THIS CODE IS NOW DEPRECATED
# Since Michel brought all endpoints into a single endpoint
# this code will not work

print "\n\nNOTE THAT THIS CODE IS NOW DEPRECATED.  Michel brought all endpoints into a single endpoint and now this code will not work.  If you stil want to run it, then you need to edit it and remove the 'exit' command.  Good luck!\n\n";
exit 0;




# Filtering of queries at the SPARQL end is just too slow.
# this version of the code does the regexp filtering on the results,
# rather than in the query

my $namedgSPARQL = 'PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>

SELECT distinct ?graph 

WHERE {
  graph ?graph {?a ?b ?c}

}';


my $RAWsubjecttypesSPARQL = 'SELECT distinct ?stype
FROM <NAMED_GRAPH_HERE>
WHERE {
 ?s a ?stype .
}
';


my $RAWpredicatetypesSPARQL = 'SELECT distinct ?p
FROM <NAMED_GRAPH_HERE>
WHERE {
 ?s a <SUBJECT_TYPE_HERE> .
?s ?p ?o .   
}
';


my $RAWobjecttypesSPARQL = 'SELECT distinct ?otype
FROM <NAMED_GRAPH_HERE>
WHERE {
 ?s a <SUBJECT_TYPE_HERE> .
?s <PREDICATE_TYPE_HERE> ?o .   
?o a ?otype
}';


my $RAWspecificobjecttypesSPARQL = 'SELECT distinct ?o
WHERE { 

    SERVICE <LOCALENDPOINT>  
         
      {SELECT ?s { ?s a <OTYPE> } LIMIT 1000 }
  

   SERVICE <REMOTEENDPOINT>
        
       { ?s a ?o .
       }
  
}
';

        
# the original query was "select distincts(datatype(?o) as ?dt) but
# this currently doesn't work on OpenLifeData endpoints.
# may be a more general problem with Virtuoso?  So... we assume
# all things attached to a given predicate MUST have the same datatype
# So just pick one...
my $RAWobjectdatatypesSPARQL = 'SELECT (datatype(?o) as ?datatype)
FROM <NAMED_GRAPH_HERE>
WHERE {
 ?s a <SUBJECT_TYPE_HERE> .
?s <PREDICATE_TYPE_HERE> ?o .   
 FILTER isLiteral(?o)
} LIMIT 1
';



my %dataset_endpoints;

open(OUT, ">endpoint_datatypes.list") || die "can't open output file for writing $!\n";

#my $endpoint = "http://sparql.openlifedata.org/";
my $endpoint = "http://s5.semanticscience.org:8890/sparql";

my $graphquery = RDF::Query::Client->new($namedgSPARQL);
my $iterator = $graphquery->execute($endpoint,  {Parameters => {timeout => 380000, format => 'application/sparql-results+json'}});
die "can't connect to endpoint\n" unless $iterator;  # in case endpoint is down
my $namedgraph;
my $highest=0;
# New Format is
# http://bio2rdf.org/affymetrix_resource:bio2rdf.dataset.affymetrix.R3


# now that Michel has all data in one endpoint, this isn't as useful
# we may need to add it back if he starts using graphs for versioning again...
while (my $row = $iterator->next){
        next unless ($row->{graph}->[1] =~ /\.([^\.]+)\.R3$/);
        my $namespace = $1;
        next if $namespace eq "ndc";
        next if $namespace eq "lsr";
        next if $namespace eq "bioportal";
        $namedgraph = $row->{graph}->[1];
        print "$namespace, $namedgraph, $endpoint\n";
        $dataset_endpoints{$namespace} = [$endpoint, $namedgraph];
}

        

foreach my $namespace(sort(keys %dataset_endpoints)){
        my ($endpoint, $namedgraph) = @{$dataset_endpoints{$namespace}};
        print "\n\n\nMoving On to $namespace\n\n\n";
        my $subjecttypesSPARQL = $RAWsubjecttypesSPARQL;
        $subjecttypesSPARQL =~ s/NAMED_GRAPH_HERE/$namedgraph/;
        
        my $typequery = RDF::Query::Client->new($subjecttypesSPARQL); # query for all output types of the form xxx_vocabulary:Resource
        my $siterator = $typequery->execute($endpoint,  {Parameters => {timeout => 380000, format => 'application/sparql-results+json'}});
        unless ($siterator){
                print "          ---------no subject types found -----------\n";
                open (ERR, ">>SPARQLerrors.list") || die "can't open error file $!\n";
                print ERR "$endpoint, $subjecttypesSPARQL\n\n";
                close ERR;
                next;
        }

        
        my %subjectPredicateHash;
        
        while (my $row = $siterator->next){
                my $stype = $row->{stype}->[1];
                
                print STDERR "   ...now checking $stype\n";

                next if $stype =~ 'owl#';
                next if $stype =~ 'dc/terms/Dataset';
                next if $stype =~ 'w3.org';
                # next if $stype =~ ':Resource';
                
                my $base_input_type = "";
                print STDERR "\n\nno match $stype\n\n" unless $stype =~ m|(http://\S+\..*):\S+$|;  # e.g.  (something-something.something:something:something):Geneotype
                $base_input_type = "$1:Resource" if $1;  # because Bio2RDF doesn't know what type something is, it always outputs :Resource, which means we need services that will consume these weakly-typed data
                
                
                my $predicatetypesSPARQL = $RAWpredicatetypesSPARQL;
                $predicatetypesSPARQL =~ s/NAMED_GRAPH_HERE/$namedgraph/;
                $predicatetypesSPARQL =~ s/SUBJECT_TYPE_HERE/$stype/;
                
                my $ptypequery = RDF::Query::Client->new($predicatetypesSPARQL); # query for all output types of the form xxx_vocabulary:Resource
                my $piterator = $ptypequery->execute($endpoint,  {Parameters => {timeout => 380000, format => 'application/sparql-results+json'}});
                unless ($piterator){
                        print "          ---------no predicate types found -----------\n";
                        open (ERR, ">>SPARQLerrors.list") || die "can't open error file $!\n";
                        print ERR "          ---------no predicate types found -----------\n";
                        print ERR "$endpoint, $predicatetypesSPARQL\n\n";
                        close ERR;
                        next;
                }

                while (my $row = $piterator->next){
                        my $ptype = $row->{p}->[1];

                        next if $ptype =~ 'w3.org';
                        next if $ptype =~ 'void#inDataset';
                        
                        $subjectPredicateHash{$stype."||||".$ptype} = 1;

                }
        }
        
        # now we have a non-redundant hash of all S + P combinations in that endpoint
        # iterate over them to find the object (O) types.
        
        foreach my $key(keys %subjectPredicateHash){
                $key =~ /(.*?)\|\|\|\|(.*?)/;
                my $stype = $1;
                my $ptype = $2;
                die "can't extract subject predicate pair from $key" unless ($stype && $ptype);

                # this query only returns things that have an rdf:type
                # therefore it will miss anything that is a Literal
                # (dont worry, we deal with those later!  :-) )
                my $objecttypesSPARQL = $RAWobjecttypesSPARQL;
                $objecttypesSPARQL =~ s/NAMED_GRAPH_HERE/$namedgraph/;
                $objecttypesSPARQL =~ s/SUBJECT_TYPE_HERE/$stype/;
                $objecttypesSPARQL =~ s/PREDICATE_TYPE_HERE/$ptype/;
                        
                my $otypequery = RDF::Query::Client->new($objecttypesSPARQL); # query for all output types of the form xxx_vocabulary:Resource
                my $oiterator = $otypequery->execute($endpoint,  {Parameters => {timeout => 380000, format => 'application/sparql-results+json'}});

                unless ($oiterator){
                        print "          ---------no object types found for $stype  and   $ptype   -----------\n";
                        open (ERR, ">>SPARQLerrors.list") || die "can't open error file $!\n";
                        print ERR "          ---------no object types found for $stype  and   $ptype   -----------\n";
                        print ERR "$endpoint, $objecttypesSPARQL\n\n";
                        close ERR;
                        next;
                }

                my $FLAG = 0;   # this is a flag that is up when it is a resource object, and down when it is a datatype object
                if ($oiterator){
                        while (my $row = $oiterator->next){
                                $FLAG=1;   # an object type was found - set flag so that we don't do the datatype query for this subject/predicate pair!
                                my $otype = $row->{otype}->[1];
        
                                next if $otype =~ 'owl#';
                                next if $otype =~ 'w3.org';
        
                                print "           Found Triple Pattern:     $stype $ptype $otype\n";
                                print OUT "$namespace\t$stype\t$ptype\t$otype\n";
                                                                
                                #die "can't match $otype to determine Bio2RDF namespace" unless ($otype =~ m|openlifedata\.org/([^_]+)_|);  # match everything after the / and up to the next '_'
                                die "can't match $otype to determine Bio2RDF namespace" unless ($otype =~ m|bio2rdf\.org/([^_]+)_|);  # match everything after the / and up to the next '_'
                                my $remotenamespace = $1;
                                print "checking $remotenamespace\n";   # is this a namespace that we have heard of before?
        
                                unless ($dataset_endpoints{$remotenamespace}){  # if the remote dataset isn't alive, then just print the triple pattern in its generic form
                                        next;  # move on to the next ojbect type
                                }
                                
                                # if we get here, then we have a :Resource that is pointing to a remote dataset that is alive.  So do the federated query
                                my ($remoteendpoint, $namedgraph) = @{$dataset_endpoints{$remotenamespace}};
                                die "can't find remote endpoint for $remotenamespace" unless $remoteendpoint;  # this should never happen, but just in case
                                my @specificotypes = &getSpecificObjectTypes($otype, $endpoint, $remoteendpoint);  # this subroutine does the federated query
                                unless ($specificotypes[0]){ # if there was no more specific type found, then the list returned from the subroutine is empty, so just write the generic type
                                        unless ($namespace eq $remotenamespace){                                                
                                                open(FAILED, ">>NoSpecificTypeFound.txt") || die "can't open the file for Michel of non-specific types $!\n";
                                                print FAILED "data in $namespace exists in $remotenamespace but has no more specific type than $otype\n\n";
                                                close FAILED;
                                        }
        
                                        print "           Failed to find anything more specific than $otype\n";
                                        
                                } else {
                                        foreach my $specificotype(@specificotypes){
                                                print "           Found Specific Triple Pattern:     $stype $ptype $specificotype\n";
                                                print OUT "$namespace\t$stype\t$ptype\t$specificotype\n";
                                        }                                                
                                }                                       
                        }
                               
                        next if ($FLAG);  # if the flag is up, then don't test the datatype                        

                        
                        my $datatypesSPARQL = $RAWobjectdatatypesSPARQL;
                        $datatypesSPARQL =~ s/NAMED_GRAPH_HERE/$namedgraph/;
                        $datatypesSPARQL =~ s/SUBJECT_TYPE_HERE/$stype/;
                        $datatypesSPARQL =~ s/PREDICATE_TYPE_HERE/$ptype/;

                        my $diterator;
                       
                       
                        my $dtypequery = RDF::Query::Client->new($datatypesSPARQL); # query for all output types of the form xxx_vocabulary:Resource
                        $dtypequery->useragent->default_headers->header(Accept => "text/plain");  # this was due to a bug in Virtuoso...
                        $diterator = $dtypequery->execute($endpoint,  {Parameters => {timeout => 380000, format => 'text/plain'}});  # and the bug may not exist anymore... but this works anyway.
                        my $content = $dtypequery->{results}->[0]->{response}->content;
                        if ($content =~ m"(http://www.w3.org/2001/XMLSchema#\S+?)\<"s) {
                                print "           Found Triple Pattern:     $stype $ptype $1\n";
                                print OUT "$namespace\t$stype\t$ptype\t$1\n";
                                
                        } else {
                                print "          ---------no data types found for $stype $ptype defaulting to STRING-----------\n";
                                my $otype = "http://www.w3.org/2001/XMLSchema#string";
                                print "           Found Triple Pattern:     $stype $ptype $otype\n";
                                print OUT "$namespace\t$stype\t$ptype\t$otype\n";
                                
                        }
                        
                        
                        # this code removed since because of a bug in the current version of Virtuoso
                        # need to ask for plain text response instead of json, and reach into the output object instead of letting the library parse it
                        #unless ($diterator){
                        #        print "          ---------no data types found for $stype $ptype defaulting to STRING-----------\n";
                        #        my $otype = "http://www.w3.org/2001/XMLSchema#string";
                        #        print "           Found Triple Pattern:     $stype $ptype $otype\n";
                        #        print OUT "$namespace\t$stype\t$ptype\t$otype\n";
                        #        print OUT "$namespace\t$base_input_type\t$ptype\t$otype\n" if $base_input_type;
                        #        next;
                        #}
                        #
                        #while (my $row = $diterator->next){
                        #        my $otype = $row->{datatype}->[0];  # oddly, if you don't group-by it is ->[1], but the query often crashes!
                        #        $otype = $row->{datatype}->[1] unless $otype;  # this is a bug in virtuoso!
                        #        unless ($otype){
                        #                print "\n\nwtf?   no result in a returned result???  \n\n";
                        #                next;
                        #        }
                        #        print "           Found Triple Pattern:     $stype $ptype $otype\n";
                        #        print OUT "$namespace\t$stype\t$ptype\t$otype\n";
                        #        print OUT "$namespace\t$base_input_type\t$ptype\t$otype\n" if $base_input_type;
                        #}
                }
        }

}
exit 1;


sub getSpecificObjectTypes {  # there should never be more than one specific type, given the query, but... lets assume it returns a list just in case
        my ($otype, $endpoint, $remoteendpoint) = @_;
        my $sparql = $RAWspecificobjecttypesSPARQL;
        $sparql =~ s/LOCALENDPOINT/$endpoint/g;
        $sparql =~ s/OTYPE/$otype/g;
        $sparql =~ s/REMOTEENDPOINT/$remoteendpoint/g;
        print "executing query $sparql\n\n";
        my $otypequery = RDF::Query::Client->new($sparql); # query for all output types of the form xxx_vocabulary:Resource
        my $oiterator = $otypequery->execute($endpoint,  {Parameters => {timeout => 380000, format => 'application/sparql-results+json'}});
        unless ($oiterator){
                print "          ---------no more specific types found -----------\n";
                open (ERR, ">>SPARQLerrors.list") || die "can't open error file $!\n";
                print ERR "$endpoint, $sparql\n\n";
                close ERR;
                return undef;
        }
        my @otypes;
        while (my $row = $oiterator->next){
                my $objecttype = $row->{o}->[1];
                next if $objecttype =~ /$otype/;
                next if $objecttype =~ 'w3.org';

                push @otypes, $objecttype;
        }
        return @otypes;              
}
