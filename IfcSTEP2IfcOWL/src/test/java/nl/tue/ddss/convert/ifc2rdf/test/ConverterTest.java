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
package nl.tue.ddss.convert.ifc2rdf.test;

import java.io.File;
import java.io.IOException;


import nl.tue.ddss.convert.ifc2rdf.Ifc2RdfConverter;



public class ConverterTest{
	public static void main(String[] args) throws IOException{
		Ifc2RdfConverter converter=new Ifc2RdfConverter();
//		converter.convert("C:/Users/Chi/Desktop/_Ramp_new_designed_1.1.ifc", "C:/Users/Chi/Desktop/_Ramp_new_designed_1.1.ifc.ttl",converter.DEFAULT_PATH, "http://www.something.com/some",false);
//		converter.convert("C:/Users/Chi/Desktop/_MainRoad_as_is_designed_1.1.ifc", "C:/Users/Chi/Desktop/_MainRoad_as_is_designed_1.1.ifc.ttl",null,converter.DEFAULT_PATH, false,false,false,true);
//		converter.convert("C:/Users/Chi/Desktop/lt_50MB/0912106-02windows_placement_inside_wall_all_1.ifc","C:/Users/Chi/Desktop/lt_50MB/0912106-02windows_placement_inside_wall_all_1.ifc.ttl",null,converter.DEFAULT_PATH, false,false,false,false);
		converter.convert("C:/Users/Chi/Desktop/ifcspff/air-terminal-element.ifc","C:/Users/Chi/Desktop/ifcspff/air-terminal-element.ttl",null,converter.DEFAULT_PATH, true,false,false,false);
//		ConverterTest.convert("C:/Users/Chi/Desktop/ifcspff");
//		ConverterTest.convert("C:/Users/Chi/Desktop/lt_50MB");
	}
	
	public static void convert(String inputFolder) throws IOException {
		File dir = new File(inputFolder);
		File[] directoryListing = dir.listFiles();
		for (File child : directoryListing) {
			if (child.getName().endsWith("ifc")) {
				try {
					Ifc2RdfConverter converter=new Ifc2RdfConverter();
					System.out.println(child.getName());
					converter.convert(child.getPath(), child.getPath()+".ttl", null,converter.DEFAULT_PATH,true,false,false,true);
				} catch (StackOverflowError e) {
					System.out.println("Fail");
					System.out.println(e.getCause());
				}

			}
		}

	}
}
