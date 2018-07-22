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
 
package nl.tue.ddss.convert;

import java.util.HashMap;

/** 
 * @author Chi
 * The class to store IFC object data
 */
public class IfcObject {

    /**
     * The express id of the object
     */
    private Integer lineNum;
    /**
     * String value of the entire line except express id.
     */
    private String fullLineAfterNum;
    /**
     * The type name
     */
    private String name = null;
    /**
     * The hashmap between property position and property value.
     */
    private HashMap<Integer,Object> list;



    public String getFullLineAfterNum() {
        return fullLineAfterNum;
    }

    public void setFullLineAfterNum(String fullLineAfterNum) {
        this.fullLineAfterNum = fullLineAfterNum;
    }

    public Integer getLineNum() {
        return lineNum;
    }

    public void setLineNum(Integer lineNum) {
        this.lineNum = lineNum;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public HashMap<Integer,Object> getObjectList() {
        return list;
    }

    public void setList(HashMap<Integer,Object> list) {
        this.list = list;
    }

}
