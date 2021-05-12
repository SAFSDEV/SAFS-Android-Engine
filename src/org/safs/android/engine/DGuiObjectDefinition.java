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

import org.safs.sockets.RemoteException;

/**
 * 
 * @author Carl Nagle, SAS Institute, Inc
 * <p>
 * FEB 07, 2013 (LeiWang)	Add a static field DGuiClassData to help get type for an object.<br/>
 * FEB 21, 2013 (LeiWang)	Adjust to work with Recognition String in "Type=XXX" format.<br/>
 * JUN 05, 2013 (LeiWang)	Handle qualifier "ID=".<br/>
 * JUN 14, 2013 (LeiWang)	Handle qualifier "ID=xxx;Index=xxx", "Name=xxx;Index=xxx".<br/>
 */
public class DGuiObjectDefinition{

	public static final String TAG = "DGODefinition ";
	
	static DSAFSTestRunner testrunner = null;
	static DGuiClassData guiclassdata = null;
	
	public static final String FPSM   = ":FPSM:";
	public static final String TYPE    = "TYPE";
	public static final String CLASS   = "CLASS";    // for both TYPE and QUALIFIER
	public static final String SUBCLASS= "SUBCLASS"; // for both TYPE and QUALIFIER
	public static final String CURRENT = "CURRENTWINDOW";

	public static final String CAPTION          = "CAPTION";
	public static final String ID               = "ID";
	public static final String NAME             = "NAME";
	public static final String NAMECONTAINS     = "NAMECONTAINS";
	public static final String PATH             = "PATH";
	public static final String PROPERTY         = "PROPERTY";
	public static final String PROPERTYCONTAINS = "PROPERTYCONTAINS";
	public static final String TEXT             = "TEXT";
	public static final String TEXTCONTAINS     = "TEXTCONTAINS";

	public static final String CLASSINDEX     = "CLASSINDEX";
	public static final String INDEX          = "INDEX";
	public static final String OBJECTINDEX    = "OBJECTINDEX";
	public static final String ABSCLASSINDEX  = "ABSCLASSINDEX";
	public static final String ABSINDEX       = "ABSINDEX";
	public static final String ABSOBJECTINDEX = "ABSOBJECTINDEX";

	public static final int TYPEID    = 0;
	public static final int CLASSID   = 1;     // for both TYPE and QUALIFIER
	public static final int CURRENTID = 2;
	
	public static final int CAPTIONID          = 3;
	public static final int IDID               = 4;
	public static final int NAMEID             = 5;
	public static final int NAMECONTAINSID     = 6;
	public static final int PATHID             = 7;
	public static final int PROPERTYID         = 8;
	public static final int PROPERTYCONTAINSID = 9;
	public static final int TEXTID             = 10;
	public static final int TEXTCONTAINSID     = 11;
	public static final int CLASSINDEXID       = 12;
	public static final int INDEXID            = 13;
	public static final int OBJECTINDEXID      = 14;
	public static final int ABSCLASSINDEXID    = 15;
	public static final int ABSINDEXID         = 16;
	public static final int ABSOBJECTINDEXID   = 17;

	public static final int SUBCLASSID   	   = 18;     // for both TYPE and QUALIFIER
	public static final int SUBCLASSINDEXID    = 19;
	public static final int ABSSUBCLASSINDEXID = 20;
	
	public String rawRecString = null;
	public DGuiObjectDefinition parent = null;
	public DGuiObjectDefinition child = null;
	public boolean fpsmmode = false;
	public String objstring = null;
	public String[] qualifiers = null;
	public int[] qualType = null;
	public String[] qualValue = null;
	public DGuiObjectIndices matched_indices = null;
	
	/**
	 * definedByType is set to true only the RecognitionString is defined by "Type=XXX".
	 * This will affect how to match qualifier "Index=XXX"
	 * 
	 * @see #isMatchingObject(Object, DGuiObjectIndices)
	 */
	private boolean definedByType = false;
	public boolean isDefinedByType() {
		if(!definedByType){
			definedByType = isDefinedBy(new int[] {TYPEID});
		}
		return definedByType;
	}
	public void setDefinedByType(boolean definedByType) {
		this.definedByType = definedByType;
	}
	
	/**
	 * definedByID is set to true only the RecognitionString contains qualifier "ID=XXX"<br>
	 * This will affect how to match qualifier "Index=XXX"<br>
	 * Normally, ID should be unique for a component within an AUT. But sometimes, it is possible<br>
	 * that there are multiple components having the same ID, so we need qualifier "Index=XXX" to distinguish.<br>
	 * 
	 * @see #isMatchingObject(Object, DGuiObjectIndices)
	 */
	private boolean definedByID = false;
	public boolean isDefinedByID() {
		if(!definedByID){
			definedByID = isDefinedBy(new int[] {IDID});
		}
		return definedByID;
	}
	public void setDefinedByID(boolean definedByID) {
		this.definedByID = definedByID;
	}
	
	/**
	 * definedByName is set to true only the RecognitionString contains qualifier "Name=XXX"<br>
	 * This will affect how to match qualifier "Index=XXX"<br>
	 * Normally, Name should be unique for a component within an AUT. But sometimes, it is possible<br>
	 * that there are multiple components having the same NAME, so we need qualifier "Index=XXX" to distinguish.<br>
	 * 
	 * @see #isMatchingObject(Object, DGuiObjectIndices)
	 */
	private boolean definedByName = false;
	public boolean isDefinedByName() {
		if(!definedByName){
			definedByName = isDefinedBy(new int[] {NAMEID});
		}
		return definedByName;
	}
	public void setDefinedByName(boolean definedByName) {
		this.definedByName = definedByName;
	}
	
	/**
	 * @param by, an int array containing the XXXID to match, for example {@link #NAMEID}
	 * @return boolean, true, if one of {@link #qualifiers} match one of XXXID in array by.
	 */
	public boolean isDefinedBy(int[] by) {
		boolean matched = false;
		
		if(qualifiers!=null && by!=null){
			outloop:
			for(int i=0; i<qualifiers.length; i++){
				for(int j=0; j<by.length; j++){
					matched = (qualType[i]==by[j]);
					if(matched) break outloop;
				}
			}
		}
		return matched;
	}

	public DGuiObjectDefinition(String recString){
	    debug(TAG+"constructor processing recString: "+ recString);
	    this.rawRecString = recString;
	    fpsmmode = (recString.substring(0, FPSM.length()).toUpperCase()==FPSM);    
	    objstring = recString.substring(fpsmmode ? FPSM.length() : 0);
	    qualifiers = objstring.split(DGuiObjectVector.DEFAULT_QUALIFIER_SEPARATOR);
	    qualType = new int[qualifiers.length];  // hold integer id of type (ex: GOD.NAMEID)
	    qualValue = new String[qualifiers.length]; // hold expected/sought value
	
	    debug(TAG+"constructor counted "+ qualifiers.length +" qualifiers.");
	    String[] q = null;
	    String ucqual = null;
	    for(int i=0;i < qualifiers.length;i++){
	        q = qualifiers[i].split(DGuiObjectVector.DEFAULT_ASSIGN_SEPARATOR);
	        debug(TAG+"constructor processing qualifier "+ (i+1) +": "+  qualifiers[i]);
	        try{ qualValue[i] = q[1]; }
	        catch(ArrayIndexOutOfBoundsException x){
	        	// CurrrentWindow or another single item recognition
	        	qualValue[i] = q[0];
	        }
	        ucqual = q[0].toUpperCase();
	        if(CLASS.equals(ucqual)){
	        	qualType[i]=CLASSID;
	        }else
	        if(SUBCLASS.equals(ucqual)){
	        	qualType[i]=SUBCLASSID;
	        }else
	        if(TYPE.equals(ucqual)){
	        	qualType[i]=TYPEID;
	        }else
	        if(CURRENT.equals(ucqual)){
	        	qualType[i]=CURRENTID;
	        }else
	        if(INDEX.equals(ucqual)){
	        	qualType[i]=INDEXID;
	        }else
	        if(CAPTION.equals(ucqual)){
	        	qualType[i]=CAPTIONID;
	        }else
	        if(NAME.equals(ucqual)){
	        	qualType[i]=NAMEID;
	        }else
	        if(NAMECONTAINS.equals(ucqual)){
	        	qualType[i]=NAMECONTAINSID;
	        }else
	        if(PROPERTY.equals(ucqual)){
	        	qualType[i]=PROPERTYID;
	        }else
	        if(PROPERTYCONTAINS.equals(ucqual)){
	        	qualType[i]=PROPERTYCONTAINSID;
	        }else
	        if(TEXT.equals(ucqual)){
	        	qualType[i]=TEXTID;
	        }else
	        if(TEXTCONTAINS.equals(ucqual)){
	        	qualType[i]=TEXTCONTAINSID;
	        }else
	        if(ID.equals(ucqual)){
	        	qualType[i]=IDID;
	        }else
	        if(PATH.equals(ucqual)){
	        	qualType[i]=PATHID;
	        }else
	        if(CLASSINDEX.equals(ucqual)){
	        	qualType[i]=CLASSINDEXID;
	        }else
	        if(OBJECTINDEX.equals(ucqual)){
	        	qualType[i]=OBJECTINDEXID;
	        }else
	        if(ABSCLASSINDEX.equals(ucqual)){
	        	qualType[i]=ABSCLASSINDEXID;
	        }else
	        if(ABSINDEX.equals(ucqual)){
	        	qualType[i]=ABSINDEXID;
	        }else
	        if(ABSOBJECTINDEX.equals(ucqual)){
	        	qualType[i]=ABSOBJECTINDEXID;
	        }else{
		        debug(TAG+"unknown qualifier type for "+ qualifiers[i]);
	        }         
	    }
	}
	
    /**
     * Must be set prior to calling debug or any of the static "get" functions.
     * Currently this is set by the DSAFSTestRunner itself during initialization.
     * @param _testrunner
     */
    public static void setSAFSTestRunner(DSAFSTestRunner _testrunner){
    	testrunner = _testrunner;
    }

    /**
     * This is called in {@link DSAFSTestRunner#beforeStart()}.
     * 
     * @see DSAFSTestRunner#beforeStart()
     */
    public static void setGuiClassData(DGuiClassData _guiclassdata){
    	guiclassdata = _guiclassdata;
    }
    public static DGuiClassData getGuiClassData(){
    	return guiclassdata;
    }
    
	/** send a debug message to our testrunner to send to our remote controller. */
    void debug(String message){    	
    	try{ testrunner.debug(message); }
    	catch(Exception x){ System.out.println(message); }
    }
    
	// indices array should contain:
	public boolean isMatchingObject( Object obj, DGuiObjectIndices indices )throws RemoteException{ 
        boolean isMatch = true; // until made false
        String qvalue = null;
        String rvalue = null;
        String[] qarray = null;
        String aclass = DGuiObjectRecognition.getObjectClassName(obj);
        String asubclass = null;
        for(int i=0;isMatch && i < qualifiers.length;i++){
            qvalue = qualValue[i];  // qvalue already lower-case for "contains" qualifiers
            switch(qualType[i]){
                case CURRENTID: 
                    rvalue = CURRENT+ " not supported, class="+aclass;
                    break;
                case CLASSID: 
                    rvalue = aclass;
                    isMatch = qvalue.equals(rvalue);
                    break;
                case SUBCLASSID: 
                	String[] classes = DGuiObjectRecognition.getObjectSuperclassNames(obj);
                	boolean matched = false;
                	int numclasses = classes == null ? 0:classes.length;
                	int count = 0;
                	while(!matched && count < numclasses){
                		//Match superclass from the most specific to Object
                		rvalue = classes[numclasses-1-count++];
                		matched = qvalue.equals(rvalue);
                		if(matched) asubclass = rvalue;
                	}
                    isMatch = matched;
                    break;
                case TYPEID:
                    rvalue = guiclassdata.getObjectClassType(obj);
                    isMatch = qvalue.equalsIgnoreCase(rvalue);
                    setDefinedByType(true);
                    break;
                case INDEXID:
                	if(isDefinedByID()){//Type=xxx;ID=xxx;Index=xxx or Class=xxx;ID=xxx;Index=xxx
                		String anid = DGuiObjectRecognition.getObjectIdString(obj);
                		rvalue = anid!=null? String.valueOf(indices.getIdIndex(anid)):"";
                		
                	}else if(isDefinedByName()){//Type=xxx;Name=xxx;Index=xxx or Class=xxx;Name=xxx;Index=xxx
                		String aname = DGuiObjectRecognition.getObjectName(obj);
                		rvalue = aname!=null? String.valueOf(indices.getNameIndex(aname)):"";
                		
                	}else{
                		//Type=xxx;Index=xxx or Class=xxx;Index=xxx
                		if(isDefinedByType()){
                			rvalue = String.valueOf(indices.getTypeIndex(guiclassdata.getObjectClassType(obj))); //typeindex
                		}else{
                			rvalue = String.valueOf(indices.getClassIndex(aclass)); //classindex
                		}
                	}
                    isMatch = qvalue.trim().equals(rvalue.trim());
                    break;
                case CAPTIONID: 
                    rvalue = DGuiObjectRecognition.getObjectCaption(obj);
                    isMatch = ( qvalue.equals(rvalue));
                    break;
                case NAMEID:  
                    rvalue = DGuiObjectRecognition.getObjectName(obj);
                    isMatch = ( qvalue.equals(rvalue));
                    setDefinedByName(true);
                    break;
                case NAMECONTAINSID: 
                    rvalue = DGuiObjectRecognition.getObjectName(obj).toLowerCase();
                    isMatch = ( rvalue.indexOf(qvalue) > -1);
                    break;
                case PROPERTYID:
                    qarray = qvalue.split(DGuiObjectVector.DEFAULT_PROPERTY_QUALIFIER_SEPARATOR);
                    qvalue = qarray[1];
                    rvalue = DGuiObjectRecognition.getObjectProperty(obj, qarray[0]);
                    isMatch = (qvalue.equals(rvalue));
                    break;
                case PROPERTYCONTAINSID:   
                    qarray = qvalue.split(DGuiObjectVector.DEFAULT_PROPERTY_QUALIFIER_SEPARATOR);
                    qvalue = qarray[1];
                    rvalue = DGuiObjectRecognition.getObjectProperty(obj,qarray[0]).toLowerCase();
                    isMatch = ( rvalue.indexOf(qvalue) > -1);
                    break;
                case TEXTID:   
                    rvalue = DGuiObjectRecognition.getObjectText(obj);
                    isMatch = ( qvalue.equals(rvalue));
                    break;
                case TEXTCONTAINSID:
                    rvalue = DGuiObjectRecognition.getObjectText(obj).toLowerCase();
                    isMatch = ( rvalue.indexOf(qvalue) > -1);
                    break;
                case IDID:
                	int ID = DGuiObjectRecognition.getObjectId(obj);
                	rvalue = Integer.toString(ID);
                	try{ isMatch =(Integer.parseInt(qvalue)==ID); }
                	catch(NumberFormatException e){ isMatch=false; }
                	setDefinedByID(true);
                    break;
                case PATHID:   
                    rvalue = "not supported";
                    isMatch = false; // not supported yet
                    break;
                case CLASSINDEXID:   
                    rvalue = String.valueOf(indices.getClassIndex(aclass));
                    isMatch = ( qvalue.trim().equals(rvalue.trim())); 
                    break;
                case SUBCLASSINDEXID:   
                    try{ rvalue = String.valueOf(indices.getClassIndex(asubclass));}
                    catch(NullPointerException x){ rvalue = "null";}
                    isMatch = ( qvalue.trim().equals(rvalue.trim())); 
                    break;
                case OBJECTINDEXID:   
                    rvalue = String.valueOf(indices.objectindex); 
                    isMatch = ( qvalue.trim().equals(rvalue.trim())); 
                    break;
                case ABSCLASSINDEXID:   
                    rvalue = String.valueOf(indices.getAbsClassIndex(aclass)); 
                    isMatch = ( qvalue.trim().equals(rvalue.trim())); 
                    break;
                case ABSINDEXID:   
                	if(isDefinedByType()){
                		rvalue = String.valueOf(indices.getAbsTypeIndex(guiclassdata.getObjectClassType(obj))); //typeindex
                	}else{
                		rvalue = String.valueOf(indices.getAbsClassIndex(aclass)); //classindex
                	}
                    isMatch = ( qvalue.trim().equals(rvalue.trim())); 
                    break;
                case ABSOBJECTINDEXID:   
                    rvalue = String.valueOf(indices.absoluteobjectindex); 
                    isMatch = ( qvalue.trim().equals(rvalue.trim())); 
                    break;
                case ABSSUBCLASSINDEXID:   
                    try{rvalue = String.valueOf(indices.getAbsSubClassIndex(asubclass));}
                    catch(NullPointerException x){ rvalue="null";}
                    isMatch = ( qvalue.trim().equals(rvalue.trim())); 
                    break;
                
                default:

            }// end isMatchingObject switch         

        }// end isMatchingObject for loop
        if(!isMatch) 
        	debug(TAG+"isMatchingObject DID NOT match or contain '"+ qvalue +"': "+ rvalue);
        else
        	matched_indices = indices.copyAllIndices();
        return isMatch;

    }// end function isMatchingObject;

}// end GOD Object Definition
