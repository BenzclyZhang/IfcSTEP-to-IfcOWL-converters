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
 * Exception class for IFC data format problems.
 */
@SuppressWarnings("serial")
public class IfcDataFormatException extends Exception{
	


	/**
	 * @param message String message
	 */
	public IfcDataFormatException(String message) {
		super(message);
	}
	
	/**
	 * @param messageFormat String message format
	 * @param args argments for string message format
	 */
	public IfcDataFormatException(String messageFormat,Object...args){
		this(String.format(messageFormat, args));
	}
	

    /**Returns an exception that an IFC object has more attributes than allowed.
     * @param lineNumber express id or entire line of the IFC object.
     * @return
     */
    public static IfcDataFormatException attributeOutOfBounds(String lineNumber){
    	String messageFormat="Attribute out of bounds in line %1s: Object in IFC files has more attributes than it is allowed";
    	return new IfcDataFormatException(messageFormat,lineNumber);
    }
    
    
    /**Returns an exception that the entity type of an IFC objects does not exist in the schema
     * @param lineNumber express id or entire line of the IFC object
     * @param wrongEntity wrong entity type name
     * @return
     */
    public static IfcDataFormatException nonExistingEntity(String lineNumber,String wrongEntity){
    	String messageFormat="Entity not exist in line %1s: %2s is not an existing entity";
    	return new IfcDataFormatException(messageFormat,lineNumber,wrongEntity);
    }
    
    /**Returns an exception that the type of data does not exist in the schema
     * @param lineNumber express id or entire line of the ifc object which has the wrong data type
     * @param wrongType wrong type name
     * @return
     */
    public static IfcDataFormatException nonExistingType(String lineNumber,String wrongType){
    	String messageFormat="Type not exist in line %1s: %2s is not an existing TYPE";
    	return new IfcDataFormatException(messageFormat,lineNumber,wrongType);
    }
    
    /**Returns an exception that a non-existing IFC object is referenced.
     * @param lineNumber express id or entire line of the IFC object which references to the non-existing object
     * @param wrongLineNumber the express id of the non-existing object
     * @return
     */
    public static IfcDataFormatException referencingNonExistingObject(String lineNumber,String wrongLineNumber){
    	String messageFormat="Referencing to non-existing object in line %1s: Reference to non-existing line number %2s";
    	return new IfcDataFormatException(messageFormat,lineNumber,wrongLineNumber);
    } 
    
    /**Returns an exception that value is out of the range of the property.
     * @param lineNumber express id or entire line of the IFC object where the error happens
     * @param wrongValue the wrong value 
     * @param valueType the required value range
     * @return
     */
    public static IfcDataFormatException valueOutOfRange(String lineNumber,String wrongValue,String valueType){
    	String messageFormat="Value out of range in line %1s: %2s is not a %3s, which is not allowed";
    	return new IfcDataFormatException(messageFormat,lineNumber,wrongValue,valueType);
    }
    
}
