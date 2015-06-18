/**
 ** Copyright (C) SAS Institute, All rights reserved.
 ** General Public License: http://www.opensource.org/licenses/gpl-license.php
 */
package org.safs.android.engine;

import java.util.ArrayList;

import org.safs.sockets.DebugListener;
import org.safs.sockets.RemoteException;

/**
 * 
 * @author Carl Nagle, SAS Institute, Inc
 * <p>
 * FEB 07, 2013 (Lei Wang)	Add a static field DGuiClassData to help get type for an object.<br/>
 * JUN 14, 2013 (Lei Wang)	Modify to capture also id's index and name's index.<br/>
 */
public class DGuiObjectVector {

	public static final String TAG = "DGOVector";

	static DSAFSTestRunner testrunner = null;
	
	/** ";" */
	public static final String DEFAULT_QUALIFIER_SEPARATOR = ";";
	/** ";\;" */
	public static final String DEFAULT_CHILD_SEPARATOR = ";\\;";
	/** "=" */
	public static final String DEFAULT_ASSIGN_SEPARATOR = "=";
	/** ":" */
	public static final String DEFAULT_PROPERTY_QUALIFIER_SEPARATOR = ":";
	
	/** 
	 * Set to true to have the window/component search only look once.  "exists now"! 
	 * Default is false. 
	 **/
	public boolean SINGLE_LOOP_SEARCH = false;
	
    String winrec = null;
    ArrayList winpath = null;
    ArrayList windefs = null;
    Object winobj = null;
    boolean fpsmmode = false;
    String comprec = null;
    ArrayList comppath = null;
    ArrayList compdefs = null;
    Object compobj = null;

    DebugListener debuglogger = null;
    
    public int secsWaitForWindow = 30;
    public int secsWaitForComponent = 30;

    //Used to manipulate class - type - libraryName, shared by all instances of GuiObjectVector
    protected static DGuiClassData dgcd = null;
    
    public DGuiObjectVector(String windowrec, String childrec, String pathString){
    	winrec = windowrec;
    	comprec = childrec;
    	//objrec = pathString;
    }

    public DGuiObjectVector(Object windowobj, String childrec, String pathString){
    	winobj = windowobj;
    	comprec = childrec;
    	//objrec = pathString;
    }

    public static void setGuiClassData(DGuiClassData dgcd){
    	DGuiObjectVector.dgcd = dgcd;
    }
    public static DGuiClassData getGuiClassData(){
    	return dgcd;
    }
    
    /**
     * Must be set prior to calling debug or any of the static "get" functions.
     * Currently this is set by the DSAFSTestRunner itself during initialization.
     * @param _testrunner
     */
    public static void setSAFSTestRunner(DSAFSTestRunner _testrunner){
    	testrunner = _testrunner;
    }
    
    void debug(String message){    	
    	try{ testrunner.debug(message); }
    	catch(Exception x){ System.out.println(message); }
    }
    
    DGuiObjectIndices indices = new DGuiObjectIndices();    
    
    /** */
    public Object getMatchingParentObject(int secsTimeout)throws RemoteException{
	    if( !(winobj == null)) return winobj;
	    debug(".getMatchingParent seeking "+ winrec);
	    winpath = splitRec(winrec);
	    windefs = getDefinitionHierarchy(winpath);
	    fpsmmode = ((DGuiObjectDefinition)windefs.get(0)).fpsmmode; // only valid on first one
	    debug(".getMatchingParent using FPSM: "+ fpsmmode);
	
	    long endtime = System.currentTimeMillis() + (1000 * secsTimeout);
	    boolean isTimeout = false;
	    //Object app = null;
	    Object[] windows = null;
	    DClassTypeInfo testobjInfo = null;
	    Object testobj = null;
	    DGuiObjectDefinition testdef = null;
	    boolean isMatched = false;
	    boolean looped = false;	    
	    while(!isTimeout && !isMatched && !(looped && SINGLE_LOOP_SEARCH)){
	        testdef = (DGuiObjectDefinition) windefs.get(0);
	        debug(".getMatchingParent seeking: "+ testdef.objstring);
	        //sys = UIATarget.localTarget();
	        //app = DGuiObjectRecognition.getForegroundApplication();
	        try{ windows = DGuiObjectRecognition.getTopLevelWindows();}
	        catch(RemoteException x){
		        debug(".getMatchingParent RemoteException '"+ x.getMessage() +"'.");
		        return null;
	        }
	        indices.resetAllIndices();
	        for(int i=0; !isMatched && i < windows.length; i++){
	            testobj = windows[i];
	            testobjInfo = captureTestObjectIndex(testobj, i+1);
			    debug(".getMatchingParent processing top-level window "+ (i+1) +", class: "+ testobjInfo.classname+", type: "+testobjInfo.typeclass);
			    
	            isMatched = testdef.isMatchingObject(testobj, indices.copyAllIndices());
	            
	            /*
	             * Adding support for a child of the top-level window to be considered the top-level parent.
	             * Ex: "CurrentWindow;\;Class=SomeChildContainer
	             */
	            if(isMatched && testdef.child != null){
				    debug(".getMatchingParent MATCHED! Processing top-level window "+ (i+1) +", children of: "+ testobjInfo.classname);
	            	testobj = searchChildren(testobj, testdef.child);
	            	isMatched = (testobj != null);
	            	//TODO If not matched, do we need to restore the indices??? Maybe yes.
//	            	if(!isMatched){
//	            		//We need to restore all indices for matching next possible window.
//	            		indices.restoreAllIndices(copy);
//	            	}
	            }
	        }
	        looped = true;
	        if(!isMatched) isTimeout = System.currentTimeMillis() > endtime;
	        if(!isTimeout && !SINGLE_LOOP_SEARCH) try{Thread.sleep(1000);}catch(Exception x){};        
	    }
	    if (isMatched) {
	        winobj = testobj;
	        debug(".getMatchingParent found matching parent for '"+ winrec +"'.");
	    }else{
	        debug(".getMatchingParent DID NOT find a match in timeout period.");
	    }
	    return winobj; // might be null if not found.
    }// end getMatchingParent

    /**
     * The ArrayList of DGuiObjectDefinitions describing the recognition path to the  
     * Window object.  For windefs, typically the Window or topmost View is windefs[0].
     * @return the current ArrayList of DGuiObjectDefinitions for the Window recognition.  
     * This can be null if no recognition path has been received or processed.
     */
    public ArrayList getWinDefs(){
    	return windefs;
    }
    
    /**
     * The ArrayList of DGuiObjectDefinitions describing the recognition path to the  
     * Component object.  For compdefs, typically the target component is the last item 
     * in the array.
     * @return the current ArrayList of DGuiObjectDefinitions for the Component recognition.  
     * This can be null if no recognition path has been received or processed.
     */
    public ArrayList getCompDefs(){
    	return compdefs;
    }
    
	/**
	 * @throws RemoteException ****************************************/
	public Object getMatchingChild(int secsTimeout) throws RemoteException{
	    if( !(compobj == null)) return compobj;
	    if(getMatchingParentObject(secsTimeout) == null) {
	        debug(".getMatchingChild == null -- no matched parent to search!!");
	        return null;
	    }
	    debug(".getMatchingChild using timeout: "+ secsTimeout+", comprec: "+ comprec);
	    comppath = splitRec(comprec);
	    compdefs = getDefinitionHierarchy(comppath);
	    if(compdefs.size() > 0) fpsmmode = ((DGuiObjectDefinition)compdefs.get(0)).fpsmmode; // only valid on first one
	    debug(".getMatchingChild using FPSM: "+ fpsmmode);
	
	    DGuiObjectIndices copy = indices.copyAllIndices(); // we might have to loop for timeout
	    long endtime = System.currentTimeMillis();
	    debug(".getMatchingChild starting search at time "+ endtime);
	    endtime += (1000 * secsTimeout); 
	    long nowtime = 0;
	    boolean isTimeout = false;
	
	    DGuiObjectDefinition testdef = (DGuiObjectDefinition) compdefs.get(0);
	    if(DGuiObjectRecognition.isTopLevelWindowRecognition(testdef.objstring)){
	        debug(".getMatchingChild ignoring TOP WINDOW reference in recognition string.");
	        testdef = testdef.child;
	        if (testdef == null) {
	            debug(".getMatchingChild invalid parent recognition solely top-level reference.");
	            return null;
	        }
	    }
	    boolean looped = false;
	    while(!isTimeout && (compobj == null) && !(looped && SINGLE_LOOP_SEARCH)){
	        compobj = searchChildren(winobj, testdef);
	        looped = true;
	        if(compobj == null) {
	            nowtime = System.currentTimeMillis();
	            if (nowtime > endtime) {
	                isTimeout = true;
	            }else if(!SINGLE_LOOP_SEARCH){
	                try{Thread.sleep(1000);}catch(Exception x){};
	                debug(".getMatchingChild TRYING AGAIN: "+ nowtime +" <= "+ endtime);
	                indices.restoreAllIndices(copy); 
	            }
	        }
	    }
	    if (compobj != null) {
	        debug(".getMatchingChild found matching child using '"+ comprec +"'.");
	    }else{
	        debug(".getMatchingChild DID NOT find a match in timeout period.");
	    }
	    return this.compobj; // will be null if not found
	
	}// end getMatchingChild

	/**
	 * @throws RemoteException ****************************************/
	public Object searchChildren(Object aparent, DGuiObjectDefinition testdef) throws RemoteException{
		DClassTypeInfo testobjInfo = null;
	    Object testobj = null;
	    boolean isMatched = false;
	    Object[] children = DGuiObjectRecognition.getChildren(aparent);
	    if(children.length == 0){
	        debug(".searchChildren object has no children to search.");
	        return null;
	    }
	    debug(".searchChildren processing children looking for: "+ testdef.objstring);
	    for(int i=0; !isMatched && i<children.length; i++){
	        testobj = children[i];
	        testobjInfo = captureTestObjectIndex(testobj, i+1);
	        debug(".searchChildren processing child class: "+ testobjInfo.classname+" ,type: "+testobjInfo.typeclass);
	        isMatched = testdef.isMatchingObject(testobj, indices.copyAllIndices());
	        if(!isMatched && !fpsmmode){
	            if(DGuiObjectRecognition.hasChildren(testobj)){
	            	DGuiObjectIndices saved = indices.saveClassIndices();
	                debug(".searchChildren seeking non-FPSM match in grandchildren.");
	                testobj = searchChildren(testobj, testdef);
	                if (testobj != null) return testobj;
	                indices.restoreClassIndices(saved);
	            }
	        }
	    }
	    if (isMatched) {
	        debug(".searchChildren matched object at this level: "+ testdef.objstring);
	        if(testdef.child == null) return testobj;       
	        debug(".searchChildren seeking children for next level");
	        return searchChildren(testobj, testdef.child);
	    }
	    return null; // not found.	    
	}// end searchChildren


	/************************************************************
	*
	* captureTestObjectIndex (testobj, objectindex)
	* This method will calculate the class-index, type-index, id-index, name-index
	* for a test object that we meet during the traversal of the whole AUT-tree.
	* 
	*************************************************************/
	public DClassTypeInfo captureTestObjectIndex(Object testobj, int objectindex){
        DClassTypeInfo aninfo = new DClassTypeInfo("Class.Name.Not.Assigned");

        if(testobj==null){
        	debug("The test object is null, which should never happen!!!");
        	return aninfo;
        }
        
	    try{
	    	aninfo.classname = DGuiObjectRecognition.getObjectClassName(testobj);
		    aninfo.classindex = indices.incrementClassIndex(aninfo.classname);
		    aninfo.absoluteclassindex = indices.getAbsClassIndex(aninfo.classname);
		    debug(".incremented classIndex: "+ aninfo.classindex+", absclassindex: "+ aninfo.absoluteclassindex+" for '"+aninfo.classname+"'");
	    }catch(NullPointerException x){}/* aninfo.classname might be null? not possible*/
	    
	    try{
	    	aninfo.typeclass = (dgcd!=null? dgcd.getObjectClassType(testobj):null);
		    aninfo.typeindex = indices.incrementTypeIndex(aninfo.typeclass);
		    aninfo.absolutetypeindex = indices.getAbsTypeIndex(aninfo.typeclass);
		    debug(".incremented typeindex: "+ aninfo.typeindex+", absolutetypeindex: "+ aninfo.absolutetypeindex+" for '"+aninfo.typeclass+"'");
	    }catch(NullPointerException x){}/* aninfo.typeclass might be null? possible*/
	    
	    try{
		    aninfo.id = DGuiObjectRecognition.getObjectIdString(testobj);
		    if(aninfo.id!=null){
		    	aninfo.idindex = indices.incrementIdIndex(aninfo.id);
		    	debug(".incremented idindex: "+ aninfo.idindex+" for '"+aninfo.id+"'");
		    }
	    }catch(Exception ignore){}
	    
	    try{
		    aninfo.name = DGuiObjectRecognition.getObjectName(testobj);
		    if(aninfo.name!=null){
		    	aninfo.nameindex = indices.incrementNameIndex(aninfo.name);
		    	debug(".incremented nameindex: "+ aninfo.nameindex+" for '"+aninfo.name+"'");
		    }
	    }catch(Exception ignore){}
	    
	    debug(".incremented objectIndex = "+ objectindex);
	    aninfo.objectindex = objectindex;
	    indices.incrementObjectIndex();
	    aninfo.absoluteobjectindex = indices.getAbsObjectIndex();
	    
	    return aninfo;
	}

	/***************************************
	 * create an ArrayList of DGuiObjectDefinition objects 
	 * created from An ArrayList of parent\child object recognition strings returned 
	 * from splitRec. The returned DGuiObjectDefinition objects in the ArrayList 
	 * have their parent/child fields set according to their position in the hierarchy.
	 **************************************/
	public static ArrayList getDefinitionHierarchy(ArrayList recArray){
		ArrayList list = new ArrayList();
	    DGuiObjectDefinition parent = null;
	    DGuiObjectDefinition child = null;
	    for(int i=0; i < recArray.size(); i++) {
	        child = new DGuiObjectDefinition((String)recArray.get(i));
	        if(parent != null) {
	            parent.child = child;
	            child.parent = parent;
	        }
	        parent = child;
	        list.add(child);
	    }
	    return list;
	}

	/***************************************
	 * split hierarchy recognition into ArrayList 
	 * of separate object recognition strings 
	 * [ parent, child, grandchild, etc.. ]
	 **************************************/
	public static ArrayList splitRec(String arec){
	    ArrayList apath = new ArrayList();
	    int sindex = 0;
	    String newrec = arec;
	    while(sindex > -1){
	        sindex = newrec.indexOf(DEFAULT_CHILD_SEPARATOR);
	        if(sindex == -1){
	            apath.add(newrec);
	        }else{
	            if(sindex > 0) apath.add(newrec.substring(0,sindex));
	            if(newrec.length() > 3){
	               newrec = newrec.substring(sindex+DEFAULT_CHILD_SEPARATOR.length());
	            }else{
	               sindex = -1; //done
	            }
	        }
	    }
	    return apath;
	}

}