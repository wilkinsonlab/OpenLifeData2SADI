package es.cbgp.old2sadi.objects;

public class OLD2SADIStatistics {

	private int numberSPOsProcessed;
	private int numberOntologiesCreated;
	private int numberConfigFilesCreated;
	private int numberSPARQLFilesCreated;
	private int numberSPOsWithDatatypeObjects;
	private int numberSPOsWithObjectTypes;
	private int numberResourcesWithRemoteLocalName;
	private int numberResourcesWithoutRemoteLocalName;
	private int numberResourcesWithLabel;
	private int numberResourcesWithoutLabel;
	private int numberResourcesWithLocalLocalName;
	private int numberResourcesWithoutLocalLocalName;
	private int numberSPARQLQueries;
	private int numberOfInverseServices;
	private int numberSPARQLFailedQueries;

	public int getNumberResourcesWithRemoteLocalName() {
		return numberResourcesWithRemoteLocalName;
	}

	public int getNumberResourcesWithoutRemoteLocalName() {
		return numberResourcesWithoutRemoteLocalName;
	}

	public int getNumberResourcesWithLabel() {
		return numberResourcesWithLabel;
	}

	public int getNumberResourcesWithoutLabel() {
		return numberResourcesWithoutLabel;
	}

	public int getNumberResourcesWithLocalLocalName() {
		return numberResourcesWithLocalLocalName;
	}

	public int getNumberResourcesWithoutLocalLocalName() {
		return numberResourcesWithoutLocalLocalName;
	}

	public int getNumberSPARQLQueries() {
		return numberSPARQLQueries;
	}

	public OLD2SADIStatistics() {
		numberSPOsProcessed = 0;
		numberOntologiesCreated = 0;
		numberConfigFilesCreated = 0;
		numberSPARQLFilesCreated = 0;
		numberSPOsWithDatatypeObjects = 0;
		numberSPOsWithObjectTypes = 0;
		numberResourcesWithRemoteLocalName = 0;
		numberResourcesWithoutRemoteLocalName = 0;
		numberResourcesWithLabel = 0;
		numberResourcesWithoutLabel = 0;
		numberResourcesWithLocalLocalName = 0;
		numberResourcesWithoutLocalLocalName = 0;
		numberSPARQLQueries = 0;
		numberOfInverseServices = 0;
		numberSPARQLFailedQueries = 0;
	}

	public void incNumberSPOsProcessed() {
		numberSPOsProcessed++;
	}

	public void incNumberOntologiesCreated() {
		numberOntologiesCreated++;
	}

	public void incNumberConfigFilesCreated() {
		numberConfigFilesCreated++;
	}

	public void incNumberSPARQLFilesCreated() {
		numberSPARQLFilesCreated++;
	}

	public void incNumberSPOsWithDatatypeObjects() {
		numberSPOsWithDatatypeObjects++;
	}

	public void incNumberSPOsWithObjectTypes() {
		numberSPOsWithObjectTypes++;
	}

	public int getNumberSPOsProcessed() {
		return numberSPOsProcessed;
	}

	public int getNumberOntologiesCreated() {
		return numberOntologiesCreated;
	}

	public int getNumberConfigFilesCreated() {
		return numberConfigFilesCreated;
	}

	public int getNumberSPARQLFilesCreated() {
		return numberSPARQLFilesCreated;
	}

	public int getNumberSPOsWithDatatypeObjects() {
		return numberSPOsWithDatatypeObjects;
	}

	public int getNumberSPOsWithObjectTypes() {
		return numberSPOsWithObjectTypes;
	}

	public void incNumberResourcesWithRemoteLocalName() {
		numberResourcesWithRemoteLocalName++;
	}

	public void incNumberResourcesWithoutRemoteLocalName() {
		numberResourcesWithoutRemoteLocalName++;
	}

	public void incNumberResourcesWithLabel() {
		numberResourcesWithLabel++;
	}

	public void incNumberResourcesWithoutLabel() {
		numberResourcesWithoutLabel++;
	}

	public void incNumberResourcesWithLocalLocalName() {
		numberResourcesWithLocalLocalName++;
	}

	public void incNumberResourcesWithoutLocalLocalName() {
		numberResourcesWithoutLocalLocalName++;
	}

	public void incNumberSPARQLQueries() {
		numberSPARQLQueries++;

	}

	public int getNumberOfInverseServices() {
		return numberOfInverseServices;
	}

	public void incNumberOfInverseServices() {
		numberOfInverseServices++;
	}

	public int getNumberSPARQLFailedQueries() {
		return numberSPARQLFailedQueries;
	}

	public void incNumberSPARQLFailedQueries() {
		numberSPARQLFailedQueries++;
	}
}
