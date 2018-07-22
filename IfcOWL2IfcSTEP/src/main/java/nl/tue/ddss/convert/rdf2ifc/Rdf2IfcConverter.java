/*******************************************************************************
 * Copyright 2017 Chi Zhang
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package nl.tue.ddss.convert.rdf2ifc;


import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;

import org.apache.jena.ext.com.google.common.base.Charsets;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFLanguages;

import nl.tue.ddss.convert.Header;
import nl.tue.ddss.convert.IfcVersion;
import nl.tue.ddss.convert.IfcVersionException;
import nl.tue.ddss.convert.Namespace;

/**
 * @author Chi
 *Abstract class to convert IfcOWL data to IFC STEP data.
 */
public abstract class Rdf2IfcConverter {

	protected Model schema = ModelFactory.createDefaultModel();
	protected IfcVersion ifcVersion;
	protected OutputStream outputStream;

	protected static final byte[] NEW_LINE = "\n".getBytes(Charsets.UTF_8);
	protected static final String NULL = "NULL";
	protected static final String OPEN_CLOSE_PAREN = "()";
	protected static final String ASTERISK = "*";
	protected static final String PAREN_CLOSE_SEMICOLON = ");";
	protected static final String DOT_0 = ".0";
	protected static final String DASH = "#";
	protected static final String DOT = ".";
	protected static final String COMMA = ",";
	protected static final String OPEN_PAREN = "(";
	protected static final String CLOSE_PAREN = ")";
	protected static final String BOOLEAN_UNKNOWN = ".U.";
	protected static final String SINGLE_QUOTE = "'";
	protected static final String DOUBLE_QUOTE = "\"";
	protected static final String BOOLEAN_FALSE = ".F.";
	protected static final String BOOLEAN_TRUE = ".T.";
	protected static final String DOLLAR = "$";

	protected final Property ISIFCENTITY = schema.getProperty(IfcOWLSupplement.isIfcEntity.getURI());
	protected final Resource OWLLIST = schema.getResource(Namespace.LIST + "OWLList");
	protected final Resource ENUMERATION = schema.getResource(Namespace.EXPRESS + "ENUMERATION");
	protected final Property ISLISTORARRAY = schema.getProperty(IfcOWLSupplement.getURI() + "isListOrArray");
	protected final Property ISSET = schema.getProperty(IfcOWLSupplement.getURI() + "isSet");
	protected final Property ISOPTIONAL = schema.getProperty(IfcOWLSupplement.getURI() + "isOptional");
	protected final Property HASDERIVEATTRIBUTE = schema.getProperty(IfcOWLSupplement.getURI() + "hasDeriveAttribute");
	protected final Resource SELECT = schema.getResource(Namespace.EXPRESS + "SELECT");
	protected final Resource EXPRESS_TRUE = schema.getProperty(Namespace.EXPRESS + "TRUE");
	protected final Resource EXPRESS_FALSE = schema.getProperty(Namespace.EXPRESS + "FALSE");
	protected final Resource EXPRESS_UNKNOWN = schema.getProperty(Namespace.EXPRESS + "UNKNOWN");

	protected final Resource NUMBER = schema.getResource(Namespace.EXPRESS + "NUMBER");
	protected final Resource INTEGER = schema.getResource(Namespace.EXPRESS + "INTEGER");
	protected final Resource REAL = schema.getResource(Namespace.EXPRESS + "REAL");
	protected final Resource BOOLEAN = schema.getResource(Namespace.EXPRESS + "BOOLEAN");
	protected final Resource LOGICAL = schema.getResource(Namespace.EXPRESS + "LOGICAL");
	protected final Resource STRING = schema.getResource(Namespace.EXPRESS + "STRING");
	protected final Resource BINARY = schema.getResource(Namespace.EXPRESS + "BINARY");

	protected final Property HASNEXT = schema.getProperty(Namespace.LIST + "hasNext");
	protected final Property HASCONTENTS = schema.getProperty(Namespace.LIST + "hasContents");
	protected final Property HASBOOLEAN = schema.getProperty(Namespace.EXPRESS + "hasBoolean");
	protected final Property HASSTRING = schema.getProperty(Namespace.EXPRESS + "hasString");
	protected final Property HASLOGICAL = schema.getProperty(Namespace.EXPRESS + "hasLogical");
	protected final Property HASDOUBLE = schema.getProperty(Namespace.EXPRESS + "hasDouble");
	protected final Property HASINTEGER = schema.getProperty(Namespace.EXPRESS + "hasInteger");
	protected final Property HASHEXBINARY = schema.getProperty(Namespace.EXPRESS + "hasHexBinary");

	protected Header header = new Header();

	protected HashMap<Resource, List<Property>> class2Properties;

	/**Constructor
	 * 
	 */
	public Rdf2IfcConverter() {
		this(null);
	}

	/**Constructor
	 * @param ifcVersion IFC version for the conversion process
	 */
	public Rdf2IfcConverter(IfcVersion ifcVersion) {
		this(ifcVersion, false);
	}

	/**Constructor
	 * @param ifcVersion IFC version for the conversion process
	 * @param updateNS Set whether to update namespace for the ifcOWL ontology. Defaut value is false. If it is true, the converter will look up the namespace from the corresponding ifcOWL file in the resources folder and use it. Therefore the corresponding ifcOWL file must be updated before, otherwise it will still use the old namespace.
	 */
	public Rdf2IfcConverter(IfcVersion ifcVersion, boolean updateNS) {
		try {
			if (updateNS == false) {
				IfcVersion.initDefaultIfcNsMap();
				IfcVersion.initDefaultNsIfcMap();
			} else {
				IfcVersion.initIfcNsMap();
				IfcVersion.initNsIfcMap();
			}
		} catch (IfcVersionException e) {
			e.printStackTrace();
		}
		this.ifcVersion = ifcVersion;
	}

	/**
	 * @param inputUri File path of the input RDF file. RDF syntax is determined by the extension of input file.
	 * @param outputFileName File path of the output IFC file.
	 * @param logFile Set whether to generate log file.
	 * @throws IOException
	 */
	public void convert(String inputUri, String outputFileName, boolean logFile) throws IOException {
		convert(inputUri, outputFileName, RDFLanguages.filenameToLang(inputUri), logFile);
	}


	/**
	 * @param inputUri File path of the input RDF file. RDF syntax is determined by the extension of input file.
	 * @param outputFileName File path of the output IFC file.
	 * @throws IOException
	 */
	public void convert(String inputUri, String outputFileName) throws IOException {
		convert(inputUri, outputFileName, RDFLanguages.filenameToLang(inputUri), false);
	}
	
	
    
	/**
	 * @param inputStream Input stream of the RDF data .
	 * @param outputStream Output steam of the IFC STEP data. 
	 * @param lang RDF language, it cannot be null.
	 * @throws IOException 
	 */
	public void convert(InputStream inputStream, OutputStream outputStream,Lang lang) throws IOException {
		convert(inputStream,outputStream,lang,null);
		
	}



	/**
	 * @param inputUri File path of the input RDF file. RDF syntax is determined by the extension of input file.
	 * @param outputFileName File path of the output IFC file.
	 * @param lang RDF language .
	 * @param logFile set whether to genrate log file .
	 * @throws IOException
	 */
	public void convert(String inputUri, String outputFileName, Lang lang, boolean logFile) throws IOException {
		if (logFile == false) {
			convert(inputUri, outputFileName, lang, null);
		} else {
			String logFilePath = outputFileName.substring(0, outputFileName.length() - 4) + ".log";
			convert(inputUri, outputFileName, lang, logFilePath);
		}

	}
	


	/**
	 * @param inputUri File path of the input RDF file. RDF syntax is determined by the extension of input file, supported extensions are *.ttl (Turtle) and *.nt (N-Triple).
	 * @param outputFile File path of the output IFC file.
	 * @param lang RDF language, if it is null, it is determined by the extension of input file name.
	 * @param logFilePath file path of the log file.
	 * @throws IOException
	 */
	public void convert(String inputUri, String outputFile, Lang lang, String logFilePath) throws IOException {
		OutputStream outputStream = new FileOutputStream(outputFile);
		convert(inputUri, outputStream, lang, logFilePath);
	}
	
	/**
	 * @param inputUri File path of the input RDF file. RDF syntax is determined by the extension of input file, supported extensions are *.ttl (Turtle) and *.nt (N-Triple).
	 * @param outputStream out put stream of the output IFC file.
	 * @param lang RDF language, if it is null, it is determined by the extension of input file name.
	 * @param logFilePath file path of the log file.
	 * @throws IOException
	 */
    public abstract void convert(String inputUri, OutputStream outputStream, Lang lang, String logFilePath) throws IOException;

	/**
	 * @param inputStream Input stream of the RDF data .
	 * @param outputStream Output steam of the IFC STEP data. 
	 * @param lang RDF language, it cannot be null.
	 * @param logFilePath file path of the log file.
	 * @throws IOException
	 */
	public abstract void convert(InputStream inputStream, OutputStream outputStream, Lang lang,String logFilePath) throws IOException;



}
