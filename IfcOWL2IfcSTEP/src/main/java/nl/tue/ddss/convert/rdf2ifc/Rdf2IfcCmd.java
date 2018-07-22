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


import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import nl.tue.ddss.convert.IfcVersion;
import nl.tue.ddss.convert.IfcVersionException;


/** Command line interface for the converter
 * @author Chi
 *
 */
public class Rdf2IfcCmd {
	
	
public static void main(String[] args) throws IOException, IfcVersionException{

 	
	Options options=new Options();
	Option logfile = new Option("l", "logfile", false, "generate log file");
	Option version = Option.builder("v").longOpt("version").argName("version").hasArg(true).required(false)
			.desc("specify IFC schema version, if not specified, use default one parsed from model.").build();
	Option updns = new Option("u", "updns", false, "whether using update namespaces in ifcOWL files in resources folder"); 
//	Option converter = Option.builder("c").longOpt("converter").argName("type").hasArg(true).required(false)
//			.desc("set which converter to use").build();
    options.addOption(logfile).addOption(version).addOption(updns);
	
		try { 	
		CommandLineParser parser = new DefaultParser();
		CommandLine cmd;

				cmd = parser.parse(options, args);
		if(cmd.getArgs()==null||cmd.getArgs().length!=2){
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("java -jar IfcOWL2IfcSTEP.jar <input.xxx> <output.ifc> [options]", options);
			
		}else{	
			Rdf2IfcCmd convert=new Rdf2IfcCmd();
			long start=System.currentTimeMillis();
			if(cmd.hasOption("version")){
				String v=cmd.getOptionValue("version");
				IfcVersion ifcVersion=IfcVersion.getIfcVersion(v);
				convert.convert(args[0], args[1],cmd.hasOption("logfile"),ifcVersion,cmd.hasOption("updns"));
			}else{
				convert.convert(args[0], args[1],cmd.hasOption("logfile"),null,cmd.hasOption("updns"));
			}
				
				long end=System.currentTimeMillis();
		    	System.out.println("Total conversion time: "+((float)(end-start))/1000+" s");
		}
	} catch (ParseException e) {
		// TODO Auto-generated catch block
		System.err.println( "Parsing command line failed.  Reason: " + e.getMessage() );
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("java -jar IfcOWL2IfcSTEP.jar <input.xxx> <output.ifc> [options]", options);
	}
}


/**Convert ifcOWL file to IFC STEP file.
 * @param inputFile URI or file path of the input TTL file.
 * @param outputFile output path of the output IFC STEP file.
 * @param logFile defines whether generates log file.
 * @param version the version of the IFC, if it is null, then the converter automatic derive IFC version from the RDF file.
 * @param updns defines whether the used ifcOWL namespace has been updated, default is false. It only makes sense when the IFC RDF file uses a new namespace other than the default one (starts with http://openbimstandards.org/standards/ifcowl/), and the corresponding ifcOWL ontology in the src/main/resources directory is also updated with the new namespace. 
 * @throws IOException 
 */
public void convert(String inputFile, String outputFile,boolean logFile,IfcVersion version,boolean updns) throws IOException {
	Rdf2IfcConverter writer=new StreamIfcWriter(version,updns);
	writer.convert(inputFile,outputFile,logFile);	
}

	
}
