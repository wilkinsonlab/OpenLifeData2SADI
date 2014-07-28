package es.cbgp.old2sadi.main;

import java.io.BufferedWriter;
import java.io.FileWriter;

public class MyLogger {

	private BufferedWriter buffer;
	private final int NUM_LINES_CLOSE_AND_OPEN = 500;
	private int numLogs = 0;
	private boolean loggerEnabled;

	public MyLogger() throws Exception {
		this.buffer = new BufferedWriter(new FileWriter(
				ConfigManager.getConfig(Constants.LOG_FILE), false));
	}

	public void log(String s) {
		if (this.loggerEnabled) {
			System.out.println(s);
			try {
				this.buffer.write(s);
				this.buffer.newLine();
				this.buffer.flush();
				this.numLogs++;
				// this.closeAndOpen();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public void logError(String s) {
		if (this.loggerEnabled) {
			System.err.println(s);
			try {
				this.buffer.write(s);
				this.buffer.newLine();
				this.buffer.flush();
				this.numLogs++;
				//this.closeAndOpen();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@SuppressWarnings("unused")
	private void closeAndOpen() throws Exception {
		if (this.numLogs > NUM_LINES_CLOSE_AND_OPEN) {
			this.buffer.close();
			this.buffer = new BufferedWriter(new FileWriter(
					ConfigManager.getConfig(Constants.LOG_FILE), true));
			this.numLogs = 0;
		}

	}

	public void close() {
		try {
			this.buffer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void setEnabled(boolean le) {
		this.loggerEnabled = le;
	}

}
