package es.cbgp.old2sadi.objects;

import java.io.Serializable;
import java.util.LinkedList;

/**
 * Endpoint class
 * @author Alejandro Rodríguez González - Centre for Biotechnology and Plant Genomics
 *
 */
public class Endpoint implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6842448802856445606L;
	private String epURL;
	private String name;
	private String dataset;
	private String resourceObject;
	private LinkedList<SPO> spos;
	private boolean SPOsRetrieved;
	private String graphEndpoint;

	public Endpoint(String u, String n, String d, String ro) {
		this.epURL = u;
		this.name = n;
		this.dataset = d;
		this.resourceObject = ro;
		this.spos = new LinkedList<SPO>();
	}
	public Endpoint(String n) {
		this.name = n;
		this.spos = new LinkedList<SPO>();
	}

	public String getResourceObject() {
		return this.resourceObject;
	}
	
	public void setResourceObject(String ro) {
		this.resourceObject = ro;
	}
	public LinkedList<SPO> getSPOs() {
		return this.spos;
	}

	public void setEndpointURL(String u) {
		this.epURL = u;
	}

	public String getEndpointURL() {
		return this.epURL;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String n) {
		this.name = n;
	}

	public String getDataset() {
		return this.dataset;
	}

	public void setDataset(String d) {
		this.dataset = d;
	}

	public void addSPOs(LinkedList<SPO> s) {
		this.spos = s;

	}
	public void addSPO(SPO spo) {
		this.spos.add(spo);
	}
	public boolean contains(SPO spo) {
		return this.spos.contains(spo);
	}
	public void setSPOsRetrieved(boolean b) {
		this.SPOsRetrieved = b;
	}
	
	public boolean getSPOsRetrieved() {
		return this.SPOsRetrieved;
	}
	/**
	 * Method to set the graph endpoint.
	 * @param graphEndpoint Receives the value.
	 */
	public void setGraphEndpoint(String ge) {
		this.graphEndpoint = ge;
	}
	public String getGraphEndpoint() {
		return this.graphEndpoint;
			
	}
	/**
	 * Method to reset the spos of the endpoint.
	 */
	public void resetSPOs() {
		this.spos = new LinkedList<SPO>();
		
	}
}
