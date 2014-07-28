package es.cbgp.old2sadi.objects;

public class ResourceName {

	private String name;
	private String error;
	private boolean hasErrors;
	private String URI;

	public ResourceName() {
		this.name = null;
		this.error = "";
		this.URI = null;
	}

	public void setName(String n) {
		this.name = n;
	}

	public String getName() {
		return this.name;
	}

	public boolean hasErrors() {
		return this.hasErrors;
	}
	
	public void addError(String m) {
		this.hasErrors = true;
		this.error += m + "\n";
	}

	public String getURI() {
		return this.URI;
	}
	public void setURI(String u) {
		this.URI = u;
	}
	public String getError() {
		String ret = "";
		ret += "Errors about resource '" + URI + "'\n";
		ret += this.error;
		return ret;
	}

}
