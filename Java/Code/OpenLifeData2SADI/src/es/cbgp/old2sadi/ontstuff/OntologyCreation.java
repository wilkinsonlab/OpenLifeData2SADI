package es.cbgp.old2sadi.ontstuff;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.OWLDeclarationAxiom;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLProperty;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.vocabulary.RDFS;

import es.cbgp.old2sadi.main.ConfigManager;
import es.cbgp.old2sadi.main.Constants;
import es.cbgp.old2sadi.main.MyLogger;
import es.cbgp.old2sadi.main.StaticUtils;
import es.cbgp.old2sadi.objects.OLD2SADIStatistics;
import es.cbgp.old2sadi.objects.Endpoint;
import es.cbgp.old2sadi.objects.ResourceName;
import es.cbgp.old2sadi.objects.SPO;

/**
 * Class to create the ontology, configuration and sparql files.
 * 
 * @author Alejandro Rodríguez González - Centre for Biotechnology and Plant
 *         Genomics
 * 
 */
public class OntologyCreation {

	private Endpoint endpoint;
	private OntModel ontModel;
	private OWLOntologyManager manager;
	private OWLOntology ontology;
	private String defaultIRI;
	private String sadiServiceOutputClass;
	private BufferedWriter configFile;
	private OLD2SADIStatistics bs;
	private MyLogger logger;
	private String inverseSuffix;

	/**
	 * Constructor receives the endpoint to proces.
	 * 
	 * @param ep
	 *            Endpoint
	 * @param version
	 *            Version of the release executed.
	 * @throws Exception
	 *             It can throw an exception.
	 */
	public OntologyCreation(Endpoint ep, OLD2SADIStatistics bs, MyLogger logger)
			throws Exception {
		this.logger = logger;
		this.defaultIRI = ConfigManager.getConfig(Constants.DEFAULT_IRI_BASE)
				+ ep.getName() + "/";
		this.bs = bs;
		this.sadiServiceOutputClass = ConfigManager
				.getConfig(Constants.SADI_SERVICE_OUTPUT_CLASS);
		this.endpoint = ep;
		this.inverseSuffix = ConfigManager.getConfig(Constants.INVERSE_SUFFIX);
	}

	/**
	 * Run method extracts all the SPO patterns and create the files.
	 * 
	 * @throws Exception
	 *             It can throw an exception.
	 */
	public void run() throws Exception {
		for (int i = 0; i < this.endpoint.getSPOs().size(); i++) {
			SPO spo = this.endpoint.getSPOs().get(i);
			createOntology(spo);
		}
	}

	/**
	 * This method receives the SPO that is going to be processed.
	 * 
	 * @param spo
	 *            SPO object.
	 * @throws Exception
	 *             It can throw an exception.
	 */
	private void createOntology(SPO spo) throws Exception {
		logger.log("SPO: " + spo.toString());
		bs.incNumberSPOsProcessed();
		this.ontModel = ModelFactory.createOntologyModel();
		String s, p, o = null;
		s = getFileName(spo.getSubject());
		p = getFileName(spo.getPredicate());

		/**
		 * We can have two types of objects:
		 * 
		 * Datatypes or objects. Both are treated differently.
		 */

		if (spo.getObject().contains(Constants.XML_SCHEMA)) {
			spo.setObjectType(Constants.XML_SCHEMA);
			o = StaticUtils.getXMLSChemaType(spo.getObject());
			this.bs.incNumberSPOsWithDatatypeObjects();
		} else {
			this.bs.incNumberSPOsWithObjectTypes();
			spo.setObjectType(Constants.OBJECT);
			o = getFileName(spo.getObject());
		}

		/*
		 * We create the string to save files subject_predicate_object
		 */
		String saveBasicFile = s + "_" + p + "_" + o;
		String saveBasicFileInverse = o + "_" + (p + this.inverseSuffix) + "_"
				+ s;
		/*
		 * We check if already exists this file.
		 */
		boolean alreadyExists = checkSave(saveBasicFile);
		if (!alreadyExists) {
			createOntology(saveBasicFile, spo, s);
		} else {
			logger.logError("Error. File structure already exists (duplicated SPO pattern?): "
					+ saveBasicFile);
		}
		if (spo.getObjectType().equalsIgnoreCase(Constants.OBJECT)) {
			this.bs.incNumberOfInverseServices();
			/*
			 * And the same with the inverse, (when the object is not a data type).
			 */
			SPO spoInverse = new SPO(spo.getObject(), spo.getPredicate()
					+ this.inverseSuffix, spo.getSubject());
			s = getFileName(spoInverse.getSubject());
			spoInverse.setObjectType(spo.getObjectType());
			alreadyExists = checkSave(saveBasicFileInverse);
			if (!alreadyExists) {
				createOntology(saveBasicFileInverse, spoInverse, s);
			} else {
				logger.logError("Error. File structure already exists (duplicated SPO pattern?): "
						+ saveBasicFileInverse);
			}
		}
	}

	private void createOntology(String sbf, SPO spo, String s) throws Exception {
		/*
		 * If not exists..
		 */
		logger.log("Creating SPO pattern file: " + sbf);
		String saveNameOntology = sbf + ".owl";
		String saveNameSparqlQuery = sbf + ".sparql";
		String saveNameConfigFile = sbf + ".cfg";
		this.configFile = new BufferedWriter(new FileWriter("ontologies/"
				+ this.endpoint.getName() + "/" + saveNameConfigFile));
		/*
		 * By default, input class name is the value of 's', but in those cases
		 * where we have concatenated the vocabulary name with the class name
		 * because class name was composed by numbers (e.g:
		 * http://bio2rdf.org/psi-mi:0193), we had to process it again.
		 * 
		 * We could have right now: psi-mi_0193 (from
		 * http://bio2rdf.org/psi-mi:0193) We need: psi-mi:0193
		 * 
		 * We check if it is the case (checking if it contains "_"). If it
		 * contains _, we replace it with the :.
		 */
		String inputClassName = s;
		if (inputClassName.contains("_")) {
			inputClassName = inputClassName.replace('_', ':');
		}
		configFile.write(Constants.INPUTCLASS_NAME + Constants.EQUALS
				+ inputClassName);
		configFile.newLine();

		/*
		 * We create the files.
		 */
		createOntology(saveNameOntology, spo);
		createSPARQLQueryFile(saveNameSparqlQuery, spo);
		saveConfigFile(saveNameConfigFile, spo);
	}

	/**
	 * Method to get the file name.
	 * 
	 * @param uri
	 *            Receives the URI.
	 * @return Returns the value.
	 * @throws Exception
	 *             It can throw an exception.
	 */
	private String getFileName(String uri) throws Exception {
		String partsColon[] = uri.split(":");
		if (partsColon.length == 3) {
			String ns = StaticUtils.getUntilFirstSlashBackwards(partsColon[1]);
			String res = partsColon[2];
			ns = ns.replace('/', '_');
			res = res.replace('/', '_');
			res = res.replace(':', '_');
			return ns + "_" + res;
		} else {
			String partsNumberSign[] = uri.split("#");
			if (partsNumberSign.length == 2) {
				String ns = StaticUtils
						.getUntilFirstSlashBackwards(partsNumberSign[0]);
				String res = partsNumberSign[1];
				ns = ns.replace('/', '_');
				res = res.replace('/', '_');
				res = res.replace(':', '_');
				return ns + "_" + res;
			} else {
				String partsSlash[] = uri.split("/");
				String ret = partsSlash[partsSlash.length - 1];
				ret = ret.replace('/', '_');
				ret = ret.replace(':', '_');
				return ret;
			}
		}
	}

	private void getValidName(String uri, ResourceName rn) {
		OntModel om = ModelFactory.createOntologyModel();
		Resource r = om.createResource(uri);
		if (!StaticUtils.isEmpty(r.getLocalName())) {
			bs.incNumberResourcesWithLocalLocalName();
			rn.setName(r.getLocalName());
		} else {
			bs.incNumberResourcesWithoutLocalLocalName();
			String partsAlmoha[] = uri.split("#");
			if ((partsAlmoha != null) && (partsAlmoha.length == 2)) {
				rn.setName(partsAlmoha[1]);
			}
			String partsColon[] = uri.split(":");
			if ((partsColon != null) && (partsColon.length == 3)) {
				/*
				 * We assume the possible format http://.../namespace:Resource
				 * (two :)
				 */
				String finalName = "";
				String candidate = partsColon[2];
				if (StaticUtils.isAllNumbers(candidate)) {
					/*
					 * There are some resources which are identified by numbers.
					 * For example:
					 * 
					 * http://bio2rdf.org/psi-mi:0326
					 * 
					 * In those cases, in order to try to obtain a more readable
					 * name, we join the namespace and the resource with an
					 * underline: psi-mi_0326 as name.
					 */
					String namespace = StaticUtils
							.getUntilFirstSlashBackwards(partsColon[1]);
					finalName = namespace + "_" + candidate;
				} else {
					finalName = candidate;
				}
				rn.setName(finalName);
			} else {
				rn.setName("Unknown");
				rn.addError("Three methods to obtain resource name failed. Set to 'Unknown'");
			}
		}
	}

	/**
	 * Method to get the local name of a resource.
	 * 
	 * @param uri
	 *            Receives the resource URI.
	 * @return Return the local name.
	 */
	@SuppressWarnings("unused")
	private ResourceName getResourceLocalName(String uri) {
		/*
		 * First step, we try to get the name of the resource loading the
		 * ontology.
		 */
		ResourceName localName = new ResourceName();
		localName.setURI(uri);
		getLocalNameResource(uri, localName);
		if (localName.getName() != null) {
			bs.incNumberResourcesWithRemoteLocalName();
			/*
			 * If we have a name.. bingo!
			 */
			return localName;
		} else {
			bs.incNumberResourcesWithoutRemoteLocalName();
			/*
			 * Else.. we try the second step: label.
			 */
			getLabelFrom(uri, localName);
			if (localName.getName() != null) {
				bs.incNumberResourcesWithLabel();
				/*
				 * If we have a label.. bingo!
				 */
			} else {
				bs.incNumberResourcesWithoutLabel();
				/*
				 * Else.. third and final step!
				 */
				getValidName(uri, localName);
			}
		}
		return localName;
	}

	/**
	 * Method to get the local name of a resource given a URI, loading the
	 * resource into a ontology model.
	 * 
	 * @param uri
	 *            Receives the URI.
	 * @return The object with the content.
	 */
	private void getLocalNameResource(String uri, ResourceName rn) {
		try {
			/*
			 * We create an ontology model and we read from the uri provided.
			 */
			this.ontModel = ModelFactory.createOntologyModel();
			this.ontModel.read(uri);
			/*
			 * We try to get the resource associated to the URI
			 */
			Resource r = this.ontModel.getResource(uri);
			if (r == null) {
				/*
				 * If it is null, it doesn't exist?
				 */
				rn.setName(null);
				rn.addError("Error loading the resource reading into an ontology model: Null resource. Not exists?");
			} else {
				/*
				 * If is not null.. if the local name has any value.. we return
				 * this value.
				 */
				if (!StaticUtils.isEmpty(r.getLocalName())) {
					rn.setName(r.getLocalName());
				} else {
					/*
					 * If not.. we don't have anything else to do here.
					 */
					rn.setName(null);
					rn.addError("Error loading the resource reading into an ontology model: Resource exists, but no local name found.");
				}
			}
		} catch (Exception e) {
			rn.setName(null);
			rn.addError("Error loading the resource reading into an ontology model: "
					+ e.getClass() + ": " + e.getMessage());
		}
	}

	/**
	 * Method to get the label from a resource.
	 * 
	 * @param r
	 *            Receives the resource.
	 * @return Returns the label (removing attached information between brackets
	 *         "name [cod]". Just "name").
	 */
	private void getLabelFrom(String r, ResourceName rn) {
		try {
			this.ontModel = ModelFactory.createOntologyModel();
			this.ontModel.read(r);
			Statement st = ontModel.getResource(r).getProperty(RDFS.label);
			if (st != null) {
				String lb = st.getObject().toString();
				String parts[] = lb.split(" ");
				String label = "";
				for (int i = 0; i < parts.length; i++) {
					if (parts[i].charAt(0) != '[') {
						label += parts[i] + " ";
					}
				}
				label = label.trim();
				label = label.replace(' ', '_');
				rn.setName(label);
			} else {
				rn.setName(null);
				rn.addError("No label found for this resource!");
			}
		} catch (Exception e) {
			rn.setName(null);
			rn.addError("Error loading the resource reading into an ontology model (to get the label): "
					+ e.getClass() + ": " + e.getMessage());
		}
	}

	/**
	 * Method to save the configuration file.
	 * 
	 * @param sncfg
	 *            Receives the file.
	 * @throws Exception
	 *             It can throws an exception.
	 */
	private void saveConfigFile(String sncfg, SPO spo) throws Exception {
		configFile.write(Constants.ORIGINAL_ENDPOINT + Constants.EQUALS
				+ ConfigManager.getConfig(Constants.ENDPOINT_BASE)
				+ this.endpoint.getName() + ConfigManager.getConfig(Constants.SPARQL_ENDPOINT_TERMINATION));
		configFile.newLine();
		configFile.write(Constants.GENERIC_ENDPOINT + Constants.EQUALS
				+ ConfigManager.getConfig(Constants.ENDPOINT_BASE)
				+ this.endpoint.getName() + ConfigManager.getConfig(Constants.SPARQL_ENDPOINT_TERMINATION));
		configFile.newLine();
		configFile.write(Constants.OBJECT_TYPE + Constants.EQUALS
				+ spo.getObject());
		configFile.newLine();
		configFile.close();
		this.bs.incNumberConfigFilesCreated();
	}

	/**
	 * Method to create the SPARQL query file.
	 * 
	 * @param snsq
	 *            Receives the file.
	 * @param spo
	 *            Receives the SPO.
	 * @throws Exception
	 *             It can throws an exception.
	 */
	private void createSPARQLQueryFile(String snsq, SPO spo) throws Exception {
		if (spo.getPredicate().contains(this.inverseSuffix)) {
			/*
			 * If we have an inverse predicate, we create the associate sparql
			 * query file (they are different).
			 */
			createSPARQLQueryFileInversePredicate(snsq, spo);

		} else {
			/*
			 * For normal predicate.
			 */
			createSPARQLQueryFileNormalPredicate(snsq, spo);
		}
	}

	/**
	 * Method to create the SPARQL query file with an inverse predicate.
	 * 
	 * @param snsq
	 *            Receives the file name to store the query.
	 * @param spo
	 *            Receives the SPO.
	 * @throws Exception
	 *             It can throw an exception.
	 */
	private void createSPARQLQueryFileInversePredicate(String snsq, SPO spo)
			throws Exception {
		String query = "";
		Resource r = this.ontModel.getResource(spo.getPredicate().replace(this.inverseSuffix, ""));
		query += "PREFIX pre: <" + r.getNameSpace() + ">\n";
		query += "SELECT *\n";
		query += "FROM <" + this.endpoint.getGraphEndpoint() + ">\n";
		query += "WHERE {\n";
		query += "\t?obj pre:" + r.getLocalName() + " %VAR.\n";
		query += "\tOPTIONAL {?obj  rdfs:label ?label}\n";
		query += "}\n";
		BufferedWriter bW = new BufferedWriter(new FileWriter("ontologies/"
				+ this.endpoint.getName() + "/" + snsq));
		bW.write(query);
		bW.close();
		this.bs.incNumberSPARQLFilesCreated();
	}

	/**
	 * Method to create the SPARQL query file with a normal predicate.
	 * 
	 * @param snsq
	 *            Receives the file name to store the query.
	 * @param spo
	 *            Receives the SPO.
	 * @throws Exception
	 *             It can throw an exception.
	 */
	private void createSPARQLQueryFileNormalPredicate(String snsq, SPO spo)
			throws Exception {
		String query = "";
		Resource r = this.ontModel.getResource(spo.getPredicate());
		query += "PREFIX pre: <" + r.getNameSpace() + ">\n";
		query += "SELECT *\n";
		query += "FROM <" + this.endpoint.getGraphEndpoint() + ">\n";
		query += "WHERE {\n";
		query += "\t%VAR pre:" + r.getLocalName() + " ?obj.\n";
		query += "\tOPTIONAL {?obj  rdfs:label ?label}\n";
		query += "}\n";
		BufferedWriter bW = new BufferedWriter(new FileWriter("ontologies/"
				+ this.endpoint.getName() + "/" + snsq));
		bW.write(query);
		bW.close();
		this.bs.incNumberSPARQLFilesCreated();
	}

	/**
	 * Method to create the ontology.
	 * 
	 * @param sno
	 *            Receives the file.
	 * @param spo
	 *            Receives the SPO.
	 * @throws Exception
	 *             It can throw an exception.
	 */
	private void createOntology(String sno, SPO spo) throws Exception {
		this.manager = OWLManager.createOWLOntologyManager();
		this.ontology = manager.createOntology();
		OWLDataFactory factory = manager.getOWLDataFactory();
		IRI subjectClass = IRI.create(spo.getSubject());
		configFile.write(Constants.INPUTCLASS_URI + Constants.EQUALS
				+ subjectClass.toURI().toString());
		configFile.newLine();
		IRI objectClass = IRI.create(spo.getObject());
		IRI sadiOutputClass = IRI.create(defaultIRI + sno + "#"
				+ this.sadiServiceOutputClass);
		configFile.write(Constants.OUTPUTCLASS_NAME + Constants.EQUALS
				+ this.sadiServiceOutputClass);
		configFile.newLine();
		configFile.write(Constants.OUTPUTCLASS_URI + Constants.EQUALS
				+ sadiOutputClass.toURI().toString());
		configFile.newLine();
		IRI ontologyIRI = IRI.create(defaultIRI + sno);
		/*
		 * Subject, object and sadioutput class
		 */
		OWLClass subjectOWLClass = factory.getOWLClass(subjectClass);
		OWLClass sadiOutputOWLClass = factory.getOWLClass(sadiOutputClass);

		OWLClass objectOWLClass = null;
		OWLDatatype objectDataType = null;
		/*
		 * Here, we have to check if we are dealing with normal objects or
		 * literals.
		 */
		if (!spo.getObjectType().equalsIgnoreCase(Constants.XML_SCHEMA)) {

			/*
			 * If we don't have a literal, we take the object class.
			 */

			objectOWLClass = factory.getOWLClass(objectClass);
		} else {
			objectDataType = factory.getOWLDatatype(objectClass);
		}

		/*
		 * Ontology
		 */
		ontology = manager.createOntology(ontologyIRI);

		/*
		 * Subject class for axioms.
		 */
		OWLDeclarationAxiom declarationAxiomSC = factory
				.getOWLDeclarationAxiom(subjectOWLClass);
		/*
		 * We add subject class to the ontology.
		 */
		manager.addAxiom(ontology, declarationAxiomSC);
		/*
		 * Object class for axioms.
		 */

		/*
		 * Here, we have to check if we are dealing with normal objects or
		 * literals.
		 */
		OWLDeclarationAxiom declarationAxiomOC = null;

		if (!spo.getObjectType().equalsIgnoreCase(Constants.XML_SCHEMA)) {
			declarationAxiomOC = factory.getOWLDeclarationAxiom(objectOWLClass);
		} else {
			declarationAxiomOC = factory.getOWLDeclarationAxiom(objectDataType);
		}
		/*
		 * We add object class to the ontology.
		 */
		manager.addAxiom(ontology, declarationAxiomOC);

		/*
		 * SADI output class for axioms.
		 */
		OWLDeclarationAxiom declarationAxiomSOC = factory
				.getOWLDeclarationAxiom(sadiOutputOWLClass);
		/*
		 * We add service output class to the ontology.
		 */
		manager.addAxiom(ontology, declarationAxiomSOC);
		/*
		 * Predicate
		 */
		IRI predicateIRI = IRI.create(spo.getPredicate());

		@SuppressWarnings("rawtypes")
		OWLProperty predicate = null;
		/*
		 * If we are using XML schema type, predicate is a data property
		 * 
		 * If not, is an object property
		 */
		if (spo.getObjectType().equalsIgnoreCase(Constants.XML_SCHEMA)) {
			predicate = factory.getOWLDataProperty(predicateIRI);
		} else {
			predicate = factory.getOWLObjectProperty(predicateIRI);
		}

		configFile.write(Constants.PREDICATE_NAME + Constants.EQUALS
				+ processToRemoveColonAndPrefix(predicateIRI.getFragment()));
		configFile.newLine();
		configFile.write(Constants.PREDICATE_URI + Constants.EQUALS
				+ predicateIRI.toURI().toString());
		configFile.newLine();

		/*
		 * Predicate as axiom.
		 */
		OWLDeclarationAxiom declarationAxiomPred = factory
				.getOWLDeclarationAxiom(predicate);
		/*
		 * We add predicate to the ontology.
		 */
		manager.addAxiom(ontology, declarationAxiomPred);

		/*
		 * OWL Expression (predicate some ObjectClass)
		 */
		OWLClassExpression predicateSomeObject;
		/*
		 * If we are using XML schema type, predicate is a data property
		 * 
		 * If not, is an object property
		 */
		if (spo.getObjectType().equalsIgnoreCase(Constants.XML_SCHEMA)) {
			predicateSomeObject = factory.getOWLDataSomeValuesFrom(
					(OWLDataProperty) predicate, objectDataType);
		} else {
			predicateSomeObject = factory.getOWLObjectSomeValuesFrom(
					(OWLObjectProperty) predicate, objectOWLClass);
		}

		/*
		 * OWL Expression (subject and X)
		 * 
		 * X = (predicate some ObjectClass)
		 * 
		 * Hence, final is: (subject and (predicate some ObjectClass))
		 */
		OWLClassExpression subjectAndPredicateSomeObject = factory
				.getOWLObjectIntersectionOf(subjectOWLClass,
						predicateSomeObject);

		/*
		 * We establish that SADI service output class is equivalent to X
		 * 
		 * SADIOutputClass equivalentTo X
		 * 
		 * X = (subject and (predicate some ObjectClass))
		 * 
		 * Hence:
		 * 
		 * SADIOutputClass equivalentTo (subject and (predicate some
		 * ObjectClass))
		 */
		OWLEquivalentClassesAxiom ax = factory.getOWLEquivalentClassesAxiom(
				subjectAndPredicateSomeObject, sadiOutputOWLClass);
		AddAxiom addAx = new AddAxiom(ontology, ax);
		manager.applyChange(addAx);

		manager.saveOntology(ontology, new FileOutputStream("ontologies/"
				+ this.endpoint.getName() + "/" + sno));
		this.bs.incNumberOntologiesCreated();
	}

	/**
	 * Method to remove colon and prefix from an URI.
	 * 
	 * @param fragment
	 *            Receives the URI.
	 * @return Returns the value.
	 */
	private String processToRemoveColonAndPrefix(String fragment) {
		if (fragment.contains(":")) {
			String parts[] = fragment.split(":");
			if (parts.length == 2) {
				return parts[1];
			}
		}
		return fragment;
	}

	/**
	 * Method to check if a file exists.
	 * 
	 * @param s
	 *            Receives the file.
	 * @return Returns a boolean.
	 */
	private boolean checkSave(String s) {
		File f = new File("ontologies/" + this.endpoint.getName() + "/");
		if (!f.exists()) {
			f.mkdir();
		}
		f = new File("ontologies/" + this.endpoint.getName() + "/" + s + ".owl");
		return f.exists();
	}
}
