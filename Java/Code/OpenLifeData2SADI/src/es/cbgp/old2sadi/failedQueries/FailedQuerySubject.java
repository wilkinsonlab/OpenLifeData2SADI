package es.cbgp.old2sadi.failedQueries;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Properties;

import org.apache.commons.codec.digest.DigestUtils;

import es.cbgp.old2sadi.main.ConfigManager;
import es.cbgp.old2sadi.main.Constants;
import es.cbgp.old2sadi.main.MyLogger;
import es.cbgp.old2sadi.objects.Endpoint;

public class FailedQuerySubject extends FailedQuery {

	public FailedQuerySubject(File ffq, String q, Endpoint e, int t, long s, MyLogger l, String err) {
		super(ffq, q, e, t, s, l, err);
	}
	public FailedQuerySubject(String q, Endpoint e, int t, long s, MyLogger l, String err) {
		super(q, e, t, s, l, err);
	}
	public void save() throws Exception {
		String foldfq = ConfigManager.getConfig(Constants.FAILED_QUERIES_FOLDER);
		File fileToSave = new File(foldfq + getHash() + ".fq");
		logger.log("[" + this.getEndpoint().getName() + "] [Failed Query: " + this.getStringType() + "]. Saving failed query in file " + fileToSave.getAbsoluteFile().getName());
		Properties prop = new Properties();
		prop.setProperty(Constants.FAILED_QUERY_ERROR, this.getError());
		prop.setProperty(Constants.FAILED_QUERY_QUERY, this.getQuery());
		prop.setProperty(Constants.FAILED_QUERY_KEY_TYPE, Integer.toString(this.getType()));
		saveEndpoint(this.getEndpoint(), prop);
		prop.store(new FileOutputStream(fileToSave), "");
	}

	public String getHash() {
		String sHash = this.getEndpoint().getName() + this.getType() + this.getSeed();
		return DigestUtils.md5Hex(sHash);
	}

}
