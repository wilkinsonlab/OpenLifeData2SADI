package es.cbgp.old2sadi.objects;

public class Filter {

	private String filterField;
	
	public String getFilterField() {
		return filterField;
	}

	public void setFilterField(String filterField) {
		this.filterField = filterField;
	}

	public Filter(String f) {
		this.filterField = f;
	}
}
