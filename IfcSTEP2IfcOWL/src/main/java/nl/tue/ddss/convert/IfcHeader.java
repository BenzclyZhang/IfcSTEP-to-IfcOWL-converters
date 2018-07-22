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
package nl.tue.ddss.convert;

import org.apache.jena.rdf.model.Resource;



import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.ResourceFactory;


/**
 * The vocabulary for IFC header
 * @author Chi
 *
 */
public class IfcHeader {
	/**
	 * The namespace of the vocabulary as a string
	 */
	public static final String uri ="http://www.openbim.org/ifcowl/ifcheader#";
	/**
	 * The prefix of the namespace
	 */
	public static final String ns="ifch";

    /** returns the URI for this schema
        @return the URI for this schema
    */
    public static String getURI()
        { return uri; }

    protected static final Resource resource( String local )
        { return ResourceFactory.createResource( uri + local );}

    protected static final Property property( String local )

        { return ResourceFactory.createProperty( uri, local ); }

 


    /**
     * description property
     */
    public static final Property description = property( "description" );
    /**
     * implementation level property
     */
    public static final Property implementation_level = property( "implementation_level" );
    /**
     * name property
     */
    public static final Property name = property( "name" );
    /**
     * time stamp property
     */
    public static final Property time_stamp = property( "time_stamp" );
    /**
     * author property
     */
    public static final Property author = property( "author" );
    /**
     * organization property
     */
    public static final Property organization = property( "organization" );
    /**
     * preprocessor version property
     */
    public static final Property preprocessor_version = property( "preprocessor_version" );
    /**
     * originating system property
     */
    public static final Property originating_system = property( "originating_system" );
    /**
     * authorization property
     */
    public static final Property authorization = property( "authorization" );
    /**
     * schema identifiers property
     */
    public static final Property schema_identifiers = property( "schema_identifiers" );
    

}
