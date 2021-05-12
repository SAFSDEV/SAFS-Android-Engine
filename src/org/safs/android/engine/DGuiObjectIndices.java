/**
 * Copyright (C) SAS Institute, All rights reserved.
 * General Public License: https://www.gnu.org/licenses/gpl-3.0.en.html
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
**/
package org.safs.android.engine;

import java.util.Hashtable;

public class DGuiObjectIndices {
    	
	public Hashtable classindex = new Hashtable();
	public Hashtable subclassindex = new Hashtable();
	public Hashtable typeindex = new Hashtable();
    int objectindex = 0;
    public Hashtable absoluteclassindex = new Hashtable();
    public Hashtable absolutetypeindex = new Hashtable();
    int absoluteobjectindex = 0;
    public Hashtable absolutesubclassindex = new Hashtable();
    
    public Hashtable idindex = new Hashtable();
    public Hashtable nameindex = new Hashtable();
    
    public DGuiObjectIndices(){ resetAllIndices();}

    static DSAFSTestRunner testrunner = null;
    /**
     * Must be set prior to calling debug or any of the static "get" functions.
     * Currently this is set by the DSAFSTestRunner itself during initialization.
     * @param _testrunner
     */
    public static void setSAFSTestRunner(DSAFSTestRunner _testrunner){
    	testrunner = _testrunner;
    }
    
	/** send a debug message to our testrunner to send to our remote controller. */
    void debug(String message){    	
    	try{ testrunner.debug(message); }
    	catch(Exception x){ System.out.println(message); }
    }
    
    
    public void resetAllIndices(){
	    classindex = new Hashtable();
	    subclassindex = new Hashtable();
	    typeindex = new Hashtable();
	    objectindex = 0;
	    absoluteclassindex = new Hashtable();
	    absolutetypeindex = new Hashtable();
	    absoluteobjectindex = 0;
	    absolutesubclassindex = new Hashtable();
	    idindex = new Hashtable();
	    nameindex = new Hashtable();
    }	    
    
    public DGuiObjectIndices copyAllIndices(){
    	DGuiObjectIndices copy = new DGuiObjectIndices();    	
    	copy.classindex = (Hashtable) classindex.clone();
    	copy.subclassindex= (Hashtable)subclassindex.clone();
    	copy.typeindex =(Hashtable) typeindex.clone();
    	copy.objectindex= objectindex;
    	copy.absoluteclassindex=(Hashtable) absoluteclassindex.clone();
    	copy.absolutesubclassindex=(Hashtable) absolutesubclassindex.clone();
    	copy.absolutetypeindex=(Hashtable) absolutetypeindex.clone();
    	copy.absoluteobjectindex=absoluteobjectindex;
    	copy.idindex=(Hashtable) idindex.clone();
    	copy.nameindex=(Hashtable) nameindex.clone();
    	return copy;
    }
    
    public void restoreAllIndices(DGuiObjectIndices copy){
    	resetAllIndices();
    	classindex = (Hashtable) copy.classindex.clone();
    	subclassindex = (Hashtable) copy.subclassindex.clone();
    	typeindex = (Hashtable) copy.typeindex.clone();
    	objectindex = copy.objectindex;
    	absoluteclassindex = (Hashtable) copy.absoluteclassindex.clone();
    	absolutesubclassindex = (Hashtable) copy.absolutesubclassindex.clone();
    	absolutetypeindex = (Hashtable) copy.absolutetypeindex.clone();
    	absoluteobjectindex = copy.absoluteobjectindex;
    	idindex = (Hashtable) copy.idindex;
    	nameindex = (Hashtable) copy.nameindex;
    }

    /************************************************************
    *
    * saveClassIndices ()
    *
    *************************************************************/
    public DGuiObjectIndices saveClassIndices(){
    	DGuiObjectIndices saved = new DGuiObjectIndices();
        saved.classindex = (Hashtable) classindex.clone();
        saved.subclassindex = (Hashtable) subclassindex.clone();
        return saved;
    }

    /************************************************************
    *
    * restoreClassIndices ()
    *
    *************************************************************/
    public void restoreClassIndices(DGuiObjectIndices saved){
    	classindex = (Hashtable) saved.classindex.clone();
    	subclassindex = (Hashtable) saved.subclassindex.clone();	    	
    }

    private int incrementIndex(Hashtable table, String key){
    	Integer oint = (Integer) table.get(key);
    	int val = oint == null? 1: oint.intValue()+1;
    	table.put(key, new Integer(val));
    	return val;
    }
    
    private int getIndex(Hashtable table, String key){
    	Integer cint = (Integer)table.get(key);
    	return cint == null ? 0 : cint.intValue();
    }

    /** increments both classindices and absoluteclassindices.
     * @return int classindex after increment */
    public int incrementClassIndex(String aclass){
    	incrementIndex(absoluteclassindex, aclass);
    	return incrementIndex(classindex, aclass);
    }
    
    /**
     * return the current classindex for the given class 
     * @param aclass
     * @return index or 0 if not found	     */
    public int getClassIndex(String aclass){
    	return getIndex(classindex, aclass);
    }

    /**
     * return the current absclassindex for the given class 
     * @param aclass
     * @return index or 0 if not found	     */
    public int getAbsClassIndex(String aclass){
    	return getIndex(absoluteclassindex, aclass);
    }

    /** increments both subclassindices and absolutesubclassindices.
     * @return int subclassindex after increment */
    public int incrementSubClassIndex(String aclass){
    	incrementIndex(absolutesubclassindex, aclass);
    	return incrementIndex(subclassindex, aclass);
    }

    /**
     * return the current subclassindex for the given class 
     * @param aclass
     * @return index or 0 if not found	     */
    public int getSubClassIndex(String aclass){
    	return getIndex(subclassindex, aclass);
    }

    /**
     * return the current abssubclassindex for the given class 
     * @param aclass
     * @return index or 0 if not found	     */
    public int getAbsSubClassIndex(String aclass){
    	return getIndex(absolutesubclassindex, aclass);
    }

    /** increments both typeindices and absolutetypeindices.
     * @return int typeindex after increment */
    public int incrementTypeIndex(String aclass){
    	incrementIndex(absolutetypeindex, aclass);
    	return incrementIndex(typeindex, aclass);
    }

    /**
     * return the current classindex for the given type 
     * @param atype
     * @return index or 0 if not found	     */
    public int getTypeIndex(String atype){
    	return getIndex(typeindex, atype);
    }

    /**
     * return the current absclassindex for the given type 
     * @param atype
     * @return index or 0 if not found	     */
    public int getAbsTypeIndex(String atype){
    	return getIndex(absolutetypeindex, atype);
    }

    /** increments both objectindex and absoluteobjectindex.
     * @return int objectindex after increment */
    public int incrementObjectIndex(){
    	++absoluteobjectindex;
    	return ++ objectindex;
    }

    /**
     * return the current objectindex 
     * @return objectindex */
    public int getObjectIndex(){
    	return objectindex;
    }

    /**
     * return the absoluteobjectindex 
     * @return absoluteobjectindex */
    public int getAbsObjectIndex(){
    	return absoluteobjectindex;
    }
    
    /** increments idindex.
     * @return int idindex after increment */
    public int incrementIdIndex(String anid){
    	return incrementIndex(idindex, anid);
    }

    /**
     * return the current idindex for the given id 
     * @param anid
     * @return index or 0 if not found	     */
    public int getIdIndex(String anid){
    	return getIndex(idindex, anid);
    }
    
    /** increments nameindex.
     * @return int nameindex after increment */
    public int incrementNameIndex(String aname){
    	return incrementIndex(nameindex, aname);
    }
    
    /**
     * return the current nameindex for the given name 
     * @param aname
     * @return index or 0 if not found	     */
    public int getNameIndex(String aname){
    	return getIndex(nameindex, aname);
    }
}
