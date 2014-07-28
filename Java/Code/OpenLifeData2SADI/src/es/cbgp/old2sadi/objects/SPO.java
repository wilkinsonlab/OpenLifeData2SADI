package es.cbgp.old2sadi.objects;

import java.io.Serializable;

/**
 * SPO
 * 
 * @author Alejandro Rodríguez González - Centre for Biotechnology and Plant
 *         Genomics
 * 
 */
public class SPO implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1612076094039066142L;
	
	private String subject;
	private String predicate;
	private String object;
	private String endpoint;

	private String objectType;

	public SPO(String ep, String s, String p, String o) {
		this.subject = s;
		this.predicate = p;
		this.object = o;
		this.endpoint = ep;
	}

	public SPO(String s, String p, String o) {
		this.subject = s;
		this.predicate = p;
		this.object = o;
	}
	public String getEndpoint() {
		return endpoint;
	}

	public void setEndpoint(String endpoint) {
		this.endpoint = endpoint;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public String getPredicate() {
		return predicate;
	}

	public void setPredicate(String predicate) {
		this.predicate = predicate;
	}

	public String getObject() {
		return object;
	}

	public void setObject(String object) {
		this.object = object;
	}

	public boolean equals(Object o) {
		if (o instanceof SPO) {
			SPO ob = (SPO)o;
			return ob.toString().equalsIgnoreCase(this.toString());
		}
		return false;
	}
	
	public String toString() {
		return "[ " + this.subject + ", " + this.predicate + ", " + this.object
				+ " ]";
	}

	public void setObjectType(String t) {
		this.objectType = t;
	}

	public String getObjectType() {
		return objectType;
	}
}
