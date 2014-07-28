package es.cbgp.old2sadi.ontstuff;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;

import org.apache.commons.lang3.StringUtils;

import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.datatypes.TypeMapper;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.sparql.engine.http.QueryEngineHTTP;

import es.cbgp.old2sadi.failedQueries.FailedQuery;
import es.cbgp.old2sadi.failedQueries.FailedQueryObjectDataType;
import es.cbgp.old2sadi.failedQueries.FailedQueryObjectObjectType;
import es.cbgp.old2sadi.failedQueries.FailedQueryPredicates;
import es.cbgp.old2sadi.failedQueries.FailedQuerySpecificObjectObjectType;
import es.cbgp.old2sadi.failedQueries.FailedQuerySubject;
import es.cbgp.old2sadi.main.ConfigManager;
import es.cbgp.old2sadi.main.Constants;
import es.cbgp.old2sadi.main.Logic;
import es.cbgp.old2sadi.main.MyLogger;
import es.cbgp.old2sadi.main.StaticUtils;
import es.cbgp.old2sadi.objects.OLD2SADIStatistics;
import es.cbgp.old2sadi.objects.Endpoint;
import es.cbgp.old2sadi.objects.Filter;
import es.cbgp.old2sadi.objects.ResultAndMessage;
import es.cbgp.old2sadi.objects.SPO;
import es.cbgp.old2sadi.objects.Substitution;

/**
 * Class to perform the SPARQL query.
 * 
 * @author Alejandro Rodríguez González - Centre for Biotechnology and Plant
 *         Genomics
 * 
 */
public class SPARQLQueryEngine extends Thread {

	private MyLogger logger;
	private OLD2SADIStatistics bs;
	private Endpoint endpoint;
	private Logic logic;
	private LinkedList<Endpoint> endpoints;

	private FailedQuery failedQuery;
	/**
	 * This flag is used to know, when the thread is thrown, if we want:
	 * 
	 * 0. Execute the query of the endpoint completely. 1. Execute a single
	 * query.
	 */
	private int typeOfRun;

	/*************************************************************************************
	 ********************************** CONSTRUCTORS *************************************
	 *************************************************************************************/

	/**
	 * Constructor.
	 * 
	 * @param logger
	 *            Logger.
	 * @param bs
	 *            Statistics.
	 * @param failedQueries
	 *            Failed queries (to fill).
	 * @param endpoints
	 *            Endpoints.
	 */
	public SPARQLQueryEngine(MyLogger logger, OLD2SADIStatistics bs,
			LinkedList<Endpoint> endpoints) {
		this.typeOfRun = Constants.SPARQL_QUERY_ENGINE_RUN_ENTIRE_ENDPOINT;
		this.logger = logger;
		this.bs = bs;
		this.endpoints = endpoints;
	}

	/**
	 * Constructor.
	 * 
	 * @param log
	 *            Logger.
	 * @param bs
	 *            Statistics.
	 * @param fq
	 *            Failed query.
	 */
	public SPARQLQueryEngine(MyLogger log, OLD2SADIStatistics bs, FailedQuery fq) {
		this.typeOfRun = Constants.SPARQL_QUERY_ENGINE_RUN_SINGLE_QUERY;
		this.failedQuery = fq;
		this.logger = log;
		this.bs = bs;
	}

	/*************************************************************************************
	 ********************************** MAIN METHOD **************************************
	 *************************************************************************************/

	/**
	 * Method to query the endpoint. Main Methoid.
	 * 
	 * @param ep
	 *            Receives the endpoint.
	 * @throws Exception
	 *             It can throw an exception.
	 */
	private void queryOLDEndpoint(Endpoint ep) throws Exception {
		this.endpoint = ep;
		logger.log("Querying endpoint: " + ep.getName());
		logger.log("\t[!] Endpoint URL: " + ep.getEndpointURL());
		logger.log("\t[!] Dataset: " + ep.getDataset());

		/*
		 * Here, we have all the subjects.
		 */
		ArrayList<Resource> subjects = getOLDSubjects();

		/*
		 * Now we have to get all the predicates, given these subjects
		 */
		if (subjects.size() > 0) {
			ArrayList<Resource> predicates = new ArrayList<Resource>();
			logger.log("[" + this.endpoint.getName() + "]: "
					+ "Obtaining predicates ....");
			for (int i = 0; i < subjects.size(); i++) {
				Resource subject = subjects.get(i);
				ArrayList<Resource> predsForThisSubject = getOLDPredicatesForSubject(
						subject, subjects);
				predicates.addAll(predsForThisSubject);
			}

			/*
			 * At this point, we have all the subjects and predicates, now we
			 * need the objects
			 */

			if (predicates.size() > 0) {
				logger.log("[" + this.endpoint.getName() + "]: "
						+ "Obtaining objects...");

				// Object types
				getOLDObjectObjectTypes(subjects, predicates);

				// Data types
				getOLDObjectDataTypes(subjects, predicates);

			}
		}
		logger.log("[" + this.endpoint.getName() + "]: "
				+ "Number of SPOs retrieved for endpoint "
				+ this.endpoint.getName() + ": "
				+ this.endpoint.getSPOs().size());
		saveEndpointSPOs(this.endpoint);
	}

	/*************************************************************************************
	 ********************************** SUBJECTS METHOD(S) *******************************
	 *************************************************************************************/

	/**
	 * Method to get subjects in Bio2RDF R3.
	 * 
	 * @return Return the list of resources.
	 * @throws Exception
	 *             It can throw an exception.
	 */
	private ArrayList<Resource> getOLDSubjects() throws Exception {

		logger.log("[" + this.endpoint.getName() + "]: "
				+ "Obtaining subjects ....");
		/*
		 * We attempt to do the first query (get subjects from this graph).
		 * 
		 * We need to change the graph name from default query to the one
		 * obtained.
		 */
		ArrayList<Substitution> subs = new ArrayList<Substitution>();
		subs.add(new Substitution(Constants.NAMED_GRAPH_REPLACEMENT,
				this.endpoint.getDataset()));
		/*
		 * We get the file with the query.
		 */
		String f = ConfigManager.getConfig(Constants.OLD_SUBJECTS_SPARQL_FILE);
		/*
		 * We get the query.
		 */
		String querySubject = loadQueryFromFileWithSubstitutions(f, subs);
		/*
		 * We get the filters (results will be filtered after the query)
		 */
		String queryFilters = ConfigManager
				.getConfig(Constants.SUBJECT_TYPES_QUERY_FILTER);
		LinkedList<Filter> filters = getFilters(queryFilters);
		ArrayList<Resource> subjects = executeResourceTypeQuery(querySubject,
				"?stype", filters, Constants.QUERY_SUBJECT, null, null, null);
		logger.log("[" + this.endpoint.getName() + "]: " + subjects.size()
				+ " subjects found!");
		return subjects;
	}

	/*************************************************************************************
	 ********************************** PREDICATES METHOD(S) *****************************
	 *************************************************************************************/

	/**
	 * Method to get the predicates for a given subject.
	 * 
	 * @param subject
	 *            Receives the subject.
	 * @return Returns the values.
	 * @throws Exception
	 *             It can throw an exception.
	 */
	private ArrayList<Resource> getOLDPredicatesForSubject(Resource subject,
			ArrayList<Resource> subjects) throws Exception {
		logger.log("[" + this.endpoint.getName() + "]: "
				+ "Obtaining predicates for subject " + subject.getURI());
		ArrayList<Substitution> subs = new ArrayList<Substitution>();
		String f = ConfigManager
				.getConfig(Constants.OLD_PREDICATES_SPARQL_FILE);
		subs = new ArrayList<Substitution>();
		subs.add(new Substitution(Constants.NAMED_GRAPH_REPLACEMENT,
				this.endpoint.getDataset()));
		subs.add(new Substitution(Constants.SUBJECT_TYPE_REPLACEMENT, subject
				.getURI()));
		String queryPredPerSubject = loadQueryFromFileWithSubstitutions(f, subs);
		/*
		 * We get the filters (results will be filtered after the query)
		 */
		String queryFilters = ConfigManager
				.getConfig(Constants.PREDICATES_FOR_SUBJECT_TYPES_QUERY_FILTER);
		LinkedList<Filter> filters = getFilters(queryFilters);
		ArrayList<Resource> predsForThisSubject = executeResourceTypeQuery(
				queryPredPerSubject, "?p", filters, Constants.QUERY_PREDICATE,
				subject, null, subjects);
		logger.log("[" + this.endpoint.getName() + "]: "
				+ predsForThisSubject.size() + " predicates found for subject "
				+ subject.getURI());
		return predsForThisSubject;
	}

	/*************************************************************************************
	 ********************************** OBJECT METHOD(S) *********************************
	 *************************************************************************************/

	/*************************************************************************************
	 ************************ SPECIFIC OBJECT TYPE METHOD(S) *****************************
	 *************************************************************************************/
	/**
	 * Method to execute specific object type query.
	 * 
	 * @param finalQuery
	 *            Receives the query.
	 * @param variable
	 *            Receives the variable to get results.
	 * @param dstEndpoint
	 *            Source endpoint.
	 * @param srcEndpoint
	 *            Destiny endpoint.
	 * @param object
	 * @param predicate
	 * @param subject
	 * @return Returns the values.
	 * @throws Exception
	 *             It can throws exception.
	 */
	public ArrayList<Resource> executeSpecificObjectTypeQuery(
			String finalQuery, String variable, String srcEndpoint,
			String dstEndpoint, Resource subject, Resource predicate,
			Resource object) throws Exception {
		ArrayList<Resource> resultsToReturn = new ArrayList<Resource>();
		long t1 = System.currentTimeMillis();
		Query query = null;
		QueryEngineHTTP qexec = null;
		try {
			query = QueryFactory.create(finalQuery);
			qexec = new QueryEngineHTTP(this.endpoint.getEndpointURL(), query);
			qexec.setSelectContentType("text/csv");
			ResultSet results = qexec.execSelect();
			this.bs.incNumberSPARQLQueries();
			String rscAppendix = ConfigManager
					.getConfig(Constants.RESOURCE_APPENDIX);
			while (results.hasNext()) {
				QuerySolution qs = results.next();
				Object varToGet = qs.getResource(variable);
				if (varToGet == null) {
					String rsc = getDataFromQuerySolution(qs.toString());
					varToGet = getResourceFromString(rsc);
				}
				if (varToGet != null) {
					if (varToGet instanceof Resource) {
						/*
						 * If the result didn't contain ":Resource" is a
						 * specific type.
						 */
						if (!((Resource) varToGet).toString().contains(
								rscAppendix)) {
							resultsToReturn.add((Resource) varToGet);
						}
					} else {

						logger.logError("["
								+ this.endpoint.getName()
								+ "]: "
								+ "¡¡Error!! Found a object different of type resource: "
								+ varToGet.toString());
					}
				}
			}
			long t2 = System.currentTimeMillis();
			logger.log("[" + this.endpoint.getName() + "]: "
					+ "Total query time: " + ((t2 - t1) / 1000) + " s");
		} catch (Exception e) {
			System.out.println("FAIL!! - SPECIFIC OBJECT TYPE");
			System.out.println("Endpoint: " + this.endpoint.getEndpointURL());
			System.out.println(finalQuery);
			e.printStackTrace();
			// System.exit(-1);
			if (this.typeOfRun == Constants.SPARQL_QUERY_ENGINE_RUN_ENTIRE_ENDPOINT) {
				this.bs.incNumberSPARQLFailedQueries();
				new FailedQuerySpecificObjectObjectType(finalQuery,
						this.endpoint,
						Constants.QUERY_OBJECT_SPECIFIC_OBJECT_TYPE,
						System.currentTimeMillis(), this.logger,
						e.getMessage(), srcEndpoint, dstEndpoint, subject,
						predicate, object).save();
			}
			logger.logError("[ERROR] Error querying endpoint '"
					+ this.endpoint.getEndpointURL() + "': " + e.getMessage());
		} finally {
			if (qexec != null) {
				qexec.close();
			}
		}
		return resultsToReturn;
	}

	/**
	 * Method to add specific object types to the SPO list.
	 * 
	 * @param specificObjectTypes
	 *            Receive the list of specific types retrieved.
	 * @param subject
	 *            Receives the subject.
	 * @param predicate
	 *            Receives the predicate.
	 * @param object
	 *            Receives the object.
	 */
	private void addSpecificObjectObjectTypes(
			ArrayList<Resource> specificObjectTypes, Resource subject,
			Resource predicate, Resource object) {
		for (int m = 0; m < specificObjectTypes.size(); m++) {
			SPO spo = new SPO(subject.getURI(), predicate.getURI(),
					specificObjectTypes.get(m).getURI());
			if (!this.endpoint.contains(spo)) {
				this.endpoint.addSPO(spo);
				logger.log("[" + this.endpoint.getName() + "]: "
						+ "New SPO (with specific object type): "
						+ subject.getURI() + ", " + predicate.getURI() + ", "
						+ object.getURI());
			}
		}
	}

	/**
	 * Method to get specific object types for a given source type.
	 * 
	 * @param srcEndpoint
	 *            Receives the source endpoint.
	 * @param dstEndpoint
	 *            Receives the destiny endpoint.
	 * @param objectType
	 *            Object type to check.
	 * @param object
	 * @param predicate
	 * @return Return a list of resources.
	 */
	private ArrayList<Resource> getOLDSpecificObjectObjectTypes(
			String srcEndpoint, String dstEndpoint, Resource subject,
			Resource predicate, Resource object) throws Exception {
		String valueLimit = ConfigManager
				.getConfig(Constants.SPECIFIC_OBJECT_TYPE_QUERY_LIMIT_VALUE);
		String sparqlTermination = ConfigManager
				.getConfig(Constants.SPARQL_ENDPOINT_TERMINATION);
		String endpointBase = ConfigManager.getConfig(Constants.ENDPOINT_BASE);

		ArrayList<Substitution> subs = new ArrayList<Substitution>();
		subs.add(new Substitution(Constants.LOCAL_ENDPOINT_REPLACEMENT,
				endpointBase + srcEndpoint + sparqlTermination));
		subs.add(new Substitution(Constants.REMOTE_ENDPOINT_REPLACEMENT,
				endpointBase + dstEndpoint + sparqlTermination));
		subs.add(new Substitution(Constants.OBJECT_TYPE_REPLACEMENT, object
				.toString()));
		subs.add(new Substitution(Constants.SPECIFIC_OBJECT_TYPE_QUERY_LIMIT,
				valueLimit));
		String queryFile = ConfigManager
				.getConfig(Constants.OLD_SPECIFIC_OBJECTS_TYPE_SPARQL_FILE);
		String querySpecificObject = loadQueryFromFileWithSubstitutions(
				queryFile, subs);
		ArrayList<Resource> specificObjectTypes = executeSpecificObjectTypeQuery(
				querySpecificObject, "?o", srcEndpoint, dstEndpoint, subject,
				predicate, object);

		return specificObjectTypes;
	}

	/*************************************************************************************
	 ********************************** OBJECT DATA TYPE METHOD(S) ************************
	 *************************************************************************************/

	/**
	 * Method to get object data types for a list of subjects and predicates.
	 * 
	 * @param subjects
	 *            Receive the list of subjects.
	 * @param predicates
	 *            Receive the list of predicates.
	 * @throws Exception
	 *             It can throw an exception.
	 */
	private void getOLDObjectDataTypes(ArrayList<Resource> subjects,
			ArrayList<Resource> predicates) throws Exception {
		for (int i = 0; i < subjects.size(); i++) {
			Resource subject = subjects.get(i);
			for (int j = 0; j < predicates.size(); j++) {
				Resource predicate = predicates.get(j);
				ArrayList<RDFDatatype> objectDataTypes = getOLDObjectDataType(
						subject, predicate);
				addObjectDataTypesToEndpoint(objectDataTypes, subject,
						predicate);
			}
		}
	}

	/**
	 * Method to get object data type for a concrete subject and predicate.
	 * 
	 * @param subject
	 *            The subject.
	 * @param predicate
	 *            The predicate.
	 * @throws Exception
	 *             It can throw an exception.
	 */
	private ArrayList<RDFDatatype> getOLDObjectDataType(Resource subject,
			Resource predicate) throws Exception {
		String f = ConfigManager
				.getConfig(Constants.OLD_OBJECTS_DATA_TYPE_SPARQL_FILE);
		logger.log("[" + this.endpoint.getName() + "]: "
				+ "Obtaining data types objects for subject "
				+ subject.getURI() + " and predicate " + predicate.getURI());
		ArrayList<Substitution> subs = new ArrayList<Substitution>();
		subs = new ArrayList<Substitution>();
		subs.add(new Substitution(Constants.NAMED_GRAPH_REPLACEMENT,
				this.endpoint.getDataset()));
		subs.add(new Substitution(Constants.SUBJECT_TYPE_REPLACEMENT, subject
				.getURI()));
		subs.add(new Substitution(Constants.PREDICATE_TYPE_REPLACEMENT,
				predicate.getURI()));
		String queryObjectPerPredicateAndSubject = loadQueryFromFileWithSubstitutions(
				f, subs);
		ArrayList<RDFDatatype> objectsForThisSubjectAndPredicate = executeDataTypeQuery(
				queryObjectPerPredicateAndSubject, "?dt", subject, predicate);
		return objectsForThisSubjectAndPredicate;
	}

	/**
	 * Method to add the object data types found to the endponint.
	 * 
	 * @param objectDataTypes
	 *            Receive the list of data types found.
	 * @param subject
	 *            The subject.
	 * @param predicate
	 *            The predicate.
	 */
	private void addObjectDataTypesToEndpoint(
			ArrayList<RDFDatatype> objectDataTypes, Resource subject,
			Resource predicate) {
		logger.log("[" + this.endpoint.getName() + "]: "
				+ objectDataTypes.size()
				+ " objects (data type) found for subject " + subject.getURI()
				+ " and predicate " + predicate.getURI());
		for (int k = 0; k < objectDataTypes.size(); k++) {
			RDFDatatype object = objectDataTypes.get(k);
			if ((object != null) && (object.getURI() != null)) {
				SPO spo = new SPO(subject.getURI(), predicate.getURI(),
						object.getURI());
				if (!this.endpoint.contains(spo)) {
					this.endpoint.addSPO(spo);
					logger.log("[" + this.endpoint.getName() + "]: "
							+ "New SPO: " + subject.getURI() + ", "
							+ predicate.getURI() + ", " + object.getURI());
				}
			} else {
				logger.logError("[" + this.endpoint.getName() + "]: "
						+ "Data type URI not found (in object). Subject: "
						+ subject.getURI() + ". Predicate: "
						+ predicate.getURI() + ". Object: " + object);
			}
		}
	}

	/**
	 * Method to obtain the objects of a concrete pair of subject predicate. The
	 * objects of this query are data types.
	 * 
	 * @param finalQuery
	 *            Final query to execute.
	 * @param variable
	 *            Variable to get in the select.
	 * @param serviceEndpoint
	 *            Endpoint.
	 * @return Return a list of resources.
	 * @throws Exception
	 *             It can throw an exception.
	 */
	public ArrayList<RDFDatatype> executeDataTypeQuery(String finalQuery,
			String variable, Resource subject, Resource predicate)
			throws Exception {
		ArrayList<RDFDatatype> resultsToReturn = new ArrayList<RDFDatatype>();
		// logger.log("Query to perform:\n\n" + finalQuery + "\n\n");
		long t1 = System.currentTimeMillis();
		Query query = null;
		QueryEngineHTTP qexec = null;
		try {
			query = QueryFactory.create(finalQuery);
			qexec = new QueryEngineHTTP(this.endpoint.getEndpointURL(), query);
			qexec.setSelectContentType("text/csv");
			ResultSet results = qexec.execSelect();
			this.bs.incNumberSPARQLQueries();
			while (results.hasNext()) {
				QuerySolution qs = results.next();
				String datatype = getDataFromQuerySolution(qs.toString());
				if (datatype != null) {
					RDFDatatype rd = TypeMapper.getInstance().getTypeByName(
							datatype);
					if (rd == null) {
						/*
						 * Because of the bad typing, if we get a result (empty)
						 * we assume that it is the default data type (string)
						 */
						rd = TypeMapper
								.getInstance()
								.getTypeByName(
										ConfigManager
												.getConfig(Constants.DEFAULT_DATA_TYPE));
					}
					resultsToReturn.add(rd);
				} else {
					logger.logError("["
							+ this.endpoint.getName()
							+ "]: "
							+ "Error obtaining object datatype from this query solution: "
							+ qs.toString());
				}
			}
			long t2 = System.currentTimeMillis();
			logger.log("[" + this.endpoint.getName() + "]: "
					+ "Total query time: " + ((t2 - t1) / 1000) + " s");
		} catch (Exception e) {
			if (this.typeOfRun == Constants.SPARQL_QUERY_ENGINE_RUN_ENTIRE_ENDPOINT) {
				this.bs.incNumberSPARQLFailedQueries();
				new FailedQueryObjectDataType(finalQuery, this.endpoint,
						Constants.QUERY_OBJECT_DATA_TYPE,
						System.currentTimeMillis(), this.logger,
						e.getMessage(), subject, predicate).save();
			}
			logger.logError("[ERROR] Error querying endpoint '"
					+ this.endpoint.getEndpointURL() + "': " + e.getMessage());
		} finally {
			if (qexec != null) {
				qexec.close();
			}
		}
		return resultsToReturn;
	}

	/*************************************************************************************
	 ******************************* OBJECT OBJECT TYPE METHOD(S) ************************
	 *************************************************************************************/

	/**
	 * Method to get object object types in Bio2RDF2 R3
	 * 
	 * @param subjects
	 *            Receive the list of subjects.
	 * @param predicates
	 *            Receive the list of predicates.
	 * @throws Exception
	 *             It can throw an exception.
	 */
	private void getOLDObjectObjectTypes(ArrayList<Resource> subjects,
			ArrayList<Resource> predicates) throws Exception {
		for (int i = 0; i < subjects.size(); i++) {
			Resource subject = subjects.get(i);
			for (int j = 0; j < predicates.size(); j++) {
				Resource predicate = predicates.get(j);
				ArrayList<Resource> objectsForThisSubjectAndPredicate = getOLDObjectObjectType(
						subject, predicate);
				addObjectObjectTypesToEndpoint(
						objectsForThisSubjectAndPredicate, subject, predicate);
			}
		}
	}

	/**
	 * Method to get the object object types for a given subject and predicate.
	 * 
	 * @param subject
	 *            Receives the subject.
	 * @param predicate
	 *            Receives the predicate.
	 * @return Return the list of resources.
	 * @throws Exception
	 *             It can throws an exception.
	 */
	private ArrayList<Resource> getOLDObjectObjectType(Resource subject,
			Resource predicate) throws Exception {
		String f = ConfigManager
				.getConfig(Constants.OLD_OBJECTS_OBJECT_TYPE_SPARQL_FILE);
		logger.log("[" + this.endpoint.getName() + "]: "
				+ "Obtaining object types objects for subject "
				+ subject.getURI() + " and predicate " + predicate.getURI());
		ArrayList<Substitution> subs = new ArrayList<Substitution>();
		subs.add(new Substitution(Constants.NAMED_GRAPH_REPLACEMENT,
				this.endpoint.getDataset()));
		subs.add(new Substitution(Constants.SUBJECT_TYPE_REPLACEMENT, subject
				.getURI()));
		subs.add(new Substitution(Constants.PREDICATE_TYPE_REPLACEMENT,
				predicate.getURI()));
		String queryObjectPerPredicateAndSubject = loadQueryFromFileWithSubstitutions(
				f, subs);
		String queryFilters = ConfigManager
				.getConfig(Constants.OBJECTS_FOR_PREDICATES_AND_SUBJECT_TYPES_QUERY_FILTER);
		LinkedList<Filter> filters = getFilters(queryFilters);
		ArrayList<Resource> objectsForThisSubjectAndPredicate = executeResourceTypeQuery(
				queryObjectPerPredicateAndSubject, "?otype", filters,
				Constants.QUERY_OBJECT_OBJECT_TYPE, subject, predicate, null);
		return objectsForThisSubjectAndPredicate;
	}

	/**
	 * Method to add object object types to endpoint.
	 * 
	 * @param objectsForThisSubjectAndPredicate
	 *            List of objects to add.
	 * @param subject
	 *            Receives the subject.
	 * @param predicate
	 *            Receives the predicate.
	 * @throws Exception
	 *             It can throws exception.
	 */
	private void addObjectObjectTypesToEndpoint(
			ArrayList<Resource> objectsForThisSubjectAndPredicate,
			Resource subject, Resource predicate) throws Exception {
		logger.log("[" + this.endpoint.getName() + "]: "
				+ objectsForThisSubjectAndPredicate.size()
				+ " objects (object type) found for subject "
				+ subject.getURI() + " and predicate " + predicate.getURI());
		for (int k = 0; k < objectsForThisSubjectAndPredicate.size(); k++) {
			Resource object = objectsForThisSubjectAndPredicate.get(k);
			String rscAppendix = ConfigManager
					.getConfig(Constants.RESOURCE_APPENDIX);

			if (object.toString().contains(rscAppendix)) {
				String dstEndpoint = getEndpointFromResourceObject(object
						.toString());
				if (dstEndpoint != null) {
					/*
					 * If we have a dst endpoint.
					 */
					if (!dstEndpoint.equalsIgnoreCase(this.endpoint.getName())) {
						/*
						 * And is different of the original where we are..
						 */
						if (isValidDestinyEndpoint(dstEndpoint)) {
							logger.log("["
									+ this.endpoint.getName()
									+ "]: Found more specific object type for object. Creating federated query against endpoint "
									+ dstEndpoint);
							ArrayList<Resource> specificObjectTypes = getOLDSpecificObjectObjectTypes(
									this.endpoint.getName(), dstEndpoint,
									subject, predicate, object);
							if (specificObjectTypes.size() > 0) {
								addSpecificObjectObjectTypes(
										specificObjectTypes, subject,
										predicate, object);
							}
						} else {
							logger.log("["
									+ this.endpoint.getName()
									+ "]: Found more specific object type for object. However, destiny endpoint ("
									+ dstEndpoint + ") is not valid.");
						}
					}
				}
			} else {
				/*
				 * We don't have specific types so we just insert the generic.
				 */
				SPO spo = new SPO(subject.getURI(), predicate.getURI(),
						object.getURI());
				if (!this.endpoint.contains(spo)) {
					this.endpoint.addSPO(spo);
					logger.log("[" + this.endpoint.getName() + "]: "
							+ "New SPO: " + subject.getURI() + ", "
							+ predicate.getURI() + ", " + object.getURI());
				}
			}

		}
	}

	/*************************************************************************************
	 ********************************** MAIN QUERY (RESOURCE) ****************************
	 *************************************************************************************/

	/**
	 * Method to obtain the resources of a concrete pair of subject predicate.
	 * The objects of this query are object types.
	 * 
	 * @param finalQuery
	 *            Final query to execute.
	 * @param variable
	 *            Variable to get in the select.
	 * @param serviceEndpoint
	 *            Endpoint.
	 * @param type
	 *            Receives the type of query (subject, predicate, object, ...)
	 * @param rscSubject
	 *            Receives the associated subject resource (if applicable).
	 * @param rscPredicate
	 *            Receives the associated predicate resource (if applicable).
	 * 
	 * @return Return a list of resources.
	 * @throws Exception
	 *             It can throw an exception.
	 */
	public ArrayList<Resource> executeResourceTypeQuery(String finalQuery,
			String variable, LinkedList<Filter> filters, int type,
			Resource rscSubject, Resource rscPredicate,
			ArrayList<Resource> subjects) throws Exception {
		ArrayList<Resource> resultsToReturn = new ArrayList<Resource>();
		long t1 = System.currentTimeMillis();
		Query query = null;
		QueryExecution qexec = null;
		try {
			query = QueryFactory.create(finalQuery);
			qexec = QueryExecutionFactory.sparqlService(
					this.endpoint.getEndpointURL(), query);
			ResultSet results = qexec.execSelect();
			this.bs.incNumberSPARQLQueries();
			while (results.hasNext()) {
				QuerySolution qs = results.next();
				Object varToGet = qs.getResource(variable);
				if (varToGet instanceof Resource) {
					if (filters.size() > 0) {
						if (!isFiltered(((Resource) varToGet).toString(),
								filters)) {
							resultsToReturn.add((Resource) varToGet);
						}
					}
				} else {
					logger.logError("["
							+ this.endpoint.getName()
							+ "]: "
							+ "¡¡Error!! Found a object different of type resource: "
							+ varToGet.toString());
				}
			}
			long t2 = System.currentTimeMillis();
			logger.log("[" + this.endpoint.getName() + "]: "
					+ "Total query time: " + ((t2 - t1) / 1000) + " s");
		} catch (Exception e) {
			if (this.typeOfRun == Constants.SPARQL_QUERY_ENGINE_RUN_ENTIRE_ENDPOINT) {
				this.bs.incNumberSPARQLFailedQueries();
				/*
				 * We only save failed queries if we are executing entire
				 * endpoint
				 */
				switch (type) {

				case Constants.QUERY_SUBJECT:
					System.out.println("FAIL!! - SUBJECT TYPE");
					System.out.println("Endpoint: "
							+ this.endpoint.getEndpointURL());
					System.out.println(finalQuery);
					e.printStackTrace();
					new FailedQuerySubject(finalQuery, this.endpoint, type,
							System.currentTimeMillis(), this.logger,
							e.getMessage()).save();
					break;
				case Constants.QUERY_PREDICATE:
					System.out.println("FAIL!! - PREDICATE TYPE");
					System.out.println("Endpoint: "
							+ this.endpoint.getEndpointURL());
					System.out.println(finalQuery);
					e.printStackTrace();
					/*
					 * If we were executing this method to get predicates and it
					 * fails we create a specific type of FailedQuery
					 */new FailedQueryPredicates(finalQuery, this.endpoint,
							type, System.currentTimeMillis(), this.logger,
							e.getMessage(), rscSubject, subjects).save();
					break;
				case Constants.QUERY_OBJECT_OBJECT_TYPE:
					/*
					 * If we were executing this method to get objects (object
					 * type) and it fails we create a specific type of
					 * FailedQuery
					 */
					System.out.println("FAIL!! - OBJECT TYPE");
					System.out.println("Endpoint: "
							+ this.endpoint.getEndpointURL());
					System.out.println(finalQuery);
					e.printStackTrace();
					new FailedQueryObjectObjectType(finalQuery, this.endpoint,
							type, System.currentTimeMillis(), this.logger,
							e.getMessage(), rscSubject, rscPredicate).save();
					break;
				}
			}
			logger.logError("[ERROR] Error querying endpoint '"
					+ this.endpoint.getEndpointURL() + "': " + e.getMessage());
		} finally {
			if (qexec != null) {
				qexec.close();
			}
		}
		return resultsToReturn;
	}

	/*************************************************************************************
	 ********************************** GET GRAPH METHOD *********************************
	 *************************************************************************************/

	/**
	 * Method to get the most updated datasetgraph of a concrete endpoint.
	 * 
	 * @param epURL
	 *            Receives the URL of the endpoint.
	 * @param epName
	 *            Receives the name of the endpoint.
	 * @return Returns the graph.
	 * @throws Exception
	 *             It can throw an exception.
	 */
	public String getMostUpdatedGraph(String epURL, String epName)
			throws Exception {
		LinkedList<String> graphs = new LinkedList<String>();
		String queryStr = StaticUtils.loadFileToString(ConfigManager
				.getConfig(Constants.GRAPH_QUERY_SPARQL_FILE));
		Query query = null;
		QueryExecution qexec = null;
		try {
			query = QueryFactory.create(queryStr);
			qexec = QueryExecutionFactory.sparqlService(epURL, query);
			ResultSet results = qexec.execSelect();
			this.bs.incNumberSPARQLQueries();
			String old_datasetspattern = ConfigManager
					.getConfig(Constants.OLD_DATASET_GRAPH_PATTERN);
			String statistics_ds_avoid = ConfigManager
					.getConfig(Constants.OLD_DATASET_GRAPH_PATTERN_STATISTICS_AVOID);
			while (results.hasNext()) {
				QuerySolution qs = results.next();
				Resource graph = qs.getResource("?g");
				String graphStr = graph.toString();
				if (graphStr.contains(old_datasetspattern)) {
					if (!graphStr.contains(statistics_ds_avoid)) {
						graphs.add(graphStr);
					}
				}
			}
		} catch (Exception e) {
			logger.logError("[ERROR] Error querying endpoint '" + epName
					+ "' (URL: " + epURL + "): " + e.getMessage());
		} finally {
			if (qexec != null) {
				qexec.close();
			}
		}
		Collections.sort(graphs);
		if (graphs.size() > 0) {
			return graphs.get(graphs.size() - 1);
		}
		return null;
	}

	/*************************************************************************************
	 ********************************** RUN (THREADS) ************************************
	 *************************************************************************************/

	public void run() {
		try {
			switch (this.typeOfRun) {
			case Constants.SPARQL_QUERY_ENGINE_RUN_ENTIRE_ENDPOINT:
				this.runEntireEndpoint();
				break;
			case Constants.SPARQL_QUERY_ENGINE_RUN_SINGLE_QUERY:
				this.runFailedQuery();
				break;
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Method to execute the class behaviour on a given endpoint to execute all
	 * the possible queries.
	 * 
	 * @throws Exception
	 *             It can throws an exception.
	 */
	private void runEntireEndpoint() throws Exception {
		long timeEndpointStart = System.currentTimeMillis();
		this.queryOLDEndpoint(this.endpoint);
		long timeEndpointEnd = System.currentTimeMillis();
		logger.log("Time to process endpoint "
				+ this.endpoint.getName()
				+ ": "
				+ StaticUtils.convertSecondsToTime(new BigDecimal(
						((timeEndpointEnd - timeEndpointStart) / 1000))));
		this.endpoint.setSPOsRetrieved(true);
		this.logic.executeDumpJoiner();

	}

	/*************************************************************************************
	 ********************************** FAILED QUERIES METHODS ***************************
	 *************************************************************************************/

	/**
	 * Method to run the failed queries.
	 * 
	 * @throws Exception
	 *             It can throws an exception.
	 */
	private void runFailedQuery() throws Exception {
		logger.log("Running failed query ["
				+ failedQuery.getEndpoint().getName() + "]["
				+ failedQuery.getStringType() + "].");
		this.endpoint = this.failedQuery.getEndpoint();
		ResultAndMessage result = null;
		switch (this.failedQuery.getType()) {
		case Constants.QUERY_SUBJECT:
			/*
			 * If the query failed obtaining the subjects, that mean that we
			 * have to query the entire endpoint.
			 */
			this.queryOLDEndpoint(this.failedQuery.getEndpoint());
			break;
		case Constants.QUERY_PREDICATE:
			result = runFailedQueryPredicates();
			break;
		case Constants.QUERY_OBJECT_DATA_TYPE:
			result = runFailedQueryObjectDataType();
			break;
		case Constants.QUERY_OBJECT_OBJECT_TYPE:
			result = runFailedQueryObjectObjectType();
			break;
		case Constants.QUERY_OBJECT_SPECIFIC_OBJECT_TYPE:
			result = runFailedQuerySpecificObjectType();
			break;
		}
		if (result.getResult()) {
			logger.log("Failed query ' " + this.failedQuery.getFileFailedQuery().getAbsoluteFile().toString() + "' executed correctly. Deleting..");
			this.failedQuery.getFileFailedQuery().delete();
			/*
			 * Si se ha ejecutado correctamente..
			 */
		}
		else {
			logger.log("Failed query ' " + this.failedQuery.getFileFailedQuery().getAbsoluteFile().toString() + "' was not executed correctly.");
			logger.log("Reason: " + result.getMessage());
		}
	}

	/**
	 * Method to query the endpoint to retrieve endpoints.. and continue.
	 * 
	 * @throws Exception
	 *             It can throws an exception.
	 */
	private ResultAndMessage runFailedQueryPredicates() {
		try {
			FailedQueryPredicates fqp = (FailedQueryPredicates) this.failedQuery;
			ArrayList<Resource> predicates = this.getOLDPredicatesForSubject(
					fqp.getAssociatedSubject(),
					fqp.getAssociatedListOfSubjects());
			if (predicates.size() > 0) {
				logger.log("[" + this.endpoint.getName() + "]: "
						+ "Obtaining objects...");

				// Data types
				getOLDObjectDataTypes(fqp.getAssociatedListOfSubjects(),
						predicates);

				// Object types
				getOLDObjectObjectTypes(fqp.getAssociatedListOfSubjects(),
						predicates);
				return new ResultAndMessage(true);

			}
		} catch (Exception e) {
			String msg = "[" + this.endpoint.getName()
					+ "] Error running failed query predicates: "
					+ e.getMessage();
			logger.log(msg);
			return new ResultAndMessage(false, msg);
		}
		return new ResultAndMessage(true);
	}

	/**
	 * Method to query the endpoint to retrieve specific object types.
	 * 
	 * @throws Exception
	 *             It can throws an exception.
	 */
	private ResultAndMessage runFailedQuerySpecificObjectType() {
		try {
			FailedQuerySpecificObjectObjectType fqsoot = (FailedQuerySpecificObjectObjectType) this.failedQuery;
			ArrayList<Resource> objectSpecificObjectTypes = getOLDSpecificObjectObjectTypes(
					fqsoot.getSourceEndpoint(), fqsoot.getDestinyEndpoint(),
					fqsoot.getAssociatedSubject(),
					fqsoot.getAssociatedPredicate(),
					fqsoot.getAssociatedObject());
			addSpecificObjectObjectTypes(objectSpecificObjectTypes,
					fqsoot.getAssociatedSubject(),
					fqsoot.getAssociatedPredicate(),
					fqsoot.getAssociatedObject());
		} catch (Exception e) {
			String msg = "[" + this.endpoint.getName()
					+ "] Error running failed query specific object type: "
					+ e.getMessage();
			logger.log(msg);
			return new ResultAndMessage(false, msg);
		}
		return new ResultAndMessage(true);
	}

	/**
	 * Method to query the endpoint to retrieve object object types.
	 * 
	 * @throws Exception
	 *             It can throws an exception.
	 */
	private ResultAndMessage runFailedQueryObjectObjectType() {
		try {
			FailedQueryObjectObjectType fqoot = (FailedQueryObjectObjectType) this.failedQuery;
			ArrayList<Resource> objectObjectTypes = getOLDObjectObjectType(
					fqoot.getAssociatedSubject(),
					fqoot.getAssociatedPredicate());
			addObjectObjectTypesToEndpoint(objectObjectTypes,
					fqoot.getAssociatedSubject(),
					fqoot.getAssociatedPredicate());
		} catch (Exception e) {
			String msg = "[" + this.endpoint.getName()
					+ "] Error running failed query object object type: "
					+ e.getMessage();
			logger.log(msg);
			return new ResultAndMessage(false, msg);
		}
		return new ResultAndMessage(true);
	}

	/**
	 * Method to query the endpoint to retrieve object data types.
	 * 
	 * @throws Exception
	 *             It can throws an exception.
	 */
	private ResultAndMessage runFailedQueryObjectDataType() {
		try {
			FailedQueryObjectDataType fqodt = (FailedQueryObjectDataType) this.failedQuery;
			ArrayList<RDFDatatype> objectDataTypes = getOLDObjectDataType(
					fqodt.getAssociatedSubject(),
					fqodt.getAssociatedPredicate());
			addObjectDataTypesToEndpoint(objectDataTypes,
					fqodt.getAssociatedSubject(),
					fqodt.getAssociatedPredicate());
		} catch (Exception e) {
			String msg = "[" + this.endpoint.getName()
					+ "] Error running failed query object data type: "
					+ e.getMessage();
			logger.log(msg);
			return new ResultAndMessage(false, msg);
		}
		return new ResultAndMessage(true);
	}

	/*************************************************************************************
	 ********************************** AUXILIAR METHODS *********************************
	 *************************************************************************************/

	/**
	 * Method to get a resource from string.
	 * 
	 * @param v
	 *            Receives the string.
	 * @return Returns the object.
	 */
	private Resource getResourceFromString(String v) {
		OntModel om = ModelFactory.createOntologyModel();
		Resource r = om.createResource(v);
		return r;
	}

	/**
	 * Method to check if a query contains a filtered string.
	 * 
	 * @param query
	 *            Result Query value.
	 * @param filters
	 *            The filters.
	 * @return Return a boolean.
	 */
	private boolean isFiltered(String queryResult, LinkedList<Filter> filters) {
		for (int i = 0; i < filters.size(); i++) {
			if (queryResult.contains(filters.get(i).getFilterField())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Method to get the datatype from Querysolution toString() value
	 * 
	 * @param qs
	 *            Receives the string
	 * @return Returns the value
	 */
	private String getDataFromQuerySolution(String qs) {
		String parts[] = qs.split("=");
		if (parts.length == 2) {
			String dt = parts[1];
			return StringUtils.substringBetween(dt, Character.toString('"'));
		}
		return null;
	}

	/**
	 * Method to check if the destiny endpoint is valid.
	 * 
	 * @param dstEndpoint
	 *            Receives the endpoint.
	 * @return Returns true or false.
	 */
	private boolean isValidDestinyEndpoint(String dstEndpoint) {
		for (int i = 0; i < this.endpoints.size(); i++) {
			if (this.endpoints.get(i).getName().equalsIgnoreCase(dstEndpoint)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * This method is for, given a resource object (in this form:
	 * http://openlifedata.org/merops_vocabulary:Resource) obtain the endpoint
	 * to the one which is pointing (in this case: merops).
	 * 
	 * @param rsc
	 *            Receive the resource.
	 * @return Returns the value.
	 */
	private String getEndpointFromResourceObject(String rsc) {
		String partsRsc[] = rsc.split("/");
		if (partsRsc.length == 4) {
			String epAndRsc = partsRsc[3];
			// epAndRsc = merops_vocabulary:Resource
			String epAndRscParts[] = epAndRsc.split("_");
			if (epAndRscParts.length == 2) {
				return epAndRscParts[0];
			}
		}
		return null;
	}

	/**
	 * Method to get the filters from the string (csv format).
	 * 
	 * @param queryFilters
	 *            The string.
	 * @return The list of filters.
	 */
	private LinkedList<Filter> getFilters(String queryFilters) {
		LinkedList<Filter> filters = new LinkedList<Filter>();
		if (!StaticUtils.isEmpty(queryFilters)) {
			String parts[] = queryFilters.split(Constants.COMMA_SEPARATOR);
			for (int i = 0; i < parts.length; i++) {
				filters.add(new Filter(parts[i]));
			}
		}
		return filters;
	}

	/**
	 * Method to save a dump of the SPOs of an endpoint
	 * 
	 * @param ep
	 *            Receives the endpoint
	 * @throws Exception
	 *             Throws exception.
	 */
	private void saveEndpointSPOs(Endpoint ep) throws Exception {
		String file = "endpointsdump/" + ep.getName() + ".dump";
		BufferedWriter bW = new BufferedWriter(new FileWriter(file));
		if (ep.getSPOs().size() > 0) {
			for (int i = 0; i < ep.getSPOs().size(); i++) {
				SPO spo = ep.getSPOs().get(i);
				bW.write(ep.getName() + "\t" + spo.getSubject() + "\t"
						+ spo.getPredicate() + "\t" + spo.getObject());
				bW.newLine();
			}
		} else {
			bW.write("NO SPOS");
			bW.newLine();
		}
		bW.close();
	}

	/**
	 * Method to load a query from an external file doing some replacements on
	 * the data.
	 * 
	 * @param f
	 *            Receives the file.
	 * @param subs
	 *            List of replacements.
	 * @return Return the final query.
	 * @throws Exception
	 *             It can throw an exception.
	 */
	private String loadQueryFromFileWithSubstitutions(String f,
			ArrayList<Substitution> subs) throws Exception {
		String query = "";
		BufferedReader bL = new BufferedReader(new FileReader(f));
		while (bL.ready()) {
			String read = bL.readLine();
			Substitution sub = getSubstituion(read, subs);
			if (sub != null) {
				read = read.replace(sub.getOriginalString(),
						sub.getReplacedString());
			}
			query += read + "\r\n";
		}
		bL.close();
		return query;
	}

	/**
	 * Method to, given a concrete string readed in the file, knows if contains
	 * the pattern that needs to be replaced.
	 * 
	 * @param read
	 *            Original string readed.
	 * @param subs
	 *            List of possible replacements.
	 * @return Return the object with the replacement (if found)
	 */
	private Substitution getSubstituion(String read,
			ArrayList<Substitution> subs) {
		// System.out.println("read: " + read);
		for (int i = 0; i < subs.size(); i++) {
			String origStr = subs.get(i).getOriginalString();
			// System.out.println("\t to replace: " + origStr);
			if (read.trim().contains(origStr.trim())) {
				// System.out.println("\t REPLACE!: " + origStr);
				return subs.get(i);
			}
		}
		return null;
	}

	public void setSPOQuery(Endpoint endpoint) {
		this.endpoint = endpoint;
	}

	public void setLogic(Logic l) {
		this.logic = l;
	}
}
