#!perl -w

use RDF::Trine;

my $ontologyfolder = "OpenLifeData2SADI_ontologies";
my $endpoint = "http://sparql.openlifedata.org/";

open(ENDPOINTDATATYPES, $ARGV[0]) || die "can't open endpoint datatypes file $!\n";

open (ONTOLOGYTEMP, "./templates/OntologyTemplate.towl") || die "can't open ontology template $!\n";
my @onttemp = <ONTOLOGYTEMP>;
my $ontologytemplateobject = join "", @onttemp;
close ONTOLOGYTEMP;

open (ONTOLOGYTEMP, "./templates/OntologyTemplate_datatype.towl") || die "can't open ontology template $!\n";
@onttemp = <ONTOLOGYTEMP>;
my $ontologytemplatedatatype = join "", @onttemp;
close ONTOLOGYTEMP;

print $ontologytemplateobject;
print $ontologytemplatedatatype;

#open (CONFIGTEMP, "./templates/ConfigTemplate.tconfig") || die "can't open config template $!\n";
#my @conftemp = <CONFIGTEMP>;
#my $conftemplate = join @conftemp;
#open (SPARQLTEMP, "./templates/SPARQLTemplate.tsparql")|| die "can't open sparql template $!\n";


while (my $line = <ENDPOINTDATATYPES>) {
    my ($namespace, $inputURI, $predicateURI, $outputaddedURI) = split /\s+/, $line;
    die "didn't match input $inputURI\n" unless ($inputURI =~ /.*\/(.*?)[\/:#]([\w\-\_\d\(\)\/]+)$/);
    
    my $inputLabel  = $1."_".$2;
    $inputLabel =~ s/\///g;
    #print "$inputURI\n";
    
    die "didn't match predicate $predicateURI\n" unless ($predicateURI =~ /.*\/(\S+?)[\/:#]([\w\-\_\.\d]+)$/);
    my $predicateLabel  = $2;
    my $predicateNamespace = $1;

    die "didn't match output $outputaddedURI\n" unless ($outputaddedURI =~ /.*\/(\S+?)[\/:#]([\w\-\_\(\)\d]+)$/);
    my $outputNamespace = $1;
    my $outputAddedLabel = $2;
    
    my $servicename = $inputLabel . "_". $predicateNamespace ."_". $predicateLabel;
    my $outputURI = "http://biordf.org/$ontologyfolder/$namespace/$servicename.owl#ServiceOutput";
    
    unless (-e $ontologyfolder){
        mkdir $ontologyfolder;
    }
    unless (-e "$ontologyfolder/$namespace"){
        mkdir "$ontologyfolder/$namespace";
    }
    
    #my ($example_input,$example_output) = createExemplarData($endpoint, $inputURI, $predicateURI, $outputURI, $outputaddedURI);

    
    open(OUT, ">$ontologyfolder/$namespace/$servicename.cfg") || die "can't open output config file $ontologyfolder/$namespace/$servicename.cfg  $!\n";  
    print OUT "INPUTCLASS_NAME=$inputLabel\n";
    print OUT "INPUTCLASS_URI=$inputURI\n";
    print OUT "OUTPUTCLASS_NAME=ServiceOutput\n";
    print OUT "OUTPUTCLASS_URI=$outputURI\n";
    print OUT "PREDICATE_NAME=$predicateLabel\n";
    print OUT "PREDICATE_URI=$predicateURI\n";
    print OUT "ORIGINAL_ENDPOINT=$endpoint\n";
    print OUT "OBJECT_TYPE=$outputaddedURI\n";
    close OUT;
   
   
   open(OUT, ">$ontologyfolder/$namespace/$servicename.owl") || die "can't open output owl file $ontologyfolder/$namespace/$servicename.owl  $!\n";
   my $currentontologytype;
    if ($outputaddedURI =~ /XMLSchema/) {
        $current_ontology_type = $ontologytemplatedatatype
    } else {
        $current_ontology_type = $ontologytemplateobject;
    }
    $current_ontology_type =~ s/ONTOLOGY_URI/http:\/\/biordf.org\/$ontologyfolder\/$endpoint\/$servicename.owl/g;
    $current_ontology_type =~ s/PREDICATE_URI/$predicateURI/g;
    $current_ontology_type =~ s/OUTPUT_CLASS_URI/$outputURI/g;
    $current_ontology_type =~ s/INPUT_CLASS_URI/$inputURI/g;
    $current_ontology_type =~ s/ADDED_CLASS_URI/$outputaddedURI/g;
    print OUT $current_ontology_type;
    close OUT;


    open(OUT, ">$ontologyfolder/$namespace/$servicename.sparql") || die "can't open output owl file $ontologyfolder/$namespace/$servicename.sparql  $!\n";
    print OUT "SELECT *
        WHERE {
	%VAR <$predicateURI> ?obj.
	OPTIONAL {?obj  rdfs:label ?label}
}";
    close OUT;
    
    
    
    
    
}

#sub createExemplarData {
#    my ($endpoint, $inputURI, $predicateURI, $outputURI, $outputaddedURI) = @_;
#    my $sparql = "SELECT ?s ?o where {
#	?s a <$inputURI> .
#	?s <$predicateURI> ?o .
#	?o a <$outputaddedURI>
#	} LIMIT 1";
#	
#    my $graphquery = RDF::Query::Client->new($sparql);
#    my $iterator = $graphquery->execute($endpoint,  {Parameters => {timeout => 0, format => 'application/sparql-results+json'}});
#    unless ($iterator){
#	print "  query \n\n$sparql\n\n failed \n\n";
#	next;
#    }
#    
#    # for each of those predicates, figure out what datatype that literal is        
#    while (my $row = $dtiterator1->next){
#	my $s = $row->{s}->[1];
#	my $o = $row->{o}->[1];
#	print "$s as input  $o as output\n";
#    }
#
#}
