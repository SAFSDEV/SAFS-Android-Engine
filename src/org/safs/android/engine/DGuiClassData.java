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


import java.io.InputStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;

import org.safs.GuiClassData;
import org.safs.IndependantLog;
import org.safs.tools.drivers.DriverConstant;

/**
 * This class is primarily used to open and read in external data files that map class 
 * names to class types as well as mapping class types to the standard class processors.
 * <p>
 * Currently, we only really use the class name to processor mapping at this time.
 * <p>  
 * <ul>
 * <li>Example class mapping: android.widget.CompoundButton		= CompoundButton 
 * <li>Example  type mapping: CompoundButton					= Button
 * </ul>
 * Default class to type mapping is stored in "java_objects_map.dat".<br/>
 * Default types to library mapping is stored in "object_types_map.dat".<br/>
 * <p>
 * Custom class to type mapping is stored in "custom_java_objects_map.dat".<br/>
 * Custom types to library mapping is stored in "custom_object_types_map.dat".<br/>
 * 
 * FEB 07, 2013 (LeiWang)	Modify to let it be a subclass of org.safs.GuiClassData.<br/>
 **/
public class DGuiClassData extends GuiClassData{

	public static final String DEFAULT_JAVA_OBJECTS_MAP = "java_objects_map";
	public static final String DEFAULT_OBJECT_TYPES_MAP = "object_types_map";
	public static final String CUSTOM_JAVA_OBJECTS_MAP  = "custom_java_objects_map";
	public static final String CUSTOM_OBJECT_TYPES_MAP  = "custom_object_types_map";
	
	/** default type: "View" */
	public static final String DEFAULT_ANDROID_CLASS_TYPE      = "View";

	/** default library name: "View" */
	public static final String DEFAULT_ANDROID_OBJECT_TYPE      	= "View";
	
    /**
     * Used to get at Android objects via Solo or OS Widgets and Views
     */
    DSAFSTestRunner testrunner = null;
    
    public DGuiClassData(DSAFSTestRunner testrunner){
    	this.testrunner = testrunner;
    	IndependantLog.setDebugListener(testrunner);
    }
    
    /**
     * Must be set prior to calling debug or any of the static "get" functions.
     * Currently this is set by the DSAFSTestRunner itself during initialization.
     * @param _testrunner
     */
    public void setSAFSTestRunner(DSAFSTestRunner _testrunner){
    	testrunner = _testrunner;
    }        
    
    protected void debug(String message){
    	try{ if(testrunner!=null) testrunner.debug(message);}catch(Exception x){}
    }
	
	public void loadMappingFile() {
		InputStream in = null;
		
		if (classesmap == null) classesmap = new Properties();
		
		try{
			in = testrunner.readRawFile(DEFAULT_JAVA_OBJECTS_MAP);
			if(in != null){
				debug("DGCD.loading standard object class mapping: "+DEFAULT_JAVA_OBJECTS_MAP);
				classesmap.load(in);
				in.close();
			}else{
				debug("Fail DGCD.loading standard object class mapping: "+DEFAULT_JAVA_OBJECTS_MAP);
			}
			
			in = testrunner.readRawFile(CUSTOM_JAVA_OBJECTS_MAP);
			if(in != null){
				debug("DGCD.loading custom object class mapping: "+CUSTOM_JAVA_OBJECTS_MAP);
				Properties temp = new Properties();
				debug("DGCD.merging custom object class mapping.");
				temp.load(in);
				classesmap.putAll(temp);
				in.close();
			}else{
				debug("Fail DGCD.loading standard object class mapping: "+CUSTOM_JAVA_OBJECTS_MAP);
			}
			
			// init and fill Hashtables
			if(classassigns==null) classassigns = new Hashtable<String,Vector<String>>(25);

			String aclass    = null;
			String classtype = null;
			Vector<String> classlist = null;
			
			Enumeration<?> classes = classesmap.propertyNames();
			
			while (classes.hasMoreElements()){
				
				aclass    = (String) classes.nextElement();
				
				// may have a comma-separated list
				classtype = classesmap.getProperty(aclass).toUpperCase(); 					
				StringTokenizer toker = new StringTokenizer(classtype, DEFAULT_TYPE_SEPARATOR);
				String aclasstype = null;
				
				// should always have at least 1 token
				while(toker.hasMoreTokens()){
				aclasstype = toker.nextToken().trim();
				classlist = (Vector<String>) classassigns.get(aclasstype);
					if (classlist == null){
						classlist = new Vector<String>(10,10);
						classlist.addElement(aclass);
						classassigns.put(aclasstype, classlist);
					}
					else{
						classlist.addElement(aclass);
					}
				}
			}				
			// trim all Vectors to remove empty elements				
			classes = classassigns.keys();
			while(classes.hasMoreElements()){
				aclass = (String)classes.nextElement();
				classlist = (Vector<String>) classassigns.get(aclass);
				classlist.trimToSize();
				debug(aclass +": "+ classlist);
			}
		
			debug("DGCD: classmap content: "+classesmap.toString());
		}catch(Exception ex){
			debug("DGCD: Met "+ex.getClass().getSimpleName()+" : "+ex.getMessage());				
		}
		
		if (classtypesmap == null) classtypesmap = new Properties();
		try{
			in = testrunner.readRawFile(DEFAULT_OBJECT_TYPES_MAP);
			if(in != null){
				debug("DGCD.loading standard object types mapping: "+DEFAULT_OBJECT_TYPES_MAP);
				classtypesmap.load(in);
				in.close();
			}
			
			in = testrunner.readRawFile(CUSTOM_OBJECT_TYPES_MAP);
			if(in != null){
				debug("DGCD.loading custom object types mapping: "+CUSTOM_OBJECT_TYPES_MAP);
				Properties temp = new Properties();
				debug("DGCD.merging custom object types mapping.");
				temp.load(in);
				classtypesmap.putAll(temp);
				in.close();
			}
				
			debug("DGCD: typesmap content: "+classtypesmap.toString());
		}catch(Exception ex){
			debug("DGCD: Met "+ex.getClass().getSimpleName()+" : "+ex.getMessage());				
		}
	}

	/**
	 * Retrieves the class processor key we have stored for the provided class name (if any).
	 * We will cycle through all possible superclasses (except Object) looking for 
	 * a match to a superclass.
	 * <p>
	 * @param classname the actual classname sought as a known class type.
	 * 
	 * @param obj the object we are going to evaluate for "type"
	 * 
	 * @return the mapped class type(s) for the provided class name (i.e. android.widget.CheckBox).
	 *          <CODE>null</CODE> if no mapped type is found.  
	 *         This can be a comma-separated list of possible class type matches.
	 *         
	 * @author AUG 16, 2013 Refactored to support recursive and allowGeneric options.
	 * 
	 * @see #getMappedClassType(String, Object, boolean, boolean)
	 */
	public String getMappedClassType(String classname, Object theObject)
	{
		return getMappedClassType(classname, theObject, true, true);
	}
	
	/**
	 * Retrieves the class Type we have stored for the provided class name (if any).
	 * We will cycle through all possible superclasses (except Object) looking for 
	 * a match to a superclass if recursive is true.
	 * <p>
	 * Tool-dependent subclasses will most likely have to subclass this class and 
	 * provide similar mechanisms for evaluating the class hierarchy.
	 * 
	 * @param classname the actual classname sought as a known class type.
	 * 
	 * @param obj the object we are going to evaluate for "type"
	 * 
	 * @param recursive true to look for superclass Type matches. false for only a direct class=type match.
	 * 
	 * @param allowGeneric true to allow Type=Generic if no match is found for the provided classname.
	 *                     false -- no Generic Type will be returned if the classname does not map.
	 * 
	 * @return the class Type (i.e. JavaPanel) for the provided class name (i.e. javax.swing.JPanel).
	 *          <CODE>null</CODE> if no mapped type is found.  The classtype may be 
	 *          returned as a comma-separated list of all types supported for the class.
	 * 
	 * @author AUG 16, 2013 CarlNagle Refactored to support recursive and allowGeneric options. 
	 */
	public String getMappedClassType(String classname, Object theObject, boolean recursive, boolean allowGeneric)
	{
		if(classname == null){
			debug("DGCD classname: null, returning null mapped classtype.");
			return null;
		}
		debug("DGCD.getMappedClassType():classname: "+classname);
		
		if(classesmap==null) loadMappingFile();
		
		String type = super.getMappedClassType(classname, theObject, recursive, allowGeneric);
		return (DEFAULT_CLASS_TYPE.equals(type))? DEFAULT_ANDROID_CLASS_TYPE:type;
	}
	
	/**
	 * Returns the case-sensitive, generic object type for the given classType.
	 * For Android engine, use this method.
	 * Do NOT use {@link GuiClassData#getGenericObjectType(String)}<br>
	 */
	public static String getGenericObjectType(String classType){
		String libraryClassName = GuiClassData.getGenericObjectType(classType);
		return (DEFAULT_OBJECT_TYPE.equals(libraryClassName))? DEFAULT_ANDROID_OBJECT_TYPE:libraryClassName;
	}
	
	/**
	 * <p>
	 * Return a type for an object.
	 * The type is defined in file {@link #DEFAULT_JAVA_OBJECTS_MAP}.dat
	 * For one class, sometimes it can be mapped to multiple types, a string separated by
	 * {@value #DEFAULT_TYPE_SEPARATOR}. This method will return one of them.
	 * 
	 * @return String, The Type of this object.
	 * 
	 * @see #getMappedClassType(String, Object)
	 * @see #deduceOneClassType(String, String)
	 */
	public String getObjectClassType(Object theObject){
		if(theObject==null){
			return null;
		}
		String type = getMappedClassType(theObject.getClass().getName(), theObject);
		
		return deduceOneClassType(DriverConstant.ANDROID_CLIENT_TEXT, type);
	}
	
	//TODO For the following static method, maybe we can remove them and just use
	//that of GuiClassData. We just need to merge the content of array 
	//CONTAINER_TYPES, ALT_NAME_TYPES, TOOLTIP_CONTAINER_TYPES and POPUP_MENU_CLASSES
	//to that of GuiClassData
	
	// are these just containers?  or Invisible containers?
	// originally from org.safs.rational.ProcessContainer
	// not yet configured for Android
    public static final String[] CONTAINER_TYPES = {
    	"ViewGroup", 
    	"Window"
    };
    
	// originally from org.safs.rational.ProcessContainer
	// not yet configured for Android
    public static final String[] ALT_NAME_TYPES = {
    	"PushButton", 
    	"Label"
    };
	
	// originally from org.safs.rational.ProcessContainer
	// not yet configured for Android
    public static final String[] TOOLTIP_CONTAINER_TYPES = {
    	"JavaPanel",
    	"Panel"
    };
    
	// originally from org.safs.rational.ProcessContainer
	// not yet configured for Android
    public static final String[] POPUP_MENU_CLASSES = {
    	".Menupopup"
    };
	
	public static boolean isContainerType(String mappedClassType){
		try{
	        for(int i=0; i< CONTAINER_TYPES.length; i++) 
	            if(classtypeContainsClassType(mappedClassType, CONTAINER_TYPES[i])) return true;
		}catch(NullPointerException x){;}
        return false;
	}

	public static boolean isPopupMenuClass(String classname){
		try{
	        for(int i=0; i< POPUP_MENU_CLASSES.length; i++) 
	            if(POPUP_MENU_CLASSES[i].equals(classname)) return true;
		}catch(NullPointerException x){;}
        return false;
	}
	
	public static boolean isToolTipContainerType(String mappedClassType){
		try{
	        for(int i=0; i< TOOLTIP_CONTAINER_TYPES.length; i++) 
	            if(classtypeContainsClassType(mappedClassType, TOOLTIP_CONTAINER_TYPES[i])) return true;
		}catch(NullPointerException x){;}
        return false;
	}

    public static boolean isAltNameType(String type) {
        try{
            for(int i=0; i < ALT_NAME_TYPES.length; i++) 
	            if(classtypeContainsClassType(type, ALT_NAME_TYPES[i])) return true;
		}catch(NullPointerException x){;}
        return false;
    }		
}


