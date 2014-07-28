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

public class FailedQueryObjectObjectType extends FailedQuery {

	private Resource associatedSubject;
	private Resource associatedPredicate;

	public FailedQueryObjectObjectType(File ffq, String q, Endpoint e, int t, long s, MyLogger l, String err, 
			Resource as, Resource ap) {
		super(ffq, q, e, t, s, l, err);
		this.associatedSubject = as;
		this.associatedPredicate = ap;
	}
	public FailedQueryObjectObjectType(String q, Endpoint e, int t, long s, MyLogger l, String err, 
			Resource as, Resource ap) {
		super(q, e, t, s, l, err);
		this.associatedSubject = as;
		this.associatedPredicate = ap;
	}
	public Resource getAssociatedSubject() {
		return this.associatedSubject;
	}

	public Resource getAssociatedPredicate() {
		return this.associatedPredicate;
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
		saveEndpoint(this.getEndpoint(), prop);
		prop.store(new FileOutputStream(fileToSave), "");
	}

	public String getHash() {
		String sHash = this.getEndpoint().getName() + this.getAssociatedSubject().toString() + this.getAssociatedPredicate().toString() + this.getType() + this.getSeed() + "OT";
		return DigestUtils.md5Hex(sHash);
	}
}
