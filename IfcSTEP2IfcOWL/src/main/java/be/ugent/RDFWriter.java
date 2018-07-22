package be.ugent;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntProperty;
import org.apache.jena.ontology.OntResource;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.riot.system.StreamRDFWriter;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.openbimstandards.ifcowl.ExpressReader;
import org.openbimstandards.vo.AttributeVO;
import org.openbimstandards.vo.EntityVO;
import org.openbimstandards.vo.IFCVO;
import org.openbimstandards.vo.TypeVO;


import fi.ni.rdf.Namespace;
import nl.tue.ddss.convert.Header;
import nl.tue.ddss.convert.IfcDataFormatException;
import nl.tue.ddss.convert.IfcHeader;

/*
 * Copyright 2016 Pieter Pauwels, Ghent University; Jyrki Oraskari, Aalto University; Lewis John McGibbney, Apache
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License atf
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 * Class to read IFC SPF file and write to RDF file. Modified by Chi Zhang.
 * @author Chi
 *
 */
public class RDFWriter {

    // input variables
	private static final Logger LOGGER = Logger.getLogger(RDFWriter.class.getName());
    private final String baseURI;
    private final String ontNS;
    private static final String expressURI = "https://w3id.org/express";
    private static final String expressNS = expressURI + "#";
    private static final String listURI = "https://w3id.org/list";
    private static final String listNS = listURI + "#";

    // EXPRESS basis
    private final Map<String, EntityVO> ent;
    private final Map<String, TypeVO> typ;

    // conversion variables
    private int IDcounter = 0;
    private Map<Long, IFCVO> linemap = new LinkedHashMap<Long, IFCVO>();

    private StreamRDF rdfWriter;
    private InputStream inputStream;
    private final OntModel ontModel;
    private final OntModel expressModel;
    private final OntModel listModel;
    
    // for removing duplicates in line entries
    private Map<String, Resource> listOfUniqueResources = new HashMap<String, Resource>();
    private Map<Long, Long> listOfDuplicateLineEntries = new HashMap<Long, Long>();

    // Taking care of avoiding duplicate resources
    private Map<String, Resource> propertyResourceMap = new HashMap<String, Resource>();
    private Map<String, Resource> resourceMap = new HashMap<String, Resource>();

    private boolean removeDuplicates = false;
    private boolean expIdAsProperty=false;
    


	public boolean getExpIdAsProperty() {
		return expIdAsProperty;
	}

	public void setExpIdAsProperty(boolean expIdAsProperty) {
		this.expIdAsProperty = expIdAsProperty;
	}

	private boolean logToFile=false;

    public RDFWriter(OntModel ontModel, OntModel expressModel, OntModel listModel, InputStream inputStream, String baseURI, Map<String, EntityVO> ent, Map<String, TypeVO> typ, String ontURI) {
        this.ontModel = ontModel;
        this.expressModel = expressModel;
        this.listModel = listModel;
        this.inputStream = inputStream;
        this.baseURI = baseURI;
        this.ent = ent;
        this.typ = typ;
        this.ontNS = ontURI;
    }
    
	public void parseModel2Stream(OutputStream out, Header header, Lang lang)
			throws IOException{
		LOGGER.setLevel(Level.ALL);
		if(logToFile){
			FileHandler fhandler=new FileHandler(logFile);
            fhandler.setLevel(Level.ALL);
	        SimpleFormatter formatter = new SimpleFormatter();  
	        fhandler.setFormatter(formatter);  
			LOGGER.addHandler(fhandler);
		}
		if (lang == null) {
			lang = RDFLanguages.TURTLE;
		}
		setRdfWriter(StreamRDFWriter.getWriterStream(out, lang));
		getRdfWriter().base(getBaseURI());
		getRdfWriter().prefix("ifcowl", getOntNS());
		getRdfWriter().prefix("inst", getBaseURI());
		getRdfWriter().prefix("list", getListns());
		getRdfWriter().prefix("express", getExpressns());
		getRdfWriter().prefix("rdf", Namespace.RDF);
		getRdfWriter().prefix("xsd", Namespace.XSD);
		getRdfWriter().prefix("owl", Namespace.OWL);
		getRdfWriter().prefix("ifch", IfcHeader.getURI());
		getRdfWriter().start();
		getRdfWriter()
				.triple(new Triple(NodeFactory.createURI(getBaseURI()), RDF.type.asNode(), OWL.Ontology.asNode()));
		getRdfWriter().triple(new Triple(NodeFactory.createURI(getBaseURI()), OWL.imports.asNode(),
				NodeFactory.createURI(getOntNS())));
		writeHeader(header);
		// Read the whole file into a linemap Map object
		readModel();

		System.out.println("Model parsed!");

		if (removeDuplicates) {
			resolveDuplicates();
		}

		// map entries of the linemap Map object to the ontology Model and make
		// new instances in the model
		boolean parsedSuccessfully;
		try {
			parsedSuccessfully = mapEntries();
		

		if (!parsedSuccessfully)
			return;

		System.out.println("Entries mapped, now creating instances...");
		// if(indexFile==true){
		// jw.
		// }
		createInstances();
		System.out.println("Finished!");
		}  catch (IfcDataFormatException ie){
			LOGGER.log(Level.SEVERE , "Caught IfcDataFormatException: "+ie.getMessage());

			ie.printStackTrace();
			System.out.println("Converter quitted!!");
		}
		// Save memory
		linemap.clear();
		linemap = null;

		getRdfWriter().finish();
	}
	
	public void writeHeader(Header header) {
		if (header.getDescription() != null) {
			for (String s:header.getDescription()){
				getRdfWriter().triple(new Triple(NodeFactory.createURI(getBaseURI()), IfcHeader.description.asNode(),
						NodeFactory.createLiteral(s)));
			}
		}
		if (header.getImplementation_level() != null) {
			getRdfWriter().triple(new Triple(NodeFactory.createURI(getBaseURI()), IfcHeader.implementation_level.asNode(), NodeFactory.createLiteral(header.getImplementation_level())));
		}
		if (header.getName() != null) {
			getRdfWriter().triple(new Triple(NodeFactory.createURI(getBaseURI()), IfcHeader.name.asNode(), NodeFactory.createLiteral(header.getName())));
		}
		if (header.getTime_stamp() != null) {
			getRdfWriter().triple(new Triple(NodeFactory.createURI(getBaseURI()), IfcHeader.time_stamp.asNode(), NodeFactory.createLiteral(header.getTime_stamp())));
		}
		if (header.getAuthor() != null) {
			for (String s:header.getAuthor()){
				getRdfWriter().triple(new Triple(NodeFactory.createURI(getBaseURI()), IfcHeader.author.asNode(),
						NodeFactory.createLiteral(s)));
			}
		}
		if (header.getOrganization() != null) {
			for (String s:header.getOrganization()){
				getRdfWriter().triple(new Triple(NodeFactory.createURI(getBaseURI()), IfcHeader.organization.asNode(),
						NodeFactory.createLiteral(s)));
			}
		}
		if (header.getPreprocessor_version() != null) {
			getRdfWriter().triple(new Triple(NodeFactory.createURI(getBaseURI()), IfcHeader.preprocessor_version.asNode(), NodeFactory.createLiteral(header.getPreprocessor_version())));
		}
		if (header.getOriginating_system() != null) {
			getRdfWriter().triple(new Triple(NodeFactory.createURI(getBaseURI()), IfcHeader.originating_system.asNode(), NodeFactory.createLiteral(header.getOriginating_system())));
		}
		if (header.getAuthorization() != null) {
			getRdfWriter().triple(new Triple(NodeFactory.createURI(getBaseURI()), IfcHeader.authorization.asNode(), NodeFactory.createLiteral(header.getAuthorization())));
		}
		if (header.getSchema_identifiers() != null) {
			for (String s:header.getSchema_identifiers()){
				getRdfWriter().triple(new Triple(NodeFactory.createURI(getBaseURI()), IfcHeader.schema_identifiers.asNode(),
						NodeFactory.createLiteral(s)));
			}
		}
	}


    public void readModel() {
        try {
            DataInputStream in = new DataInputStream(inputStream);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            try {
                String strLine;
                while ((strLine = br.readLine()) != null) {
                    if (strLine.length() > 0) {
                        if (strLine.charAt(0) == '#') {
                            StringBuffer sb = new StringBuffer();
                            String stmp = strLine;
                            sb.append(stmp.trim());
                            while (!stmp.endsWith(");")&&!(stmp.contains(");"))) {
                                stmp = br.readLine();
                                if (stmp == null)
                                    break;
                                sb.append(stmp.trim());
                            }
                            parseIfcLineStatement(sb.toString().substring(1));
                        }
                    }
                }
            } finally {
                br.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void parseIfcLineStatement(String line) {
        IFCVO ifcvo = new IFCVO();
        ifcvo.setFullLineAfterNum(line.substring(line.indexOf("=") + 1));
        int state = 0;
        StringBuffer sb = new StringBuffer();
        int clCount = 0;
        LinkedList<Object> current = (LinkedList<Object>) ifcvo.getObjectList();
        Stack<LinkedList<Object>> listStack = new Stack<LinkedList<Object>>();
        for (int i = 0; i < line.length(); i++) {
        	 char ch = line.charAt(i);
            switch (state) {
                case 0:
                if (ch == '=') {
                    ifcvo.setLineNum(toLong(sb.toString()));
                    sb.setLength(0);
                    state++;
                    continue;
                } else if (Character.isDigit(ch))
                    sb.append(ch);
                    break;
                case 1: // (
                if (ch == '(') {
                    ifcvo.setName(sb.toString());
                    sb.setLength(0);
                    state++;
                    continue;
                } else if (ch == ';') {
                    ifcvo.setName(sb.toString());
                    sb.setLength(0);
                    state = Integer.MAX_VALUE;
                } else if (!Character.isWhitespace(ch))
                    sb.append(ch);
                    break;
                case 2: // (... line started and doing (...
                if (ch == '\'') {
                    state++;
                }
                if (ch == '(') {
                    listStack.push(current);
                    LinkedList<Object> tmp = new LinkedList<Object>();
                    if (sb.toString().trim().length() > 0)
                        current.add(sb.toString().trim());
                    sb.setLength(0);
                    current.add(tmp);
                    current = tmp;
                    clCount++;
                } else if (ch == ')') {
                    if (clCount == 0) {
                        if (sb.toString().trim().length() > 0)
                            current.add(sb.toString().trim());
                        sb.setLength(0);
                        state = Integer.MAX_VALUE; // line is done
                        continue;
                    } else {
                        if (sb.toString().trim().length() > 0)
                            current.add(sb.toString().trim());
                        sb.setLength(0);
                        clCount--;
                        current = listStack.pop();
                    }
                } else if (ch == ',') {
                    if (sb.toString().trim().length() > 0)
                        current.add(sb.toString().trim());
                    current.add(Character.valueOf(ch));

                    sb.setLength(0);
                } else {
                    sb.append(ch);
                }
                    break;
                case 3: // (...
                if (ch == '\'') {
                    state--;
                    sb.append(ch);
                } else {
                    sb.append(ch);
                }
                    break;
                default:
                // Do nothing
            }
        }
        linemap.put(ifcvo.getLineNum(), ifcvo);
        IDcounter++;
    }

    private void resolveDuplicates() throws IOException {
        Map<String, IFCVO> listOfUniqueResources = new HashMap<String, IFCVO>();
        List<Long> entriesToRemove = new ArrayList<Long>();
        for (Map.Entry<Long, IFCVO> entry : linemap.entrySet()) {
            IFCVO vo = entry.getValue();
            String t = vo.getFullLineAfterNum();
            if (!listOfUniqueResources.containsKey(t))
                listOfUniqueResources.put(t, vo);
            else {

                entriesToRemove.add(entry.getKey());// linemap.remove(entry.getKey());
                listOfDuplicateLineEntries.put(vo.getLineNum(), listOfUniqueResources.get(t).getLineNum());
            }
        }
            LOGGER.log(Level.INFO,"found and removed " + listOfDuplicateLineEntries.size() + " duplicates! \r\n");
        for (Long x : entriesToRemove) {
            linemap.remove(x);
        }
    }

    private boolean mapEntries() throws IOException, IfcDataFormatException{
        for (Map.Entry<Long, IFCVO> entry : linemap.entrySet()) {
            IFCVO vo = entry.getValue();

            // mapping properties to IFCVOs
            for (int i = 0; i < vo.getObjectList().size(); i++) {
                Object o = vo.getObjectList().get(i);
                if (Character.class.isInstance(o)) {
                    if ((Character) o != ',') {
                        LOGGER.log(Level.WARNING ,"We found a character that is not a comma in line: "+"#"+vo.getLineNum()+"="+vo.getFullLineAfterNum());
                }} else if (String.class.isInstance(o)) {
                    String s = (String) o;
                    if (s.length() < 1)
                        continue;
                    if (s.charAt(0) == '#') {
                        Object or = null;
                        if (listOfDuplicateLineEntries.containsKey(toLong(s.substring(1))))
                            or = linemap.get(listOfDuplicateLineEntries.get(toLong(s.substring(1))));
                        else
                            or = linemap.get(toLong(s.substring(1)));
                        if (or == null) {
								throw IfcDataFormatException.referencingNonExistingObject("#"+vo.getLineNum(),s);
                        }
                        vo.getObjectList().set(i, or);
                    }
                } else if (LinkedList.class.isInstance(o)) {
                    @SuppressWarnings("unchecked")
                    LinkedList<Object> tmpList = (LinkedList<Object>) o;

                    for (int j = 0; j < tmpList.size(); j++) {
                        Object o1 = tmpList.get(j);
                        if (Character.class.isInstance(o)) {
                            if ((Character) o != ',') {
                                	LOGGER.log(Level.WARNING,"We found a character that is not a comma in line: "+"#"+vo.getLineNum()+"="+vo.getFullLineAfterNum());
                            }
                        } else if (String.class.isInstance(o1)) {
                            String s = (String) o1;
                            if (s.length() < 1)
                                continue;
                            if (s.charAt(0) == '#') {
                                Object or = null;
                                if (listOfDuplicateLineEntries.containsKey(toLong(s.substring(1))))
                                    or = linemap.get(listOfDuplicateLineEntries.get(toLong(s.substring(1))));
                                else
                                    or = linemap.get(toLong(s.substring(1)));
                                if (or == null) {
                                	throw IfcDataFormatException.referencingNonExistingObject(s,"#"+vo.getLineNum()+"="+vo.getFullLineAfterNum());
                                } else
                                    tmpList.set(j, or);
                            } else {
                                // list/set of values
                                tmpList.set(j, s);
                            }
                        } else if (LinkedList.class.isInstance(o1)) {
                            @SuppressWarnings("unchecked")
                            LinkedList<Object> tmp2List = (LinkedList<Object>) o1;
                            for (int j2 = 0; j2 < tmp2List.size(); j2++) {
                                Object o2 = tmp2List.get(j2);
                                if (String.class.isInstance(o2)) {
                                    String s = (String) o2;
                                    if (s.length() < 1)
                                        continue;
                                    if (s.charAt(0) == '#') {
                                        Object or = null;
                                        if (listOfDuplicateLineEntries.containsKey(toLong(s.substring(1))))
                                            or = linemap.get(listOfDuplicateLineEntries.get(toLong(s.substring(1))));
                                        else
                                            or = linemap.get(toLong(s.substring(1)));
                                        if (or == null) {
                                        	throw IfcDataFormatException.referencingNonExistingObject(s,"#"+vo.getLineNum()+"="+vo.getFullLineAfterNum());
                                      
                                        } else
                                            tmp2List.set(j2, or);
                                    }
                                }
                            }
                            tmpList.set(j, tmp2List);
                        }
                    }
                }
            }
        }
        return true;
    }

    private void createInstances() throws IOException, IfcDataFormatException {
        for (Map.Entry<Long, IFCVO> entry : linemap.entrySet()) {
            IFCVO ifcLineEntry = entry.getValue();
            String typeName = "";
            if (ent.containsKey(ifcLineEntry.getName()))
                typeName = ent.get(ifcLineEntry.getName()).getName();
            else if (typ.containsKey(ifcLineEntry.getName()))
                typeName = typ.get(ifcLineEntry.getName()).getName();

            OntClass cl = ontModel.getOntClass(getOntNS() + typeName);
            Resource hasExpressID=ontModel.getResource(getExpressns()+"hasExpressID");
            if(cl==null){
            	throw IfcDataFormatException.nonExistingEntity(ifcLineEntry.getName(),"#"+ifcLineEntry.getLineNum()+"= "+ifcLineEntry.getFullLineAfterNum());
            }
            
            Resource r=getResource(getBaseURI()+createLocalName(typeName + "_" + ifcLineEntry.getLineNum()),cl);
            if(expIdAsProperty==true){
            getRdfWriter().triple(new Triple(r.asNode(),hasExpressID.asNode(),ResourceFactory.createTypedLiteral(ifcLineEntry.getLineNum().toString(), XSDDatatype.XSDinteger).asNode()));
            }
            listOfUniqueResources.put(ifcLineEntry.getFullLineAfterNum(), r);
            fillProperties(ifcLineEntry, r, cl);           
            

                LOGGER.log(Level.FINE,"Convert IFC object #"+ifcLineEntry.getLineNum()+"="+ifcLineEntry.getName().toUpperCase());

            
        }
        // The map is used only to avoid duplicates.
        // So, it can be cleared here
        propertyResourceMap.clear();
    }

    TypeVO typeRemembrance = null;
	private String logFile;

	private void fillProperties(IFCVO ifcLineEntry, Resource r, OntClass cl)
			throws IOException, IfcDataFormatException {
		EntityVO evo = ent.get(ExpressReader.formatClassName(ifcLineEntry.getName()));

		if (evo == null) {
			throw IfcDataFormatException.nonExistingEntity(ifcLineEntry.getName(),
					"#"+ifcLineEntry.getLineNum()+"="+ifcLineEntry.getFullLineAfterNum());

		} else {
			final String subject = createLocalName(evo.getName() + "_" + ifcLineEntry.getLineNum());
			// final String subject = evo.getName() + "_" +
			// ifcLineEntry.getLineNum();
			typeRemembrance = null;
			int attributePointer = 0;
			for (Object o : ifcLineEntry.getObjectList()) {
				if (Character.class.isInstance(o)) {
					if ((Character) o != ',') {
	                        LOGGER.log(Level.WARNING ,"We found a character that is not a comma in line: "+"#"+ifcLineEntry.getLineNum()+"="+ifcLineEntry.getFullLineAfterNum());
					}
				} else if (String.class.isInstance(o)) {
	//				if (logToFile)
	//					bw.write("fillProperties 4 - fillPropertiesHandleStringObject(evo)" + "\r\n");
					attributePointer = fillPropertiesHandleStringObject(r, evo, subject, attributePointer, o,ifcLineEntry);
				} else if (IFCVO.class.isInstance(o)) {
	//				if (logToFile)
	//					bw.write("fillProperties 5 - fillPropertiesHandleIfcObject(evo)" + "\r\n");
					attributePointer = fillPropertiesHandleIfcObject(r, evo, attributePointer, o, ifcLineEntry);
				} else if (LinkedList.class.isInstance(o)) {
	//				if (logToFile)
	//					bw.write("fillProperties 6 - fillPropertiesHandleListObject(evo)" + "\r\n");
					attributePointer = fillPropertiesHandleListObject(r, evo, attributePointer, o,ifcLineEntry);
				}
//				if (logToFile)
//					bw.flush();
			}
		}

//		if (logToFile)
//			bw.flush();
	}

    // --------------------------------------
    // 6 MAIN FILLPROPERTIES METHODS
    // --------------------------------------

    private int fillPropertiesHandleStringObject(Resource r, EntityVO evo, String subject, int attributePointer, Object o,IFCVO ivo) throws IOException, IfcDataFormatException {
        if (!((String) o).equals("$") && !((String) o).equals("*")) {
            if (typ.get(ExpressReader.formatClassName((String) o)) == null) {
                if ((evo != null) && (evo.getDerivedAttributeList() != null)) {
                    if (evo.getDerivedAttributeList().size() <= attributePointer) {
                       throw IfcDataFormatException.attributeOutOfBounds("#"+ivo.getLineNum()+"="+ivo.getFullLineAfterNum());
                    }
                    final AttributeVO avo=evo.getDerivedAttributeList().get(attributePointer);
                    final String propURI = getOntNS() + avo.getLowerCaseName();
                    final String literalString = filterExtras((String) o);
                    if (avo.isSet()){
                    	throw IfcDataFormatException.valueOutOfRange("#"+ivo.getLineNum(),(String)o,"SET");
                    }
                    OntProperty p = ontModel.getOntProperty(propURI);
                    OntResource range = p.getRange();
                    if (range.isClass()) {
                        if (range.asClass().hasSuperClass(expressModel.getOntClass(getExpressns() + "ENUMERATION"))) {
                            // Check for ENUM
                            addEnumProperty(r, p, range, literalString,ivo);
                        } else if (range.asClass().hasSuperClass(expressModel.getOntClass(getExpressns() + "SELECT"))) {
                            // Check for SELECT
  //                          if (logToFile)
  //                              bw.write("*OK 25*: found subClass of SELECT Class, now doing nothing with it: " + p + " - " + range.getLocalName() + " - " + literalString + "\r\n");
                            createLiteralProperty(r, p, range, literalString,ivo);
                        } else if (range.asClass().hasSuperClass(listModel.getOntClass(getListns() + "OWLList"))) {
                            // Check for LIST
                            throw IfcDataFormatException.valueOutOfRange("#"+ivo.getLineNum(),(String)o,range.getLocalName());
                        } else {
                            createLiteralProperty(r, p, range, literalString,ivo);
                        }
                    } else {
                       
                            LOGGER.log(Level.WARNING,"found other kind of property: " + p + " - " + range.getLocalName() + "\r\n");
                    }
                } else {
                
                        LOGGER.log(Level.WARNING,"Nothing happened. Not sure if this is good or bad, possible or not." + "\r\n");
                }
                attributePointer++;
            } else {
                typeRemembrance = typ.get(ExpressReader.formatClassName((String) o));
                // if (typeRemembrance == null) {
                // if (logToFile)
                // bw.write("*ERROR 11*: The following TYPE is not found: "
                // + ExpressReader.formatClassName((String) o)
                // + "\r\nQuitting the application without output!\r\n ");
                // System.err.println(
                // "*ERROR 11*: The following TYPE is not found: " +
                // ExpressReader.formatClassName((String) o)
                // + "\r\nQuitting the application without output!\r\n ");
                // }
            }
        } else
            attributePointer++;
        return attributePointer;
    }

    private int fillPropertiesHandleIfcObject(Resource r, EntityVO evo, int attributePointer, Object o,IFCVO ivo) throws IOException, IfcDataFormatException {
        if ((evo != null) && (evo.getDerivedAttributeList() != null) && (evo.getDerivedAttributeList().size() > attributePointer)) {
            final AttributeVO avo=evo.getDerivedAttributeList().get(attributePointer);
            if (avo.isSet()){
            	throw IfcDataFormatException.valueOutOfRange("#"+ivo.getLineNum(),"#"+((IFCVO)o).getLineNum(),"SET");
            }
            final String propURI = getOntNS() + evo.getDerivedAttributeList().get(attributePointer).getLowerCaseName();
            EntityVO evorange = ent.get(ExpressReader.formatClassName(((IFCVO) o).getName()));

            OntProperty p = ontModel.getOntProperty(propURI);
            OntClass range=p.getRange().asClass();
            OntClass rclass = ontModel.getOntClass(getOntNS() + evorange.getName());
            if (!rclass.hasSuperClass(range)){
            	throw IfcDataFormatException.valueOutOfRange("#"+ivo.getLineNum(),"#"+((IFCVO)o).getLineNum(),range.getLocalName());
            }
            else{
            Resource r1=ResourceFactory.createResource(getBaseURI() + createLocalName(evorange.getName() + "_" + ((IFCVO) o).getLineNum()));

            getRdfWriter().triple(new Triple(r.asNode(), p.asNode(), r1.asNode()));
 //           if (logToFile)
 //               bw.write("*OK 1*: added property: " + r.getLocalName() + " - " + p.getLocalName() + " - " + r1.getLocalName() + "\r\n");
            }
            } else {
        	throw IfcDataFormatException.attributeOutOfBounds("#"+ivo.getLineNum()+"="+ivo.getFullLineAfterNum());
        }
        attributePointer++;
        return attributePointer;
    }

    @SuppressWarnings("unchecked")
    private int fillPropertiesHandleListObject(Resource r, EntityVO evo, int attributePointer, Object o,IFCVO ivo) throws IOException, IfcDataFormatException {

        final LinkedList<Object> tmpList = (LinkedList<Object>) o;
        LinkedList<String> literals = new LinkedList<String>();
        LinkedList<Resource> listRemembranceResources = new LinkedList<Resource>();
        LinkedList<IFCVO> IFCVOs = new LinkedList<IFCVO>();

        // process list
        for (int j = 0; j < tmpList.size(); j++) {
            Object o1 = tmpList.get(j);
            if (Character.class.isInstance(o1)) {
                Character c = (Character) o1;
                if (c != ',') {
              
                        LOGGER.log(Level.WARNING,"We found a character that is not a comma. That is odd. Check!" + "\r\n");
                }
            } else if (String.class.isInstance(o1)) {
                TypeVO t = typ.get(ExpressReader.formatClassName((String) o1));
                if (typeRemembrance == null) {
                    if (t != null) {
                        typeRemembrance = t;
                    } else {
                        literals.add(filterExtras((String) o1));
                    }
                } else {
                    if (t != null) {
                        if (t == typeRemembrance) {
                            // Ignore and continue with life
                        } else {
                            // Panic
                 
                                LOGGER.log(Level.WARNING,"Found two different types in one list. This is worth checking.\r\n ");
                        }
                    } else {
                        literals.add(filterExtras((String) o1));
                    }
                }
            } else if (IFCVO.class.isInstance(o1)) {
                if ((evo != null) && (evo.getDerivedAttributeList() != null) && (evo.getDerivedAttributeList().size() > attributePointer)) {
                    String propURI = evo.getDerivedAttributeList().get(attributePointer).getLowerCaseName();
                    OntProperty p = ontModel.getOntProperty(getOntNS() + propURI);
                    OntResource typerange = p.getRange();

                    if (typerange.asClass().hasSuperClass(listModel.getOntClass(getListns() + "OWLList"))) {
                        // EXPRESS LISTs
                        String listvaluepropURI = getOntNS() + typerange.getLocalName().substring(0, typerange.getLocalName().length() - 5);
                        OntResource listrange = ontModel.getOntResource(listvaluepropURI);

                        if (listrange.asClass().hasSuperClass(listModel.getOntClass(getListns() + "OWLList"))) {
                      
                                LOGGER.log(Level.WARNING,"Found supposedly unhandled ListOfList, but this should not be possible." + "\r\n");
                        } else {
                            fillClassInstanceList(tmpList, typerange, p, r);
                            j = tmpList.size() - 1;
                        }
                    } else {
                        // EXPRESS SETs
                        EntityVO evorange = ent.get(ExpressReader.formatClassName(((IFCVO) o1).getName()));
       //                 OntResource rclass = ontModel.getOntResource(getOntNS() + evorange.getName());
                        
                  /*      if(typeRemembrance!=null&&literals.size()>0){
                        	addSinglePropertyFromTypeRemembrance(r, p, literals.getFirst(), typeRemembrance);
                        	literals.clear();
                        	typeRemembrance=null;
                        }*/
                        
                        if (literals.size() > 0) {
                            String prop = getOntNS() + evo.getDerivedAttributeList().get(attributePointer).getLowerCaseName();
                            OntProperty pp = ontModel.getOntProperty(prop);
                            OntResource trange = pp.getRange();
                            if (typeRemembrance != null) {
                                if ((evo != null) && (evo.getDerivedAttributeList() != null) && (evo.getDerivedAttributeList().size() > attributePointer)) {
                                    if (trange.asClass().hasSuperClass(listModel.getOntClass(getListns() + "OWLList")))
                                        addRegularListProperty(r, p, literals, typeRemembrance,ivo);
                                    else {
                                        addSinglePropertyFromTypeRemembrance(r, p, literals.getFirst(), typeRemembrance,ivo);
                                        if (literals.size() > 1) {
                                            if (logToFile)
                                               LOGGER.log(Level.WARNING,"We are ignoring a number of literal values here." + "\r\n");
                                        }
                                    }
                                } else {
                               
                                       LOGGER.log(Level.WARNING ,"Nothing happened. Not sure if this is good or bad, possible or not." + "\r\n");
                                }
                                typeRemembrance = null;
                            } else if ((evo != null) && (evo.getDerivedAttributeList() != null) && (evo.getDerivedAttributeList().size() > attributePointer)) {
                                if (typerange.asClass().hasSuperClass(listModel.getOntClass(getListns() + "OWLList")))
                                    addRegularListProperty(r, p, literals, null,ivo);
                                else
                                    for (int i = 0; i < literals.size(); i++)
                                        createLiteralProperty(r, p, typerange, literals.get(i),ivo);
                            } else {
                         
                                     LOGGER.log(Level.WARNING ,"Nothing happened. Not sure if this is good or bad, possible or not." + "\r\n");
                            }
                            literals.clear();
                        }

                        
                        Resource r1=ResourceFactory.createResource(getBaseURI() + createLocalName(evorange.getName() + "_" + ((IFCVO) o1).getLineNum()));
                        getRdfWriter().triple(new Triple(r.asNode(), p.asNode(), r1.asNode()));
//                        if (logToFile)
//                            bw.write("*OK 5*: added property: " + r.getLocalName() + " - " + p.getLocalName() + " - " + r1.getLocalName() + "\r\n");
                    }
                } else {
        
                        LOGGER.log(Level.WARNING ,"Nothing happened. Not sure if this is good or bad, possible or not." + "\r\n");
                }
            } else if (LinkedList.class.isInstance(o1)) {
                if (typeRemembrance != null) {
                    LinkedList<Object> tmpListInList = (LinkedList<Object>) o1;
                    for (int jj = 0; jj < tmpListInList.size(); jj++) {
                        Object o2 = tmpListInList.get(jj);
                        if (Character.class.isInstance(o2)) {
                            if ((Character) o2 != ',') {
                  
                                    LOGGER.log(Level.WARNING ,"We found a character that is not a comma. That should not be possible" + "\r\n");
                            }
                        } else if (String.class.isInstance(o2)) {
                            literals.add(filterExtras((String) o2));
                        } else if (IFCVO.class.isInstance(o2)) {
                            // Lists of IFC entities
                   
                                 LOGGER.log(Level.WARNING ,"Nothing happened. Not sure if this is good or bad, possible or not." + "\r\n");
                        } else if (LinkedList.class.isInstance(o2)) {
                            // this happens only for types that are equivalent
                            // to lists (e.g. IfcLineIndex in IFC4_ADD1)
                            // in this case, the elements of the list should be
                            // treated as new instances that are equivalent to
                            // the correct lists
                            LinkedList<Object> tmpListInListInList = (LinkedList<Object>) o2;
                            for (int jjj = 0; jjj < tmpListInListInList.size(); jjj++) {
                                Object o3 = tmpListInListInList.get(jjj);
                                if (Character.class.isInstance(o3)) {
                                    if ((Character) o3 != ',') {
                         
                                             LOGGER.log(Level.WARNING ,"We found a character that is not a comma. That should not be possible" + "\r\n");
                                    }
                                } else if (String.class.isInstance(o3)) {
                                    literals.add(filterExtras((String) o3));
                                } else {
                     
                                         LOGGER.log(Level.WARNING ,"Nothing happened. Not sure if this is good or bad, possible or not." + "\r\n");
                                }
                            }

                            // exception. when a list points to a number of
                            // linked lists, it could be that there are multiple
                            // different entities are referenced
                            // example: #308=
                            // IFCINDEXEDPOLYCURVE(#309,(IFCLINEINDEX((1,2)),IFCARCINDEX((2,3,4)),IFCLINEINDEX((4,5)),IFCARCINDEX((5,6,7))),.F.);
                            // in this case, it is better to immediately print
                            // all relevant entities and properties for each
                            // case (e.g. IFCLINEINDEX((1,2))),
                            // and reset typeremembrance for the next case (e.g.
                            // IFCARCINDEX((4,5))).

                            if ((evo != null) && (evo.getDerivedAttributeList() != null) && (evo.getDerivedAttributeList().size() > attributePointer)) {

                                OntClass cl = ontModel.getOntClass(getOntNS() + typeRemembrance.getName());
                                Resource r1 = getResource(getBaseURI() + createLocalName(typeRemembrance.getName() + "_" + IDcounter), cl);
                                IDcounter++;
                                OntResource range = ontModel.getOntResource(getOntNS() + typeRemembrance.getName());

                                String[] primTypeArr = typeRemembrance.getPrimarytype().split(" ");

                                String primType = getOntNS() + primTypeArr[primTypeArr.length - 1].replace(";", "");
                                OntResource listrange = ontModel.getOntResource(primType);

                                List<Object> literalObjects = new ArrayList<Object>();
                                literalObjects.addAll(literals);
                                addDirectRegularListProperty(r1, range, listrange, literalObjects, 0,ivo);

                                // put relevant top list items in a list, which
                                // can then be parsed at the end of this method
                                listRemembranceResources.add(r1);
                            }

                            typeRemembrance = null;
                            literals.clear();
                        } else {
                
                                 LOGGER.log(Level.WARNING ,"Nothing happened. Not sure if this is good or bad, possible or not." + "\r\n");
                        }
                    }
                } else {
                    LinkedList<Object> tmpListInList = (LinkedList<Object>) o1;
                    for (int jj = 0; jj < tmpListInList.size(); jj++) {
                        Object o2 = tmpListInList.get(jj);
                        if (Character.class.isInstance(o2)) {
                            if ((Character) o2 != ',') {
                        
                                    LOGGER.log(Level.WARNING ,"We found a character that is not a comma. That should not be possible" + "\r\n");
                            }
                        } else if (String.class.isInstance(o2)) {
                            literals.add(filterExtras((String) o2));
                        } else if (IFCVO.class.isInstance(o2)) {
                            IFCVOs.add((IFCVO) o2);
                        } else if (LinkedList.class.isInstance(o2)) {
                        	String propURI = evo.getDerivedAttributeList().get(attributePointer).getLowerCaseName();
                        	 OntProperty p = ontModel.getOntProperty(getOntNS() + propURI);
                             OntResource typerange = p.getRange();
                        	throw IfcDataFormatException.valueOutOfRange("#"+ivo.getLineNum(),tmpListInList.toString(),typerange.getLocalName());
                //            if (logToFile)
                //                bw.write("*ERROR 19*: Found List of List of List. Code cannot handle that." + "\r\n");
                //            System.err.println("*ERROR 19*: Found List of List of List. Code cannot handle that.");
                        } else {
                    
                                 LOGGER.log(Level.WARNING ,"Nothing happened. Not sure if this is good or bad, possible or not." + "\r\n");
                        }
                    }
                    if ((evo != null) && (evo.getDerivedAttributeList() != null) && (evo.getDerivedAttributeList().size() > attributePointer)) {

                        String propURI = getOntNS() + evo.getDerivedAttributeList().get(attributePointer).getLowerCaseName();
                        OntProperty p = ontModel.getOntProperty(propURI);
                        OntClass typerange = p.getRange().asClass();

                        if (typerange.asClass().hasSuperClass(listModel.getOntClass(getListns() + "OWLList"))) {
                            String listvaluepropURI = typerange.getLocalName().substring(0, typerange.getLocalName().length() - 5);
                            OntResource listrange = ontModel.getOntResource(getOntNS() + listvaluepropURI);
                            Resource r1 = getResource(getBaseURI() + createLocalName(listvaluepropURI + "_" + IDcounter), listrange);
                   //         Resource r1 = getResource(getBaseURI() + listvaluepropURI + "_" + IDcounter, listrange);
                            IDcounter++;
                            List<Object> objects = new ArrayList<Object>();
                            if (IFCVOs.size() > 0) {
                                objects.addAll(IFCVOs);
                                OntResource listcontentrange = getListContentType(listrange.asClass());
                                addDirectRegularListProperty(r1, listrange, listcontentrange, objects, 1,ivo);
                            } else if (literals.size() > 0) {
                                objects.addAll(literals);
                                OntResource listcontentrange = getListContentType(listrange.asClass());
                                addDirectRegularListProperty(r1, listrange, listcontentrange, objects, 0,ivo);
                            }
                            listRemembranceResources.add(r1);
                        } else {
                        	if(IFCVOs.size()>0){
                        	throw IfcDataFormatException.valueOutOfRange("#"+ivo.getLineNum(),IFCVOs.toString(),typerange.getLocalName());
                        	}else if(literals.size()>0){
                        		throw IfcDataFormatException.valueOutOfRange("#"+ivo.getLineNum(),literals.toString(),typerange.getLocalName());
                        	}
                        }
                    }

                    literals.clear();
                    IFCVOs.clear();
                }
            } else {
          
                    LOGGER.log(Level.WARNING ,"We found something that is not an IFC entity, not a list, not a string, and not a character. Check!" + "\r\n");
            
            }
        }

        // interpret parse
        if (literals.size() > 0) {
            String propURI = getOntNS() + evo.getDerivedAttributeList().get(attributePointer).getLowerCaseName();
            OntProperty p = ontModel.getOntProperty(propURI);
            OntResource typerange = p.getRange();
            if (typeRemembrance != null) {
                if ((evo != null) && (evo.getDerivedAttributeList() != null) && (evo.getDerivedAttributeList().size() > attributePointer)) {
                    if (typerange.asClass().hasSuperClass(listModel.getOntClass(getListns() + "OWLList")))
                        addRegularListProperty(r, p, literals, typeRemembrance,ivo);
                    else {
                        addSinglePropertyFromTypeRemembrance(r, p, literals.getFirst(), typeRemembrance,ivo);
                        if (literals.size() > 1) {
                 
                                LOGGER.log(Level.WARNING ,"We are ignoring a number of literal values here." + "\r\n");
                        }
                    }
                } else {
              
                        LOGGER.log(Level.WARNING ,"Nothing happened. Not sure if this is good or bad, possible or not." + "\r\n");
                }
                typeRemembrance = null;
            } else if ((evo != null) && (evo.getDerivedAttributeList() != null) && (evo.getDerivedAttributeList().size() > attributePointer)) {
                if (typerange.asClass().hasSuperClass(listModel.getOntClass(getListns() + "OWLList")))
                    addRegularListProperty(r, p, literals, null,ivo);
                else
                    for (int i = 0; i < literals.size(); i++)
                        createLiteralProperty(r, p, typerange, literals.get(i),ivo);
            } else {
        
                    LOGGER.log(Level.WARNING ,"Nothing happened. Not sure if this is good or bad, possible or not." + "\r\n");
            }
        }
        if (listRemembranceResources.size() > 0) {
            if ((evo != null) && (evo.getDerivedAttributeList() != null) && (evo.getDerivedAttributeList().size() > attributePointer)) {
                String propURI = getOntNS() + evo.getDerivedAttributeList().get(attributePointer).getLowerCaseName();
                OntProperty p = ontModel.getOntProperty(propURI);
                addListPropertyToGivenEntities(r, p, listRemembranceResources);
            }
        }

        attributePointer++;
        return attributePointer;
    }


    // --------------------------------------
    // EVERYTHING TO DO WITH LISTS
    // --------------------------------------

    private void addSinglePropertyFromTypeRemembrance(Resource r, OntProperty p, String literalString, TypeVO typeremembrance,IFCVO ivo) throws IOException, IfcDataFormatException {
        OntResource range = ontModel.getOntResource(getOntNS() + typeremembrance.getName());

        if (range.isClass()) {
            if (range.asClass().hasSuperClass(expressModel.getOntClass(getExpressns() + "ENUMERATION"))) {
                // Check for ENUM
                addEnumProperty(r, p, range, literalString,ivo);
            } else if (range.asClass().hasSuperClass(expressModel.getOntClass(getExpressns() + "SELECT"))) {
                // Check for SELECT
   //             if (logToFile)
  //                  bw.write("*OK 24*: found subClass of SELECT Class, now doing nothing with it: " + p + " - " + range.getLocalName() + " - " + literalString + "\r\n");
                createLiteralProperty(r, p, range, literalString,ivo);
            } else if (range.asClass().hasSuperClass(listModel.getOntClass(getListns() + "OWLList"))) {
                // Check for LIST
            	throw IfcDataFormatException.valueOutOfRange("#"+ivo.getLineNum(),literalString,range.getLocalName());
            } else {
                createLiteralProperty(r, p, range, literalString,ivo);
            }
        } else {

                LOGGER.log(Level.WARNING ,"found other kind of property: " + p + " - " + range.getLocalName() + "\r\n");
        }
    }

    private void addEnumProperty(Resource r, Property p, OntResource range, String literalString,IFCVO ivo) throws IOException, IfcDataFormatException {
        for (ExtendedIterator<? extends OntResource> instances = range.asClass().listInstances(); instances.hasNext();) {
            OntResource rangeInstance = instances.next();
            if (rangeInstance.getProperty(RDFS.label).getString().equalsIgnoreCase(filterPoints(literalString))) {
                getRdfWriter().triple(new Triple(r.asNode(), p.asNode(), rangeInstance.asNode()));
 //               if (logToFile)
 //                   bw.write("*OK 2*: added ENUM statement " + r.getLocalName() + " - " + p.getLocalName() + " - " + rangeInstance.getLocalName() + "\r\n");
                return;
            }
        }
        throw IfcDataFormatException.valueOutOfRange("#"+ivo.getLineNum(),literalString,range.getLocalName());
    }

    private void addLiteralToResource(Resource r1, OntProperty valueProp, String xsdType, String literalString,IFCVO ivo) throws IOException, IfcDataFormatException {
        if (xsdType.equalsIgnoreCase("integer"))
            addLiteral(r1, valueProp, ResourceFactory.createTypedLiteral(literalString, XSDDatatype.XSDinteger));
        else if (xsdType.equalsIgnoreCase("double"))
            addLiteral(r1, valueProp, ResourceFactory.createTypedLiteral(literalString, XSDDatatype.XSDdouble));
        else if (xsdType.equalsIgnoreCase("hexBinary"))
            addLiteral(r1, valueProp, ResourceFactory.createTypedLiteral(literalString, XSDDatatype.XSDhexBinary));
        else if (xsdType.equalsIgnoreCase("boolean")) {
            if (literalString.equalsIgnoreCase(".F."))
                addLiteral(r1, valueProp, ResourceFactory.createTypedLiteral("false", XSDDatatype.XSDboolean));
            else if (literalString.equalsIgnoreCase(".T."))
                addLiteral(r1, valueProp, ResourceFactory.createTypedLiteral("true", XSDDatatype.XSDboolean));
            else if (logToFile)
            	throw IfcDataFormatException.valueOutOfRange("#"+ivo.getLineNum(),literalString,"Boolean");
        } else if (xsdType.equalsIgnoreCase("logical")) {
            if (literalString.equalsIgnoreCase(".F."))
                addProperty(r1, valueProp, expressModel.getResource(getExpressns() + "FALSE"));
            else if (literalString.equalsIgnoreCase(".T."))
                addProperty(r1, valueProp, expressModel.getResource(getExpressns() + "TRUE"));
            else if (literalString.equalsIgnoreCase(".U."))
                addProperty(r1, valueProp, expressModel.getResource(getExpressns() + "UNKNOWN"));
            else if (logToFile)
            	throw IfcDataFormatException.valueOutOfRange("#"+ivo.getLineNum(),literalString,"Logical");
        } else if (xsdType.equalsIgnoreCase("string"))
            addLiteral(r1, valueProp, ResourceFactory.createTypedLiteral(literalString, XSDDatatype.XSDstring));
        else
            addLiteral(r1, valueProp, ResourceFactory.createTypedLiteral(literalString));

 //       if (logToFile)
  //          bw.write("*OK 4*: added literal: " + r1.getLocalName() + " - " + valueProp + " - " + literalString + "\r\n");
    }

    // LIST HANDLING
    private void addDirectRegularListProperty(Resource r, OntResource range, OntResource listrange, List<Object> el, int mySwitch,IFCVO ivo) throws IOException, IfcDataFormatException {
        // OntResource range = p.getRange();

        if (range.isClass()) {
            // OntResource listrange = getListContentType(range.asClass());

            if (listrange.asClass().hasSuperClass(listModel.getOntClass(getListns() + "OWLList"))) {

                    LOGGER.log(Level.WARNING,"Found unhandled ListOfList" + "\r\n");
            } else {
                List<Resource> reslist = new ArrayList<Resource>();
                // createrequirednumberofresources
                for (int i = 0; i < el.size(); i++) {
                    if (i == 0)
                        reslist.add(r);
                    else {
                    	Resource r1 = getResource(getBaseURI() + createLocalName(range.getLocalName() + "_" + IDcounter), range);
                        reslist.add(r1);
                        IDcounter++;
                    }
                }

                if (mySwitch == 0) {
                    // bind the properties with literal values only if we are
                    // actually dealing with literals
                    List<String> literals = new ArrayList<String>();
                    for (int i = 0; i < el.size(); i++) {
                        literals.add((String) el.get(i));
                    }
                    addListInstanceProperties(reslist, literals, listrange,ivo);
                } else {
                    for (int i = 0; i < reslist.size(); i++) {
                        Resource r1 = reslist.get(i);
                        IFCVO vo = (IFCVO) el.get(i);
                        EntityVO evorange = ent.get(ExpressReader.formatClassName(((IFCVO) vo).getName()));
                  //      OntResource rclass = ontModel.getOntResource(getOntNS() + evorange.getName());
                  //      Resource r2 = getResource(getBaseURI() + evorange.getName() + "_" + ((IFCVO) vo).getLineNum(), rclass);
                        Resource r2=ResourceFactory.createResource(getBaseURI() + createLocalName(evorange.getName() + "_" + ((IFCVO) vo).getLineNum()));
                 //       Resource r2=ResourceFactory.createResource(getBaseURI() + evorange.getName() + "_" + ((IFCVO) vo).getLineNum());
 //                       if (logToFile)
//                            bw.write("*OK 21*: created resource: " + r2.getLocalName() + "\r\n");
                        IDcounter++;
                        getRdfWriter().triple(new Triple(r1.asNode(), listModel.getOntProperty(getListns() + "hasContents").asNode(), r2.asNode()));
//                        if (logToFile)
//                            bw.write("*OK 22*: added property: " + r1.getLocalName() + " - " + "-hasContents-" + " - " + r2.getLocalName() + "\r\n");

                        if (i < el.size() - 1) {
                            getRdfWriter().triple(new Triple(r1.asNode(), listModel.getOntProperty(getListns() + "hasNext").asNode(), reslist.get(i + 1).asNode()));
//                            if (logToFile)
//                                bw.write("*OK 23*: added property: " + r1.getLocalName() + " - " + "-hasNext-" + " - " + reslist.get(i + 1).getLocalName() + "\r\n");
                        }
                    }
                }
            }
        }
    }

    private void addRegularListProperty(Resource r, OntProperty p, List<String> el, TypeVO typeRemembranceOverride,IFCVO ivo) throws IOException, IfcDataFormatException {
        OntResource range = p.getRange();
        if (range.isClass()) {
            OntResource listrange = getListContentType(range.asClass());
            if (typeRemembranceOverride != null) {
                OntClass cla = ontModel.getOntClass(getOntNS() + typeRemembranceOverride.getName());
                listrange = cla;
            }

            if (listrange == null) {

                    LOGGER.log(Level.WARNING ,"We could not find what kind of content is expected in the LIST." + "\r\n");
            } else {
                if (listrange.asClass().hasSuperClass(listModel.getOntClass(getListns() + "OWLList"))) {
                	throw IfcDataFormatException.valueOutOfRange("#"+ivo.getLineNum(),el.toString(),range.getLocalName());
                } else {
                    List<Resource> reslist = new ArrayList<Resource>();
                    // createrequirednumberofresources
                    for (int ii = 0; ii < el.size(); ii++) {
                    	Resource r1 = getResource(getBaseURI() + createLocalName(range.getLocalName() + "_" + IDcounter), range);
//                        Resource r1 = getResource(getBaseURI() + range.getLocalName() + "_" + IDcounter, range);
                        reslist.add(r1);
                        IDcounter++;
                        if (ii == 0) {
                            getRdfWriter().triple(new Triple(r.asNode(), p.asNode(), r1.asNode()));
  //                          if (logToFile)
 //                               bw.write("*OK 7*: added property: " + r.getLocalName() + " - " + p.getLocalName() + " - " + r1.getLocalName() + "\r\n");
                        }
                    }
                    // bindtheproperties
                    addListInstanceProperties(reslist, el, listrange,ivo);
                }
            }
        }
    }

    private void createLiteralProperty(Resource r, OntResource p, OntResource range, String literalString,IFCVO ivo) throws IOException, IfcDataFormatException {
        String xsdType = getXSDTypeFromRange(range);
        if (xsdType == null) {
            xsdType = getXSDTypeFromRangeExpensiveMethod(range);
        }
        if (xsdType != null) {
            String xsdTypeCAP = Character.toUpperCase(xsdType.charAt(0)) + xsdType.substring(1);
            OntProperty valueProp = expressModel.getOntProperty(getExpressns() + "has" + xsdTypeCAP);
            String key = valueProp.toString() + ":" + xsdType + ":" + literalString;

 //           Resource r1 = propertyResourceMap.get(key);
 //           if (r1 == null) {
            Resource r1 = ResourceFactory.createResource(getBaseURI() + createLocalName(range.getLocalName() + "_" + IDcounter));
 //              Resource r1 = ResourceFactory.createResource(getBaseURI() + range.getLocalName() + "_" + IDcounter);
                getRdfWriter().triple(new Triple(r1.asNode(), RDF.type.asNode(), range.asNode()));
  //              if (logToFile)
 //                   bw.write("*OK 17*: created resource: " + r1.getLocalName() + "\r\n");
                IDcounter++;
                propertyResourceMap.put(key, r1);
                addLiteralToResource(r1, valueProp, xsdType, literalString,ivo);
 //           }
            getRdfWriter().triple(new Triple(r.asNode(), p.asNode(), r1.asNode()));
//            if (logToFile)
//                bw.write("*OK 3*: added property: " + r.getLocalName() + " - " + p.getLocalName() + " - " + r1.getLocalName() + "\r\n");
        } else {
  
                LOGGER.log(Level.WARNING ,"XSD type not found for: " + p + " - " + range.getURI() + " - " + literalString + "\r\n");
        }
    }

    private void addListPropertyToGivenEntities(Resource r, OntProperty p, List<Resource> el) throws IOException {
        OntResource range = p.getRange();
        if (range.isClass()) {
            OntResource listrange = getListContentType(range.asClass());

            if (listrange.asClass().hasSuperClass(listModel.getOntClass(getListns() + "OWLList"))) {
//                if (logToFile)
 //                   bw.write("*OK 20*: Handling list of list" + "\r\n");
                listrange = range;
            }
            for (int i = 0; i < el.size(); i++) {
                Resource r1 = el.get(i);
                Resource r2 = ResourceFactory.createResource(getBaseURI() + createLocalName(range.getLocalName() + "_" + IDcounter));
//                Resource r2 = ResourceFactory.createResource(getBaseURI() + range.getLocalName() + "_" + IDcounter); // was
                                                                                                                // listrange
                getRdfWriter().triple(new Triple(r2.asNode(), RDF.type.asNode(), range.asNode()));
 //               if (logToFile)
 //                   bw.write("*OK 14*: added property: " + r2.getLocalName() + " - rdf:type - " + range.getLocalName() + "\r\n");
                IDcounter++;
                Resource r3 = ResourceFactory.createResource(getBaseURI() + createLocalName(range.getLocalName() + "_" + IDcounter));
 //               Resource r3 = ResourceFactory.createResource(getBaseURI() + range.getLocalName() + "_" + IDcounter);

                if (i == 0) {
                    getRdfWriter().triple(new Triple(r.asNode(), p.asNode(), r2.asNode()));
//                    if (logToFile)
//                        bw.write("*OK 15*: added property: " + r.getLocalName() + " - " + p.getLocalName() + " - " + r2.getLocalName() + "\r\n");
                }
                getRdfWriter().triple(new Triple(r2.asNode(), listModel.getOntProperty(getListns() + "hasContents").asNode(), r1.asNode()));
 //               if (logToFile)
//                    bw.write("*OK 16*: added property: " + r2.getLocalName() + " - " + "-hasContents-" + " - " + r1.getLocalName() + "\r\n");

                if (i < el.size() - 1) {
                    getRdfWriter().triple(new Triple(r2.asNode(), listModel.getOntProperty(getListns() + "hasNext").asNode(), r3.asNode()));
//                    if (logToFile)
//                        bw.write("*OK 17*: added property: " + r2.getLocalName() + " - " + "-hasNext-" + " - " + r3.getLocalName() + "\r\n");
                }
            }
        }
    }

    private void fillClassInstanceList(LinkedList<Object> tmpList, OntResource typerange, OntProperty p, Resource r) throws IOException, IfcDataFormatException {
        List<Resource> reslist = new ArrayList<Resource>();
        List<IFCVO> entlist = new ArrayList<IFCVO>();

        // createrequirednumberofresources
        for (int i = 0; i < tmpList.size(); i++) {
            if (IFCVO.class.isInstance(tmpList.get(i))) {
            	Resource r1 = getResource(getBaseURI() + createLocalName(typerange.getLocalName() + "_" + IDcounter), typerange);
                reslist.add(r1);
                IDcounter++;
                entlist.add((IFCVO) tmpList.get(i));
                if (i == 0) {
                    getRdfWriter().triple(new Triple(r.asNode(), p.asNode(), r1.asNode()));
                }
            }
        }

        // bindtheproperties
        String listvaluepropURI = getOntNS() + typerange.getLocalName().substring(0, typerange.getLocalName().length() - 5);
        OntResource listrange = ontModel.getOntResource(listvaluepropURI);

        addClassInstanceListProperties(reslist, entlist, listrange);
    }

    private void addClassInstanceListProperties(List<Resource> reslist, List<IFCVO> entlist, OntResource listrange) throws IOException {
        OntProperty listp = listModel.getOntProperty(getListns() + "hasContents");
        OntProperty isfollowed = listModel.getOntProperty(getListns() + "hasNext");

        for (int i = 0; i < reslist.size(); i++) {
            Resource r = reslist.get(i);


            EntityVO evorange = ent.get(ExpressReader.formatClassName(entlist.get(i).getName()));
            if (evorange == null) {
                TypeVO typerange = typ.get(ExpressReader.formatClassName(entlist.get(i).getName()));
//               OntResource rclass = ontModel.getOntResource(getOntNS() + typerange.getName());
//                Resource r1 = getResource(getBaseURI() + typerange.getName() + "_" + entlist.get(i).getLineNum(), rclass);
                Resource r1=ResourceFactory.createResource(getBaseURI() + createLocalName(typerange.getName() + "_" + entlist.get(i).getLineNum()));
//                Resource r1=ResourceFactory.createResource(getBaseURI() + typerange.getName() + "_" + entlist.get(i).getLineNum());
                getRdfWriter().triple(new Triple(r.asNode(), listp.asNode(), r1.asNode()));
//                if (logToFile)
//                    bw.write("*OK 8*: created property: " + r.getLocalName() + " - " + listp.getLocalName() + " - " + r1.getLocalName() + "\r\n");
            } else {
//               OntResource rclass = ontModel.getOntResource(getOntNS() + evorange.getName());
//                Resource r1 = getResource(getBaseURI() + evorange.getName() + "_" + entlist.get(i).getLineNum(), rclass);
                Resource r1=ResourceFactory.createResource(getBaseURI() + createLocalName(evorange.getName() + "_" + entlist.get(i).getLineNum()));
//                Resource r1=ResourceFactory.createResource(getBaseURI() + evorange.getName() + "_" + entlist.get(i).getLineNum());
                getRdfWriter().triple(new Triple(r.asNode(), listp.asNode(), r1.asNode()));
 //               if (logToFile)
 //                   bw.write("*OK 9*: created property: " + r.getLocalName() + " - " + listp.getLocalName() + " - " + r1.getLocalName() + "\r\n");
            }

            if (i < reslist.size() - 1) {
                getRdfWriter().triple(new Triple(r.asNode(), isfollowed.asNode(), reslist.get(i + 1).asNode()));
 //               if (logToFile)
 //                   bw.write("*OK 10*: created property: " + r.getLocalName() + " - " + isfollowed.getLocalName() + " - " + reslist.get(i + 1).getLocalName() + "\r\n");
            }
        }
    }

    private void addListInstanceProperties(List<Resource> reslist, List<String> listelements, OntResource listrange,IFCVO ivo) throws IOException, IfcDataFormatException {
        // GetListType
        String xsdType = getXSDTypeFromRange(listrange);
        if (xsdType == null)
            xsdType = getXSDTypeFromRangeExpensiveMethod(listrange);
        if (xsdType != null) {
            String xsdTypeCAP = Character.toUpperCase(xsdType.charAt(0)) + xsdType.substring(1);
            OntProperty valueProp = expressModel.getOntProperty(getExpressns() + "has" + xsdTypeCAP);

            // Adding Content only if found
            for (int i = 0; i < reslist.size(); i++) {
                Resource r = reslist.get(i);
                String literalString = listelements.get(i);
                String key = valueProp.toString() + ":" + xsdType + ":" + literalString;
                Resource r2 = propertyResourceMap.get(key);
                if (r2 == null) {
                	r2 = ResourceFactory.createResource(getBaseURI() + createLocalName(listrange.getLocalName() + "_" + IDcounter));
                    getRdfWriter().triple(new Triple(r2.asNode(), RDF.type.asNode(), listrange.asNode()));
 //                   if (logToFile)
 //                       bw.write("*OK 19*: created resource: " + r2.getLocalName() + "\r\n");
                    IDcounter++;
                    propertyResourceMap.put(key, r2);
                    addLiteralToResource(r2, valueProp, xsdType, literalString,ivo);
                }
                getRdfWriter().triple(new Triple(r.asNode(), listModel.getOntProperty(getListns() + "hasContents").asNode(), r2.asNode()));
 //               if (logToFile)
 //                   bw.write("*OK 11*: added property: " + r.getLocalName() + " - " + "-hasContents-" + " - " + r2.getLocalName() + "\r\n");

                if (i < listelements.size() - 1) {
                    getRdfWriter().triple(new Triple(r.asNode(), listModel.getOntProperty(getListns() + "hasNext").asNode(), reslist.get(i + 1).asNode()));
 //                   if (logToFile)
 //                       bw.write("*OK 12*: added property: " + r.getLocalName() + " - " + "-hasNext-" + " - " + reslist.get(i + 1).getLocalName() + "\r\n");
                }
            }
        } else {

                LOGGER.log(Level.WARNING ,"XSD type not found for: " + listrange.getLocalName() + "\r\n");
        }
    }

    // HELPER METHODS
    private String filterExtras(String txt) {
 //       StringBuffer sb = new StringBuffer();

 //      for (int n = 0; n < txt.length(); n++) {
  //        char ch = txt.charAt(n);
 //           switch (ch) {
 //               case '\'':
 //                   break;
//                case '=':
//                    break;
 //               default:
 //              sb.append(ch);
//            }
 //       }
 //       return sb.toString();
    	if(txt.startsWith("\'")||txt.startsWith("\"")){
    		txt=txt.substring(1);
    	}
    	if(txt.endsWith("\'")||txt.endsWith("\"")){
    		txt=txt.substring(0, txt.length()-1);
    	}
    	return txt;
    }

    private String filterPoints(String txt) {
        StringBuffer sb = new StringBuffer();
        for (int n = 0; n < txt.length(); n++) {
            char ch = txt.charAt(n);
            switch (ch) {
                case '.':
                    break;
                default:
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    private Long toLong(String txt) {
        try {
            return Long.valueOf(txt);
        } catch (Exception e) {
            return Long.MIN_VALUE;
        }
    }

    private void addLiteral(Resource r, OntProperty valueProp, Literal l) {
        getRdfWriter().triple(new Triple(r.asNode(), valueProp.asNode(), l.asNode()));
    }

    private void addProperty(Resource r, OntProperty valueProp, Resource r1) {
        getRdfWriter().triple(new Triple(r.asNode(), valueProp.asNode(), r1.asNode()));
    }

    private OntResource getListContentType(OntClass range) throws IOException {
        if (range.asClass().getURI().equalsIgnoreCase(getExpressns() + "STRING_List") || range.asClass().hasSuperClass(expressModel.getOntClass(getExpressns() + "STRING_List")))
            return expressModel.getOntResource(getExpressns() + "STRING");
        else if (range.asClass().getURI().equalsIgnoreCase(getExpressns() + "REAL_List") || range.asClass().hasSuperClass(expressModel.getOntClass(getExpressns() + "REAL_List")))
            return expressModel.getOntResource(getExpressns() + "REAL");
        else if (range.asClass().getURI().equalsIgnoreCase(getExpressns() + "INTEGER_List") || range.asClass().hasSuperClass(expressModel.getOntClass(getExpressns() + "INTEGER_List")))
            return expressModel.getOntResource(getExpressns() + "INTEGER");
        else if (range.asClass().getURI().equalsIgnoreCase(getExpressns() + "BINARY_List") || range.asClass().hasSuperClass(expressModel.getOntClass(getExpressns() + "BINARY_List")))
            return expressModel.getOntResource(getExpressns() + "BINARY");
        else if (range.asClass().getURI().equalsIgnoreCase(getExpressns() + "BOOLEAN_List") || range.asClass().hasSuperClass(expressModel.getOntClass(getExpressns() + "BOOLEAN_List")))
            return expressModel.getOntResource(getExpressns() + "BOOLEAN");
        else if (range.asClass().getURI().equalsIgnoreCase(getExpressns() + "LOGICAL_List") || range.asClass().hasSuperClass(expressModel.getOntClass(getExpressns() + "LOGICAL_List")))
            return expressModel.getOntResource(getExpressns() + "LOGICAL");
        else if (range.asClass().getURI().equalsIgnoreCase(getExpressns() + "NUMBER_List") || range.asClass().hasSuperClass(expressModel.getOntClass(getExpressns() + "NUMBER_List")))
            return expressModel.getOntResource(getExpressns() + "NUMBER");
        else if (range.asClass().hasSuperClass(listModel.getOntClass(getListns() + "OWLList"))) {
            String listvaluepropURI = getOntNS() + range.getLocalName().substring(0, range.getLocalName().length() - 5);
            return ontModel.getOntResource(listvaluepropURI);
        } else {
   
                LOGGER.log(Level.WARNING,"did not find listcontenttype for : " + range.getLocalName() + "\r\n");

            return null;
        }
    }

    private String getXSDTypeFromRange(OntResource range) {
        if (range.asClass().getURI().equalsIgnoreCase(getExpressns() + "STRING") || range.asClass().hasSuperClass(expressModel.getOntClass(getExpressns() + "STRING")))
            return "string";
        else if (range.asClass().getURI().equalsIgnoreCase(getExpressns() + "REAL") || range.asClass().hasSuperClass(expressModel.getOntClass(getExpressns() + "REAL")))
            return "double";
        else if (range.asClass().getURI().equalsIgnoreCase(getExpressns() + "INTEGER") || range.asClass().hasSuperClass(expressModel.getOntClass(getExpressns() + "INTEGER")))
            return "integer";
        else if (range.asClass().getURI().equalsIgnoreCase(getExpressns() + "BINARY") || range.asClass().hasSuperClass(expressModel.getOntClass(getExpressns() + "BINARY")))
            return "hexBinary";
        else if (range.asClass().getURI().equalsIgnoreCase(getExpressns() + "BOOLEAN") || range.asClass().hasSuperClass(expressModel.getOntClass(getExpressns() + "BOOLEAN")))
            return "boolean";
        else if (range.asClass().getURI().equalsIgnoreCase(getExpressns() + "LOGICAL") || range.asClass().hasSuperClass(expressModel.getOntClass(getExpressns() + "LOGICAL")))
            return "logical";
        else if (range.asClass().getURI().equalsIgnoreCase(getExpressns() + "NUMBER") || range.asClass().hasSuperClass(expressModel.getOntClass(getExpressns() + "NUMBER")))
            return "double";
        else
            return null;
    }

    private String getXSDTypeFromRangeExpensiveMethod(OntResource range) {
        ExtendedIterator<OntClass> iter = range.asClass().listSuperClasses();
        while (iter.hasNext()) {
            OntClass superc = iter.next();
            if (!superc.isAnon()) {
                String type = getXSDTypeFromRange(superc);
                if (type != null)
                    return type;
            }
        }
        return null;
    }
    

    private Resource getResource(String uri, OntResource rclass) throws IfcDataFormatException {
        Resource r = resourceMap.get(uri);
        if (r == null) {
            r = ResourceFactory.createResource(uri);
            resourceMap.put(uri, r);
                getRdfWriter().triple(new Triple(r.asNode(), RDF.type.asNode(), rclass.asNode()));

        }
        
        return r;
    }
    
    private String createLocalName(String s){
    //	if(expIdInName==true){
    		return s;
   // 	}else{
   // 	 String result = UUID.nameUUIDFromBytes(s.getBytes()).toString();
   // 	 return result;
    //	}
    }

    public boolean isRemoveDuplicates() {
        return removeDuplicates;
    }

    public void setRemoveDuplicates(boolean removeDuplicates) {
        this.removeDuplicates = removeDuplicates;
    }


	public void setLogToFile(boolean logToFile) {
		// TODO Auto-generated method stub
		this.logToFile=logToFile;
	}

	public StreamRDF getRdfWriter() {
		return rdfWriter;
	}



	public void setRdfWriter(StreamRDF ttlWriter) {
		this.rdfWriter = ttlWriter;
	}



	public String getBaseURI() {
		return baseURI;
	}



	public String getOntNS() {
		return ontNS;
	}



	public static String getListns() {
		return listNS;
	}



	public static String getExpressns() {
		return expressNS;
	}

	public void setlogFile(String logFile) {
		if(logFile==null){
			logToFile=false;
		}else{
		// TODO Auto-generated method stub
		this.logFile=logFile;
		logToFile=true;
		}
	}



}
