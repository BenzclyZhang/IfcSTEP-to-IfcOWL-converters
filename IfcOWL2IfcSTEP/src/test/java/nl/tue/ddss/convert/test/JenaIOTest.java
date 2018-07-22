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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

public class JenaIOTest {
	
	public static void main(String[] args) throws FileNotFoundException{
		Model model=ModelFactory.createDefaultModel();
		InputStream in=new FileInputStream("C:/Users/Chi/Desktop/air-terminal-element.ttl");
		model.read(in,null,"TTL");
		OutputStream out=new FileOutputStream("C:/Users/Chi/Desktop/air-terminal-element_1.jsonld");
		model.write(out,"JSON-LD");
	}

}
