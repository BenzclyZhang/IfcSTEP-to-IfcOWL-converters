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

/**
 * @author Chi
 * The class to manage exceptions related to IFC version (e.g. cannot determine IFC version)
 */
@SuppressWarnings("serial")
public class IfcVersionException extends Exception {
	public IfcVersionException(String message) {
		super(message);
	}
}
