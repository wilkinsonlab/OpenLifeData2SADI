package es.cbgp.old2sadi.failedQueries;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Properties;

import org.apache.commons.codec.digest.DigestUtils;

import com.hp.hpl.jena.rdf.model.Resource;

import es.cbgp.old2sadi.main.ConfigManager;
import es.cbgp.old2sadi.main.Constants;
import es.cbgp.old2sadi.main.MyLogger;
import es.cbgp.old2sadi.objects.Endpoint;

public class FailedQueryPredicates extends FailedQuery {

	private Resource associatedSubject;
	private ArrayList<Resource> associatedListOfSubjects;
	public FailedQueryPredicates(File ffq, String q, Endpoint e, int t, long s, MyLogger l, String err, Resource as, ArrayList<Resource> asls) {
		super(ffq, q, e, t, s, l, err);
		this.associatedSubject = as;
		this.associatedListOfSubjects = asls;
	}
	public FailedQueryPredicates(String q, Endpoint e, int t, long s, MyLogger l, String err, Resource as, ArrayList<Resource> asls) {
		super(q, e, t, s, l, err);
		this.associatedSubject = as;
		this.associatedListOfSubjects = asls;
	}
	public Resource getAssociatedSubject() {
		return this.associatedSubject;
	}
	
	public ArrayList<Resource> getAssociatedListOfSubjects() {
		return this.associatedListOfSubjects;
	}

	public void save() throws Exception {
		String foldfq = ConfigManager.getConfig(Constants.FAILED_QUERIES_FOLDER);
		File fileToSave = new File(foldfq + getHash() + ".fq");
		Properties prop = new Properties();
		prop.setProperty(Constants.FAILED_QUERY_ERROR, this.getError());
		prop.setProperty(Constants.FAILED_QUERY_QUERY, this.getQuery());
		prop.setProperty(Constants.FAILED_QUERY_KEY_TYPE, Integer.toString(this.getType()));
		prop.setProperty(Constants.FAILED_QUERY_ASSOCIATED_SUBJECT, this.getAssociatedSubject().toString());
		prop.setProperty(Constants.FAILED_QUERY_ASSOCIATED_SUBJECTS, getListOfSubjectsToSave());
		saveEndpoint(this.getEndpoint(), prop);
		prop.store(new FileOutputStream(fileToSave), "");
	}

	private String getListOfSubjectsToSave() {
		String v = "";
		for (int i = 0; i < this.associatedListOfSubjects.size(); i++) {
			v += this.associatedListOfSubjects.get(i).toString() + "@";
		}
		return v.substring(0, v.length() - 1);
	}

	public String getHash() {
		String sHash = this.getEndpoint().getName() + this.getAssociatedSubject().toString() + this.getAssociatedListOfSubjects().toString() + this.getType() + this.getSeed();
		return DigestUtils.md5Hex(sHash);
	}
}
