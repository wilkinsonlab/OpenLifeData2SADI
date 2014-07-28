package es.cbgp.old2sadi.failedQueries;


import java.io.File;
import java.io.FileOutputStream;
import java.util.Properties;

import org.apache.commons.codec.digest.DigestUtils;

import com.hp.hpl.jena.rdf.model.Resource;

import es.cbgp.old2sadi.main.ConfigManager;
import es.cbgp.old2sadi.main.Constants;
import es.cbgp.old2sadi.main.MyLogger;
import es.cbgp.old2sadi.objects.Endpoint;

public class FailedQuerySpecificObjectObjectType extends FailedQuery {

	private Resource associatedObject;
	private Resource associatedPredicate;
	private Resource associatedSubject;
	private String sourceEndpoint;
	private String destinyEndpoint;

	
	public FailedQuerySpecificObjectObjectType(File ffq, String q, Endpoint e, int t, long se, MyLogger l, String err, String srcEp, String dstEp, Resource s, Resource p, Resource o) {
		super(ffq, q, e, t, se, l, err);
		this.associatedSubject = s;
		this.associatedPredicate = p;
		this.associatedObject = o;
		this.sourceEndpoint = srcEp;
		this.destinyEndpoint = dstEp;
	}
	
	public FailedQuerySpecificObjectObjectType(String q, Endpoint e, int t, long se, MyLogger l, String err, String srcEp, String dstEp, Resource s, Resource p, Resource o) {
		super(q, e, t, se, l, err);
		this.associatedSubject = s;
		this.associatedPredicate = p;
		this.associatedObject = o;
		this.sourceEndpoint = srcEp;
		this.destinyEndpoint = dstEp;
	}
	public Resource getAssociatedPredicate() {
		return associatedPredicate;
	}

	public Resource getAssociatedSubject() {
		return associatedSubject;
	}

	public String getSourceEndpoint() {
		return sourceEndpoint;
	}

	public String getDestinyEndpoint() {
		return destinyEndpoint;
	}

	public Resource getAssociatedObject() {
		return associatedObject;
	}
	
	public void save() throws Exception {
		String foldfq = ConfigManager.getConfig(Constants.FAILED_QUERIES_FOLDER);
		File fileToSave = new File(foldfq + getHash() + ".fq");
		Properties prop = new Properties();
		prop.setProperty(Constants.FAILED_QUERY_ERROR, this.getError());
		prop.setProperty(Constants.FAILED_QUERY_QUERY, this.getQuery());
		prop.setProperty(Constants.FAILED_QUERY_KEY_TYPE, Integer.toString(this.getType()));
		prop.setProperty(Constants.FAILED_QUERY_ASSOCIATED_SUBJECT, this.getAssociatedSubject().toString());
		prop.setProperty(Constants.FAILED_QUERY_ASSOCIATED_PREDICATE, this.getAssociatedPredicate().toString());
		prop.setProperty(Constants.FAILED_QUERY_ASSOCIATED_OBJECT, this.getAssociatedObject().toString());
		prop.setProperty(Constants.FAILED_QUERY_SOURCE_ENDPOINT, this.getSourceEndpoint());
		prop.setProperty(Constants.FAILED_QUERY_DESTINY_ENDPOINT, this.getDestinyEndpoint());
		saveEndpoint(this.getEndpoint(), prop);
		prop.store(new FileOutputStream(fileToSave), "");
	}

	public String getHash() {
		String sHash = this.getEndpoint().getName() + this.getAssociatedSubject().toString() + this.getAssociatedPredicate().toString() + this.getAssociatedObject().toString() + this.getSourceEndpoint() + this.getDestinyEndpoint() + this.getType() + this.getSeed();
		return DigestUtils.md5Hex(sHash);
	}

}
