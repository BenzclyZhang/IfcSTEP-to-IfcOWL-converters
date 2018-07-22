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
package nl.tue.ddss.convert.test;

import java.io.File;
import java.io.IOException;

import nl.tue.ddss.convert.rdf2ifc.Rdf2IfcCmd;




public class ConverterTest {
	
	public static void main(String[] args) throws IOException{

		Rdf2IfcCmd converter=new Rdf2IfcCmd();
		converter.convert("C:/Users/Chi/Desktop/ifcspff/air-terminal-element.ttl","C:/Users/Chi/Desktop/ifcspff_1/air-terminal-element.ifc",true,null,false);
//		converter.convert("C:/Users/Chi/Desktop/ifcspff/tessellation-with-blob-texture.ifc.ttl","C:/Users/Chi/Desktop/ifcspff_1/tessellation-with-blob-texture.ifc",false,null,false);
//	converter.convert("C:/Users/Chi/Desktop/_Ramp_new_designed_1.1.ifc.ttl", "C:/Users/Chi/Desktop/_Ramp_new_designed_1.1.ttl.ifc");
//		converter.convert("C:/Users/Chi/Desktop/_MainRoad_as_is_designed_1.1.ifc.ttl", "C:/Users/Chi/Desktop/_MainRoad_as_is_designed_1.1.ttl.ifc",false,null,false);
//			converter.convert("C:/Users/Chi/Dropbox/IFCtoRDF_IFCDOCSAMPLES/bsmartsamples/comparisonrun_IfcDoc_20170111/ifcowl_20170111/construction-scheduling-task.ttl", "C:/Users/Chi/Desktop/lt_50MB_1_1/construction-scheduling-task.ifc");
//			converter.convert("C:/Users/Chi/Dropbox/IFCtoRDF_IFCDOCSAMPLES/bsmartsamples/comparisonrun_IfcDoc_20170111/ifcowl_20170111/tessellation-with-blob-texture.ttl", "C:/Users/Chi/Desktop/lt_50MB_1_1/tessellation-with-blob-texture.ifc");
//			converter.convert("C:/Users/Chi/Dropbox/IFCtoRDF_IFCDOCSAMPLES/bsmartsamples/comparisonrun_IfcDoc_20170111/ifcowl_20170111/tessellation-with-image-texture.ttl", "C:/Users/Chi/Desktop/lt_50MB_1_1/ifcowl_20170111/tessellation-with-image-texture.ifc");
//			converter.convert("C:/Users/Chi/Dropbox/IFCtoRDF_IFCDOCSAMPLES/bsmartsamples/comparisonrun_IfcDoc_20170111/ifcowl_20170111/tessellation-with-individual-colors.ttl", "C:/Users/Chi/Desktop/lt_50MB_1_1/tessellation-with-individual-colors.ifc");
//			converter.convert("C:/Users/Chi/Dropbox/IFCtoRDF_IFCDOCSAMPLES/bsmartsamples/comparisonrun_IfcDoc_20170111/ifcowl_20170111/tessellation-with-pixel-texture.ttl", "C:/Users/Chi/Desktop/lt_50MB_1_1/tessellation-with-pixel-texture.ifc");
//		ConverterTest.convert("C:/Users/Chi/Dropbox/IFCtoRDF_IFCDOCSAMPLES/bsmartsamples/comparisonrun_IfcDoc_20170111/ifcowl_20170111","C:/Users/Chi/Desktop/lt_50MB_1_1/");
//		ConverterTest.convert("C:/Users/Chi/Desktop/ifcspff","C:/Users/Chi/Desktop/ifcspff_1/");
//		ConverterTest.convert("C:/Users/Chi/Desktop/lt_50MB","C:/Users/Chi/Desktop/lt_50MB_1/");
	}
	
	
	public static void convert(String inputFolder,String outputFolder) throws IOException {
		File dir = new File(inputFolder);
		File[] directoryListing = dir.listFiles();
		for (File child : directoryListing) {
			if (child.getName().endsWith("ttl")) {
				try {
					Rdf2IfcCmd converter=new Rdf2IfcCmd();
					converter.convert(child.getPath(), outputFolder+child.getName().substring(0, child.getName().length()-4)+".ifc",false,null,false);
				} catch (StackOverflowError e) {
					System.out.println("Fail");
					System.out.println(e.getCause());
				} catch (NullPointerException e1){
					System.out.println(child.getName()+": "+e1.getMessage());
					continue;
				} catch (ClassCastException e2){
					System.out.println(child.getName()+e2.getMessage());
					continue;
				}

			}
		}

	}

}
