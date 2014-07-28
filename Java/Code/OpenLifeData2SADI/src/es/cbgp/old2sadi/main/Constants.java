package es.cbgp.old2sadi.main;

/**
 * Class with some fixed values used in the processes.
 * 
 * @author Alejandro Rodríguez González - Centre for Biotechnology and Plant
 *         Genomics
 * 
 */
public class Constants {

	public final static int QUERY_SUBJECT = 0;
	public final static int QUERY_PREDICATE = 1;
	public final static int QUERY_OBJECT_DATA_TYPE = 2;
	public final static int QUERY_OBJECT_OBJECT_TYPE = 3;
	public final static int QUERY_OBJECT_SPECIFIC_OBJECT_TYPE = 4;

	public final static int SPARQL_QUERY_ENGINE_RUN_ENTIRE_ENDPOINT = 0;
	public final static int SPARQL_QUERY_ENGINE_RUN_SINGLE_QUERY = 1;

	public final static String FAILED_QUERIES_FOLDER = "FAILED_QUERIES_FOLDER";

	public final static String DEFAULT_DATA_TYPE = "DEFAULT_DATA_TYPE";
	public final static String LOGGER_ENABLED = "LOGGER_ENABLED";
	public final static String INVERSE_SUFFIX = "INVERSE_SUFFIX";
	public final static String COMMA_SEPARATOR = ",";
	public final static String DEFAULT_IRI_BASE = "DEFAULT_IRI_BASE";
	public final static String SADI_SERVICE_OUTPUT_CLASS = "SADI_SERVICE_OUTPUT_CLASS";
	public final static String DELETE_ONTOLOGIES_ALREADY_CREATED = "DELETE_ONTOLOGIES_ALREADY_CREATED";
	public final static String INPUTCLASS_NAME = "INPUTCLASS_NAME";
	public final static String INPUTCLASS_URI = "INPUTCLASS_URI";
	public final static String PREDICATE_NAME = "PREDICATE_NAME";
	public final static String PREDICATE_URI = "PREDICATE_URI";
	public final static String OUTPUTCLASS_NAME = "OUTPUTCLASS_NAME";
	public final static String OUTPUTCLASS_URI = "OUTPUTCLASS_URI";
	public final static String ORIGINAL_ENDPOINT = "ORIGINAL_ENDPOINT";
	public final static String GENERIC_ENDPOINT = "GENERIC_ENDPOINT";
	public final static String BIO2RDF_DEFAULT_EP = "BIO2RDF_DEFAULT_EP";
	public final static String OBJECT_TYPE = "OBJECT_TYPE";
	public final static String EQUALS = "=";
	public final static String HTTP = "http://";

	public final static String OLD_SUBJECTS_SPARQL_FILE = "OLD_SUBJECTS_SPARQL_FILE";
	public final static String OLD_PREDICATES_SPARQL_FILE = "OLD_PREDICATES_SPARQL_FILE";
	public final static String OLD_OBJECTS_OBJECT_TYPE_SPARQL_FILE = "OLD_OBJECTS_OBJECT_TYPE_SPARQL_FILE";
	public final static String OLD_OBJECTS_DATA_TYPE_SPARQL_FILE = "OLD_OBJECTS_DATA_TYPE_SPARQL_FILE";
	public final static String OLD_SPECIFIC_OBJECTS_TYPE_SPARQL_FILE = "OLD_SPECIFIC_OBJECTS_TYPE_SPARQL_FILE";

	public final static String FILE_PARAMETER = "-f";
	public final static String OLD_PARAMETER = "-o";
	public final static String FAILED_QUERIES_PARAMETER = "-fq";
	public final static String OLD_PARAMETER_SCRATCH = "-s";
	public final static String OLD_PARAMETER_CONTINUE = "-c";

	public final static String FROM_DATASET_REPLACEMENT = "@DATASET";
	public final static String NAMED_GRAPH_REPLACEMENT = "@NAMED_GRAPH";
	public final static String SUBJECT_TYPE_REPLACEMENT = "@SUBJECT_TYPE";
	public final static String PREDICATE_TYPE_REPLACEMENT = "@PREDICATE_TYPE";
	public final static String LOCAL_ENDPOINT_REPLACEMENT = "@LOCAL_ENDPOINT";
	public final static String REMOTE_ENDPOINT_REPLACEMENT = "@REMOTE_ENDPOINT";
	public final static String OBJECT_TYPE_REPLACEMENT = "@OBJECT_TYPE";
	public final static String SPECIFIC_OBJECT_TYPE_QUERY_LIMIT = "@QUERY_LIMIT";

	public final static String SPECIFIC_OBJECT_TYPE_QUERY_LIMIT_VALUE = "SPECIFIC_OBJECT_TYPE_QUERY_LIMIT_VALUE";

	public final static String R2_SPARQL_QUERY_FILE = "R2_SPARQL_QUERY_FILE";
	public final static String R3_SPARQL_QUERY_FILE = "R3_SPARQL_QUERY_FILE";
	public final static String GRAPH_QUERY_SPARQL_FILE = "GRAPH_QUERY_SPARQL_FILE";
	public final static String LOAD_GRAPH_DATASET_FROM_ENDPOINT = "LOAD_GRAPH_DATASET_FROM_ENDPOINT";
	public final static String OLD_DATASET_GRAPH_PATTERN = "OLD_DATASET_GRAPH_PATTERN";
	public final static String OLD_QUERY_TIMEOUT = "OLD_QUERY_TIMEOUT ";
	public final static String OLD_DATASET_GRAPH_PATTERN_STATISTICS_AVOID = "OLD_DATASET_GRAPH_PATTERN_STATISTICS_AVOID";

	public final static String RESOURCE_APPENDIX = "RESOURCE_APPENDIX";

	public final static String ENDPOINTS_TO_SKIP = "ENDPOINTS_TO_SKIP";

	public final static String XML_SCHEMA = "XMLSchema";
	public final static String OBJECT = "Object";

	public final static String LOG_FILE = "LOG_FILE";

	public final static String ENDPOINT = "ENDPOINT";

	public final static String DELETE_PREVIOUS_ONTOLOGIES = "DELETE_PREVIOUS_ONTOLOGIES";
	public final static String DELETE_LOG_FILE = "DELETE_LOG_FILE";

	public final static String NAME = "NAME";
	public final static String RESOURCE_OBJECT = "RESOURCE_OBJECT";

	public final static String BIO2RDF_HIERARCHY_FIX = "BIO2RDF_HIERARCHY_FIX";

	public final static String DUMPS_FOLDER = "DUMPS_FOLDER";

	public final static String JOINED_DUMP_FILE = "JOINED_DUMP_FILE";

	public final static String NO_SPOS = "NO SPOS";

	public final static String ENDPOINT_BASE = "ENDPOINT_BASE";

	public final static int EXECUTION_MODE_OLD = 1;
	public final static int EXECUTION_MODE_FILE = 0;
	public final static int EXECUTION_MODE_FAILED_QUERIES = 2;

	public final static String SUBJECT_TYPES_QUERY_FILTER = "SUBJECT_TYPES_QUERY_FILTER";
	public final static String PREDICATES_FOR_SUBJECT_TYPES_QUERY_FILTER = "PREDICATES_FOR_SUBJECT_TYPES_QUERY_FILTER";
	public final static String OBJECTS_FOR_PREDICATES_AND_SUBJECT_TYPES_QUERY_FILTER = "OBJECTS_FOR_PREDICATES_AND_SUBJECT_TYPES_QUERY_FILTER";

	public final static String SPARQL_ENDPOINT_TERMINATION = "SPARQL_ENDPOINT_TERMINATION";

	public final static String FAILED_QUERY_KEY_TYPE = "FAILED_QUERY_KEY_TYPE";
	public final static String FAILED_QUERY_ASSOCIATED_SUBJECT = "FAILED_QUERY_ASSOCIATED_SUBJECT";
	public final static String FAILED_QUERY_ASSOCIATED_SUBJECTS = "FAILED_QUERY_ASSOCIATED_SUBJECTS";
	public final static String FAILED_QUERY_ASSOCIATED_PREDICATE = "FAILED_QUERY_ASSOCIATED_PREDICATE";
	public final static String FAILED_QUERY_ASSOCIATED_OBJECT = "FAILED_QUERY_ASSOCIATED_OBJECT";
	public final static String FAILED_QUERY_SOURCE_ENDPOINT = "FAILED_QUERY_SOURCE_ENDPOINT";
	public final static String FAILED_QUERY_DESTINY_ENDPOINT = "FAILED_QUERY_DESTINY_ENDPOINT";
	public final static String FAILED_QUERY_ENDPOINT = "FAILED_QUERY_ENDPOINT";
	public final static String FAILED_QUERY_ERROR = "FAILED_QUERY_ERROR";
	public final static String FAILED_QUERY_QUERY = "FAILED_QUERY_QUERY";
	public final static String FAILED_QUERY_ENDPOINT_NAME = "FAILED_QUERY_ENDPOINT_NAME";
	public final static String FAILED_QUERY_ENDPOINT_DATASET = "FAILED_QUERY_ENDPOINT_DATASET";
	public final static String FAILED_QUERY_ENDPOINT_URL = "FAILED_QUERY_ENDPOINT_URL";
	public final static String FAILED_QUERY_ENDPOINT_GRAPH = "FAILED_QUERY_ENDPOINT_GRAPH";
	public final static String FAILED_QUERY_ENDPOINT_RESOURCE_OBJECT = "FAILED_QUERY_ENDPOINT_RESOURCE_OBJECT";

	
}
