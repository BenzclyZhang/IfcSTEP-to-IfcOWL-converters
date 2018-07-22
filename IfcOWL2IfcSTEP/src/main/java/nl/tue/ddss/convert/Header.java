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

import java.util.ArrayList;
import java.util.List;

/**
 * The class to store IFC Header data.
 * @author Chi Zhang
 *
 */
public class Header {
	//

	/**
	 * description as list of string
	 */
	private List<String> description = new ArrayList<String>();
	/**
	 * implementation level as string
	 */
	private String implementation_level;
	/**
	 * nama as string
	 */
	private String name;
	/**
	 * time stamp as string
	 */
	private String time_stamp;
	/**
	 * author as string
	 */
	private List<String> author = new ArrayList<String>();
	/**
	 * 
	 */
	private List<String> organization = new ArrayList<String>();
	/**
	 * 
	 */
	private String preprocessor_version;
	/**
	 * 
	 */
	private String originating_system;
	/**
	 * 
	 */
	private String authorization;
	/**
	 * 
	 */
	private List<String> schema_identifiers = new ArrayList<String>();

	/**
	 * @return
	 */
	public List<String> getDescription() {
		return description;
	}

	/**
	 * @param description
	 */
	public void setDescription(List<String> description) {
		this.description = description;
	}

	/**
	 * @return
	 */
	public String getImplementation_level() {
		return implementation_level;
	}

	/**
	 * @param implementation_level
	 */
	public void setImplementation_level(String implementation_level) {
		this.implementation_level = implementation_level;
	}

	/**
	 * @return
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return
	 */
	public String getTime_stamp() {
		return time_stamp;
	}

	/**
	 * @param time_stamp
	 */
	public void setTime_stamp(String time_stamp) {
		this.time_stamp = time_stamp;
	}

	/**
	 * @return
	 */
	public List<String> getAuthor() {
		return author;
	}

	/**
	 * @param author
	 */
	public void setAuthor(List<String> author) {
		this.author = author;
	}

	/**
	 * @return
	 */
	public List<String> getOrganization() {
		return organization;
	}

	/**
	 * @param organization
	 */
	public void setOrganization(List<String> organization) {
		this.organization = organization;
	}

	/**
	 * @return
	 */
	public String getPreprocessor_version() {
		return preprocessor_version;
	}

	/**
	 * @param preprocessor_version
	 */
	public void setPreprocessor_version(String preprocessor_version) {
		this.preprocessor_version = preprocessor_version;
	}

	/**
	 * @return
	 */
	public String getOriginating_system() {
		return originating_system;
	}

	/**
	 * @param originating_system
	 */
	public void setOriginating_system(String originating_system) {
		this.originating_system = originating_system;
	}

	/**
	 * @return
	 */
	public String getAuthorization() {
		return authorization;
	}

	/**
	 * @param authorization
	 */
	public void setAuthorization(String authorization) {
		this.authorization = authorization;
	}

	/**
	 * @return
	 */
	public List<String> getSchema_identifiers() {
		return schema_identifiers;
	}

	/**
	 * @param schema_identifiers
	 */
	public void setSchema_identifiers(List<String> schema_identifiers) {
		this.schema_identifiers = schema_identifiers;
	}

}
