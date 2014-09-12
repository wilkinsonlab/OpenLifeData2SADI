#!/usr/bin/perl -w
use strict;
use Try::Tiny;

=head1 NAME

 OpenLifeData2SADI2OPMW.pl  - a script to create a comprehensive
           connectivity map between all pipelineable OpenLifeData2SADI
           services.  This is expressed as an
           Open Provenance Model Workflow model template.
 
=head1 USAGE

  The only thing you might need to configure in this script
  is the regexp that matches your OpenLifeData2SADI services.
  For all of our services, this is 'OpenLifeData2SADI', which is
  a subfolder underneath our 'cgi-bin' folder. 

=cut


use RDF::Trine;
use RDF::Query::Client;
use RDF::Trine::Serializer::Turtle;


my $store = RDF::Trine::Store::Memory->new();
my $model = RDF::Trine::Model->new($store);
  # Create a namespace object for the foaf vocabulary
my $opmw = RDF::Trine::Namespace->new( 'http://www.opmw.org/ontology/' );
my $rdfs = RDF::Trine::Namespace->new(  'http://www.w3.org/2000/01/rdf-schema#');
my $rdf =  RDF::Trine::Namespace->new( 'http://www.w3.org/1999/02/22-rdf-syntax-ns#');

 # Create a node object for the FOAF name property
my $label = $rdfs->label;
my $type = $rdf->type;
my $comment = $rdfs->comment;

my $uses = $opmw->uses;
my $generatedBy = $opmw->isGeneratedBy;
my $wfData = $opmw->WorkflowTemplateArtifact;
my $wfServ = $opmw->WorkflowTemplateProcess;

open(OUT, ">OpenLifeData2SADI2OPMW.ttl") || die "can't create output file $!\n";
close OUT;


my $pairquery = <<EOQ;
PREFIX  dc:   <http://protege.stanford.edu/plugins/owl/dc/protege-dc.owl#>
PREFIX  rdfs: <http://www.w3.org/2000/01/rdf-schema#>
PREFIX  sadi: <http://sadiframework.org/ontologies/sadi.owl#>
PREFIX  xsd:  <http://www.w3.org/2001/XMLSchema#>
PREFIX  owl:  <http://www.w3.org/2002/07/owl#>
PREFIX  rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
PREFIX  serv: <http://www.mygrid.org.uk/mygrid-moby-service#>

 SELECT distinct ?s1 ?s2 
FROM <http://sadiframework.org/registry/>
WHERE
  { ?s1 rdf:type serv:serviceDescription .
    ?s2 rdf:type serv:serviceDescription .

    ?s1 serv:providedBy ?org .
    ?org dc:publisher "openlifedata2sadi.wilkinsonlab.info" .
    ?s2 serv:hasOperation ?operation2 .

    ?s1 sadi:decoratesWith ?blank .
    ?blank owl:someValuesFrom ?output_type .

    ?operation2 serv:inputParameter ?in2 .
    ?in2  serv:objectType ?output_type .

  }

EOQ


my $detailsquery = <<EOQ2;
PREFIX  dc:   <http://protege.stanford.edu/plugins/owl/dc/protege-dc.owl#>
PREFIX  rdfs: <http://www.w3.org/2000/01/rdf-schema#>
PREFIX  sadi: <http://sadiframework.org/ontologies/sadi.owl#>
PREFIX  xsd:  <http://www.w3.org/2001/XMLSchema#>
PREFIX  owl:  <http://www.w3.org/2002/07/owl#>
PREFIX  rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
PREFIX  serv: <http://www.mygrid.org.uk/mygrid-moby-service#>

 SELECT ?name1 ?name2 ?desc1 ?desc2 ?output_type 
FROM <http://sadiframework.org/registry/>
WHERE
  { SERV1 rdf:type serv:serviceDescription .
    SERV2 rdf:type serv:serviceDescription .
    SERV1 serv:providedBy ?org . 
    ?org dc:publisher "openlifedata2sadi.wilkinsonlab.info" .
    SERV1 serv:hasServiceDescriptionText ?desc1 .
    SERV2 serv:hasServiceDescriptionText ?desc2 .
    SERV2 serv:hasOperation ?operation2 .
    SERV1 serv:hasServiceNameText ?name1 .
    SERV2 serv:hasServiceNameText ?name2 .

    SERV1 sadi:decoratesWith ?blank .
    ?blank owl:someValuesFrom ?output_type .

    ?operation2 serv:inputParameter ?in2 .
    ?in2  serv:objectType ?output_type .

  }

EOQ2

   my $client = RDF::Query::Client->new($pairquery);
   print STDERR "\n\nExecuting query\n$pairquery\n\n";
   

    my $iterator;    
    $iterator = $client->execute('http://sadiframework.org/registry/sparql', {Parameters => {timeout => 380000, format => 'application/sparql-results+json'}});
   die $client->error if $client->error;
   die "\n\nquery $pairquery failed\n\n" unless $iterator;

	my %services;
	while (my $row = $iterator->next) {
        	my ($serv1,$serv2) = (
			$row->{s1}->as_string,
			$row->{s2}->as_string);
		$services{"$serv1$serv2"} = [$serv1, $serv2];
	}       # this is just to get a non-redundant list

	foreach my $pair(keys %services){
                my ($serv1, $serv2) = @{$services{"$pair"}};   # s1 feeds s2

		#  now we need to get the details for each pair (doing this as a single operation results in an HTTP timeout... huge result set!)
		my $thisdetailsquery = $detailsquery; # need to make a copy because we're doing variable substitutino in the template
		$thisdetailsquery =~ s/SERV1/$serv1/gs;	
		$thisdetailsquery =~ s/SERV2/$serv2/gs;

		my $client = RDF::Query::Client->new($thisdetailsquery);

		sleep 1;
		print STDERR "\n\nExecuting query\n$thisdetailsquery\n\n";
   
		my $iterator;    
    		$iterator = $client->execute('http://sadiframework.org/registry/sparql', {Parameters => {format => 'application/sparql-results+json'}});
		die $client->error if $client->error;
		unless ($iterator) {
			print "\n\nQuery Failed, trying again\n\n";
			sleep 15;
	    		$iterator = $client->execute('http://sadiframework.org/registry/sparql', {Parameters => {format => 'application/sparql-results+json'}});
			die $client->error if $client->error;
			die "query failed" unless $iterator;
		}		
		while (my $row = $iterator->next){

			my ($name1, $name2, $desc1, $desc2, $datatype) = (
				$row->{name1}->as_string,
				$row->{name2}->as_string,
				$row->{desc1}->as_string,
				$row->{desc2}->as_string,
				$row->{output_type}->as_string);

			print STDERR join "\t", ($serv1, $datatype, $name1, $desc1, $serv2, $name2, $desc2), "\n\n";

		       my $stm;
			$stm = statement($serv1, $type, $wfServ);
	       		$model->add_statement($stm);
       			$stm = statement($serv1, $label, $name1);
       			$model->add_statement($stm);
       			$stm = statement($serv1, $comment, $desc1);
       			$model->add_statement($stm);
       
       
		       $stm = statement($datatype, $type, $wfData);
       			$model->add_statement($stm);
       			$stm = statement($serv2, $uses, $datatype); 
       			$model->add_statement($stm);
       			$stm = statement($serv2, $label, $name2); 
       			$model->add_statement($stm);
       			$stm = statement($serv2, $comment, $desc2); 
       			$model->add_statement($stm);
       
       			$stm = statement($datatype, $generatedBy, $serv1);
       			$model->add_statement($stm);
       
       
		}
   
	}  # loop and do the next pair

print STDERR "NOW SERIALIZING.... this may take a while...";

my $serializer = RDF::Trine::Serializer::Turtle->new(namespaces => {
                                                                    opmw => 'http://www.opmw.org/ontology/',
                                                                    rdfs => 'http://www.w3.org/2000/01/rdf-schema#',
                                                                    rdf => 'http://www.w3.org/1999/02/22-rdf-syntax-ns#'});

my $turtle = $serializer->serialize_model_to_string($model);
open(OUT, ">>OpenLifeData2SADI2OPMW.ttl") || die "can't open output $!\n";
print OUT $turtle;
close OUT;



# I personally didn't find this very useful, but if you want to see the map... uncomment this code

#use RDF::Trine::Exporter::GraphViz;
# my $ser = RDF::Trine::Exporter::GraphViz->new( as => 'dot',
#						style => {rankdir => 'LR'},
#						namespaces => {
#                                                                     opmw => 'http://www.opmw.org/ontology/',
#                                                                     rdfs => 'http://www.w3.org/2000/01/rdf-schema#',
#                                                                     rdf => 'http://www.w3.org/1999/02/22-rdf-syntax-ns#',
#								     sadi => 'http://biordf.org/cgi-bin/SADI/Bio2RDF2SADI/SADI/',} );
#  $ser->to_file( 'graph.svg', $model );


exit 1;


sub statement {
	my ($s, $p, $o) = @_;
	unless (ref($s) =~ /Trine/){
		$s =~ s/[\<\>]//g;
		$s = RDF::Trine::Node::Resource->new($s);
	}
	unless (ref($p) =~ /Trine/){
		$p =~ s/[\<\>]//g;
		$p = RDF::Trine::Node::Resource->new($p);
	}
	unless (ref($o) =~ /Trine/){
		if ($o =~ /^http\:\/\//){
			$o =~ s/[\<\>]//g;
			$o = RDF::Trine::Node::Resource->new($o);
		} elsif ($o =~ /\D/) {
			$o = RDF::Trine::Node::Literal->new($o);
		} else {
			$o = RDF::Trine::Node::Literal->new($o);				
		}
	}
	my $statement = RDF::Trine::Statement->new($s, $p, $o);
	return $statement;
}

