/*******************************************************************************
 * Copyright 2017 Chi Zhang
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
package nl.tue.ddss.convert.ifc2rdf;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Map;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFLanguages;
import org.openbimstandards.vo.EntityVO;
import org.openbimstandards.vo.TypeVO;

import be.ugent.RDFWriter;
import nl.tue.ddss.convert.Header;
import nl.tue.ddss.convert.HeaderParser;
import nl.tue.ddss.convert.IfcVersion;
import nl.tue.ddss.convert.IfcVersionException;

/**
 * The class to convert from IFC STEP file to RDF file.
 * @author Chi Zhang
 *
 */
public class Ifc2RdfConverter {

	/**
	 * 
	 */
	private String timeLog = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());

	/**
	 * Default namespace for the output RDF file.
	 */
	public final String DEFAULT_PATH = "http://linkedbuildingdata.net/ifc/resources" + timeLog + "/";
	



	
	
	
	/** Convert from an IFC STEP file to an ifcOWL file.
	 * @param inputFile File path of the IFC STEP file. 
	 * @param outputFile File path of the output RDF file. RDF syntax is determined by the extension of output file, supported extensions are *.ttl (Turtle) and *.nt (N-Triple).
	 */
	public void convert(String inputFile,String outputFile) throws IOException{
		convert(inputFile,outputFile,null);
	}
	
	/**
	 * @param inputFile File path of the IFC STEP file.
	 * @param outputStream Output stream of the RDF file. 
	 * @param lang The generated RDF syntax. Supported formats are Turtle, N-triples. It is based on the StreamRDFWriter in Jena https://jena.apache.org/documentation/io/streaming-io.html. 
	 */
	public void convert(String inputFile,OutputStream outputStream,Lang lang) throws IOException{
		convert(inputFile,outputStream,lang, null);
	}
	
	/**
	 * @param inputFile File path of the IFC STEP file.
	 * @param outputFile Output file path of the RDF file. RDF syntax is determined by the extension of output file, supported extensions are *.ttl (Turtle) and *.nt (N-Triple).
	 * @param baseURI Namespace for the output RDF file, by default it is "http://linkedbuildingdata.net/ifc/resources" + timeLog+"/".
	 */
	public void convert(String inputFile,String outputFile,String baseURI) throws IOException{
		convert(inputFile,outputFile,null,baseURI);
	}
	
	/**
	 * @param inputFile File path of the IFC STEP file.
	 * @param outputFile Output file path of the RDF file. RDF syntax is determined by the extension of output file, supported extensions are *.ttl (Turtle) and *.nt (N-Triple).
	 * @param ifcVersion Set the IFC version of the input IFC file, supported IFC versions are IFC2X3_TC1, IFC2X3_FINAL, IFC4, IFC4X1_RC3, IFC4_ADD1 and IFC4_ADD2. If it is null, the converter parses the header in IFC file to automatically determine the IFC version. Only three IFC versions are supported using this way, they are IFC2X3_TC1 (if header contains "IFC2X3"), IFC4x1_RC3 (if header contains "IFC4x1") and IFC4_ADD1 (if header contains "IFC4").
	 * @param baseURI Namespace for the output RDF file, by default it is "http://linkedbuildingdata.net/ifc/resources" + timeLog+"/".
	 */
	public void convert(String inputFile,String outputFile,String ifcVersion,String baseURI) throws IOException{
		convert(inputFile,outputFile,ifcVersion,baseURI,false,false,false,false);
	}
	
	/**
	 * @param inputFile File path of the IFC STEP file.
	 * @param outputStream Output stream of the RDF file.
	 * @param lang The generated RDF syntax. Supported formats are Turtle, N-triples. It is based on the StreamRDFWriter in Jena https://jena.apache.org/documentation/io/streaming-io.html. 
	 * @param baseURI Namespace for the output RDF file, by default it is "http://linkedbuildingdata.net/ifc/resources" + timeLog+"/".
	 */
	public void convert(String inputFile,OutputStream outputStream,Lang lang,String baseURI) throws IOException{

		convert(inputFile,outputStream,lang,null,baseURI);
	}
	
	/**
	 * @param inputFile File path of the IFC STEP file.
	 * @param outputStream Output stream of the RDF file.
	 * @param lang The generated RDF syntax. Supported formats are Turtle, N-triples. It is based on the StreamRDFWriter in Jena https://jena.apache.org/documentation/io/streaming-io.html. 
	 * @param ifcVersion Set the IFC version of the input IFC file, supported IFC versions are IFC2X3_TC1, IFC2X3_FINAL, IFC4, IFC4X1_RC3, IFC4_ADD1 and IFC4_ADD2. If it is null, the converter parses the header in IFC file to automatically determine the IFC version. Only three IFC versions are supported using this way, they are IFC2X3_TC1 (if header contains "IFC2X3"), IFC4x1_RC3 (if header contains "IFC4x1") and IFC4_ADD1 (if header contains "IFC4").
	 * @param baseURI Namespace for the output RDF file, by default it is "http://linkedbuildingdata.net/ifc/resources" + timeLog+"/".
	 */
	public void convert(String inputFile,OutputStream outputStream,Lang lang,String ifcVersion,String baseURI) throws IOException{
		convert(inputFile,outputStream,lang,ifcVersion,baseURI,null,false,false,false);
	}
	
	/**
	 * @param inputFile File path of the IFC STEP file.
	 * @param outputFile Output file path of the RDF file. RDF syntax is determined by the extension of output file, supported extensions are *.ttl (Turtle) and *.nt (N-Triple).
	 * @param ifcVersion Set the IFC version of the input IFC file, supported IFC versions are IFC2X3_TC1, IFC2X3_FINAL, IFC4, IFC4X1_RC3, IFC4_ADD1 and IFC4_ADD2. If it is null, the converter parses the header in IFC file to automatically determine the IFC version. Only three IFC versions are supported using this way, they are IFC2X3_TC1 (if header contains "IFC2X3"), IFC4x1_RC3 (if header contains "IFC4x1") and IFC4_ADD1 (if header contains "IFC4").
	 * @param baseURI Namespace for the output RDF file. If it is null, the default value is "http://linkedbuildingdata.net/ifc/resources" + timeLog+"/".
	 * @param logToFile Set whether log to file, default is false. If it is true, the log file is generated in the same directory of the output file.
	 * @param expid Set whether generate express id as a separate property for instances. Default is false.
	 * @param merge Set whether to remove duplicate objects. Default is false. If it is set to true, the round trip IFC file might have less objects.
	 * @param updateNS Set whether to update namespace for the ifcOWL ontology. Defaut value is false. If it is true, the converter will look up the namespace from the corresponding ifcOWL file in the resources folder and use it. Therefore the corresponding ifcOWL file must be updated before, otherwise it will still use the old namespace.
	 */
	public void convert(String inputFile, String outputFile, String ifcVersion,String baseURI, boolean logToFile,boolean expid,boolean merge,boolean updateNS)
			throws IOException {
			Lang lang=RDFLanguages.filenameToLang(outputFile);
	        convert(inputFile,outputFile,lang,ifcVersion,baseURI,logToFile,expid,merge,updateNS);
	}
	
	/**
	 * @param inputFile File path of the IFC STEP file.
	 * @param outputFile Output file path of the RDF file.
	 * @param lang The generated RDF syntax. It is based on the StreamRDFWriter in Jena https://jena.apache.org/documentation/io/streaming-io.html. Supported formats are Turtle, N-triples.
	 * @param ifcVersion Set the IFC version of the input IFC file, supported IFC versions are IFC2X3_TC1, IFC2X3_FINAL, IFC4, IFC4X1_RC3, IFC4_ADD1 and IFC4_ADD2. If it is null, the converter parses the header in IFC file to automatically determine the IFC version. Only three IFC versions are supported using this way, they are IFC2X3_TC1 (if header contains "IFC2X3"), IFC4x1_RC3 (if header contains "IFC4x1") and IFC4_ADD1 (if header contains "IFC4").
	 * @param baseURI Namespace for the output RDF file. If it is null, the default value is "http://linkedbuildingdata.net/ifc/resources" + timeLog+"/".
	 * @param logToFile Set whether log to file, default is false. If it is true, the log file is generated in the same directory of the output file.
	 * @param expid Set whether generate express id as a separate property for instances. Default is false.
	 * @param merge Set whether to remove duplicate objects. Default is false. If it is set to true, the round trip IFC file might have less objects.
	 * @param updateNS Set whether to update namespace for the ifcOWL ontology. Defaut value is false. If it is true, the converter will look up the namespace from the corresponding ifcOWL file in the resources folder and use it. Therefore the corresponding ifcOWL file must be updated before, otherwise it will still use the old namespace.
	 */
	public void convert(String inputFile, String outputFile, Lang lang,String ifcVersion,String baseURI, boolean logToFile,boolean expid,boolean merge,boolean updateNS)
			throws IOException {
			FileOutputStream out = new FileOutputStream(outputFile);
			String logFile=null;
			if(logToFile){
				logFile=outputFile.substring(0,outputFile.length())+".log";
			}
	        convert(inputFile,out,lang,ifcVersion,baseURI,logFile,expid,merge,updateNS);
	}
	
	/**
	 * @param inputFile inputFile File path of the IFC STEP file.
	 * @param outputStream outputStream Output stream of the RDF file.
	 * @param lang The generated RDF syntax. Supported formats are Turtle, N-triples. It is based on the StreamRDFWriter in Jena https://jena.apache.org/documentation/io/streaming-io.html. 
	 * @param ifcVersion Set the IFC version of the input IFC file, supported IFC versions are IFC2X3_TC1, IFC2X3_FINAL, IFC4, IFC4X1_RC3, IFC4_ADD1 and IFC4_ADD2. If it is null, the converter parses the header in IFC file to automatically determine the IFC version. Only three IFC versions are supported using this way, they are IFC2X3_TC1 (if header contains "IFC2X3"), IFC4x1_RC3 (if header contains "IFC4x1") and IFC4_ADD1 (if header contains "IFC4").
	 * @param baseURI Namespace for the output RDF file. If it is null, the default value is "http://linkedbuildingdata.net/ifc/resources" + timeLog+"/".
	 * @param logFile Path of the log file
	 * @param expid Set whether generate express id as a separate property for instances. Default is false.
	 * @param merge Set whether to remove duplicate objects. Default is false. If it is set to true, the round trip IFC file might have less objects.
	 * @param updateNS Set whether to update namespace for the ifcOWL ontology. Defaut value is false. If it is true, the converter will look up the namespace from the corresponding ifcOWL file in the resources folder and use it. Therefore the corresponding ifcOWL file must be updated before, otherwise it will still use the old namespace.
	 */
	@SuppressWarnings("unchecked")
	public void convert(String inputFile, OutputStream outputStream, Lang lang,String ifcVersion,String baseURI, String logFile,boolean expid,boolean merge,boolean updateNS)
			throws IOException {
		OntModel schema = null;
		InputStream in = null;

		try{
			if(updateNS){
		IfcVersion.initIfcNsMap();
		}else{
			IfcVersion.initDefaultIfcNsMap();
		}
		IfcVersion version=null;
		FileInputStream input=new FileInputStream(inputFile);
		Header header = HeaderParser.parseHeader(input);
		if(ifcVersion!=null){
			version=IfcVersion.getIfcVersion(ifcVersion);
		}else{
		    version=IfcVersion.getIfcVersion(header);
		}
		
		String ontNS=IfcVersion.IfcNSMap.get(version);

		// CONVERSION
			schema = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM_TRANS_INF);
			in = getClass().getClassLoader().getResourceAsStream(version.getLabel() + ".ttl");
			Reader reader=new InputStreamReader(in);
			schema.read(reader, null, "TTL");

			String expressTtl = "express.ttl";
			InputStream expressTtlStream = getClass().getClassLoader().getResourceAsStream(expressTtl);
			OntModel expressModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM_TRANS_INF);
			expressModel.read(expressTtlStream, null, "TTL");

			String rdfList = "list.ttl";
			InputStream rdfListStream = getClass().getClassLoader().getResourceAsStream(rdfList);
			OntModel listModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM_TRANS_INF);
			listModel.read(rdfListStream, null, "TTL");

			schema.add(expressModel);
			schema.add(listModel);
			// Model im = om.getDeductionsModel();

			InputStream fis = getClass().getClassLoader().getResourceAsStream("ent" + version.getLabel() + ".ser");
			ObjectInputStream ois = new ObjectInputStream(fis);
			Map<String, EntityVO> ent = null;
			try {
				ent = (Map<String, EntityVO>) ois.readObject();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} finally {
				ois.close();
			}

			fis = getClass().getClassLoader().getResourceAsStream("typ" + version.getLabel() + ".ser");
			ois = new ObjectInputStream(fis);
			Map<String, TypeVO> typ = null;
			try {
				typ = (Map<String, TypeVO>) ois.readObject();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} finally {
				ois.close();
			}
			InputStream inputStream=new FileInputStream(inputFile);
			RDFWriter conv = new RDFWriter(schema, expressModel, listModel, inputStream, baseURI, ent, typ, ontNS);
			conv.setRemoveDuplicates(merge);
			conv.setExpIdAsProperty(expid);


			conv.setlogFile(logFile);
			if (lang==null){
				lang=RDFLanguages.TURTLE;
			}
			String s = "# baseURI: " + baseURI;
			s += "\r\n# imports: " + ontNS.substring(0, ontNS.length()-1) + "\r\n\r\n";
			outputStream.write(s.getBytes());
			outputStream.flush();
			System.out.println("Start to convert...");
			conv.parseModel2Stream(outputStream, header,lang);
		} catch (IOException e1) {
			e1.printStackTrace();
		}  catch (IfcVersionException e) {
//			LOGGER.log(Level.SEVERE,"Caught IfcVersionException: "+e.getMessage());
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally {
			try {
				in.close();
			} catch (Exception e1) {
				e1.printStackTrace();
			}
			try {
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		}
	}








}
