THIS CODE IS NOT USEFUL!   I ONLY KEEP IT FOR REFERENCE PURPOSES!

#use strict;
#use warnings;
#use RDF::Query::Client;
#use LWP::Simple;
#
#my $namedgSPARQL = 'PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
#PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
#
#SELECT distinct ?graph 
#
#WHERE {
#  graph ?graph {?a ?b ?c}
#
#}';
#
#
#my $RAWresourcetypeSPARQL = 'PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
#PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
#PREFIX void: <http://rdfs.org/ns/void#>
#PREFIX bio2rdf: <http://bio2rdf.org/>
#PREFIX http: <http://www.w3.org/2006/http#>
#
#SELECT ?stype ?p ?otype 
#FROM <NAMED_GRAPH_HERE>
#WHERE {
# ?s a ?stype .
# ?s ?p ?o .
# ?o a ?otype .
#
# FILTER (?p != void:inDataset) .
#  FILTER regex(?otype, ":Resource") .
#}
#GROUP BY ?stype ?p ?otype
#';
#
#my $RAWliteraltypeSPARQL = 'PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
#PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
#PREFIX void: <http://rdfs.org/ns/void#>
#PREFIX bio2rdf: <http://bio2rdf.org/>
#PREFIX http: <http://www.w3.org/2006/http#>
#
#SELECT ?sType ?aPred datatype(?o) as ?type
#FROM <NAMED_GRAPH_HERE> 
#WHERE {
# ?s a ?sType .
# ?s ?aPred ?o .
# FILTER (?aPred != void:inDataset) . 
# FILTER isLiteral(?o)
#
#} 
#group by ?sType ?aPred datatype(?o)
#';
#
#my $RAWdatapointSPARQL = 'PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
#PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
#PREFIX void: <http://rdfs.org/ns/void#>
#PREFIX bio2rdf: <http://bio2rdf.org/>
#PREFIX http: <http://www.w3.org/2006/http#>
#
#SELECT ?o
#FROM <NAMED_GRAPH_HERE>
#WHERE {
# ?o a <DATA_TYPE_HERE>
#} 
#limit 1';
#
#my $RAWtypesSPARQL = 'PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
#PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
#SELECT distinct(?dtype)
#FROM <NAMED_GRAPH_HERE>
#WHERE {
#        <DATA_POINT_HERE>  a ?dtype
#} 
#'; 
##         FILTER regex(?dtype, ":Resource")
#
#my %dataset_endpoints;
#
#open(OUT, ">endpoint_datatypes.csv") || die "can't open output file for writing $!\n";
#     
#
#my $bio2rdfendpoints = get('http://s4.semanticscience.org/bio2rdf/3/');
#while (($bio2rdfendpoints =~ /\[(\w+)\].*?(http:\/\/s4.semanticscience.org:\d+\/sparql)/sg) ) {
#        my ($namespace, $endpoint) = ($1, $2);
#        my $graphquery = RDF::Query::Client->new($namedgSPARQL);
#        my $iterator = $graphquery->execute($endpoint);
#        next unless $iterator;  # in case endpoint is down
#        my $namedgraph;
#        my $highest=0;
#        while (my $row = $iterator->next){
#                next unless ($row->{graph}->[1] =~ /bio2rdf.dataset.*?(\d+)$/);
#                my $this = $1;
#                ($namedgraph = $row->{graph}->[1]) if ($this > $highest);
#                print "$namespace, $namedgraph, \n";
#        }
#
#        $dataset_endpoints{$namespace} = [$endpoint, $namedgraph];
#}
#
#foreach my $namespace(keys %dataset_endpoints){
#        my ($endpoint, $namedgraph) = @{$dataset_endpoints{$namespace}};
#        print "\n\n\nMoving On to $namespace\n\n\n";
#        my $resourcetypeSPARQL = $RAWresourcetypeSPARQL;
#        $resourcetypeSPARQL =~ s/NAMED_GRAPH_HERE/$namedgraph/;
#        
#        my $typequery = RDF::Query::Client->new($resourcetypeSPARQL); # query for all output types of the form xxx_vocabulary:Resource
#        my $iterator = $typequery->execute($endpoint);
#        unless ($iterator){print "          ---------no resource types found -----------\n"; next;}
#        my $datapoint;
#        while (my $row = $iterator->next){
#                my $otype = $row->{otype}->[1];
#                print "           Found Object Type $otype\n";
#                my $datapointSPARQL = $RAWdatapointSPARQL;
#                $datapointSPARQL =~ s/DATA_TYPE_HERE/$otype/;  # prepare to look for things 
#                $datapointSPARQL =~ s/NAMED_GRAPH_HERE/$namedgraph/;
#                print "Executing query:\n$datapointSPARQL\nagainst $endpoint\n\n";
#                my $dataquery = RDF::Query::Client->new($datapointSPARQL);
#                my $dataiterator = $dataquery->execute($endpoint);
#                my $datarow = $dataiterator->next;
#                $datapoint = $datarow->{o}->[1];  # a datapoint of that object type
#                print "           Datapoint $datapoint is of type $otype\n";
#                # The next query has to be executed against a different endpoint with a different named graph
#                $otype =~ /\/([\w\-\+]+)[\.\w\-\+]*?_vocabulary:/;  # comes out as e.g. hgnc_vocabulary:Resource.  we need "hgnc" to get the hgnc endpoint from our index
#                my $external_namespace = $1?$1:"";
#                print "           Checking $external_namespace ($otype)\n";
#                print "\a\a\a\a\a\a\ano external namespace found in $otype" unless $external_namespace;
#                print OUT "\a\a\a\a\a\a\ano external namespace found in $otype" unless $external_namespace;
#                next unless $external_namespace;
#                print "NO Bio2RDF ENDPOINT FOUND FOR $external_namespace\n\n\n\n\n" unless $dataset_endpoints{$external_namespace};
#                next unless $dataset_endpoints{$external_namespace}; # sometimes get stuff like "owl_vacabulary:Resource"...??
#                print "\n\n           $external_namespace has a Bio2RDF endpoint\n";
#                my ($externalendpoint, $externalnamedgraph) = @{$dataset_endpoints{$external_namespace}};
#                my $typesSPARQL = $RAWtypesSPARQL;
#                $typesSPARQL =~ s/DATA_POINT_HERE/$datapoint/; # now we want to know what OTHER types it has - more specific types
#                $typesSPARQL =~ s/NAMED_GRAPH_HERE/$externalnamedgraph/; # now we want to know what OTHER types it has - more specific types
#                print "Executing external data endpoint query:\n$typesSPARQL\n\nagainst: $externalendpoint\n\n";
#                my $othertypesquery = RDF::Query::Client->new($typesSPARQL);
#                my $othertypesiterator = $othertypesquery->execute($externalendpoint);
#                next unless $othertypesiterator;
#                my $external_object_type = $otype;  # by default, leave it exactly how it was in the referring endpoint, e.g. hgnc_vocabulary:Resource
#                while (my $othertypesrow = $othertypesiterator->next) {
#                        my $othertype = $othertypesrow->{dtype}->[1];
#                        print "                                              $othertype\n";
#                        next if $othertype =~ /Resource/;
#                        next if $othertype =~ /owl\#/;
#                        $external_object_type = $othertype;  # change it so that we can detect it was changed
#                        print OUT "$namespace\t$otype  IS_ALSO_A $othertype\n";
#                        print "            --- $namespace\t$otype  IS_ALSO_A $othertype\n";
#                }
#                if ($external_object_type eq $otype){  # if it didn't map to anything novel in the other dataset...
#                        print OUT "$namespace\t$otype  IS_JUST_A $otype\n";
#                        print "            ---*** $namespace\t$otype  IS_JUST_A $otype\n\n";
#                }
#        }
#
#        
#}
#
#exit 1;
##
##my $sparql = "select ?p ?o
##where {
##  <http://bio2rdf.org/drugbank:DB00001> a <http://bio2rdf.org/drugbank_vocabulary:Drug> .
##  <http://bio2rdf.org/drugbank:DB00001> ?p ?o .
##}";
##
##my $query = RDF::Query::Client->new($sparql);
##my $iterator = $query->execute('http://s4.semanticscience.org:14006/sparql');  # execute the query against the URL of the endpoint
##
##if ($iterator){ 
##        while (my $row = $iterator->next){ print "$row\n\n"}
##}
##
