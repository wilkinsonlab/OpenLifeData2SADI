package es.cbgp.old2sadi.main;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Properties;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;

import es.cbgp.old2sadi.failedQueries.FailedQuery;
import es.cbgp.old2sadi.failedQueries.FailedQueryObjectDataType;
import es.cbgp.old2sadi.failedQueries.FailedQueryObjectObjectType;
import es.cbgp.old2sadi.failedQueries.FailedQueryPredicates;
import es.cbgp.old2sadi.failedQueries.FailedQuerySpecificObjectObjectType;
import es.cbgp.old2sadi.failedQueries.FailedQuerySubject;
import es.cbgp.old2sadi.objects.OLD2SADIStatistics;
import es.cbgp.old2sadi.objects.Endpoint;
import es.cbgp.old2sadi.objects.SPO;
import es.cbgp.old2sadi.ontstuff.OntologyCreation;
import es.cbgp.old2sadi.ontstuff.SPARQLQueryEngine;

/**
 * Logic class. Execute the processes.
 * 
 * @author Alejandro Rodríguez González - Centre for Biotechnology and Plant
 *         Genomics
 * 
 */
public class Logic {

	private LinkedList<Endpoint> endpoints;
	private LinkedList<String> endpointsToSkip;
	private boolean loadGraphDatasetFromEndpoint;
	private boolean deleteLogFile;
	private String typeOfExecution;
	private File inputFile;
	private OLD2SADIStatistics bs;
	private long timeStart;
	private long timeEnd;
	private MyLogger logger;
	private boolean deletePreviousOntologies;
	private File dumpFile;
	private long tGetSPOsStart;

	/*
	 * This attribute has the following values:
	 * 
	 * 0. If an external file is used. 1. If OLD is used.
	 */
	private int executionMode;
	private boolean loggerEnabled;

	/**
	 * Constructor to execute to get data from OLD.
	 * 
	 * @param typeOfExecution
	 *            Receives if the execution is to continue or to start.
	 * @throws Exception
	 *             It can throw an exception.
	 */
	public Logic(String typeOfExecution) throws Exception {
		this.loadCommonConfig();
		this.executionMode = Constants.EXECUTION_MODE_OLD;
		this.typeOfExecution = typeOfExecution;
		this.loadGraphDatasetFromEndpoint = Boolean.parseBoolean(ConfigManager
				.getConfig(Constants.LOAD_GRAPH_DATASET_FROM_ENDPOINT));
	}

	public Logic() throws Exception {
		this.loadCommonConfig();
		this.executionMode = Constants.EXECUTION_MODE_FAILED_QUERIES;
	}

	/**
	 * Constructor to execute from an external file.
	 * 
	 * @param f
	 *            Receives input file.
	 */
	public Logic(File f) throws Exception {
		this.loadCommonConfig();
		this.executionMode = Constants.EXECUTION_MODE_FILE;
		this.inputFile = f;
	}

	/**
	 * Method to load common config to both types of execution (from OLD, from
	 * File)
	 * 
	 * @throws Exception
	 *             It can throws an exception
	 */
	private void loadCommonConfig() throws Exception {
		this.logger = new MyLogger();
		this.loggerEnabled = Boolean.parseBoolean(ConfigManager
				.getConfig(Constants.LOGGER_ENABLED));
		this.logger.setEnabled(this.loggerEnabled);
		this.deletePreviousOntologies = Boolean.parseBoolean(ConfigManager
				.getConfig(Constants.DELETE_PREVIOUS_ONTOLOGIES));
		this.deleteLogFile = Boolean.parseBoolean(ConfigManager
				.getConfig(Constants.DELETE_LOG_FILE));
		if (this.deleteLogFile) {
			boolean deleted = new File(
					ConfigManager.getConfig(Constants.LOG_FILE)).delete();
			if (deleted) {
				System.out.println("Log file deleted!");
			} else {
				System.err.println("Error deleting log file :-(");
			}
		}
		this.endpointsToSkip = loadSkippedEndpoints();
		this.timeStart = System.currentTimeMillis();
	}

	/**
	 * Method to execute OLD2SADI to get data from failed queries.
	 * 
	 * @throws Exception
	 *             It can throws an Exception.
	 */
	public void executeOLD2SADIFailedQueries() throws Exception {
		logger.log("Executing OLD2SADI in failed queries recover mode..");
		String fqFolderStr = ConfigManager
				.getConfig(Constants.FAILED_QUERIES_FOLDER);
		File fqFolder = new File(fqFolderStr);
		File fqFiles[] = fqFolder.listFiles();
		if (fqFiles.length > 0) {
			logger.log("Number of failed queries found: " + fqFiles.length);
			LinkedList<FailedQuery> failedQueries = new LinkedList<FailedQuery>();
			for (int i = 0; i < fqFiles.length; i++) {
				File fq = fqFiles[i];
				Properties prop = new Properties();
				prop.load(new FileInputStream(fq));
				int type = Integer.parseInt(prop
						.getProperty(Constants.FAILED_QUERY_KEY_TYPE));
				Endpoint ep = getEndpointFromPropertiesFile(prop);
				if (ep != null) {
					FailedQuery fquery = null;
					switch (type) {
					case Constants.QUERY_SUBJECT:
						fquery = loadFailedQuerySubject(prop, fq, ep);
						break;
					case Constants.QUERY_PREDICATE:
						fquery = loadFailedQueryPredicate(prop, fq, ep);
						break;
					case Constants.QUERY_OBJECT_OBJECT_TYPE:
						fquery = loadFailedQueryObjectObjectType(prop, fq, ep);
						break;
					case Constants.QUERY_OBJECT_DATA_TYPE:
						fquery = loadFailedQueryObjectDataType(prop, fq, ep);
						break;
					case Constants.QUERY_OBJECT_SPECIFIC_OBJECT_TYPE:
						fquery = loadFailedQuerySpecificObjectType(prop, fq, ep);
						break;
					}
					if (fquery != null) {
						failedQueries.add(fquery);
					}
				}
			}
			for (int i = 0; i < failedQueries.size(); i++) {
				SPARQLQueryEngine sqe = new SPARQLQueryEngine(this.logger, this.bs, failedQueries.get(i));
				sqe.start();
			}
			//
			// AQUI NOS QUEDAMOS.. DESPUES DE EJECUTAR LAS FALLIDAS, SI ALGUNA HA FUNCIONADO
			// DEBERÍAMOS COGER SUS SPOS Y METERLOS EN EL DUMP CORRESPONDIENTE
			//
			//
		} else {
			logger.log("Failed queries folder (" + fqFolderStr
					+ ") didn't contain any file. No queries to recover..");
			logger.log("Exiting..");
			System.exit(0);
		}

	}

	/**
	 * Method to get endpoint object from properties file.
	 * @param prop Receive the properties object.
	 * @return Return the value.
	 */
	private Endpoint getEndpointFromPropertiesFile(Properties prop) {
		String name = prop.getProperty(Constants.FAILED_QUERY_ENDPOINT_NAME);
		String dataset = prop.getProperty(Constants.FAILED_QUERY_ENDPOINT_DATASET);
		String endpointURL = prop.getProperty(Constants.FAILED_QUERY_ENDPOINT_URL);
		String graph = prop.getProperty(Constants.FAILED_QUERY_ENDPOINT_GRAPH);
		String resObj = prop.getProperty(Constants.FAILED_QUERY_ENDPOINT_RESOURCE_OBJECT);
		Endpoint ep = new Endpoint(endpointURL, name, dataset, resObj);
		ep.setGraphEndpoint(graph);
		return ep;
	}

	private FailedQuerySpecificObjectObjectType loadFailedQuerySpecificObjectType(Properties prop, File fq,
			Endpoint ep) throws Exception {
		String query = prop.getProperty(Constants.FAILED_QUERY_QUERY);
		String error = prop.getProperty(Constants.FAILED_QUERY_ERROR);
		String srcEp = prop.getProperty(Constants.FAILED_QUERY_SOURCE_ENDPOINT);
		String dstEp = prop
				.getProperty(Constants.FAILED_QUERY_DESTINY_ENDPOINT);
		Resource sub = getResourceFrom(prop
				.getProperty(Constants.FAILED_QUERY_ASSOCIATED_SUBJECT));
		Resource pre = getResourceFrom(prop
				.getProperty(Constants.FAILED_QUERY_ASSOCIATED_PREDICATE));
		Resource obj = getResourceFrom(prop
				.getProperty(Constants.FAILED_QUERY_ASSOCIATED_OBJECT));
		if (StaticUtils.areValid(query, error, srcEp, dstEp, sub, pre, obj)) {
			FailedQuerySpecificObjectObjectType fqsoot = new FailedQuerySpecificObjectObjectType(
					fq, query, ep, Constants.QUERY_OBJECT_SPECIFIC_OBJECT_TYPE,
					System.currentTimeMillis(), this.logger, error, srcEp,
					dstEp, sub, pre, obj);
			logger.log("Failed query loaded! ["
					+ fqsoot.getEndpoint().getName() + "]["
					+ fqsoot.getStringType() + "].");
			return fqsoot;
		} else {
			logger.log("Error recovering failed query from file "
					+ fq.getAbsoluteFile().toString()
					+ ". Some parameter is invalid (null or empty).");
			return null;
		}
	}

	private FailedQueryObjectDataType loadFailedQueryObjectDataType(Properties prop, File fq,
			Endpoint ep) throws Exception {
		String error = prop.getProperty(Constants.FAILED_QUERY_ERROR);
		String query = prop.getProperty(Constants.FAILED_QUERY_QUERY);
		Resource sub = getResourceFrom(prop
				.getProperty(Constants.FAILED_QUERY_ASSOCIATED_SUBJECT));
		Resource pre = getResourceFrom(prop
				.getProperty(Constants.FAILED_QUERY_ASSOCIATED_PREDICATE));
		if (StaticUtils.areValid(query, error, sub, pre)) {
			FailedQueryObjectDataType fqodt = new FailedQueryObjectDataType(fq, query, ep,
					Constants.QUERY_OBJECT_DATA_TYPE,
					System.currentTimeMillis(), this.logger, error, sub, pre);
			logger.log("Failed query loaded! ["
					+ fqodt.getEndpoint().getName() + "]["
					+ fqodt.getStringType() + "].");
			return fqodt;
		} else {
			logger.log("Error recovering failed query from file "
					+ fq.getAbsoluteFile().toString()
					+ ". Some parameter is invalid (null or empty).");
			return null;
		}
	}

	private FailedQueryObjectObjectType loadFailedQueryObjectObjectType(Properties prop, File fq,
			Endpoint ep) throws Exception {
		String error = prop.getProperty(Constants.FAILED_QUERY_ERROR);
		String query = prop.getProperty(Constants.FAILED_QUERY_QUERY);
		Resource sub = getResourceFrom(prop
				.getProperty(Constants.FAILED_QUERY_ASSOCIATED_SUBJECT));
		Resource pre = getResourceFrom(prop
				.getProperty(Constants.FAILED_QUERY_ASSOCIATED_PREDICATE));
		if (StaticUtils.areValid(query, error, sub, pre)) {
			FailedQueryObjectObjectType fqoot = new FailedQueryObjectObjectType(fq, query, 
					ep, Constants.QUERY_OBJECT_OBJECT_TYPE,
					System.currentTimeMillis(), this.logger, error, sub, pre);
			logger.log("Failed query loaded! ["
					+ fqoot.getEndpoint().getName() + "]["
					+ fqoot.getStringType() + "].");
			return fqoot;
		} else {
			logger.log("Error recovering failed query from file "
					+ fq.getAbsoluteFile().toString()
					+ ". Some parameter is invalid (null or empty).");
			return null;
		}
	}

	private FailedQueryPredicates loadFailedQueryPredicate(Properties prop, File fq, Endpoint ep)
			throws Exception {
		String error = prop.getProperty(Constants.FAILED_QUERY_ERROR);
		String query = prop.getProperty(Constants.FAILED_QUERY_QUERY);
		Resource sub = getResourceFrom(prop
				.getProperty(Constants.FAILED_QUERY_ASSOCIATED_SUBJECT));
		String subsStr = prop
				.getProperty(Constants.FAILED_QUERY_ASSOCIATED_SUBJECTS);
		ArrayList<Resource> subs = new ArrayList<Resource>();
		String subsParts[] = subsStr.split("@");
		for (int i = 0; i < subsParts.length; i++) {
			Resource r = getResourceFrom(subsParts[i]);
			subs.add(r);
		}
		if (StaticUtils.areValid(query, error, sub, subsStr)) {
			FailedQueryPredicates fqp = new FailedQueryPredicates(fq, query, ep,
					Constants.QUERY_PREDICATE, System.currentTimeMillis(),
					this.logger, error, sub, subs);
			logger.log("Failed query loaded! ["
					+ fqp.getEndpoint().getName() + "][" + fqp.getStringType()
					+ "].");
			return fqp;
		} else {
			logger.log("Error recovering failed query from file "
					+ fq.getAbsoluteFile().toString()
					+ ". Some parameter is invalid (null or empty).");
			return null;
		}
	}

	private FailedQuerySubject loadFailedQuerySubject(Properties prop, File fq, Endpoint ep)
			throws Exception {
		String error = prop.getProperty(Constants.FAILED_QUERY_ERROR);
		String query = prop.getProperty(Constants.FAILED_QUERY_QUERY);
		if (StaticUtils.areValid(query, error)) {
			FailedQuerySubject fqs = new FailedQuerySubject(fq, query, ep,
					Constants.QUERY_SUBJECT, System.currentTimeMillis(),
					this.logger, error);
			logger.log("Failed query loaded! ["
					+ fqs.getEndpoint().getName() + "][" + fqs.getStringType()
					+ "].");
			return fqs;
		} else {
			logger.log("Error recovering failed query from file "
					+ fq.getAbsoluteFile().toString()
					+ ". Some parameter is invalid (null or empty).");
			return null;
		}
	}

	/**
	 * Method to create a resource object from a String.
	 * 
	 * @param v
	 *            Receives the value.
	 * @return Returns the object.
	 */
	private Resource getResourceFrom(String v) {
		OntModel om = ModelFactory.createOntologyModel();
		Resource r = om.createResource(v);
		return r;
	}

	/**
	 * Method to execute OLD2SADI from OLD.
	 * 
	 * @throws Exception
	 *             It can throw an exception.
	 */

	public void executeOLD2SADIFromOLD() throws Exception {
		/*
		 * This should be executed always for any mode.
		 */
		this.commonExecution();

		String typeOfExecutionMsg = null;
		LinkedList<String> loadedEndpoints = new LinkedList<String>();
		if (this.typeOfExecution
				.equalsIgnoreCase(Constants.OLD_PARAMETER_CONTINUE)) {
			typeOfExecutionMsg = "Trying to continue from last execution.";
			logger.log("OpenLifeData2SADI. Mode: Endpoint data retrieval. "
					+ typeOfExecutionMsg);
			loadedEndpoints = this
					.deleteEmptyDumpsAndGetAlreadyLoadedEndpoints();
		} else {
			typeOfExecutionMsg = "Starting from scratch.";
			logger.log("OpenLifeData2SADI. Mode: Endpoint data retrieval. "
					+ typeOfExecutionMsg);
			this.deleteDumps();
			this.deleteFailedQueries();
		}
		/*
		 * Load available endpoints.
		 */
		loadEndpoints(loadedEndpoints);

		/*
		 * Here comes the hard process.
		 */
		tGetSPOsStart = System.currentTimeMillis();
		retrieveDataFromEndpoints();
		/*
		 * Once we have finished this process, we join the dumps in a single
		 * file.
		 */

		executeDumpJoiner();

		/*
		 * Once we have the "dumped file" with all the SPOs, we must execute the
		 * same process that it is executed when we run OLD2SADI using an
		 * external file.
		 */

		this.timeEnd = System.currentTimeMillis();
		logger.log("OpenLifeData2SADI finished!");
		if (dumpFile != null)
			logger.log("Indexes has been saved in " + dumpFile.toString());
		logger.log("Total time: "
				+ StaticUtils.convertSecondsToTime(new BigDecimal(
						((timeEnd - timeStart) / 1000))));
	}

	/**
	 * Method to delete failed queries.
	 * 
	 * @throws Exception
	 *             It can throws an exception.
	 */
	private void deleteFailedQueries() throws Exception {
		File dumpsDir = new File(
				ConfigManager.getConfig(Constants.FAILED_QUERIES_FOLDER));
		File files[] = dumpsDir.listFiles();
		for (int i = 0; i < files.length; i++) {
			files[i].delete();
		}
	}

	/**
	 * Method to, finally, get the SPOs from a dump file and create the
	 * ontologies.
	 * 
	 * @throws Exception
	 *             It can throws exception.
	 */
	private void getSPOsFromFileAndCreateOntologies() throws Exception {
		loadDataFromDumpFile(dumpFile);
		createOntologies();
	}

	/**
	 * Method to execute OLD2SADI from an external file (file mode).
	 * 
	 * @throws Exception
	 *             It can throw an exception.
	 */
	public void executeOLD2SADIFromFile() throws Exception {
		/*
		 * This should be executed always for any mode.
		 */
		this.commonExecution();
		/*
		 * Here starts the file loading part.
		 */
		this.dumpFile = this.inputFile;
		logger.log("OpenLifeData2SADI. Mode: File input");

		/*
		 * In file execution we basically load the data from an external file.
		 * In normal execution we did the same, but before, we get the data from
		 * OLD and dump it into a file. This is to allow, if a process crash, to
		 * execute it again avoiding already loaded endpoints.
		 */
		this.getSPOsFromFileAndCreateOntologies();

		this.timeEnd = System.currentTimeMillis();
		logger.log("OpenLifeData2SADI finished!");
		logger.log("Total time: "
				+ StaticUtils.convertSecondsToTime(new BigDecimal(
						((timeEnd - timeStart) / 1000))));
	}

	/**
	 * This process should be executed independently of the type of execution.
	 * 
	 * @throws Exception
	 *             It can thrown an exception.
	 */
	private void commonExecution() throws Exception {
		logger.log("Executing OpenLifeData2SADI ...");
		this.bs = new OLD2SADIStatistics();
		/*
		 * We delete previous existing files.
		 */
		if (deletePreviousOntologies) {
			deletePreviousOntologies();
		}
	}

	/**
	 * Method to load from the configuration file those endpoints that should be
	 * skipped in the creation of the ontologies.
	 * 
	 * @return Return the list.
	 */
	private LinkedList<String> loadSkippedEndpoints() throws Exception {
		LinkedList<String> epSkip = new LinkedList<String>();
		String epSkipCfg = ConfigManager.getConfig(Constants.ENDPOINTS_TO_SKIP);
		String parts[] = epSkipCfg.split(",");
		for (int i = 0; i < parts.length; i++) {
			epSkip.add(parts[i]);
		}
		return epSkip;
	}

	/**
	 * Method to load the spos from an external file.
	 * 
	 * It remove duplicates.
	 * 
	 * The file format shoud be:
	 * 
	 * <endpoint> <s>
	 * <p>
	 * <o>
	 * 
	 * @throws Exception
	 *             It can throw an exception.
	 */
	private void loadDataFromDumpFile(File ifile) throws Exception {
		logger.log("Loading indexes from file "
				+ ifile.getAbsoluteFile().toString() + " ... ");
		this.endpoints = new LinkedList<Endpoint>();
		BufferedReader bL = new BufferedReader(new FileReader(ifile));
		int nLoaded = 0;
		int nTotal = 0;
		while (bL.ready()) {
			String readed = bL.readLine();
			String parts[] = readed.split("\t");
			if (parts.length == 4) {
				nTotal++;
				String endpoint = parts[0];
				String subject = parts[1];
				String predicate = parts[2];
				String object = parts[3];
				Endpoint ep = getEndpoint(endpoint);
				SPO spo = new SPO(subject, predicate, object);
				if (!ep.contains(spo)) {
					ep.addSPO(new SPO(subject, predicate, object));
					nLoaded++;
				} else {
					logger.log("Duplicate SPO. Dataset: " + ep.getName()
							+ ". SPO: " + spo.toString());
				}
			}
		}
		bL.close();
		logger.log("Done!");
		logger.log("\tNumber of SPOs in the file: " + nTotal);
		logger.log("\tNumber of SPOs loaded (removing duplicates): " + nLoaded
				+ " SPOs");
	}

	/**
	 * Method to get the given endpoint.
	 * 
	 * @param endpoint
	 *            Receives the name.
	 * @return Return the endpoint.
	 */
	private Endpoint getEndpoint(String endpoint) {
		for (int i = 0; i < this.endpoints.size(); i++) {
			if (this.endpoints.get(i).getName().equalsIgnoreCase(endpoint)) {
				return this.endpoints.get(i);
			}
		}
		Endpoint ep = new Endpoint(endpoint);
		try {
			String sparqlEp = getSPARQLEndpoint(endpoint);
			if (!StaticUtils.isEmpty(sparqlEp)) {
				ep.setEndpointURL(sparqlEp);
			}
			String resourceObject = getResourceObjectEndpoint(endpoint);
			if (!StaticUtils.isEmpty(resourceObject)) {
				ep.setResourceObject(resourceObject);
			}
			String graphEndpoint = getGraphEndpoint(sparqlEp, endpoint);
			if (!StaticUtils.isEmpty(graphEndpoint)) {
				ep.setGraphEndpoint(graphEndpoint);
			}
		} catch (Exception e) {
			this.logger.logError("Error obtaining endpoint URL. Endpoint: "
					+ endpoint + ". Error: " + e.getMessage());
		}
		this.endpoints.add(ep);
		return ep;
	}

	/**
	 * If we are loading SPO's from an external file we don't have information
	 * about which is the sparql endpoint. Given that we have this information
	 * in .ep files, we try to get it from there.
	 * 
	 * @param endpoint
	 *            Endpoint name.
	 * @return Return the value.
	 */
	private String getSPARQLEndpoint(String endpoint) throws Exception {
		String file = "endpoints/" + endpoint + ".ep";
		Properties prop = new Properties();
		prop.load(new FileInputStream(new File(file)));
		String sparqlep = prop.getProperty(Constants.ENDPOINT);
		return sparqlep;
	}

	/**
	 * If we are loading SPO's from an external file we don't have information
	 * about which is the resource object. Given that we have this information
	 * in .ep files, we try to get it from there.
	 * 
	 * @param endpoint
	 *            Endpoint name.
	 * @return Return the value.
	 */
	private String getResourceObjectEndpoint(String endpoint) throws Exception {
		String file = "endpoints/" + endpoint + ".ep";
		Properties prop = new Properties();
		prop.load(new FileInputStream(new File(file)));
		String sparqlep = prop.getProperty(Constants.RESOURCE_OBJECT);
		return sparqlep;
	}

	/**
	 * Method to join the dumps.
	 * 
	 * @throws Exception
	 *             It can throws an exception
	 */
	public void executeDumpJoiner() throws Exception {
		if (allEndpointsWhereProcessed()) {
			joinDumps();
			String whereSPOsCameFrom = null;
			switch (this.executionMode) {
			case Constants.EXECUTION_MODE_FILE:
				whereSPOsCameFrom = "external file";
				break;
			case Constants.EXECUTION_MODE_OLD:
				whereSPOsCameFrom = "OpenLifeData endpoints";
				break;
			}
			this.loadDataFromDumpFile(dumpFile);
			long tGetSPOsEnd = System.currentTimeMillis();
			/*
			 * We create the ontologies.
			 */
			logger.log("Retrieval of SPOs from " + whereSPOsCameFrom
					+ " finished!");
			logger.log("Number of endpoints processed: "
					+ this.endpoints.size());

			if (this.executionMode == Constants.EXECUTION_MODE_OLD) {
				logger.log("Number of SPARQL queries performed: "
						+ this.bs.getNumberSPARQLQueries());
				logger.log("Number of SPARQL queries failed: "
						+ this.bs.getNumberSPARQLFailedQueries());
			}
			logger.log("Number of SPOs processed: "
					+ this.bs.getNumberSPOsProcessed());
			logger.log("Duration of this process: "
					+ StaticUtils.convertSecondsToTime(new BigDecimal(
							((tGetSPOsEnd - tGetSPOsStart) / 1000))));
		}
	}

	/**
	 * Method to check if all the endpoints were processed in the separate
	 * threads.
	 * 
	 * @return Return if all were processed.
	 */
	private boolean allEndpointsWhereProcessed() {
		for (int i = 0; i < this.endpoints.size(); i++) {
			if (!this.endpoints.get(i).getSPOsRetrieved()) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Method to join all the dump files.
	 * 
	 * @throws Exception
	 *             It can throw an exception.
	 */
	private void joinDumps() throws Exception {
		logger.log("Joining dumps..");
		if (dumpFile == null) {
			logger.log("No dump file has been set. Using file by default.");
			dumpFile = new File(
					ConfigManager.getConfig(Constants.JOINED_DUMP_FILE));
		} else {
			logger.log("Dump file has been set.");
		}
		LinkedList<SPO> spos = new LinkedList<SPO>();
		String dumpFile = ConfigManager.getConfig(Constants.JOINED_DUMP_FILE);
		this.dumpFile = new File(dumpFile);
		File dumpFolder = new File(
				ConfigManager.getConfig(Constants.DUMPS_FOLDER));
		for (int i = 0; i < dumpFolder.listFiles().length; i++) {
			loadSPOsFromFile(dumpFolder.listFiles()[i], spos);
		}
		BufferedWriter bW = new BufferedWriter(new FileWriter(this.dumpFile));
		for (int i = 0; i < spos.size(); i++) {
			SPO spo = spos.get(i);
			bW.write(spo.getEndpoint() + "\t" + spo.getSubject() + "\t"
					+ spo.getPredicate() + "\t" + spo.getObject());
			bW.newLine();
		}
		bW.close();
		logger.log("Dump joiner process finished!");
	}

	/**
	 * Method to load the SPOs from an external file.
	 * 
	 * @param file
	 *            File to get the data.
	 * @param spos
	 *            List to insert the readed SPOs
	 * @throws Exception
	 *             It can throw an exception
	 */
	private void loadSPOsFromFile(File file, LinkedList<SPO> spos)
			throws Exception {
		logger.log("Processing file: " + file.getAbsoluteFile().getName());
		BufferedReader bL = new BufferedReader(new FileReader(file));
		int nspos = 0;
		while (bL.ready()) {
			String readed = bL.readLine();
			String parts[] = readed.split("\t");
			if (parts.length == 4) {
				SPO spo = new SPO(parts[0], parts[1], parts[2], parts[3]);
				if (!spos.contains(spo)) {
					nspos++;
					spos.add(spo);
				}
			} else {
				if (!readed.equalsIgnoreCase(Constants.NO_SPOS)) {
					logger.logError("Invalid line: " + readed);
				}
			}
		}
		bL.close();
		logger.log(nspos + " SPOs loaded!");
	}

	/**
	 * Method to delete empty dump files.
	 * 
	 * @throws Exception
	 *             It can throw an exception
	 */
	private LinkedList<String> deleteEmptyDumpsAndGetAlreadyLoadedEndpoints()
			throws Exception {
		LinkedList<String> loadedEndpoints = new LinkedList<String>();
		File dumpsDir = new File(
				ConfigManager.getConfig(Constants.DUMPS_FOLDER));
		File files[] = dumpsDir.listFiles();
		for (int i = 0; i < files.length; i++) {
			loadedEndpoints.add(StaticUtils.removeExtension(files[i]
					.getAbsoluteFile().getName()));
			logger.log("Dump file with data "
					+ files[i].getAbsoluteFile().getName()
					+ " has been added to skip list.");
		}
		return loadedEndpoints;
	}

	/**
	 * Method to delete the dumps.
	 * 
	 * @throws Exception
	 *             It can throw an exception.
	 */
	private void deleteDumps() throws Exception {
		File dumpsDir = new File(
				ConfigManager.getConfig(Constants.DUMPS_FOLDER));
		File files[] = dumpsDir.listFiles();
		for (int i = 0; i < files.length; i++) {
			files[i].delete();
		}
	}

	/**
	 * Method to get the SPOs from endpoints SPARQL queries.
	 * 
	 * @throws Exception
	 *             It can throw an exception.
	 */
	private void retrieveDataFromEndpoints() throws Exception {
		logger.log("Querying endpoints...");
		for (int i = 0; i < endpoints.size(); i++) {
			/*
			 * For each endpoint, we query it's sparql endpoint and get the SPO
			 * patterns.
			 */
			SPARQLQueryEngine sqe = new SPARQLQueryEngine(this.logger, this.bs,
					this.endpoints);
			sqe.setSPOQuery(endpoints.get(i));
			sqe.setLogic(this);
			sqe.start();
		}
		logger.log("Endpoints query process finished!");
	}

	/**
	 * Method to create the ontologies.
	 * 
	 * @throws Exception
	 *             It can throw an exception.
	 */
	private void createOntologies() throws Exception {
		logger.log("Creating ontologies, sparql and configuration files ...");
		long createOntologiesStart = System.currentTimeMillis();
		/*
		 * For each SPO endpoint we create the SPO patterns as files.
		 */
		for (int i = 0; i < endpoints.size(); i++) {

			if (!StaticUtils.isEmpty(this.endpoints.get(i).getGraphEndpoint())) {
				logger.log("Creating data for endpoint: "
						+ endpoints.get(i).getName());
				OntologyCreation oc = new OntologyCreation(
						this.endpoints.get(i), this.bs, this.logger);
				oc.run();
			} else {
				logger.log("Couldn't create ontologies for endpoint "
						+ this.endpoints.get(i).getName()
						+ ". Graph not available.");
			}
		}
		long createOntologiesEnd = System.currentTimeMillis();
		logger.log("Ontologies creation process finished!");
		logger.log("Number of ontologies created: "
				+ this.bs.getNumberOntologiesCreated());
		logger.log("Number of sparql query files created: "
				+ this.bs.getNumberSPARQLFilesCreated());
		logger.log("Number of config files created: "
				+ this.bs.getNumberConfigFilesCreated());
		logger.log("Number of SPOs with datatype objects: "
				+ this.bs.getNumberSPOsWithDatatypeObjects());
		logger.log("Number of SPOs with object types: "
				+ this.bs.getNumberSPOsWithObjectTypes());
		logger.log("Number of inverse services created: "
				+ this.bs.getNumberOfInverseServices());
		logger.log("Number of resources with remote local name: "
				+ this.bs.getNumberResourcesWithRemoteLocalName());
		logger.log("Number of resources without remote local name: "
				+ this.bs.getNumberResourcesWithoutRemoteLocalName());
		logger.log("Number of resources with label: "
				+ this.bs.getNumberResourcesWithLabel());
		logger.log("Number of resources without label: "
				+ this.bs.getNumberResourcesWithoutLabel());
		logger.log("Number of resources with local local name: "
				+ this.bs.getNumberResourcesWithLocalLocalName());
		logger.log("Number of resources without local local name: "
				+ this.bs.getNumberResourcesWithoutLocalLocalName());
		logger.log("Duration of this process: "
				+ ((createOntologiesEnd - createOntologiesStart) / 1000) + " s");
	}

	/**
	 * Method to delete previous ontologies.
	 * 
	 * @throws Exception
	 *             It can throws an exception.
	 */
	private void deletePreviousOntologies() throws Exception {
		logger.log("Deleting previous ontologies... ");
		boolean del = Boolean.parseBoolean(ConfigManager
				.getConfig(Constants.DELETE_ONTOLOGIES_ALREADY_CREATED));
		if (del) {
			deleteAllFiles("ontologies/");
		}
		logger.log("Done!");
	}

	/**
	 * Method to delete all files from a current directory.
	 * 
	 * @param w
	 *            Receives the folder.
	 */
	private void deleteAllFiles(String w) {
		File dirs[] = new File(w).listFiles();
		for (int i = 0; i < dirs.length; i++) {
			File files[] = new File(dirs[i].toString()).listFiles();
			if (files != null && files.length > 0) {
				for (int j = 0; j < files.length; j++) {
					files[j].delete();
				}
			}
			dirs[i].delete();
		}
	}

	/**
	 * Method to load the endpoints from the endpoints files.
	 * 
	 * @param loadedEndpoints
	 *            Receive the already loaded endpoints (if it is from scratch,
	 *            this value will be empty).
	 * @throws Exception
	 */
	private void loadEndpoints(LinkedList<String> loadedEndpoints)
			throws Exception {
		this.endpoints = new LinkedList<Endpoint>();
		File folder = new File("endpoints/");
		File[] files = folder.listFiles();
		for (int i = 0; i < files.length; i++) {
			String endpointFile = files[i].getAbsoluteFile().getName();
			if (!loadedEndpoints.contains(StaticUtils
					.removeExtension(endpointFile))) {
				loadEndpoint(files[i]);
			} else {
				logger.log("Skipping endpoint from endpoint file "
						+ endpointFile
						+ ". We already have SPOs from this endpoint!");
			}
		}
	}

	/**
	 * Method to load the endpoint from the file.
	 * 
	 * @param f
	 *            Receives the file.
	 * @throws Exception
	 *             It can throw an exception.
	 */
	private void loadEndpoint(File f) throws Exception {
		logger.log("Loading endpoint..");
		Properties prop = new Properties();
		prop.load(new FileInputStream(f));
		String name = prop.getProperty(Constants.NAME);
		logger.log(".. Name: " + name);
		if (isSkippedEndpoint(name)) {
			logger.log("This endpoint should be skipped according to the configuration. Skipping!");
			return;
		}
		String ep = prop.getProperty(Constants.ENDPOINT);
		logger.log(".. URL: " + ep);
		String resource_object = prop.getProperty(Constants.RESOURCE_OBJECT);
		if (!StaticUtils.isEmpty(resource_object)) {
			logger.log(".. Resource object: " + resource_object);
		} else {
			logger.log(".. Resource object not found!");
		}
		String dataset = "";
		if (loadGraphDatasetFromEndpoint) {
			dataset = getGraphEndpoint(ep, name);
		} else {
			dataset = "http://bio2rdf.org/bio2rdf-statistics-" + name;
		}

		logger.log(".. Dataset Graph: "
				+ ((dataset == null) ? "Not found!" : dataset));
		if (!StaticUtils.isEmpty(dataset)) {
			this.endpoints
					.add(new Endpoint(ep, name, dataset, resource_object));
			logger.log("Endpoint succesfully added!");
		} else {
			logger.logError("Endpoint skipped. Dataset not found.");
		}
	}

	/**
	 * Method to get the dataset graph of a given endpoint.
	 * 
	 * @param ep
	 *            It receives the URL of the endpoint.
	 * @param name
	 *            It receives the name of the endpoint.
	 * @return It returns the value.
	 * @throws Exception
	 *             It can throw an exception.
	 */
	private String getGraphEndpoint(String ep, String name) throws Exception {
		SPARQLQueryEngine sqe = new SPARQLQueryEngine(this.logger, this.bs,
				this.endpoints);
		String dt;
		try {
			dt = sqe.getMostUpdatedGraph(ep, name);
		} catch (Exception e) {
			logger.logError("Error obtaining graph to query endpoint " + ep);
			throw new Exception(e);
		}
		return dt;
	}

	/**
	 * Method to know if an endpoint should be skipped.
	 * 
	 * @param name
	 *            Receives the endpoint name.
	 * @return Returns a boolean.
	 */
	private boolean isSkippedEndpoint(String name) {
		for (int i = 0; i < this.endpointsToSkip.size(); i++) {
			if (this.endpointsToSkip.get(i).equalsIgnoreCase(name)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Method to set the dump file.
	 * 
	 * @param fileDump
	 *            The value.
	 */
	public void setDumpFile(String fileDump) {
		this.dumpFile = new File(fileDump);
	}

}
