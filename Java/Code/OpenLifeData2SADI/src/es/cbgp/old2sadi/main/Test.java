package es.cbgp.old2sadi.main;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.sparql.engine.http.QueryEngineHTTP;


public class Test {

	public Test() {
		run();
	}
	private void run() {
			String finalQuery = getQuery();
			String serviceEndpoint = "http://s4.semanticscience.org:14001/sparql";	
			Query query = null;
			QueryEngineHTTP qexec = null;
			
			try {
				System.out.println(finalQuery);
				query = QueryFactory.create(finalQuery);
				qexec = QueryExecutionFactory.createServiceRequest(serviceEndpoint, query);
				qexec.setSelectContentType("text/csv");
				ResultSet results = qexec.execSelect();
				while (results.hasNext()) {
					QuerySolution qs = results.next();
					System.out.println(qs.toString());
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if (qexec != null) {
					qexec.close();
				}
			}
		}
	

	private String getQuery() {
		String q = "";
		q += "SELECT distinct (datatype(?o) as ?dt)\n";
		q += "FROM <http://bio2rdf.org/bio2rdf.dataset:bio2rdf-affymetrix-20131220>\n";
		q += "WHERE {\n";
		q += "?s a <http://bio2rdf.org/affymetrix_vocabulary:Probeset> .\n";
		q += "?s <http://purl.org/dc/terms/identifier> ?o .\n";
		q += "FILTER isLiteral(?o)\n";
		q += "}\n";
		q += "group by ?o\n";
		return q;
	}

	public static void main(String args[]) {
		new Test();
	}
}
