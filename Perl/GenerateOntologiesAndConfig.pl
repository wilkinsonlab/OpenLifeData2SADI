#!perl -w

use RDF::Trine;

my $ontologyfolder = "./OpenLifeData2SADI_ontologies";

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
    my ($endpoint, $inputURI, $predicateURI, $outputaddedURI) = split /\s+/, $line;
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
    my $outputURI = "http://biordf.org/$ontologyfolder/$endpoint/$servicename.owl#ServiceOutput";
    
    unless (-e $ontologyfolder){
        mkdir $ontologyfolder;
    }
    unless (-e "$ontologyfolder/$endpoint"){
        mkdir "$ontologyfolder/$endpoint";
    }
    
    
    open(OUT, ">$ontologyfolder/$endpoint/$servicename.cfg") || die "can't open output config file $ontologyfolder/$endpoint/$servicename.cfg  $!\n";  
    print OUT "INPUTCLASS_NAME=$inputLabel\n";
    print OUT "INPUTCLASS_URI=$inputURI\n";
    print OUT "OUTPUTCLASS_NAME=ServiceOutput\n";
    print OUT "OUTPUTCLASS_URI=$outputURI\n";
    print OUT "PREDICATE_NAME=$predicateLabel\n";
    print OUT "PREDICATE_URI=$predicateURI\n";
    print OUT "ORIGINAL_ENDPOINT=http://sparql.openlifedata.org/sparql/\n";
    #print OUT "GENERIC_ENDPOINT=http://openlifedata.org/affymetrix/sparql/
    print OUT "OBJECT_TYPE=$outputaddedURI\n"; 
    close OUT;
   
   
   open(OUT, ">$ontologyfolder/$endpoint/$servicename.owl") || die "can't open output owl file $ontologyfolder/$endpoint/$servicename.owl  $!\n";
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


    open(OUT, ">$ontologyfolder/$endpoint/$servicename.sparql") || die "can't open output owl file $ontologyfolder/$endpoint/$servicename.sparql  $!\n";
    print OUT "SELECT *
        WHERE {
	?obj <$predicateURI> %VAR.
	OPTIONAL {?obj  rdfs:label ?label}
}";
    close OUT;
    
    
}
