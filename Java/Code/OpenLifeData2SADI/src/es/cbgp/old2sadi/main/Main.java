package es.cbgp.old2sadi.main;

import java.io.File;

/**
 * Main class.
 * 
 * @author Alejandro Rodríguez González - Centre for Biotechnology and Plant
 *         Genomics
 * 
 */
public class Main {

	/**
	 * Constructor.
	 * @param args Arguments.
	 * @throws Exception It can throw an exception
	 */
	public Main(String args[]) throws Exception {
		switch (args.length) {
		case 1:
			executeOLD2SADI(args);
			break;
		case 2:
			executeOLD2SADI(args);
			break;
		case 3:
			executeOLD2SADI(args);
			break;
		default:
			showErrorMessage();
			break;
		}

	}

	/**
	 * Method to start OLD2SADI.
	 * @param args Receives the arguments.
	 * @throws Exception It can throw an exception
	 */
	private void executeOLD2SADI(String[] args) throws Exception {
		if (args[0].equalsIgnoreCase(Constants.FILE_PARAMETER)) {
			executeOLD2SADIFromFile(args);
			return;
		}
		if (args[0].equalsIgnoreCase(Constants.OLD_PARAMETER)) {
			executeOLD2SADIFromOLD(args);
			return;
		} 
		if (args[0].equalsIgnoreCase(Constants.FAILED_QUERIES_PARAMETER)) {
			executeOLD2SADIFailedQueries();
			return;
		}
		else {
			showErrorMessage();
		}

	}



	/**
	 * Method to show the error message.
	 */
	private void showErrorMessage() {
		System.err.println("Error. Use: ");
		System.err
				.println("\t-f <filename> to load SPO triples from an external file");
		System.err
		.println("\t-fq to try to re-execute failed queries and add it to their dumps.");
		System.err.println("\t-o <-s,-c> <file name: optional> to load SPO triples from OpenLifeData.");
		System.err.println("\tFile name is an optional parameter to indicate the file where the SPOs will be stored.");
		System.err
				.println("\tIf you use '-s' as second parameter, it will start the process from scratch.");
		System.err
				.println("\tIf you use '-c' as second parameter, it will try to continue the process avoiding already loaded endpoints.");
	}

	/**
	 * Method to execute OLD2SADI using an external SPO file.
	 * 
	 * @param args
	 *            Receive the arguments.
	 */
	private void executeOLD2SADIFromFile(String args[]) throws Exception {
		File f = new File(args[1]);
		if (!f.exists()) {
			System.err.println("File " + args[1] + " doesn't exist.");
			System.err.println("Exiting ...");
		} else {
			Logic lo = new Logic(f);
			lo.executeOLD2SADIFromFile();
			return;
		}
	}

	/**
	 * Method to execute OLD2SADI from OLD endpoints.
	 * @param args It receive the arguments.
	 * @throws Exception It can throw an exception.
	 */
	private void executeOLD2SADIFromOLD(String args[]) throws Exception {
		if (args[1].equalsIgnoreCase(Constants.OLD_PARAMETER_SCRATCH) || args[1].equalsIgnoreCase(Constants.OLD_PARAMETER_CONTINUE)) {
			Logic lo = new Logic(args[1]);
			if (args.length == 3) {
				String fileDump = args[2];
				lo.setDumpFile(fileDump);
			}
			lo.executeOLD2SADIFromOLD();
		}
		else {
			showErrorMessage();
		}
	}

	private void executeOLD2SADIFailedQueries() throws Exception {
		Logic lo = new Logic();
		lo.executeOLD2SADIFailedQueries();
		
	}
	/**
	 * Main method.
	 * @param args Arguments.
	 */
	public static void main(String[] args) {
		try {
			new Main(args);
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Error executing OLD2SADI: " + e.getMessage());
		}
	}
}
