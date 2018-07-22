package nl.tue.ddss.convert.ifc2rdf;

import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/** Command line interface of the converter
 * @author Chi
 *
 */
public class Ifc2RdfCmd {
	
	
	public static void main(String[] args) {

		Options options = new Options();

		Option baseuri = Option.builder("b").longOpt("baseuri").argName("uri").hasArg(true).required(false)
				.desc("set base uri for converted RDF instances").build();
		Option version = Option.builder("v").longOpt("version").argName("schema_version").hasArg(true).required(false)
				.desc("manually set used schema version (available values are \"IFC2X3_TC1\",\"IFC2X3_FINAL\",\"IFC4\",\"IFC4X1_RC3\",\"IFC4_ADD1\",\"IFC4_ADD2\")").build();
		Option log = new Option("l", "log", false, "generate log file");
		Option expid=new Option("e","expid",false,"use express id as a separate property for IFC object resources (by default express id is only baked into URI names of IFC object resources)");
		Option merge=new Option("m","merge",false,"merge duplicated objects (it might make roundtrip IFC file have less objects)");
		Option updns = new Option("u", "updns", false, "update ifcOWL namespaces using namespaces in referenced TTL ifcOWL files (only make sense if namespaces in TTL file changed)");
		
		options.addOption(baseuri);
		options.addOption(version);
		options.addOption(log);
		options.addOption(expid);
		options.addOption(merge);
		options.addOption(updns);

		CommandLineParser parser = new DefaultParser();
		CommandLine cmd;
		try {
			cmd = parser.parse(options, args);
			if (cmd.getArgs() == null || cmd.getArgs().length != 2) {

				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp("java -jar IfcSTEP2IfcOWL.jar <input.ifc> <output.xxx> [options]", options);
			} else {
				String baseURI = cmd.getOptionValue("baseuri");
				String ifcVersion=cmd.getOptionValue("version");
				Ifc2RdfConverter converter = new Ifc2RdfConverter();
				if (baseURI != null) {
					converter.convert(args[0], args[1], ifcVersion,baseURI, cmd.hasOption("log"),cmd.hasOption("expid"),cmd.hasOption("merge"),cmd.hasOption("updns"));
				} else {
					long start = System.currentTimeMillis();

					converter.convert(args[0], args[1], ifcVersion,converter.DEFAULT_PATH, 
							cmd.hasOption("log"),cmd.hasOption("expid"),cmd.hasOption("merge"),cmd.hasOption("updns"));
					long end = System.currentTimeMillis();
				//	System.out.println("Finished!");
					System.out.println("Total conversion time: " + ((float) (end - start)) / 1000 + " s");
				}
			}
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			System.err.println("Parsing command line failed.  Reason: " + e.getMessage());
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("java -jar IfcSTEP2IfcOWL.jar <input.ifc> <output.xxx> [options]", options);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
