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

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

/**
 * Vocabulary used in ifcOWL supplyment files. It is used to add some semantics which are missing in ifcOWL ontologies
 * @author Chi
 *
 */
public class IfcOWLSupplement {
	/**
	 * uri namespace of the vocabulary
	 */
	public static final String uri = "http://www.tue.nl/ddss/ifcOWL_supplementary#";
	
	 /**
	 * @return
	 */
	public static String getURI()
     { return uri; }


protected static final Resource resource( String local )
     { return ResourceFactory.createResource( uri + local ); }

protected static final Property property( String local )
     { return ResourceFactory.createProperty( uri, local ); }



 
 /**
 * The property to define whether a class in ifcOWL is an IFC entity.
 */
public static final Property isIfcEntity = property("isIfcEntity");
 /**
 * The property to define whether a class in ifcOWL is a top level entity (e.g. IfcRoot).
 */
public static final Property isTopLevelIfcEntity=property("isTopLevelIfcEntity");
 /**
 * The property index to define the position of an attribute for its entity.
 */
public static final Property index=property("index");
 /**
 * The property to define the value of a attribute is a list of array
 */
public static final Property isListOrArray=property("isListOrArray");
 /**
 * The property to define the value of a attribute is a set
 */
public static final Property isSet=property("isSet");
 /**
 * The property to define the value of a attribute is a optional
 */
public static final Property isOptional=property("isOptional");
 /**
 * The property to associate derived attribute with the entity
 */
public static final Property hasDeriveAttribute=property("hasDeriveAttribute");
 

}
