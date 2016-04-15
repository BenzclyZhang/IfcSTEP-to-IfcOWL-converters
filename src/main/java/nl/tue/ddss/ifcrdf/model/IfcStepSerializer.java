package nl.tue.ddss.ifcrdf.model;

/******************************************************************************
 * Copyright (C) 2009-2015  BIMserver.org
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *****************************************************************************/

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.codec.binary.Hex;
import org.slf4j.LoggerFactory;
import org.topbraid.spin.util.JenaUtil;

import com.google.common.base.Charsets;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

public class IfcStepSerializer {
	private static final byte[] NEW_LINE = "\n".getBytes(Charsets.UTF_8);
	private static final org.slf4j.Logger LOGGER = LoggerFactory
			.getLogger(IfcStepSerializer.class);
	private static final boolean useIso8859_1 = false;
	private static final String NULL = "NULL";
	private static final String OPEN_CLOSE_PAREN = "()";
	private static final String ASTERISK = "*";
	private static final String PAREN_CLOSE_SEMICOLON = ");";
	private static final String DOT_0 = ".0";
	private static final String DASH = "#";
	private static final String DOT = ".";
	private static final String COMMA = ",";
	private static final String OPEN_PAREN = "(";
	private static final String CLOSE_PAREN = ")";
	private static final String BOOLEAN_UNDEFINED = ".U.";
	private static final String SINGLE_QUOTE = "'";
	private static final String BOOLEAN_FALSE = ".F.";
	private static final String BOOLEAN_TRUE = ".T.";
	private static final String DOLLAR = "$";

	private String headerSchema;
	private long writeCounter;

	private OutputStream outputStream;
	private Model model;
	private Model schema;
	private Resource OWLLIST;
	private Resource ENUMERATION;
	private Resource IFCENTITY;
	private Property ISMANY;
	private Property ISA;
	private Resource NUMBER;
	private Resource INTEGER;
	private Resource REAL;
	private Resource BOOLEAN;
	private Resource LOGICAL;
	private Resource STRING;
	private Resource SELECT;
	private Property ISSUBCLASSOF;

	private Property HASNEXT;
	private Property HASCONTENTS;
	private Property HASBOOLEAN;
	private Property HASSTRING;
	private Property HASLOGICAL;
	private Property HASDOUBLE;
	private Property HASINTEGER;
	private Property EXPRESS_TRUE;
	private Property EXPRESS_FALSE;
	private Property EXPRESS_UNDEFINED;
	private HashMap<Resource,List<Property>> class2Properties;

	protected void setHeaderSchema(String headerSchema) {
		this.headerSchema = headerSchema;
	}

	public IfcStepSerializer(Model model) throws FileNotFoundException {
		writeCounter=0;
		this.model = model;
		this.schema = ModelFactory.createDefaultModel();

		
		InputStream in = getClass().getResourceAsStream("IFC2X3_Schema.rdf");
		schema.read(in, null);
		ISSUBCLASSOF=schema.createProperty("http://www.co-ode.org/ontologies/list.owl#isSubClassOf");
		
		StmtIterator stmt=schema.listStatements(null,RDF.type,OWL.Class);
		List<Resource> classes=new ArrayList<Resource>();
		while(stmt.hasNext()){
			classes.add(stmt.next().getSubject());
		}
		for (Resource clazz:classes){
                Set<Resource> resources= JenaUtil.getAllSubClasses(clazz);
                for(Resource r:resources){
                	schema.add(r,ISSUBCLASSOF,clazz);
                }
		}
//		OutputStream out=new FileOutputStream("C:/users/chi/desktop/IFC2X3_Schema_modified.rdf");
//		schema.write(out);*/

		model.add(schema);

		ISA = model
				.getProperty("http://www.co-ode.org/ontologies/list.owl#isA");
		OWLLIST = model
				.getResource("http://www.co-ode.org/ontologies/list.owl#OWLList");
//		OWLLIST.addProperty(ISA, OWLLIST);
		ENUMERATION = model
				.getResource("http://purl.org/voc/express#ENUMERATION");
//		ENUMERATION.addProperty(ISA, ENUMERATION);
		IFCENTITY = model
				.getResource("http://www.buildingsmart-tech.org/ifcOWL/IFC2X3_TC1#IFCENTITY");
//		IFCENTITY.addProperty(ISA, IFCENTITY);
		ISMANY = model
				.getProperty("http://www.co-ode.org/ontologies/list.owl#isMany");
		
		NUMBER = model.getResource("http://purl.org/voc/express#NUMBER");
//		NUMBER.addProperty(ISA,NUMBER);
		INTEGER = model.getResource("http://purl.org/voc/express#INTEGER");
//		INTEGER.addProperty(ISA,INTEGER);
		REAL = model.getResource("http://purl.org/voc/express#REAL");
//		REAL.addProperty(ISA,REAL);
		BOOLEAN = model.getResource("http://purl.org/voc/express#BOOLEAN");
//		BOOLEAN.addProperty(ISA,BOOLEAN);
		LOGICAL = model.getResource("http://purl.org/voc/express#LOGICAL");
//		LOGICAL.addProperty(ISA, LOGICAL);
		STRING = model.getResource("http://purl.org/voc/express#STRING");
//		STRING.addProperty(ISA, STRING);
		SELECT=model.getResource("http://purl.org/voc/express#SELECT");

		HASNEXT = model
				.getProperty("http://www.co-ode.org/ontologies/list.owl#hasNext");
		HASCONTENTS = model
				.getProperty("http://www.co-ode.org/ontologies/list.owl#hasContents");
		HASBOOLEAN = model
				.getProperty("http://purl.org/voc/express#hasBoolean");
		HASSTRING = model.getProperty("http://purl.org/voc/express#hasString");
		HASLOGICAL = model
				.getProperty("http://purl.org/voc/express#hasLogical");
		HASDOUBLE = model.getProperty("http://purl.org/voc/express#hasDouble");
		HASINTEGER = model
				.getProperty("http://purl.org/voc/express#hasInteger");
		EXPRESS_TRUE = model.getProperty("http://purl.org/voc/express#TRUE");
		EXPRESS_FALSE = model.getProperty("http://purl.org/voc/express#FALSE");
		EXPRESS_UNDEFINED = model
				.getProperty("http://purl.org/voc/express#UNDEFINED");

		class2Properties=IfcRDFUtils.getProperties4Class(schema);
		
		

	}

	public static void main(String[] args) throws SerializerException,
			IOException {
		Model model = ModelFactory.createDefaultModel();
		System.out.println("Start to convert... ");
		long start=System.currentTimeMillis();
		InputStream in = new FileInputStream(args[0]);
		model.read(in, null, "TTL");
		
		IfcStepSerializer iss = new IfcStepSerializer(model);
		OutputStream out = new FileOutputStream(
				args[1]);
		iss.write(out);
		long end=System.currentTimeMillis();
		System.out.println("Conversion time: "+(end-start)/1000+" s");
		System.out.println("Done!");
		out.close();
	}

	public boolean write(OutputStream outputStream) throws SerializerException {
		try {
			this.outputStream = outputStream;
			writeHeader();
			String query = "\n"
					+ "prefix rdf: <"
					+ RDF.getURI()
					+ ">\n"
					+ "prefix list:<http://www.co-ode.org/ontologies/list.owl#>\n"
					+ "prefix ifc:<http://www.buildingsmart-tech.org/ifcOWL/IFC2X3_TC1#>\n"
					+ "select distinct ?instance where {\n"
					+ "   ?instance rdf:type  ?class .\n"
					+ "?class list:isA ifc:IFCENTITY .\n" + "}";
			ResultSet results = QueryExecutionFactory.create(query, model)
					.execSelect();
			
/*			String check = 
					 "prefix rdf: <"
					+ RDF.getURI()
					+ ">\n"
					+ "prefix list:<http://www.co-ode.org/ontologies/list.owl#>\n"
					+ "prefix ifc:<http://www.buildingsmart-tech.org/ifcOWL/IFC2X3_TC1#>\n"
					+"prefix owl: <http://www.w3.org/2002/07/owl#>\n"
					+ "SELECT distinct ?instance WHERE {\n"
					+ "   ?instance rdf:type  owl:Class .\n"
					+ "FILTER NOT EXISTS{\n"
					+"?instance list:isA ?y .\n" + "}\n"+"}";
			ResultSet result = QueryExecutionFactory.create(check, schema)
					.execSelect();
			ResultSetFormatter.out(result);*/
			int i=1;
			while (results.hasNext()) {
				write(results.next().getResource("instance"));
				writeCounter++;
				if(writeCounter==i*5000){
					System.out.println(writeCounter+" entities converted...");
					i++;
				}
			}
			System.out.println("In total: "+writeCounter+" entities.");
			writeFooter();
			return true;
		} catch (IOException e) {
			throw new SerializerException(e);
		}
	}

	private void writeFooter() throws IOException {
		println("ENDSEC;");
		println("END-ISO-10303-21;");
	}

	private void writeHeader() throws IOException {
		SimpleDateFormat dateFormatter = new SimpleDateFormat(
				"yyyy-MM-dd'T'HH:mm:ss");
		println("ISO-10303-21;");
		println("HEADER;");
		Date date = new Date();
		println("FILE_DESCRIPTION ((''), '2;1');");
		println("FILE_NAME ('', '" + dateFormatter.format(date)
				+ "', (''), (''), '', 'IfcOWL2IFC', '');");
		println("FILE_SCHEMA (('IFC2X3'));");
		println("ENDSEC;");
		println("DATA;");
		// println("//This program comes with ABSOLUTELY NO WARRANTY.");
		// println("//This is free software, and you are welcome to redistribute it under certain conditions. See www.bimserver.org <http://www.bimserver.org>");
	}

	private void println(String line) throws IOException {
		byte[] bytes = line.getBytes(Charsets.UTF_8);
		outputStream.write(bytes, 0, bytes.length);
		outputStream.write(NEW_LINE, 0, NEW_LINE.length);
	}

	private void print(String text) throws IOException {
		byte[] bytes = text.getBytes(Charsets.UTF_8);
		outputStream.write(bytes, 0, bytes.length);
	}



	private void write(Resource object) throws SerializerException, IOException {
		Resource ontClass = IfcRDFUtils.getClass(object, model);
		print(DASH);
		int convertedKey = getExpressId(object);
		if (convertedKey == -1) {
			throw new SerializerException(
					"Going to serialize an object with id -1 ("
							+ ontClass.getLocalName() + ")");
		}
		print(String.valueOf(convertedKey));
		print("= ");
		String upperCase = ontClass.getLocalName().toUpperCase();
		if (upperCase == null) {
			throw new SerializerException("Type not found: "
					+ ontClass.getLocalName());
		}
		print(upperCase);
		print(OPEN_PAREN);
		boolean isFirst = true;
		for (Property property : class2Properties.get(ontClass)) {
			List<Statement> stmts = model.listStatements(object, property,
					(RDFNode) null).toList();
			if (stmts.size() == 0) {
				if (!isFirst) {
					print(COMMA);
				}
				writeNull(object, property);
			} else if (stmts.size() > 1) {
				if (!isFirst) {
					print(COMMA);
				}
				writeSet(stmts,property);
			} else {
				if (!isFirst) {
					print(COMMA);
				}
				Resource o = stmts.get(0).getObject().asResource();
				if (isOwlList(o)) {
					writeList(o,property);
				}else if (isMany(property)) {
						writeSet(stmts,property);
				}
				else if (isIfcInstance(o)) {
					writeIfcInstance(o);
				} else if (isEnumeration(o)) {
					writeEnum(o);
				}  else {
					Resource clazz = o.getPropertyResourceValue(RDF.type);
					Resource range=property
							.getPropertyResourceValue(RDFS.range);
					
					if (clazz.equals(range)) {
						writePrimitive(o);
					} else {
						if(schema.contains(clazz, ISSUBCLASSOF, range)&&IfcRDFUtils.isDirectSubClassOf(range, SELECT, schema))
						writeEmbedded(clazz, o);
	//					else if(clazz.getURI().startsWith("http://purl.org/voc/express#")&&!IfcRDFUtils.isSubClassOf(range, clazz, schema)){
	//						writeEmbedded(clazz,o);
	//					}
						else
						writePrimitive(o);
					}
				}
			}isFirst=false;
		}
		println(PAREN_CLOSE_SEMICOLON);
	}
	
	private void writePrimitive(Resource val) throws IOException,
	SerializerException {
if (isLogical(val)) {
	if (val.hasProperty(HASLOGICAL, EXPRESS_TRUE)) {
		print(BOOLEAN_TRUE);
	} else if (val.hasProperty(HASLOGICAL, EXPRESS_FALSE)) {
		print(BOOLEAN_FALSE);
	} else if (val.hasProperty(HASLOGICAL, EXPRESS_UNDEFINED)) {
		print(BOOLEAN_UNDEFINED);
	}
} else if (isReal(val) || isNumber(val)) {
	Double valDouble = val.getProperty(HASDOUBLE).getObject()
			.asLiteral().getDouble();
	if ((valDouble).isInfinite() || ((valDouble).isNaN())) {
		LOGGER.info("Serializing infinite or NaN double as 0.0");
		print("0.0");
	} else {
		String string = valDouble.toString();
			if (string.endsWith(DOT_0)) {
				print(string.substring(0, string.length() - 1));
			} else {
			print(string);
			}
	}
} else if (isInteger(val)) {
	Integer valInteger = val.getProperty(HASINTEGER).getObject()
			.asLiteral().getInt();
	String string = valInteger.toString();
	if (string.endsWith(DOT_0)) {
		print(string.substring(0, string.length() - 2));
	} else {
		print(string);
	}
} else if (isBoolean(val)) {
	if (val.hasLiteral(HASBOOLEAN, true)) {
		print(BOOLEAN_TRUE);
	} else if (val.hasLiteral(HASBOOLEAN, false)) {
		print(BOOLEAN_FALSE);
	}
} else if (isString(val)) {
	print(SINGLE_QUOTE);
	String stringVal = val.getProperty(HASSTRING).getObject()
			.asLiteral().getString();
	for (int i = 0; i < stringVal.length(); i++) {
		char c = stringVal.charAt(i);
		if (c == '\'') {
			print("\'\'");
		} else if (c == '\\') {
			print("\\\\");
		} else if (c >= 32 && c <= 126) {
			// ISO 8859-1
			print("" + c);
		} else if (c < 255) {
			// ISO 10646 and ISO 8859-1 are the same < 255 , using
			// ISO_8859_1
			print("\\X\\"
					+ new String(Hex.encodeHex(Charsets.ISO_8859_1
							.encode(CharBuffer
									.wrap(new char[] { (char) c }))
							.array())).toUpperCase());
		} else {
			if (useIso8859_1) {
				// ISO 8859-1 with -128 offset
				ByteBuffer encode = Charsets.ISO_8859_1
						.encode(new String(
								new char[] { (char) (c - 128) }));
				print("\\S\\" + (char) encode.get());
			} else {
				// The following code has not been tested (2012-04-25)
				// Use UCS-2 or UCS-4

				// TODO when multiple sequential characters should be
				// encoded in UCS-2 or UCS-4, we don't really need to
				// add all those \X0\ \X2\ and \X4\ chars
				if (Character.isLowSurrogate(c)) {
					throw new SerializerException(
							"Unexpected low surrogate range char");
				} else if (Character.isHighSurrogate(c)) {
					// We need UCS-4, this is probably never happening
					if (i + 1 < stringVal.length()) {
						char low = stringVal.charAt(i + 1);
						if (!Character.isLowSurrogate(low)) {
							throw new SerializerException(
									"High surrogate char should be followed by char in low surrogate range");
						}
						try {
							print("\\X4\\"
									+ new String(
											Hex.encodeHex(Charset
													.forName("UTF-32")
													.encode(new String(
															new char[] {
																	c,
																	low }))
													.array()))
											.toUpperCase() + "\\X0\\");
						} catch (UnsupportedCharsetException e) {
							throw new SerializerException(e);
						}
						i++;
					} else {
						throw new SerializerException(
								"High surrogate char should be followed by char in low surrogate range, but end of string reached");
					}
				} else {
					// UCS-2 will do
					print("\\X2\\"
							+ new String(
									Hex.encodeHex(Charsets.UTF_16BE
											.encode(CharBuffer
													.wrap(new char[] { c }))
											.array())).toUpperCase()
							+ "\\X0\\");
				}
			}
		}
	}
	print(SINGLE_QUOTE);
} else if (isEnumeration(val)) {
	String enumVal = val.getLocalName();
	print("." + enumVal + ".");
} else {
	print(val == null ? "$" : val.toString());
}
}

	private void writeNull(Resource object, Property property)
			throws IOException {
		if (isMany(property)) {
			print(OPEN_CLOSE_PAREN);
		} else if (isDerived(property)) {
			print(ASTERISK);
		} else
			print(DOLLAR);
	}

	private void writeSet(List<Statement> stmts,Property property) throws IOException,
			SerializerException {
		boolean isFirst = true;
		print(OPEN_PAREN);
		for (Statement stmt : stmts) {
			Resource resource = stmt.getObject().asResource();
			if (!isFirst) {
				print(COMMA);
			}
			if (isIfcInstance(resource)) {
				writeIfcInstance(resource);
			}
			else if (isEnumeration(resource)) {
				writeEnum(resource);
			} else if (isOwlList(resource)) {
				writeList(resource,property);
			}
			else {
				Resource clazz = resource.getPropertyResourceValue(RDF.type);
				Resource range=property
						.getPropertyResourceValue(RDFS.range);
				if (clazz.equals(range)) {
					writePrimitive(resource);
				} else {
					if(schema.contains(clazz, ISSUBCLASSOF, range)&&IfcRDFUtils.isDirectSubClassOf(range, SELECT, schema))
					writeEmbedded(clazz, resource);
		//			else if(clazz.getURI().startsWith("http://purl.org/voc/express#")&&!IfcRDFUtils.isSubClassOf(range, clazz, schema)){
		//				writeEmbedded(clazz,resource);
		//			}
					else
					writePrimitive(resource);
				}
			}
			isFirst = false;
		}
		print(CLOSE_PAREN);
	}

	private boolean isDerived(Property property) {
		// TODO Auto-generated method stub
		return false;
	}

	private void writeEmbedded(Resource clazz, Resource object)
			throws SerializerException, IOException {
//		if(clazz.getURI().startsWith("http://purl.org/voc/express#")){
//			print("IFC"+clazz.getLocalName().toUpperCase());
//		}
//		else{
		print(clazz.getLocalName().toUpperCase());
//		}
		print(OPEN_PAREN);
		writePrimitive(object);
		print(CLOSE_PAREN);
	}

	private void writeList(Resource object,Property property) throws SerializerException,
			IOException {
		LinkedList<Resource> list = new LinkedList<Resource>();
		list = parseList(list, object);
		if (list.size() == 0) {
			print(OPEN_CLOSE_PAREN);
		} else {
			boolean isFirst = true;
			print(OPEN_PAREN);
			for (Resource resource : list) {
				if (!isFirst) {
					print(COMMA);
				}
				if (isIfcInstance(resource)) {
					writeIfcInstance(resource);
				}
				else if (isEnumeration(resource)) {
					writeEnum(resource);
				} else if (isOwlList(resource)) {
					writeList(resource,property);
				} else {
					Resource clazz = resource.getPropertyResourceValue(RDF.type);
					Resource range=property
							.getPropertyResourceValue(RDFS.range);
					if (range.getURI().endsWith("_List")){
						range=schema.getResource(range.getURI().substring(0,range.getURI().length()-5));
					}
					if (clazz.equals(range)) {
						writePrimitive(resource);
					} else {
						if(schema.contains(clazz, ISSUBCLASSOF, range)&&IfcRDFUtils.isDirectSubClassOf(range, SELECT, schema))
						writeEmbedded(clazz, resource);
//						else if(clazz.getURI().startsWith("http://purl.org/voc/express#")&&!IfcRDFUtils.isSubClassOf(range, clazz, schema)){
//							writeEmbedded(clazz,resource);
//						}
						else
						writePrimitive(resource);
					}

				}
				isFirst = false;
			}
			print(CLOSE_PAREN);
		}
	}

	private int getExpressId(Resource ifcResource) {
		return Integer.parseInt(ifcResource.getLocalName().substring(
				ifcResource.getLocalName().indexOf("_") + 1));
	}

	private LinkedList<Resource> parseList(LinkedList<Resource> list,
			Resource root) {
		if (root!=null&&root.hasProperty(HASCONTENTS)) {
			Resource element = root.getPropertyResourceValue(HASCONTENTS);
			list.add(element);
			parseList(list, root.getPropertyResourceValue(HASNEXT));
		}
		return list;
	}

	private boolean isEnumeration(Resource resource) {
		Resource clazz = resource.getPropertyResourceValue(RDF.type);
		if (clazz.equals(ENUMERATION)) {
			return true;
		} 
		else if(clazz.getPropertyResourceValue(ISA)!=null){
			if (clazz.getPropertyResourceValue(ISA).equals(ENUMERATION)) {
				return true;
		}else return false;	
		} else return false;
	}

	private boolean isIfcInstance(Resource resource) {
		Resource clazz = resource.getPropertyResourceValue(RDF.type);
		if (clazz.equals(IFCENTITY)) {
			return true;
		} 
		else if(clazz.getPropertyResourceValue(ISA)!=null){
			if (clazz.getPropertyResourceValue(ISA).equals(IFCENTITY)) {
				return true;
		}return false;	
		} else return false;
	}

	private boolean isReal(Resource resource) {
		Resource clazz = resource.getPropertyResourceValue(RDF.type);
		if (clazz.equals(REAL)) {
			return true;
		} 
		else if(clazz.getPropertyResourceValue(ISA)!=null){
			if (clazz.getPropertyResourceValue(ISA).equals(REAL)) {
				return true;
		}return false;	
		} else return false;
	}

	private boolean isInteger(Resource resource) {
		Resource clazz = resource.getPropertyResourceValue(RDF.type);
		if (clazz.equals(INTEGER)) {
			return true;
		} 
		else if(clazz.getPropertyResourceValue(ISA)!=null){
			if (clazz.getPropertyResourceValue(ISA).equals(INTEGER)) {
				return true;
		}return false;	
		} else return false;
	}

	private boolean isNumber(Resource resource) {
		Resource clazz = resource.getPropertyResourceValue(RDF.type);
		if (clazz.equals(NUMBER)) {
			return true;
		} 
		else if(clazz.getPropertyResourceValue(ISA)!=null){
			if (clazz.getPropertyResourceValue(ISA).equals(NUMBER)) {
				return true;
		}return false;	
		} else return false;
	}

	private boolean isBoolean(Resource resource) {
		Resource clazz = resource.getPropertyResourceValue(RDF.type);
		if (clazz.equals(BOOLEAN)) {
			return true;
		} 
		else if(clazz.getPropertyResourceValue(ISA)!=null){
			if (clazz.getPropertyResourceValue(ISA).equals(BOOLEAN)) {
				return true;
		}return false;	
		} else return false;
	}

	private boolean isLogical(Resource resource) {
		Resource clazz = resource.getPropertyResourceValue(RDF.type);
		if (clazz.equals(LOGICAL)) {
			return true;
		} 
		else if(clazz.getPropertyResourceValue(ISA)!=null){
			if (clazz.getPropertyResourceValue(ISA).equals(LOGICAL)) {
				return true;
		}return false;	
		} else return false;
	}

	private boolean isString(Resource resource) {
		Resource clazz = resource.getPropertyResourceValue(RDF.type);
		if (clazz.equals(STRING)) {
			return true;
		} 
		else if(clazz.getPropertyResourceValue(ISA)!=null){
			if (clazz.getPropertyResourceValue(ISA).equals(STRING)) {
				return true;
		}return false;	
		} else return false;
	}

	private boolean isOwlList(Resource resource) {
		Resource clazz = resource.getPropertyResourceValue(RDF.type);
		if (clazz.equals(OWLLIST)) {
			return true;
		} 
		else if(clazz.getPropertyResourceValue(ISA)!=null){
			if (clazz.getPropertyResourceValue(ISA).equals(OWLLIST)) {
				return true;
		}return false;	
		} else return false;
	}

	private boolean isMany(Property property) {
		if (property.hasLiteral(ISMANY, true)) {
			return true;
		} else
			return false;
	}

	private void writeIfcInstance(Resource object) throws IOException {
		print(DASH);
		print(Integer.toString(getExpressId(object)));
	}

	private void writeEnum(Resource val) throws SerializerException,
			IOException {
		if (val.getLocalName().equals(NULL)) {
			print(DOLLAR);
		} else {
			print(DOT);
			print(val.getLocalName());
			print(DOT);
		}
	}

}
