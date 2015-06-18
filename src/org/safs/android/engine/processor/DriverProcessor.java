/** 
 ** Copyright (C) SAS Institute, All rights reserved.
 ** General Public License: http://www.opensource.org/licenses/gpl-license.php
 **/
package org.safs.android.engine.processor;


import java.util.Properties;
import java.util.Vector;

import org.safs.android.engine.DGuiObjectVector;
import org.safs.android.engine.DSAFSTestRunner;
import org.safs.android.messenger.client.MessageResult;
import org.safs.android.remotecontrol.SAFSMessage;
import org.safs.text.FAILKEYS;
import org.safs.text.ResourceMessageInfo;

import com.jayway.android.robotium.remotecontrol.client.processor.CacheReferenceInterface;
import com.jayway.android.robotium.remotecontrol.client.processor.SoloProcessor;

import android.content.Context;
import android.os.IBinder;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;

/**
 * 
 * @author Carl Nagle, SAS Institute, Inc
 */
public class DriverProcessor extends  SAFSProcessor implements CacheReferenceInterface{

	String debugPrefix = getClass().getSimpleName();
	
	/** set to true by internal processes when SoloProcessor has been chained. 
	 * User can reset to false if they want these internal processes to retry connecting this chain. */
	public boolean chainedSolo = false;
	
	public DriverProcessor(DSAFSTestRunner testrunner) {
		super(testrunner);
	}

	/**
	 * @return True if remoteCommand is a DriverCommand referencing a GUI object.
	 */
	boolean isGuiDriverCommand(){
		return SAFSMessage.driver_onguiexistsgotoblockid.equalsIgnoreCase(remoteCommand) ||
		   SAFSMessage.driver_onguinotexistgotoblockid.equalsIgnoreCase(remoteCommand) ||	
		   SAFSMessage.driver_waitforgui.equalsIgnoreCase(remoteCommand) ||
		   SAFSMessage.driver_waitforguigone.equalsIgnoreCase(remoteCommand);					
	}
	
	/**
	 * Routes commands appropriately. Now supports searching EngineProcessor and SoloProcessor 
	 * object caches along with standard recognition strings.
	 */
	public void processProperties(Properties props) {
		String dbPrefix = debugPrefix +".processProperties(): ";
		debug(dbPrefix +"processing...");
		
		// check for GUI commands
		if(isGuiDriverCommand()){
			if(! extractComponentProperties(props)) return;			
			dgov = new DGuiObjectVector(winrec, comprec, null);			
			long timeout_time = System.currentTimeMillis() + (1000 * command_timeout);
			boolean done = false;
			if(!chainedSolo){
				try{
					SoloProcessor p = (SoloProcessor) testrunner.getProcessors(SAFSMessage.target_solo).firstElement();
					addCacheReferenceInterface(p);
					chainedSolo = true;
				}catch(Exception x){/* ignore */}
				try{ 
					EngineProcessor p = (EngineProcessor) testrunner.getProcessors(SAFSMessage.target_safs_engine).firstElement();
					addCacheReferenceInterface(p);
				}catch(Exception x){/* ignore */}
			}
			
			while (! done){
				try{ winobj = dgov.getMatchingParentObject(1);}
				catch(Exception rx){
					String stack = getStackTrace(rx);
					debug(dbPrefix + stack);			
					resourceMsg.reset();
					resourceMsg.setResourceBundleName(ResourceMessageInfo.BUNDLENAME_FAILEDTEXT);
					resourceMsg.setKey(FAILKEYS.ERROR_PERFORMING_1);
					resourceMsg.setParams(new String[]{remoteCommand});
					resourceDetailMsg.reset();
					resourceDetailMsg.setResourceBundleName(ResourceMessageInfo.BUNDLENAME_FAILEDTEXT);
					resourceDetailMsg.setKey(FAILKEYS.GENERIC_ERROR);
					resourceDetailMsg.setParams(new String[]{rx.getMessage()});
					setGeneralErrorWithBundleMessage(props, resourceMsg, resourceDetailMsg);
					return;
				}
				// support SoloProcessor and EngineProcessor cache keys as possible winrecs 
				if(winobj == null) winobj = getCachedObject(winrec, true);
				if(winobj == null){ // early exit
					if( SAFSMessage.driver_onguinotexistgotoblockid.equalsIgnoreCase(remoteCommand) ||
						SAFSMessage.driver_waitforguigone.equalsIgnoreCase(remoteCommand)){
						setGeneralSuccessWithSpecialInfo(props, null);
						return;
					}
				}else{ //winobj != null
					if(winname.equalsIgnoreCase(compname)){// or winrec.equalsIgnoreCase(comprec)?
						debug(dbPrefix+"sought child same as parent for "+ remoteCommand);			
						compobj = winobj;
					}else{
						try{ compobj = dgov.getMatchingChild(1);}
						catch(Exception rx){
							String stack = getStackTrace(rx);
							debug(dbPrefix + stack);			
							resourceMsg.reset();
							resourceMsg.setResourceBundleName(ResourceMessageInfo.BUNDLENAME_FAILEDTEXT);
							resourceMsg.setKey(FAILKEYS.ERROR_PERFORMING_1);
							resourceMsg.setParams(new String[]{remoteCommand});
							resourceDetailMsg.reset();
							resourceDetailMsg.setResourceBundleName(ResourceMessageInfo.BUNDLENAME_FAILEDTEXT);
							resourceDetailMsg.setKey(FAILKEYS.GENERIC_ERROR);
							resourceDetailMsg.setParams(new String[]{rx.getMessage()});
							setGeneralErrorWithBundleMessage(props, resourceMsg, resourceDetailMsg);
							return;
						}
					}
					// support SoloProcessor and EngineProcessor cache keys as possible comprecs 
					if(compobj == null) compobj = getCachedObject(comprec, true);
					if(compobj == null){
						if( SAFSMessage.driver_onguinotexistgotoblockid.equalsIgnoreCase(remoteCommand) ||
							SAFSMessage.driver_waitforguigone.equalsIgnoreCase(remoteCommand)){
							setGeneralSuccessWithSpecialInfo(props, null);
							return;
						}
					}else{ // compobj != null
						if( SAFSMessage.driver_onguiexistsgotoblockid.equalsIgnoreCase(remoteCommand) ||
							SAFSMessage.driver_waitforgui.equalsIgnoreCase(remoteCommand)){
							setGeneralSuccessWithSpecialInfo(props, null);
							return;
						}
					}
				}// end if winobj == null
				done = System.currentTimeMillis() > timeout_time;
				if(!done) try{ Thread.sleep(500);}catch(Exception ignore){}
			}// end while (! done)
			// exited while loop after timeout without success
			setGeneralWarningWithSpecialInfo(props, null);
			return;
		}// end if isGuiDriverCommand
			
		//handle other Driver Commands here
		if(SAFSMessage.driver_clearclipboard.equalsIgnoreCase(remoteCommand)){
			if(robot.clearClipboard()){
				setGeneralSuccess(props);
			}else{
				setGeneralError(props, "Fail to clear clipboard.");
			}
		}else if(SAFSMessage.driver_saveclipboardtofile.equalsIgnoreCase(remoteCommand) ||
				SAFSMessage.driver_verifyclipboardtofile.equalsIgnoreCase(remoteCommand) ||
				SAFSMessage.driver_assignclipboardvariable.equalsIgnoreCase(remoteCommand)){
			Object content = robot.getClipboardContent();
			debug(dbPrefix+"Got '"+content+"' from clipboard");
			if(content==null || !(content instanceof String)){
				setGeneralError(props, "The content of clipboard is null or not String!");
			}else{
				String contentStr = (String) content;
				debug(dbPrefix+"The Clipboard content: "+contentStr);
				setGeneralSuccessWithSpecialInfo(props,contentStr);
			}
		}else if(SAFSMessage.driver_setclipboard.equalsIgnoreCase(remoteCommand)){
			String content = props.getProperty(SAFSMessage.PARAM_1, "");
			debug(dbPrefix+"Set '"+content+"' to clipboard");
			if(robot.setClipboard("Content from SAFS", content)){
				setGeneralSuccess(props);
			}else{
				setGeneralError(props, "Fail to set '"+content+"' to clipboard.");
			}
		}else if(SAFSMessage.driver_takescreenshot.equalsIgnoreCase(remoteCommand)){
			_takeScreenShot(props);
		}else if(SAFSMessage.driver_hidesoftkeyboard.equalsIgnoreCase(remoteCommand)){
			_hideSoftKeyboard(props);
		}else if(SAFSMessage.driver_showsoftkeyboard.equalsIgnoreCase(remoteCommand)){
			_showSoftKeyboard(props);
		}else if(SAFSMessage.driver_clearappmapcache.equalsIgnoreCase(remoteCommand)){
			//TODO, What key we use to store the TestObject?? RS or (winName, compName)
			//In SAFSProcessor, we define a cache using (winName, compName) to store TestObject
			//clearCache(true /*useChain*/);//Clear the cache who uses RS as key to store TestObject
	    	clearTestObjectCache();//Clear the cache who uses (winName, compName) as key to store TestObject   	
    		setGeneralSuccess(props);
		}
	}

	/**
	 * The screenshot of the device will be got through AndroidDebugBridge at the remote-control computer<br>
	 * Here, this method will only return the rotation of the device.<br>
	 * As the image got by AndroidDebugBridge is always that you put your device with default direction.<br>
	 * If you rotate you device, that returned image will be upside down, left-turned or right-turned.<br>
	 * So we need this device rotation to rotate the image back<br>
	 * 
	 */
	void _takeScreenShot(Properties props){
	    String dbPrefix = debugPrefix +"_takeScreenShot(): ";
	    boolean success = false;
	    int rotation = 0;
	    try{
	    	WindowManager wm = (WindowManager)inst.getTargetContext().getSystemService(Context.WINDOW_SERVICE);
	    	int rotation_constant = wm.getDefaultDisplay().getRotation();
	    	
	    	switch (rotation_constant){
		    	case Surface.ROTATION_0: rotation = 0; break;
		    	case Surface.ROTATION_90: rotation = 90; break;
		    	case Surface.ROTATION_180: rotation = 180; break;
		    	case Surface.ROTATION_270: rotation = 270; break;
	    	}
	    	
	    	success = true;
	    }catch(Exception e){
	    	debug(dbPrefix+"Can't get the orientation status: met "+e.getClass().getSimpleName()+":"+e.getMessage());
	    	success = false;
	    }
	    
    	if(success){
    		setGeneralSuccessWithSpecialInfo(props, String.valueOf(rotation));
    	}else{
    		debug(dbPrefix +"did not successfully get the orientation status.");
    		resourceMsg.reset();
    		resourceMsg.setResourceBundleName(ResourceMessageInfo.BUNDLENAME_FAILEDTEXT);
    		resourceMsg.setKey(FAILKEYS.GENERIC_ERROR);
    		resourceMsg.setParams(new String[]{"Did not successfully get the orientation status."});
    		resourceMsg.setAltText("*** ERROR *** %1%", "%");
    		setGeneralErrorWithBundleMessage(props, resourceMsg, null);
    	}
	}
	
	/**
	 * Hide the soft keyboard.<br>
	 * 
	 */
	void _hideSoftKeyboard(Properties props){
	    String dbPrefix = debugPrefix +"_hideSoftKeyboard(): ";
	    boolean success = false;
	    
	    try{
	    	InputMethodManager imm = (InputMethodManager )inst.getTargetContext().getSystemService(Context.INPUT_METHOD_SERVICE);
	    	View focusedView = testrunner.getSolo().getCurrentActivity().getCurrentFocus();
	    	debug(dbPrefix+" got focused view "+focusedView.getClass().getName());
	    	IBinder binder = focusedView.getWindowToken();
	    	//The soft keyboard should normally be hidden, unless it was originally shown with {@link InputMethodManager#SHOW_FORCED}<br>
	    	imm.hideSoftInputFromWindow(binder, InputMethodManager.HIDE_NOT_ALWAYS);
	    	success = true;
	    }catch(NullPointerException n){
	    	debug(dbPrefix+"cannot handle soft keyboard. There may not be currently focused View accepting keyboard input.");
	    	success = false;
	    }catch(Exception e){
	    	debug(dbPrefix+"cannot hide soft keyboard: met "+e.getClass().getSimpleName()+":"+e.getMessage());
	    	success = false;
	    }
	    
    	if(success){
    		setGeneralSuccess(props);
    	}else{
    		debug(dbPrefix +"did not successfully hide soft keyboard.");
    		resourceMsg.reset();
    		resourceMsg.setResourceBundleName(ResourceMessageInfo.BUNDLENAME_FAILEDTEXT);
    		resourceMsg.setKey(FAILKEYS.GENERIC_ERROR);
    		resourceMsg.setParams(new String[]{"Did not successfully hide soft keyboard."});
    		resourceMsg.setAltText("*** ERROR *** %1%", "%");
    		setGeneralErrorWithBundleMessage(props, resourceMsg, null);
    	}
	}

	/**
     * Show the soft keyboard.<br>
	 * 
	 */
	void _showSoftKeyboard(Properties props){
	    String dbPrefix = debugPrefix +"_showSoftKeyboard(): ";
	    boolean success = false;
	    try{
	    	InputMethodManager imm = (InputMethodManager )inst.getTargetContext().getSystemService(Context.INPUT_METHOD_SERVICE);
	    	View focusedView = testrunner.getSolo().getCurrentActivity().getCurrentFocus();
	    	debug(dbPrefix+" got focused view "+focusedView.getClass().getName());
	    	//This is an implicit request to show the input window, not as the result of a direct request by the user.
	    	//The window may not be shown in this case.
	    	imm.showSoftInput(focusedView, InputMethodManager.SHOW_IMPLICIT);	    	
	    	success = true;
	    }catch(NullPointerException n){
	    	debug(dbPrefix+"cannot show soft keyboard. There may not be currently focused View accepting keyboard input.");
	    	success = false;
	    }
	    catch(Exception e){
	    	debug(dbPrefix+"cannot show soft keyboard: met "+e.getClass().getSimpleName()+":"+e.getMessage());
	    	success = false;
	    }
	    
    	if(success){
    		setGeneralSuccess(props);
    	}else{
    		debug(dbPrefix +"did not successfully show soft keyboard.");
    		resourceMsg.reset();
    		resourceMsg.setResourceBundleName(ResourceMessageInfo.BUNDLENAME_FAILEDTEXT);
    		resourceMsg.setKey(FAILKEYS.GENERIC_ERROR);
    		resourceMsg.setParams(new String[]{"Did not successfully show soft keyboard."});
    		resourceMsg.setAltText("*** ERROR *** %1%", "%");
    		setGeneralErrorWithBundleMessage(props, resourceMsg, null);
    	}
	}
	
	public MessageResult processMessage(String message) {
		// TODO Auto-generated method stub
		return null;
	}

	/** Used for CacheReferenceInterface instance storage. */
	private Vector chainedCache = new Vector();
	
	/** CacheReferenceInterface implementation. */
	public Object getCachedObject(String key, boolean useChain) {
		Object item = null;
		if(useChain){
			if(chainedCache.size() == 0 && !chainedSolo){ // should never happen?
				try{
					SoloProcessor p = (SoloProcessor) testrunner.getProcessors(SAFSMessage.target_solo).firstElement();
					addCacheReferenceInterface(p);
					chainedSolo = true;
				}catch(Exception x){
					debug("DriverProcessor.getCachedObject ignoring "+ x.getClass().getSimpleName()+": "+x.getMessage());					
				}
				try{
					EngineProcessor p = (EngineProcessor) testrunner.getProcessors(SAFSMessage.target_safs_engine).firstElement();
					addCacheReferenceInterface(p);
				}catch(Exception x){
					debug("DriverProcessor.getCachedObject ignoring "+ x.getClass().getSimpleName()+": "+x.getMessage());					
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
		if(useChain){
			for(int i=0;i<chainedCache.size();i++){
				CacheReferenceInterface c = (CacheReferenceInterface) chainedCache.elementAt(i);
				c.clearCache(false);//avoid infinite circular references				
			}
		}
	}			
}
