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
package org.safs.android.engine.processor;

import java.util.Hashtable;
import java.util.Properties;
import java.util.Vector;

import org.safs.android.engine.DGuiObjectDefinition;
import org.safs.android.engine.DGuiObjectRecognition;
import org.safs.android.engine.DGuiObjectVector;
import org.safs.android.engine.DSAFSTestRunner;
import org.safs.android.messenger.client.MessageResult;
import org.safs.android.remotecontrol.SAFSMessage;
import org.safs.sockets.RemoteException;
import org.safs.text.FAILKEYS;

import android.view.View;

import com.jayway.android.robotium.remotecontrol.client.SoloMessage;
import com.jayway.android.robotium.remotecontrol.client.processor.CacheReferenceInterface;
import com.jayway.android.robotium.remotecontrol.client.processor.ProcessorException;
import com.jayway.android.robotium.remotecontrol.client.processor.SoloProcessor;


/**
 * 
 * @author Carl Nagle, SAS Institute, Inc
 * 
 * @since  Feb 16, 2012<br>
 * 		   Jun 05, 2013		(LeiWang)	Handle keyword 'getaccessiblename', return view's id name.<br>
 *                                      If view's id is {@value View#NO_ID}, return "" as id.<br>
 */
public class EngineProcessor extends  SAFSProcessor implements CacheReferenceInterface{
	public static String TAG = EngineProcessor.class.getName();

	protected int INITIAL_CACHE_SIZE = 25;
	protected Hashtable<Object,Object> cache = new Hashtable<Object, Object>(INITIAL_CACHE_SIZE);
	protected Hashtable<Object,Object> _defs = new Hashtable<Object, Object>(INITIAL_CACHE_SIZE);
	
	DGuiObjectVector dgov = null;
	HighLightUtil highlightUtil = null;
	boolean tempSuccess = false;
	
	/** set to true by internal processes when SoloProcessor has been chained. 
	 * User can reset to false if they want these internal processes to retry connecting this chain. */
	public boolean chainedSolo = false;
    
	public EngineProcessor(DSAFSTestRunner testrunner) {
		super(testrunner);
		String debugPrefix = TAG + ".EngineProcessor() ";

		// we cannot use the testrunner immediately because it has to finish initializing
		// for example. testrunner.launchapplication() will throw a NullPointerException.
	}

	
	/** 
	 * Resets both the object cache and the _defs cache of associated DGuiObjectDefinitions.
	 * @see com.jayway.android.robotium.remotecontrol.client.processor.AbstractProcessor#resetExternalModeCache(java.util.Hashtable)
	 */
	@Override
	public void resetExternalModeCache(Hashtable cache) {
		super.resetExternalModeCache(cache);
		super.resetExternalModeCache(_defs);
	}
	
	public void processProperties(Properties props) {
		String debugPrefix = TAG + ".processProperties() ";
		debug(debugPrefix +" processing...");
		if(!chainedSolo){
			try{
				SoloProcessor p = (SoloProcessor) testrunner.getProcessors(SAFSMessage.target_solo).firstElement();
				p.addCacheReferenceInterface(this); // OK NullPointerException
				addCacheReferenceInterface(p);
				chainedSolo = true;
			}catch(Exception x){/* ignore */}
		}
		try{
			if(SAFSMessage.engine_clearreferencecache.equalsIgnoreCase(remoteCommand)){
				
				resetExternalModeCache(cache);
				setGeneralSuccess(props);
				
			}else if(SAFSMessage.engine_getaccessiblename.equalsIgnoreCase(remoteCommand)){
				
				_doSimpleCommand(props);
				
			}else if(SAFSMessage.engine_getcaption.equalsIgnoreCase(remoteCommand)){
				
				_doSimpleCommand(props);
				
			}else if(SAFSMessage.engine_getchildcount.equalsIgnoreCase(remoteCommand)){
	
				_doSimpleCommand(props);
	
			}else if(SAFSMessage.engine_getchildren.equalsIgnoreCase(remoteCommand)){
	
				_doSimpleCommand(props);
				
			}else if(SAFSMessage.engine_getclassindex.equalsIgnoreCase(remoteCommand)){
				
				_doSimpleCommand(props);
				
			}else if(SAFSMessage.engine_getclassname.equalsIgnoreCase(remoteCommand)){
	
				_doSimpleCommand(props);
	
			}else if(SAFSMessage.engine_getcurrentwindow.equalsIgnoreCase(remoteCommand)){
				
				_getCurrentWindow(props);
				
			}else if(SAFSMessage.engine_getid.equalsIgnoreCase(remoteCommand)){
				
				_doSimpleCommand(props);
	
			}else if(SAFSMessage.engine_getmatchingchildobjects.equalsIgnoreCase(remoteCommand)){
				
				_getMatchingChildObjects(props);
				
			}else if(SAFSMessage.engine_getmatchingparentobject.equalsIgnoreCase(remoteCommand)){
	
				_getMatchingParentObject(props);
				
			}else if(SAFSMessage.engine_getmatchingpathobject.equalsIgnoreCase(remoteCommand)){
				
				// TODO Auto-generated method stub
				
			}else if(SAFSMessage.engine_getname.equalsIgnoreCase(remoteCommand)){
	
				_doSimpleCommand(props);
	
			}else if(SAFSMessage.engine_getnonaccessiblename.equalsIgnoreCase(remoteCommand)){
	
				_doSimpleCommand(props);
	
			}else if(SAFSMessage.engine_getproperty.equalsIgnoreCase(remoteCommand)){
				
				_doGetPropertyCommand(props);
				
			}else if(SAFSMessage.engine_getpropertynames.equalsIgnoreCase(remoteCommand)){
	
				_doSimpleCommand(props);
	
			}else if(SAFSMessage.engine_getstringdata.equalsIgnoreCase(remoteCommand)){
				
				// TODO Auto-generated method stub
				
			}else if(SAFSMessage.engine_getsuperclassnames.equalsIgnoreCase(remoteCommand)){
	
				_doSimpleCommand(props);
	
			}else if(SAFSMessage.engine_gettext.equalsIgnoreCase(remoteCommand)){
				
				_doSimpleCommand(props);
				
			}else if(SAFSMessage.engine_gettoplevelcount.equalsIgnoreCase(remoteCommand)){
	
				_getTopLevelCount(props);
	
			}else if(SAFSMessage.engine_gettoplevelwindows.equalsIgnoreCase(remoteCommand)){
	
				_getTopLevelWindows(props);
				
			}else if(SAFSMessage.engine_isenabled.equalsIgnoreCase(remoteCommand)){
	
				_doSimpleCommand(props);
				
			}else if(SAFSMessage.engine_ismatchingpath.equalsIgnoreCase(remoteCommand)){
				
				// TODO Auto-generated method stub
				
			}else if(SAFSMessage.engine_isshowing.equalsIgnoreCase(remoteCommand)){
				
				_doSimpleCommand(props);
				
			}else if(SAFSMessage.engine_isvalid.equalsIgnoreCase(remoteCommand)){
				
				_doSimpleCommand(props);
				
			}else if(SAFSMessage.engine_istoplevelpopupcontainer.equalsIgnoreCase(remoteCommand)){
				
				_doSimpleCommand(props);
				
			}else if(SAFSMessage.engine_setactivewindow.equalsIgnoreCase(remoteCommand)){
				
				_doSimpleCommand(props);
				
			}else if(SAFSMessage.engine_highlightmatchingchildobjectbykey.equalsIgnoreCase(remoteCommand)){
				
				_doHighLightCommand(props);
				
			}else if(SAFSMessage.engine_clearhighlighteddialog.equalsIgnoreCase(remoteCommand)){
				
				_doClearHighLightCommand(props);
				
			}
			
			
		}catch(Throwable x){
			String stackout = getStackTrace(x);
			debug(debugPrefix+ "\n"+ stackout);
			resourceMsg.reset();
			resourceMsg.setKey(FAILKEYS.GENERIC_ERROR);
			resourceMsg.addParameter(stackout);		
			setGeneralErrorWithBundleMessage(props, resourceMsg, null);
			return;
		}
	}

	public MessageResult processMessage(String message) {
		// TODO Auto-generated method stub
		return null;
	}
	
	/** 
	 * can return NLS ERROR Keys with appropriate Parameters/Args to complete the message:
	 * <p>
	 * <ul>
	 * FAILKEYS.SUPPORT_NOT_INITIALIZED<br>
	 * </ul>  
	 */
	private void _getTopLevelWindows(Properties props) {
		String debugPrefix = TAG + "._getTopLevelWindows() ";
		Object[] windows = new Object[0];
		String resultInfo = "";
		try{ windows = DGuiObjectRecognition.getTopLevelWindows(); }
		catch(RemoteException x){ 
			debug(debugPrefix+" RemoteException=" +x.getMessage());
			// %1% support may not be properly initialized!
			resourceMsg.reset();
			resourceMsg.setKey(FAILKEYS.SUPPORT_NOT_INITIALIZED);
			resourceMsg.addParameter("Messenger/Solo");		
			setGeneralErrorWithBundleMessage(props, resourceMsg, null);
			return;
		}
		try{
			String[] keys = convertToKeys(cache, windows);
			resultInfo = SoloMessage.convertToDelimitedString(keys);	
			debug(debugPrefix +"found "+ keys.length+" top-level objects.");
		}catch(Exception x){
			debug(debugPrefix +"did not find any top-level objects!");
		}
		setGeneralSuccessWithSpecialInfo(props, resultInfo);
	}
	
	/** 
	 * can return NLS ERROR Keys with appropriate Parameters/Args to complete the message:
	 * <p>
	 * <ul>
	 * FAILKEYS.SUPPORT_NOT_INITIALIZED<br>
	 * </ul>  
	 */
	private void _getTopLevelCount(Properties props) {
		String debugPrefix = TAG + "._getTopLevelCount() ";
		debug(debugPrefix +"processing...");
		Object[] windows = new Object[0];
		String resultInfo = "";
		try{ windows = DGuiObjectRecognition.getTopLevelWindows(); }
		catch(RemoteException x){ 
			debug(debugPrefix+" RemoteException=" +x.getMessage());
			// %1% support may not be properly initialized!
			resourceMsg.reset();
			resourceMsg.setKey(FAILKEYS.SUPPORT_NOT_INITIALIZED);
			resourceMsg.addParameter("Messenger/Solo");		
			setGeneralErrorWithBundleMessage(props, resourceMsg, null);
			return;
		}catch(Exception x){
			String stackout = getStackTrace(x);
			debug(debugPrefix+ "\n"+ stackout);
			// %1% support may not be properly initialized!
			resourceMsg.reset();
			resourceMsg.setKey(FAILKEYS.GENERIC_ERROR);
			resourceMsg.addParameter(stackout);		
			setGeneralErrorWithBundleMessage(props, resourceMsg, null);
			return;
		}
		try{
			resultInfo = String.valueOf(windows.length);	
			debug(debugPrefix +"found "+ windows.length+" top-level objects.");
		}catch(Exception x){
			debug(debugPrefix +"did not find any top-level objects!");
		}
		setGeneralSuccessWithSpecialInfo(props, resultInfo);
	}
	
	/** 
	 * can return NLS ERROR Keys with appropriate Parameters/Args to complete the message:
	 * <p>
	 * <ul>
	 * FAILKEYS.SUPPORT_NOT_INITIALIZED<br>
	 * </ul>  
	 */
	private void _getCurrentWindow(Properties props) {
		String debugPrefix = TAG + "._getCurrentWindow() ";
		Object[] windows = new Object[0];
		String resultInfo = "";
		try{ windows = DGuiObjectRecognition.getTopLevelWindows(); }
		catch(RemoteException x){ 
			debug(debugPrefix+" RemoteException=" +x.getMessage());
			// %1% support may not be properly initialized!
			resourceMsg.reset();
			resourceMsg.setKey(FAILKEYS.SUPPORT_NOT_INITIALIZED);
			resourceMsg.addParameter("Messenger/Solo");		
			setGeneralErrorWithBundleMessage(props, resourceMsg, null);
			return;
		}
		try{
			resultInfo = convertToKey(cache, windows[0]);
			debug(debugPrefix +"found current window "+ resultInfo);
		}catch(Exception x){
			debug(debugPrefix +"did not find any 'current window'!");
		}
		setGeneralSuccessWithSpecialInfo(props, resultInfo);
	}
	
	private void _getMatchingChildObjects(Properties props){
		String debugPrefix = TAG + "._doGetMatchingChildObjects() ";
		String resultInfo = "";
		String uid = null;
		String recognition = null;
		try{ 
			uid = SoloMessage.getString(props, SAFSMessage.PARAM_1);
			if(uid == null || uid.length() < 1) throw new ProcessorException("Component");
			debug(debugPrefix +"using Parent UID: "+ uid);
			recognition = SoloMessage.getString(props, SAFSMessage.PARAM_2);
			if(recognition == null || recognition.length() < 1) throw new ProcessorException("Recognition");
			debug(debugPrefix +"using Component Rec: "+ recognition);
		}
		catch(ProcessorException x){
			debug(debugPrefix+" Missing '"+ x.getMessage()+"' parameter for "+ remoteCommand);			
			// %1%, wrong number of parameters: %2%.
			resourceMsg.reset();
			resourceMsg.setKey(FAILKEYS.PARAMSIZE_2);
			resourceMsg.setParams(new String[]{remoteCommand, x.getMessage()});
			setGeneralErrorWithBundleMessage(props, resourceMsg, null);
			return;
		}
		Object parent = getCachedObject(uid, true);
		if(parent == null){	
			// %1% was not successful using %2%.
			// No match found for %1%.
			resourceMsg.reset();
			resourceMsg.setKey(FAILKEYS.NO_SUCCESS_2);
			resourceMsg.setParams(new String[]{remoteCommand,uid});
			resourceDetailMsg.reset();
			resourceDetailMsg.setKey(FAILKEYS.NO_MATCH_FOUND);
			resourceDetailMsg.setParams(new String[]{uid});
			setGeneralErrorWithBundleMessage(props, resourceMsg, resourceDetailMsg);
			return;
		}
		int secsTimeout = 60;
		if(props.containsKey(SAFSMessage.PARAM_TIMEOUT)){
			try{ secsTimeout = Integer.parseInt(props.getProperty(SAFSMessage.PARAM_TIMEOUT));}
			catch(Exception x){/* use default */}
		}
		
		dgov = new DGuiObjectVector(parent,recognition,"");
		
		Object child = null;
		try{ child = dgov.getMatchingChild(secsTimeout);}
		catch(RemoteException x){ 
			debug(debugPrefix+" RemoteException=" +x.getMessage());
			// %1% support may not be properly initialized!
			resourceMsg.reset();
			resourceMsg.setKey(FAILKEYS.SUPPORT_NOT_INITIALIZED);
			resourceMsg.addParameter("Messenger/Solo");		
			setGeneralErrorWithBundleMessage(props, resourceMsg, null);
			return;
		}
		if(child!=null){
			debug(debugPrefix +"found matching child for '"+recognition+"'");
			Object[] tmp = {child};
			String[] keys = convertToKeys(cache, tmp);
			try{
				try{ resultInfo = SoloMessage.convertToDelimitedString(keys);}	
				catch(Exception idoe){
					debug(debugPrefix+idoe.getMessage());				
					// Unable to perform %2% on %1%
					resourceMsg.reset();
					resourceMsg.setKey(FAILKEYS.FAILURE_2);
					resourceMsg.setParams(new String[]{keys[0], "getUniqueDelimiter()"});				
					setGeneralErrorWithBundleMessage(props, resourceMsg, null);
					return;
				}
				setGeneralSuccessWithSpecialInfo(props, resultInfo);
				try{_defs.put(keys[0], dgov.getCompDefs().get(dgov.getCompDefs().size()-1));}
				catch(Exception x){
					debug(debugPrefix+"can't store DGuiObjectDefinition for child: "+x.getClass().getSimpleName());				
				}
				return;
			}
			catch(IndexOutOfBoundsException idoe){
				debug(debugPrefix+"can't generate a cacheID for child "+child.getClass().getSimpleName());				
				// Unable to perform %2% on %1%
				resourceMsg.reset();
				resourceMsg.setKey(FAILKEYS.FAILURE_2);
				resourceMsg.setParams(new String[]{child.getClass().getName(), "convertToKeys()"});				
				setGeneralErrorWithBundleMessage(props, resourceMsg, null);
				return;
			}
		}else{
			debug(debugPrefix +"did not find matching object for '"+recognition+"'");
			setGeneralSuccessWithSpecialInfo(props, SAFSMessage.NULL_VALUE);
			return;
		}
	}

	/** 
	 * can return NLS ERROR Keys with appropriate Parameters/Args to complete the message:
	 * <p>
	 * <ul>
	 * FAILKEYS.PARAMSIZE_2<br>
	 * FAILKEYS.SUPPORT_NOT_INITIALIZED<br>
	 * FAILKEYS.FAILURE_2<br>
	 * </ul>  
	 */
	private void _getMatchingParentObject(Properties props){
		String debugPrefix = TAG + "._getMatchingParentObject() ";

		String parentRS = null;

		try{ parentRS = SoloMessage.getString(props, SAFSMessage.PARAM_1);}
		catch(ProcessorException x){
			debug(debugPrefix+" Missing 'Recognition' parameter for "+ remoteCommand);
			
			// %1%, wrong number of parameters: %2%.
			resourceMsg.reset();
			resourceMsg.setKey(FAILKEYS.PARAMSIZE_2);
			resourceMsg.setParams(new String[]{remoteCommand, "Recognition"});
			setGeneralErrorWithBundleMessage(props, resourceMsg, null);
			return;
		}
		int secsTimeout = 20;
		if(props.containsKey(SAFSMessage.PARAM_TIMEOUT)){
			try{ secsTimeout = Integer.parseInt(props.getProperty(SAFSMessage.PARAM_TIMEOUT));}
			catch(Exception x){/* use default */}
		}
		
		dgov = new DGuiObjectVector(parentRS,parentRS,"");
		
		Object parent = null;
		try{ parent = dgov.getMatchingParentObject(secsTimeout);}
		catch(RemoteException x){ 
			debug(debugPrefix+" RemoteException=" +x.getMessage());
			// %1% support may not be properly initialized!
			resourceMsg.reset();
			resourceMsg.setKey(FAILKEYS.SUPPORT_NOT_INITIALIZED);
			resourceMsg.addParameter("Messenger/Solo");		
			setGeneralErrorWithBundleMessage(props, resourceMsg, null);
			return;
		}
		if(parent!=null){
			debug(debugPrefix +"found matching object for '"+parentRS+"'");
			Object[] tmp = {parent};
			String[] keys = convertToKeys(cache, tmp);
			try{
				setGeneralSuccessWithSpecialInfo(props, keys[0]);
				try{_defs.put(keys[0], dgov.getWinDefs().get(dgov.getWinDefs().size()-1));}
				catch(Exception x){
					debug(debugPrefix+"can't store DGuiObjectDefinition for window: "+x.getClass().getSimpleName());				
				}
				return;
			}
			catch(IndexOutOfBoundsException idoe){
				debug(debugPrefix+"can't generate a cacheID for window "+parent.getClass().getSimpleName());				
				// Unable to perform %2% on %1%
				resourceMsg.reset();
				resourceMsg.setKey(FAILKEYS.FAILURE_2);
				resourceMsg.setParams(new String[]{parent.getClass().getName(), "convertToKeys()"});				
				setGeneralErrorWithBundleMessage(props, resourceMsg, null);
				return;
			}
		}else{
			debug(debugPrefix +"did not find matching object for '"+parentRS+"'");
			setGeneralSuccessWithSpecialInfo(props, SAFSMessage.NULL_VALUE);
			return;
		}
		
	}

	private void _doGetPropertyCommand(Properties props){
		String debugPrefix = TAG + "._doGetPropertyCommand() ";
		String resultInfo = "";
		String uid = null;
		String propname = null;
		try{ 
			uid = SoloMessage.getString(props, SAFSMessage.PARAM_1);
			if(uid == null || uid.length() < 1) throw new ProcessorException("Component");
			propname = SoloMessage.getString(props, SAFSMessage.PARAM_2);
			if(propname == null || propname.length() < 1) throw new ProcessorException("Property");
		}
		catch(ProcessorException x){
			debug(debugPrefix+" Missing '"+ x.getMessage()+"' parameter for "+ remoteCommand);			
			// %1%, wrong number of parameters: %2%.
			resourceMsg.reset();
			resourceMsg.setKey(FAILKEYS.PARAMSIZE_2);
			resourceMsg.setParams(new String[]{remoteCommand, x.getMessage()});
			setGeneralErrorWithBundleMessage(props, resourceMsg, null);
			return;
		}
		// not used here at this time
		//int secsTimeout = 15;
		//if(props.containsKey(SAFSMessage.PARAM_TIMEOUT)){
		//	try{ secsTimeout = Integer.parseInt(props.getProperty(SAFSMessage.PARAM_TIMEOUT));}
		//	catch(Exception x){/* use default */}
		//}		
		Object obj = getCachedObject(uid, true);
		if(obj == null){	
			// %1% was not successful using %2%.
			// No match found for %1%.
			resourceMsg.reset();
			resourceMsg.setKey(FAILKEYS.NO_SUCCESS_2);
			resourceMsg.setParams(new String[]{remoteCommand,uid});
			resourceDetailMsg.reset();
			resourceDetailMsg.setKey(FAILKEYS.NO_MATCH_FOUND);
			resourceDetailMsg.setParams(new String[]{uid});
			setGeneralErrorWithBundleMessage(props, resourceMsg, resourceDetailMsg);
			return;
		}
		resultInfo = DGuiObjectRecognition.getObjectProperty(obj,propname);
		setGeneralSuccessWithSpecialInfo(props, resultInfo);
	}
	
	private void _doSimpleCommand(Properties props){
		String debugPrefix = TAG + "._doSimpleCommands() ";
		String resultInfo = "";
		String uid = null;
		try{ 
			uid = SoloMessage.getString(props, SAFSMessage.PARAM_1);
			if(uid == null || uid.length() < 1) throw new ProcessorException();
		}
		catch(ProcessorException x){
			debug(debugPrefix+" Missing 'Component' parameter for "+ remoteCommand);			
			// %1%, wrong number of parameters: %2%.
			resourceMsg.reset();
			resourceMsg.setKey(FAILKEYS.PARAMSIZE_2);
			resourceMsg.setParams(new String[]{remoteCommand, "Component"});
			setGeneralErrorWithBundleMessage(props, resourceMsg, null);
			return;
		}
		int secsTimeout = 15;
		if(props.containsKey(SAFSMessage.PARAM_TIMEOUT)){
			try{ secsTimeout = Integer.parseInt(props.getProperty(SAFSMessage.PARAM_TIMEOUT));}
			catch(Exception x){/* use default */}
		}		
		try{ 
			Object obj = getCachedObject(uid, true);
			DGuiObjectDefinition def = null;
			try{def = (DGuiObjectDefinition)_defs.get(uid);}catch(Exception x){
				debug(debugPrefix+" ignoring DGuiObjectDefinition.get() "+ x.getClass().getSimpleName());			
			}

			// def and/or def.matched_indices can be null if processing children AFTER a search
			
			if(obj == null){	
				// %1% was not successful using %2%.
				// No match found for %1%.
				resourceMsg.reset();
				resourceMsg.setKey(FAILKEYS.NO_SUCCESS_2);
				resourceMsg.setParams(new String[]{remoteCommand,uid});
				resourceDetailMsg.reset();
				resourceDetailMsg.setKey(FAILKEYS.NO_MATCH_FOUND);
				resourceDetailMsg.setParams(new String[]{uid});
				setGeneralErrorWithBundleMessage(props, resourceMsg, resourceDetailMsg);
				return;
			}
			if(SAFSMessage.engine_getpropertynames.equalsIgnoreCase(remoteCommand)){
				String[] names = DGuiObjectRecognition.getObjectPropertyNames(obj);
				resultInfo = SoloMessage.convertToDelimitedString(names);
				
			}else 
			if(SAFSMessage.engine_getname.equalsIgnoreCase(remoteCommand)){
				resultInfo = DGuiObjectRecognition.getObjectName(obj);
					
			}else 
			if(SAFSMessage.engine_getaccessiblename.equalsIgnoreCase(remoteCommand)){
				resultInfo = DGuiObjectRecognition.getObjectName(obj);

			}else 
			if(SAFSMessage.engine_getcaption.equalsIgnoreCase(remoteCommand)){
				resultInfo = DGuiObjectRecognition.getObjectCaption(obj);
				
			}else 
			if(SAFSMessage.engine_getchildcount.equalsIgnoreCase(remoteCommand)){
				Object[] c = DGuiObjectRecognition.getChildren(obj);
				resultInfo = (c != null) ? String.valueOf(c.length): "0";
				
			}else 
			if(SAFSMessage.engine_getchildren.equalsIgnoreCase(remoteCommand)){
				Object[] c = DGuiObjectRecognition.getChildren(obj);
				String[] keys = convertToKeys(cache, c);
				resultInfo = SoloMessage.convertToDelimitedString(keys);
				
			}else 
			if(SAFSMessage.engine_getclassname.equalsIgnoreCase(remoteCommand)){
				resultInfo = DGuiObjectRecognition.getObjectClassName(obj);
				
			}else 
			if(SAFSMessage.engine_getid.equalsIgnoreCase(remoteCommand)){
				int ID = DGuiObjectRecognition.getObjectId(obj);
				if(ID!=View.NO_ID) resultInfo = String.valueOf(ID);
				
			}else 
			if(SAFSMessage.engine_getsuperclassnames.equalsIgnoreCase(remoteCommand)){
				String[] names = DGuiObjectRecognition.getObjectSuperclassNames(obj);
				//names[] is in reverse order as needed for Engine Command
				// plus it does not have the class itself!
				String[] reversed = new String[names.length + 1];
				reversed[0] = obj.getClass().getName();
				for(int i=0;i<names.length;i++) reversed[i+1] = names[names.length -1 -i];
				resultInfo = SoloMessage.convertToDelimitedString(reversed);				
			}else 
			if(SAFSMessage.engine_gettext.equalsIgnoreCase(remoteCommand)){
				resultInfo = DGuiObjectRecognition.getObjectText(obj);
				
			}else 
			if(SAFSMessage.engine_isenabled.equalsIgnoreCase(remoteCommand)){
				resultInfo = String.valueOf(DGuiObjectRecognition.getObjectIsEnabled(obj));
				
			}else 
			if(SAFSMessage.engine_isshowing.equalsIgnoreCase(remoteCommand)){
				resultInfo = String.valueOf(DGuiObjectRecognition.getObjectIsShowing(obj));
				
			}else 
			if(SAFSMessage.engine_isvalid.equalsIgnoreCase(remoteCommand)){
				resultInfo = String.valueOf(DGuiObjectRecognition.getObjectIsValid(obj));
			}else 
			if(SAFSMessage.engine_istoplevelpopupcontainer.equalsIgnoreCase(remoteCommand)){
				resultInfo = String.valueOf(DGuiObjectRecognition.isTopLevelPopupContainer(obj));
			}else 
			if(SAFSMessage.engine_setactivewindow.equalsIgnoreCase(remoteCommand)){
				resultInfo = String.valueOf(((View)obj).requestFocus());
			}else 
			if(SAFSMessage.engine_getclassindex.equalsIgnoreCase(remoteCommand)){
				String aclass = null;
				try{
					aclass = DGuiObjectRecognition.getObjectClassName(obj);
					// def and/or matched_indices can be null if processing children AFTER a search
					try{ resultInfo = String.valueOf(def.matched_indices.getClassIndex(aclass));}
					catch(NullPointerException x){ resultInfo = String.valueOf(0);}
				}catch(Exception x){
					debug(debugPrefix+" "+x.getClass().getSimpleName()+", " +x.getMessage());
					// %1% support may not be properly initialized!
					resourceMsg.reset();
					resourceMsg.setKey(FAILKEYS.NO_SUCCESS_2);
					resourceMsg.setParams(new String[]{remoteCommand, aclass});		
					setGeneralErrorWithBundleMessage(props, resourceMsg, null);
					return;
				}
			}else{
				
			}
		}catch(IllegalThreadStateException x){ 
			debug(debugPrefix+" IllegalThreadStateException=" +x.getMessage());
			// %1% support may not be properly initialized!
			resourceMsg.reset();
			resourceMsg.setKey(FAILKEYS.SUPPORT_NOT_INITIALIZED);
			resourceMsg.addParameter("Messenger/Solo");		
			setGeneralErrorWithBundleMessage(props, resourceMsg, null);
			return;
		}
		catch(RemoteException x){ 
			debug(debugPrefix+" RemoteException=" +x.getMessage());
			// %1% support may not be properly initialized!
			resourceMsg.reset();
			resourceMsg.setKey(FAILKEYS.SUPPORT_NOT_INITIALIZED);
			resourceMsg.addParameter("Messenger/Solo");		
			setGeneralErrorWithBundleMessage(props, resourceMsg, null);
			return;
		}
		catch(NullPointerException x){ 
			debug(debugPrefix+" NullPointerException=" +x.getMessage());
			// Invalid parameter value for %1% 
			resourceMsg.reset();
			resourceMsg.setKey(FAILKEYS.BAD_PARAM);
			resourceMsg.addParameter("Component");		
			setGeneralErrorWithBundleMessage(props, resourceMsg, null);
			return;
		}
		setGeneralSuccessWithSpecialInfo(props, resultInfo);
	}
	
	private void _doHighLightCommand(Properties props){
		String debugPrefix = TAG + "._doHighLightCommand() ";
		String winUid = null;
		String compUid = null;
		try{ 
			winUid = SoloMessage.getString(props, SAFSMessage.PARAM_1);
			if(winUid == null || winUid.length() < 1) throw new ProcessorException("Window");
			compUid = SoloMessage.getString(props, SAFSMessage.PARAM_2);
			if(compUid == null || compUid.length() < 1) throw new ProcessorException("Component");
		}
		catch(ProcessorException x){
			debug(debugPrefix+" Missing '"+ x.getMessage()+"' parameter for "+ remoteCommand);			
			// %1%, wrong number of parameters: %2%.
			resourceMsg.reset();
			resourceMsg.setKey(FAILKEYS.PARAMSIZE_2);
			resourceMsg.setParams(new String[]{remoteCommand, x.getMessage()});
			setGeneralErrorWithBundleMessage(props, resourceMsg, null);
			return;
		}

//		Enumeration keys = cache.keys();
//		Object key = null;
//		while(keys.hasMoreElements()){
//			key = keys.nextElement();
//			debug(key+"="+cache.get(key));
//		}
		Object win = getCachedObject(winUid, true);
		if(win == null){	
			// %1% was not successful using %2%.
			// No match found for %1%.
//			resourceMsg.reset();
//			resourceMsg.setKey(FAILKEYS.NO_SUCCESS_2);
//			resourceMsg.setParams(new String[]{remoteCommand,winUid});
//			resourceDetailMsg.reset();
//			resourceDetailMsg.setKey(FAILKEYS.NO_MATCH_FOUND);
//			resourceDetailMsg.setParams(new String[]{winUid});
//			setGeneralErrorWithBundleMessage(props, resourceMsg, resourceDetailMsg);
//			return;
			//We don't need the window object to highlight the component
			debug(debugPrefix+" Warning: Not found window!");
		}		
		
		Object comp = getCachedObject(compUid, true);
		if(comp == null){	
			// %1% was not successful using %2%.
			// No match found for %1%.
			debug(debugPrefix+" Error: Not found component!");
			resourceMsg.reset();
			resourceMsg.setKey(FAILKEYS.NO_SUCCESS_2);
			resourceMsg.setParams(new String[]{remoteCommand,compUid});
			resourceDetailMsg.reset();
			resourceDetailMsg.setKey(FAILKEYS.NO_MATCH_FOUND);
			resourceDetailMsg.setParams(new String[]{compUid});
			setGeneralErrorWithBundleMessage(props, resourceMsg, resourceDetailMsg);
			return;
		}
		if(_highLightTestObject(win, comp)){
			setGeneralSuccessWithSpecialInfo(props, Boolean.TRUE.toString());			
		}else{
			setGeneralErrorWithSpecialInfo(props, Boolean.FALSE.toString());
		}
	}

	private void _doClearHighLightCommand(Properties props){		
		if(_clearHighLight()){
			setGeneralSuccessWithSpecialInfo(props, Boolean.TRUE.toString());			
		}else{
			setGeneralErrorWithSpecialInfo(props, Boolean.FALSE.toString());
		}
	}
	
	/**
	 * Highlight the component on android device.
	 * 
	 * @param window		The window containing the component, not used for now.
	 * @param component		The component object to be highlighted.
	 */
	private boolean _highLightTestObject(Object window, final Object component){
		final String debugPrefix = TAG + "._highLightTestObject() ";
		
		try{
			tempSuccess = false;
			debug(debugPrefix + " Try to turn on highlight for '"+component.getClass().getSimpleName()+"'");
			if(component instanceof View){
				//Update HighLightUtil with component
				if(highlightUtil==null){
					highlightUtil = new HighLightUtil(testrunner, (View) component);
				}else{
					highlightUtil.setView((View) component);
				}
				
				//Use HighLightUtil to highlight the component
				inst.runOnMainSync(new Runnable() {
					public void run() {
						try {
							tempSuccess = highlightUtil.highLight();
						} catch (Exception e) {
							debug(debugPrefix+"Met Exception "+e.getClass().getSimpleName()+":"+e.getMessage());
							tempSuccess = false;
						}
					}
				});
			}else{
				debug(debugPrefix+"The component's class is '"+component.getClass().getSimpleName()+"'. Not know how to higglight.");
			}
			
		}catch(Exception e){
			debug(debugPrefix+"Met Exception "+e.getClass().getSimpleName()+":"+e.getMessage());
		}
		
		return tempSuccess;
	}
	
	private boolean _clearHighLight(){
		final String debugPrefix = TAG + "._clearHighLight() ";
		
		try{
			tempSuccess = false;
			debug(debugPrefix + " Try to turn off highlight.");
			
			if(highlightUtil!=null){
				inst.runOnMainSync(new Runnable() {
					public void run() {
						try {
							tempSuccess = highlightUtil.clearHighLight();
						} catch (Exception e) {
							debug(debugPrefix+"Met Exception "+e.getClass().getSimpleName()+":"+e.getMessage());
							tempSuccess = false;
						}
					}
				});
			}else{
				debug(debugPrefix+"There is no previously highlighted object. Can't clear higglight.");
			}
		}catch(Exception e){
			debug(debugPrefix+"Met Exception "+e.getClass().getSimpleName()+":"+e.getMessage());
		}
		
		return tempSuccess;
	}

	/** Used for CacheReferenceInterface instance storage. */
	private Vector chainedCache = new Vector();
	
	/** CacheReferenceInterface implementation. */
	public Object getCachedObject(String key, boolean useChain) {
		Object item = getCachedItem(cache, key);
		if(item == null && useChain){
			if(chainedCache.size()==0 && !chainedSolo){ // should never happen?
				try{
					SoloProcessor p = (SoloProcessor) testrunner.getProcessors(SAFSMessage.target_solo).firstElement();
					p.addCacheReferenceInterface(this);// OK NullPointerException
					addCacheReferenceInterface(p);
					chainedSolo = true;
				}catch(Exception x){
					debug("EngineProcessor.getCachedObject ignoring "+ x.getClass().getSimpleName()+": "+x.getMessage());					
				}
			}
			for(int i=0;i<chainedCache.size()&&item==null;i++){
				CacheReferenceInterface c = (CacheReferenceInterface) chainedCache.elementAt(i);
				item = c.getCachedObject(key, false);//avoid infinite circular references				
			}
		}
		return item;
	}

	/** CacheReferenceInterface implementation. 
	 * @see CacheReferenceInterface#addCacheReferenceInterface(CacheReferenceInterface) */
	public void addCacheReferenceInterface(CacheReferenceInterface cache) {
		if(! chainedCache.contains(cache)) {
			chainedCache.add(cache);
		}
	}

	/** CacheReferenceInterface implementation. 
	 * @see CacheReferenceInterface#removeCacheReferenceInterface(CacheReferenceInterface) */
	public void removeCacheReferenceInterface(CacheReferenceInterface cache) {
		if(chainedCache.contains(cache)) {
			chainedCache.remove(cache);
		}
	}

	/** CacheReferenceInterface implementation. 
	 * @see CacheReferenceInterface#clearCache(boolean) */
	public void clearCache(boolean useChain) {
		resetExternalModeCache(cache);
		//resetExternalModeCache(activityMonitorCache); //must not be cleared until testing is done
		if(useChain){
			for(int i=0;i<chainedCache.size();i++){
				CacheReferenceInterface c = (CacheReferenceInterface) chainedCache.elementAt(i);
				c.clearCache(false);//avoid infinite circular references				
			}
		}
	}		
}
