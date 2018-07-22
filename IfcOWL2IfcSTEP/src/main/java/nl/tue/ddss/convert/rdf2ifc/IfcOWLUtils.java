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


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.sparql.util.ModelUtils;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;



/** Function wrapper to process ifcOWL data
 * @author Chi
 *
 */
public class IfcOWLUtils extends ModelUtils {

	/** Returns class for an IFC instance .
	 * @param ifcObject resource of the IFC object
	 * @param schema the ifcOWL schema model
	 * @return
	 */
	public static Resource getClass(Resource ifcObject, Model schema) {
		Resource clazz = ifcObject.getPropertyResourceValue(RDF.type);
		return clazz;
	}



	/** Returns whether a class is a direct sub class of another class
	 * @param subclass the sub class resource 
	 * @param superclass the super class resource
	 * @param schema the referenced schema model
	 * @return
	 */
	public static boolean isDirectSubClassOf(Resource subclass, Resource superclass, Model schema) {
		return schema.contains(subclass, RDFS.subClassOf, superclass);
	}

	/** Returns the property position of an property (e.g. GlobalId) according to ifcOWL ontology and ifcOWL supplyment ontology 
	 * @param p property resource
	 * @param schema schema model (with both ifcOWL ontology and corresponding ifcOWL supplyment ontology)
	 * @return
	 */
	public static int getPropertyPosition(Property p, Model schema) {
		Resource clazz = schema.listStatements(p, RDFS.domain, (RDFNode) null).next().getObject().asResource();
		Property index = schema.getProperty(IfcOWLSupplement.getURI() + "index");
		int i = p.listProperties(index).next().getInt();
		Property isTopLevelIfcEntity = schema.getProperty(IfcOWLSupplement.getURI() + "isTopLevelIfcEntity");
		while (clazz.getProperty(isTopLevelIfcEntity) == null) {
			clazz = getIfcSuperClass(clazz, schema);
			i = i + getPropList(schema, clazz, index).size();
		}
		return i;
	}

	/** Returns a hashmap between IFC class resources and list of properties
	 * @param schema schema model (with both ifcOWL ontology and corresponding ifcOWL supplyment ontology)
	 * @return
	 */
	public static HashMap<Resource, List<Property>> getProperties4Class(Model schema) {
		HashMap<Resource, List<Property>> hashMap = new HashMap<Resource, List<Property>>();
		List<Resource> classes = new ArrayList<Resource>();
		Property index = schema.getProperty(IfcOWLSupplement.getURI() + "index");
		Property isIfcEntity = schema.getProperty(IfcOWLSupplement.getURI() + "isIfcEntity");
		Property isTopLevelIfcEntity = schema.getProperty(IfcOWLSupplement.getURI() + "isTopLevelIfcEntity");
		StmtIterator stmt = schema.listLiteralStatements(null, isIfcEntity, true);
		while (stmt.hasNext()) {
			classes.add(stmt.next().getSubject());
		}
		for (Resource clazz : classes) {
			if (clazz.hasLiteral(isTopLevelIfcEntity, true)) {
				List<Property> properties = getPropList(schema, clazz, index);
				hashMap.put(clazz, properties);
				List<Resource> subClasses = getSubClasses(clazz, schema, RDFS.subClassOf);
				if (subClasses.size() > 0) {
					for (Resource subClass : subClasses) {
						recurGetProperties(hashMap, subClass, schema, index, RDFS.subClassOf, properties);
					}
				}
			}

		}
		return hashMap;
	}
	
	/** Returns all sub classes for a class
	 * @param superClass the super class
	 * @param model the schema model
	 * @return
	 */
	public static Set<Resource> getAllSubClasses(Resource superClass,Model model) {
		return getAllSubClasses(superClass,model,new HashSet<Resource>());
	}
	

	private static Set<Resource> getAllSubClasses(Resource superClass, Model model, HashSet<Resource> hashSet) {
		StmtIterator stmts=model.listStatements(null,RDFS.subClassOf,superClass);
		while (stmts.hasNext()){
			Resource r=stmts.next().getSubject();
			hashSet.add(r);
			getAllSubClasses(r,model,hashSet);
		}
		return hashSet;
	}

	/** Returns whether a class is a sub class of another class 
	 * @param subClass the sub class
	 * @param superClass the super class
	 * @return
	 */
	public static boolean hasSuperClass(Resource subClass, Resource superClass) {
		return hasSuperClass(subClass, superClass, new HashSet<Resource>());
	}
	
	
	

	private static boolean hasSuperClass(Resource subClass, Resource superClass, Set<Resource> reached) {
		for(Statement s : subClass.listProperties(RDFS.subClassOf).toList()) {
			if(superClass.equals(s.getObject())) {
				return true;
			}
			else if(!reached.contains(s.getResource())) {
				reached.add(s.getResource());
				if(hasSuperClass(s.getResource(), superClass, reached)) {
					return true;
				}
			}
		}
		return false;
	}


	private static void recurGetProperties(HashMap<Resource, List<Property>> hashMap, Resource clazz, Model schema,
			Property index, Property ifcSubClassOf, List<Property> inherited) {
		List<Property> properties = new ArrayList<Property>();
		properties.addAll(inherited);
		properties.addAll(getPropList(schema, clazz, index));
		hashMap.put(clazz, properties);
		List<Resource> subClasses = getSubClasses(clazz, schema, ifcSubClassOf);
		if (subClasses.size() > 0) {
			for (Resource subClass : subClasses) {
				recurGetProperties(hashMap, subClass, schema, index, ifcSubClassOf, properties);
			}
		}
	}


	private static List<Property> getPropList(Model schema, Resource clazz, Property index) {
		List<Property> properties = new ArrayList<Property>();
		StmtIterator propIter = schema.listStatements(null, RDFS.domain, clazz);
		while (propIter.hasNext()) {
			Resource p = propIter.next().getSubject();
			if (schema.contains(p, index)) {
				properties.add(schema.getProperty(p.getURI()));
			}
		}
		properties = reArrangePropList(properties, index);
		return properties;
	}


	private static List<Property> reArrangePropList(List<Property> props, Property index) {
		List<Property> properties = new ArrayList<Property>();
		Property[] array = new Property[props.size()];
		for (Property prop : props) {
			int i = prop.listProperties(index).next().getInt();
			array[i] = prop;
		}
		for (Property p : array) {
			properties.add(p);
		}
		return properties;
	}


	private static List<Resource> getSubClasses(Resource superClass, Model model, Property ifcSubClassOf) {
		List<Resource> classes = new ArrayList<Resource>();
		StmtIterator stmt = model.listStatements(null, ifcSubClassOf, superClass);
		while (stmt.hasNext()) {
			classes.add(stmt.next().getSubject());
		}
		return classes;
	}
	

	private static Resource getIfcSuperClass(Resource subClass, Model schema) {
		Property isIfcEntity = schema.getProperty(IfcOWLSupplement.getURI() + "isIfcEntity");
		StmtIterator stmt = schema.listStatements(subClass, RDFS.subClassOf, (RDFNode) null);
		while (stmt.hasNext()) {
			Resource clazz = stmt.next().getResource();
			if (clazz.getProperty(isIfcEntity) != null) {
				return clazz;
			}
		}
		return null;
	}



}
