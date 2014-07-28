package es.cbgp.old2sadi.objects;

public class ResultAndMessage {

	private boolean result;
	private String message;
	
	public ResultAndMessage(boolean r, String m) {
		this.result = r;
		this.message = m;
	}
	
	public ResultAndMessage(boolean r) {
		this.result = r;
	}
	public String getMessage() {
		return this.message;
	}
	public boolean getResult() {
		return this.result;
	}
}
