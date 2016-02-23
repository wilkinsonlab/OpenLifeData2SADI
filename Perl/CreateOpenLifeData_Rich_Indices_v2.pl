#!perl -w
use strict;
use warnings;
use RDF::Query::Client;
use LWP::Simple;

# Filtering of queries at the SPARQL end is just too slow.
# this version of the code does the regexp filtering on the results,
# rather than in the query

my $namedgSPARQL = 'PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>

SELECT distinct ?graph 

WHERE {
  graph ?graph {?a ?b ?c} .
  #FILTER(CONTAINS(str(?graph), "R3"))
  # interestingly, that causes a timeout on virtuoso
}';


my $RAWgetEverythingSPARQL = 'SELECT distinct ?stype ?p ?otype
FROM <NAMED_GRAPH_HERE>
WHERE {
 ?s a ?stype .
?s ?p ?o .  
  ?o a ?otype .
  FILTER(?p != <http://www.w3.org/1999/02/22-rdf-syntax-ns#type>)
  FILTER(?p != <http://rdfs.org/ns/void#inDataset>)
  
}
';



my $RAWspecificobjecttypesSPARQL = 'SELECT distinct ?o
WHERE { 
         
      ?s a <OTYPE> .
      ?s a ?o .
      FILTER(!CONTAINS(str(?o), "OTYPE")) 
  
}
';

        

my $RAWobjectdatatypesSPARQL_1 = 'SELECT  distinct ?stype ?p 
FROM <NAMED_GRAPH_HERE>
WHERE {
 ?s a ?stype .
 ?s ?p ?o .  
 FILTER isLiteral(?o) .
 FILTER(?p != <http://www.w3.org/1999/02/22-rdf-syntax-ns#type>) .
 FILTER(?p != <http://rdfs.org/ns/void#inDataset>)  
} order by ?stype';


my $RAWobjectdatatypesSPARQL_2 = 'SELECT  distinct(datatype(?o) as ?dt)
FROM <NAMED_GRAPH_HERE>
WHERE {
 ?s a <STYPE> .
 ?s <PTYPE> ?o .  
 FILTER isLiteral(?o) .
}';


my %dataset_endpoints;

# reset error log
open (ERR, ">SPARQLerrors.list") || die "can't open error file $!\n";
close ERR;


open(OUT, ">endpoint_datatypes.list") || die "can't open output file for writing $!\n";

my $endpoint = "http://sparql.openlifedata.org/";
#my $endpoint = "http://s5.semanticscience.org:8890/sparql";

my $graphquery = RDF::Query::Client->new($namedgSPARQL);
my $iterator = $graphquery->execute($endpoint,  {Parameters => {timeout => 0, format => 'application/sparql-results+json'}});
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
        print "Checking literal datatypes first...\n";

        # get the predicates that have literals as their value
        my $datatype_1_SPARQL = $RAWobjectdatatypesSPARQL_1;
        $datatype_1_SPARQL=~ s/NAMED_GRAPH_HERE/$namedgraph/;
        my $datatypequery1 = RDF::Query::Client->new($datatype_1_SPARQL); 
        my $dtiterator1 = $datatypequery1->execute($endpoint,  {Parameters => {timeout => 0, format => 'application/sparql-results+json'}});
#        my $dtiterator1 = $datatypequery1->execute($endpoint,  {Parameters => {timeout => 0, format => 'application/rdf+xml'}});
        unless ($dtiterator1){
                print "          ---------no specific datatypes from query 1 -----------\n";
                open (ERR, ">>SPARQLerrors.list") || die "can't open error file $!\n";
                print ERR "$endpoint, $datatype_1_SPARQL\n\n";
                close ERR;
                next;
        }

        # for each of those predicates, figure out what datatype that literal is        
        while (my $row = $dtiterator1->next){
                my $stype = $row->{stype}->[1];
                my $p = $row->{p}->[1];
                unless ($stype && $p){
                        print "          ---------could get stype and predicate for datatypes from query 1 -----------\n";
                        open (ERR, ">>SPARQLerrors.list") || die "can't open error file $!\n";
                        print ERR "$endpoint, $datatype_1_SPARQL\n\n";
                        close ERR;
                        next;
                }

                next if $stype =~ 'owl#';
                next if $stype =~ 'dc/terms/Dataset';
                next if $stype =~ 'w3.org';
                
                my $datatype_2_SPARQL = $RAWobjectdatatypesSPARQL_2;
                $datatype_2_SPARQL=~ s/NAMED_GRAPH_HERE/$namedgraph/;
                $datatype_2_SPARQL=~ s/STYPE/$stype/;
                $datatype_2_SPARQL=~ s/PTYPE/$p/;
                my $datatypequery2 = RDF::Query::Client->new($datatype_2_SPARQL); 
                my $dtiterator2 = $datatypequery2->execute($endpoint,  {Parameters => {timeout => 0, format => 'application/sparql-results+json'}});
                unless ($dtiterator2){
                        print "          ---------no specific datatypes from query 2 -----------\n";
                        open (ERR, ">>SPARQLerrors.list") || die "can't open error file $!\n";
                        print ERR "$endpoint, $datatype_2_SPARQL\n\n";
                        close ERR;
                        next;
                }
                my $row2 = $dtiterator2->next();  # there's only one datatype per predicate
                my $datatype;
                eval{$datatype = $row2->{dt}->[1];};
                if ($datatype) {
                        print "           Found Triple Pattern:     $stype $p $datatype\n";
                        print OUT "$namespace\t$stype\t$p\t$datatype\n";
                        
                } else {
                        print "          ---------no data types found for $stype $p defaulting to STRING-----------\n";
                        $datatype = "http://www.w3.org/2001/XMLSchema#string";
                        print "           Found Triple Pattern:     $stype $p $datatype\n";
                        print OUT "$namespace\t$stype\t$p\t$datatype\n"; 
                }
        }



        print "\n\nnow checking rdf:types...\n";

        my $getEverythingSPARQL = $RAWgetEverythingSPARQL;
        $getEverythingSPARQL =~ s/NAMED_GRAPH_HERE/$namedgraph/;
        
        my $typequery = RDF::Query::Client->new($getEverythingSPARQL); # query for all output types of the form xxx_vocabulary:Resource
        my $siterator = $typequery->execute($endpoint,  {Parameters => {timeout => 0, format => 'application/sparql-results+json'}});
        unless ($siterator){
                print "          ---------no types -----------\n";
                open (ERR, ">>SPARQLerrors.list") || die "can't open error file $!\n";
                print ERR "$endpoint, $getEverythingSPARQL\n\n";
                close ERR;
                next;
        }

                
        while (my $row = $siterator->next){
                my $stype = $row->{stype}->[1];
                my $p = $row->{p}->[1];
                my $otype = $row->{otype}->[1];
                
                next if $stype =~ 'owl#';
                next if $stype =~ 'dc/terms/Dataset';
                next if $stype =~ 'w3.org';
                next if $otype =~ 'dcat#';
                next if $otype =~ 'owl#';
                next if $otype =~ 'dc/terms/Dataset';
                next if $otype =~ 'w3.org';
                
                print "           Found Triple Pattern:     $stype $p $otype\n";
                print OUT "$namespace\t$stype\t$p\t$otype\n";
                                                                
                #die "can't match $otype to determine Bio2RDF namespace" unless ($otype =~ m|openlifedata\.org/([^_]+)_|);  # match everything after the / and up to the next '_'
                next unless ($otype =~ m|bio2rdf\.org/([^_]+)_|);  # match everything after the / and up to the next '_'; if it doesn't exist, then move on!
                my $remotenamespace = $1;
                print "checking $remotenamespace\n";   # is this a namespace that we have heard of before?

                unless ($dataset_endpoints{$remotenamespace}){  # if the remote dataset isn't alive, then just print the triple pattern in its generic form
                        next;  # move on to the next ojbect type
                }
                
                # if we get here, then we have a :Resource that is pointing to a remote dataset that is alive.  So do the federated query  (NO LONGER FEDERATED - Michel merged all endpoints into one triplestore)
                #my ($remoteendpoint, $namedgraph) = @{$dataset_endpoints{$remotenamespace}};
                #die "can't find remote endpoint for $remotenamespace" unless $remoteendpoint;  # this should never happen, but just in case
                my @specificotypes = &getSpecificObjectTypes($otype);  # this subroutine does the federated query
                unless ($specificotypes[0]){ # if there was no more specific type found, then the list returned from the subroutine is empty, so just write the generic type
                        unless ($namespace eq $remotenamespace){                                                
                                open(FAILED, ">>NoSpecificTypeFound.txt") || die "can't open the file for Michel of non-specific types $!\n";
                                print FAILED "data in $namespace exists in $remotenamespace but has no more specific type than $otype\n\n";
                                close FAILED;
                        }

                        print "           Failed to find anything more specific than $otype\n";
                        
                } else {
                        foreach my $specificotype(@specificotypes){
                                print "           Found Specific Triple Pattern:     $stype $p $specificotype\n";
                                print OUT "$namespace\t$stype\t$p\t$specificotype\n";
                        }                                                
                }                                       
        }
}
exit 1;


sub getSpecificObjectTypes {  # there should never be more than one specific type, given the query, but... lets assume it returns a list just in case
        my ($otype) = @_;
        my $sparql = $RAWspecificobjecttypesSPARQL;

        $sparql =~ s/OTYPE/$otype/g;

        print "executing query $sparql\n\n";
        my $otypequery = RDF::Query::Client->new($sparql); # query for all output types of the form xxx_vocabulary:Resource
        my $oiterator = $otypequery->execute($endpoint,  {Parameters => {timeout => 0, format => 'application/sparql-results+json'}});
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
