package es.cbgp.old2sadi.failedQueries;

import java.io.File;
import java.util.Properties;

import es.cbgp.old2sadi.main.Constants;
import es.cbgp.old2sadi.main.MyLogger;
import es.cbgp.old2sadi.objects.Endpoint;

public abstract class FailedQuery {

	private Endpoint endpoint;
	protected MyLogger logger;
	/*
	 * Type defines the type of element to retrieve:
	 * 
	 * 0. Subject(s)
	 * 1. Predicate(s)
	 * 2. Object(s) - Data type
	 * 3. Object(s) - Object type
	 * 4. Specific Object(s) - Object type
	 */
	private int type;
	private long seed;
	private String error;
	private String query;
	private File fileFailedQuery;
	
	public FailedQuery(File ffq, String q, Endpoint e, int t, long seed, MyLogger l, String err) {
		this.fileFailedQuery = ffq;
		this.endpoint = e;
		this.type = t;
		this.seed = seed;
		this.logger = l;
		this.error = err;
		this.query = q;
	}
	
	public FailedQuery(String q, Endpoint e, int t, long seed, MyLogger l, String err) {
		this.endpoint = e;
		this.type = t;
		this.seed = seed;
		this.logger = l;
		this.error = err;
		this.query = q;
	}
	
	public FailedQuery(MyLogger l) {
		this.logger = l;
	}

	public File getFileFailedQuery() {
		return this.fileFailedQuery;
	}
	public String getError() {
		return this.error;
	}
	public Endpoint getEndpoint() {
		return endpoint;
	}

	public void setEndpoint(Endpoint endpoint) {
		this.endpoint = endpoint;
	}
	public int getType() {
		return type;
	}

	public long getSeed() {
		return this.seed;
	}
	
	public String getQuery() {
		return this.query;
	}
	public abstract String getHash();
	public abstract void save() throws Exception;

	public String getStringType() {
		switch (this.type) {
		case Constants.QUERY_SUBJECT:
			return "Subject";
		case Constants.QUERY_PREDICATE:
			return "Predicate";
		case Constants.QUERY_OBJECT_DATA_TYPE:
			return "Object data type";
		case Constants.QUERY_OBJECT_OBJECT_TYPE:
			return "Object object type";
		case Constants.QUERY_OBJECT_SPECIFIC_OBJECT_TYPE:
			return "Specific object type";
		}
		return "Not available";
	}

	protected void saveEndpoint(Endpoint endpoint, Properties prop) {
		prop.setProperty(Constants.FAILED_QUERY_ENDPOINT_NAME, endpoint.getName());
		prop.setProperty(Constants.FAILED_QUERY_ENDPOINT_DATASET, endpoint.getDataset());
		prop.setProperty(Constants.FAILED_QUERY_ENDPOINT_URL, endpoint.getEndpointURL());
		prop.setProperty(Constants.FAILED_QUERY_ENDPOINT_GRAPH, endpoint.getGraphEndpoint());
		prop.setProperty(Constants.FAILED_QUERY_ENDPOINT_RESOURCE_OBJECT, endpoint.getResourceObject());
	}
}
