/** 
 ** Copyright (C) SAS Institute, All rights reserved.
 ** General Public License: http://www.opensource.org/licenses/gpl-license.php
 **/
package org.safs.android.engine.processor;

import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;

import org.safs.android.engine.DGuiClassData;
import org.safs.android.engine.DGuiObjectVector;
import org.safs.android.engine.DSAFSTestRunner;
import org.safs.android.remotecontrol.SAFSMessage;
import org.safs.text.FAILKEYS;
import org.safs.text.ResourceMessageInfo;

import android.app.Instrumentation;

import com.jayway.android.robotium.remotecontrol.client.SoloMessage;
import com.jayway.android.robotium.remotecontrol.client.processor.AbstractProcessor;
import com.jayway.android.robotium.remotecontrol.client.processor.ProcessorException;
import com.robotium.solo.Solo;

/**
 * 
 * @author Carl Nagle, SAS Institute, Inc
 * <p>
 * 26 APR, 2012 	(Lei Wang)	Add 2 methods to return the embedded Solo object and Instrumentation object.<br>
 * 07 FEB, 2013 	(Lei Wang)	Move initialization of DGuiClassData to method {@link DSAFSTestRunner#beforeStart()}.<br>
 * 19 APR, 2013 	(Lei Wang)	Add cache to store testObject by windowName and componentName.<br>
 */
 public class SAFSProcessor extends AbstractProcessor {
	 public static String TAG = SAFSProcessor.class.getSimpleName();
	 
	/**
	 * A 2nd field of superClass testRunner cast here to type DSAFSTestRunner for convenience.
	 */
	protected DSAFSTestRunner testrunner = null;
	
	protected SAFSRobot robot = null;
	
	/**
	 * KEY used in the Properties object passed between processors.
	 * If this KEY exists in the Properties object then chained processors 
	 * know the first processor has already completed the component search 
	 * and it does NOT need to be done again.
	 */
	protected static final String KEY_COMPOBJ = "compfound";
	
	/**
	 * local cache for containing the test objects.
	 * This cache contains pairs as <windownName, Hashtable<componentName, testObject>>
	 * 
	 * <b>Note:</b> Don't manipulate it directly like testObjectCache.get(key) etc.<br>
	 * Use the cache-manipulation-methods defined in {@link AbstractProcessor}<br>
	 * @see AbstractProcessor#getCachedItem(Hashtable, Object)
	 * @see AbstractProcessor#removeCachedItem(Hashtable, Object)
	 * @see AbstractProcessor#putCachedItem(Hashtable, Object, Object)
	 */
	protected static Hashtable<String,Hashtable<String, Object>> testObjectCache = 
		new Hashtable<String/*windownName*/,Hashtable<String/*componentName*/, Object/*testObject*/>>(50);
	
	/**
	 * According to windowName and componentName, try to get the testObject<br>
	 * from the cache. null will be return if no testObject can be found.<br>
	 */
	public Object getTestObject(String windowName, String componentName){
		Object testobject = null;
		
		//According to window name, we get a cache containing pair <name, testObjecct>
		Object cacheobject = getCachedItem(testObjectCache, windowName);
		Hashtable<String, Object> cache = null;
		if(cacheobject instanceof Hashtable){
			cache = (Hashtable<String, Object>) cacheobject;
			testobject = getCachedItem(cache, componentName);
		}

		return testobject;
	}
	
	/**
	 * According to windowName and componentName, set the testObject to<br>
	 * the cache.<br>
	 */
	public Object setTestObject(String windowName, String componentName, Object testobject){
		
		Object cacheobject = getCachedItem(testObjectCache, windowName);
		Hashtable<String, Object> cache = null;
		//If there is no cache related to windowName, create a new one and put it in testObjectCache
		//with windowName as the key.
		if(cacheobject==null){
			cacheobject = new Hashtable<String, Object> (INITIAL_CACHE_SIZE);
			putCachedItem(testObjectCache, windowName, cacheobject);
		}
		if(cacheobject instanceof Hashtable){
			cache = (Hashtable<String, Object>) cacheobject;
			putCachedItem(cache, componentName, testobject);
		}

		return testobject;
	}
	
	public void clearTestObjectCache(){
		testObjectCache.clear();
	}
	
	// shared by ALL Component Function Processor subclasses and chains
	protected static int secsWaitForWindow = 30;
	protected static int secsWaitForComponent = 30;
	protected static int command_timeout = 30;

	// shared by ALL Component Function Processor subclasses and chains
	protected static Object winobj = null;
	protected static Object compobj = null;	
	protected static String winname = null;
	protected static String compname = null;
	protected static String winrec = null;
	protected static String comprec = null;
	protected static DGuiObjectVector dgov = null;
	protected static DGuiClassData dgcd = null;
	
	/** solo is the Solo object embedded in testrunner
	 *  it will be used to do the real automation work 
	 *  Solo object is provided by Robotium*/
	protected Solo solo = null;
	/** ints is the Instrumentation object embedded in testrunner
	 *  it will be used to do the real automation work 
	 *  Instrumentation object is provided by Android*/
	protected Instrumentation inst = null;
	
	/** params contain the parameters of the keyword to be processed */
	protected Collection<String> params = null;
	public Collection<String> getParams () { return params; }
	public void       setParams (Collection<String> params) {this.params = params;}
	
	/**  @return the secsWaitForWindow */
	public static int getSecsWaitForWindow() {	return secsWaitForWindow; }

	/** @param secsWaitForWindow the secsWaitForWindow to set (in seconds) */
	public static void setSecsWaitForWindow(int secsWaitForWindow) {
		TestStepProcessor.secsWaitForWindow = secsWaitForWindow;
	}

	/** @return the secsWaitForComponent */
	public static int getSecsWaitForComponent() { return secsWaitForComponent; }

	/** @param secsWaitForComponent the secsWaitForComponent to set (in seconds) */
	public static void setSecsWaitForComponent(int secsWaitForComponent) {
		TestStepProcessor.secsWaitForComponent = secsWaitForComponent;
	}

	/**
	 * Load our local component name and recognition static fields with the 
	 * property string values from:
	 * <p>
	 * winname = KEY_WINNAME,<br>
	 * compname = KEY_COMPNAME,<br>
	 * winrec = KEY_WINREC,<br>
	 * comprec = KEY_COMPREC,<br>
	 * command_timeout = PARAM_TIMEOUT<br>
	 * @param props
	 * @return true if all were retrievable.  If any were not present (except command_timeout) 
	 * then the routine will set appropriate {@link #resourceMsg} values and 
	 * {@link #setGeneralErrorWithBundleMessage(Properties, ResourceMessageInfo, ResourceMessageInfo)} 
	 * and return false.
	 */
	protected boolean extractComponentProperties(Properties props){
		String dbPrefix = getClass().getSimpleName()+".extractComponentInfo(): ";
		// no search has been done in this processor chain
		try{ winname = SoloMessage.getString(props, SAFSMessage.KEY_WINNAME); }
		catch(ProcessorException x){
			debug(dbPrefix+"Missing 'WINDOW' parameter for "+ remoteCommand);			
			// %1%, wrong number of parameters: %2%.
			resourceMsg.reset();
			resourceMsg.setResourceBundleName(ResourceMessageInfo.BUNDLENAME_FAILEDTEXT);
			resourceMsg.setKey(FAILKEYS.PARAMSIZE_2);
			resourceMsg.setParams(new String[]{remoteCommand, "WINDOW"});
			setGeneralErrorWithBundleMessage(props, resourceMsg, null);
			return false;
		}
		try{ winrec = SoloMessage.getString(props, SAFSMessage.KEY_WINREC);}
		catch(ProcessorException x){
			debug(dbPrefix+"Missing 'WINREC' parameter for "+ remoteCommand);			
			// %1%, wrong number of parameters: %2%.
			resourceMsg.reset();
			resourceMsg.setResourceBundleName(ResourceMessageInfo.BUNDLENAME_FAILEDTEXT);
			resourceMsg.setKey(FAILKEYS.PARAMSIZE_2);
			resourceMsg.setParams(new String[]{remoteCommand, "WINREC"});
			setGeneralErrorWithBundleMessage(props, resourceMsg, null);
			return false;
		}
		try{ compname = SoloMessage.getString(props, SAFSMessage.KEY_COMPNAME);}
		catch(ProcessorException x){
			debug(dbPrefix+"Missing 'COMPONENT' parameter for "+ remoteCommand);			
			// %1%, wrong number of parameters: %2%.
			resourceMsg.reset();
			resourceMsg.setResourceBundleName(ResourceMessageInfo.BUNDLENAME_FAILEDTEXT);
			resourceMsg.setKey(FAILKEYS.PARAMSIZE_2);
			resourceMsg.setParams(new String[]{remoteCommand, "COMPONENT"});
			setGeneralErrorWithBundleMessage(props, resourceMsg, null);
			return false;
		}
		try{ comprec = SoloMessage.getString(props, SAFSMessage.KEY_COMPREC);}
		catch(ProcessorException x){
			debug(dbPrefix+"Missing 'COMPREC' parameter for "+ remoteCommand);			
			// %1%, wrong number of parameters: %2%.
			resourceMsg.reset();
			resourceMsg.setResourceBundleName(ResourceMessageInfo.BUNDLENAME_FAILEDTEXT);
			resourceMsg.setKey(FAILKEYS.PARAMSIZE_2);
			resourceMsg.setParams(new String[]{remoteCommand, "COMPREC"});
			setGeneralErrorWithBundleMessage(props, resourceMsg, null);
			return false;
		}
		try{ command_timeout = SoloMessage.getInteger(props, SAFSMessage.PARAM_TIMEOUT);}
		catch(ProcessorException x){ 
			/* ignoring */ 
			debug(dbPrefix+"Ignoring missing 'TIMEOUT' parameter for "+ remoteCommand);			
		}	
		return true;
	}
	
	/**
	 * resourceMsg is used to contain information of NLS message in resource bundle
	 * 
	 * @see {@link #setGeneralErrorWithBundleMessage(java.util.Properties, ResourceMessageInfo, ResourceMessageInfo)}<br>
	 * @see {@link #setGeneralSuccessWithBundleMessage(java.util.Properties, ResourceMessageInfo, ResourceMessageInfo)}<br>
	 */
	protected ResourceMessageInfo resourceMsg = new ResourceMessageInfo();

	/**
	 * resourceMsg is used to contain information of NLS detail message in resource bundle
	 * 
	 * @see {@link #setGeneralErrorWithBundleMessage(java.util.Properties, ResourceMessageInfo, ResourceMessageInfo)}<br>
	 * @see {@link #setGeneralSuccessWithBundleMessage(java.util.Properties, ResourceMessageInfo, ResourceMessageInfo)}<br>
	 */
	protected ResourceMessageInfo resourceDetailMsg = new ResourceMessageInfo();
	
	public SAFSProcessor(DSAFSTestRunner testrunner) {
		super(testrunner);
		this.testrunner = testrunner;
		getInstrumentation();
		if(robot==null){
			robot = new SAFSRobot(testrunner);
		}
	}
	
	public static DGuiClassData getDgcd() {
		return dgcd;
	}
	public static void setDgcd(DGuiClassData dgcd) {
		SAFSProcessor.dgcd = dgcd;
	}
	/**
	 * Return the Solo object contained in {@link #testrunner}<br>
	 * Attention:<br>
	 * Only after the method {@link DSAFSTestRunner#launchApplication()} has been<br>
	 * called, the Solo object can be initialized.<br>
	 * Before, the Solo object is null.<br>
	 * 
	 * @return Solo object or null if the Solo object is not ready.
	 * @see DSAFSTestRunner#launchApplication()
	 */
	public Solo getSolo(){		
		try{
			solo = testrunner.getSolo();
		}catch(Exception e){
			debug("getSolo(): met Exception="+e.getMessage());
		}
		
		return solo;
	}

	/**
	 * Set the Solo object to be used for processing.  
	 * This is generally only set by other processors already having retrieved it.
	 * @param solo
	 */
	public void setSolo(Solo solo){
		this.solo = solo;
	}
	
	/**
	 * The field {@link #testrunner} is the Instrumentation object.<br>
	 * The Instrumentation object can be used to do the automation directly on Android GUI object.
	 * 
	 * @return Instrumentation object.
	 */
	public Instrumentation getInstrumentation(){		
		try{
			inst = testrunner;
		}catch(Exception e){
			debug("getInstrumentation(): met Exception="+e.getMessage());
		}		
		return inst;
	}
	
	/**
	 * set the isremoteresult to "true"<br>
	 * set the remoteresultcode to a constant code {@link SoloMessage#STATUS_REMOTERESULT_OK_STRING}<br>
	 * 
	 * <b>Note:</b> This method will concatenate {@link #remoteCommand} with <br>
	 * {@link SoloMessage#RESULT_INFO_GENERAL_SUCCESS}<br> and set this string to remoteresultinfo<br>
	 * 
	 * set the 'resource bundle information', so that the remote-controller can log NLS message.<br>
	 * 
	 * @param props					The Properties object containing the in and out parameters
	 * @param message				ResourceMessageInfo, information about the resource bundle message
	 * @param detailMessage			ResourceMessageInfo, information about the resource bundle detail message
	 * 
	 * @see #setResourceBundleInformation(Properties, ResourceMessageInfo, ResourceMessageInfo))
	 */
	protected void setGeneralSuccessWithBundleMessage(Properties props,
			                                          ResourceMessageInfo message,
			                                          ResourceMessageInfo detailMessage){
		String debugPrefix = TAG+".setGeneralSuccessWithBundleMessage() ";
		
		setGeneralSuccess(props);
		
		if(!setResourceBundleInformation(props, message, detailMessage)){
			debug(debugPrefix+" Failed to set resource bundle's information!");
		}

	}
	
	/**
	 * set the isremoteresult to "true"<br>
	 * set the remoteresultcode to a constant code {@link SoloMessage#STATUS_REMOTERESULT_FAIL_STRING}<br>
	 * 
	 * <b>Note:</b> This method will concatenate {@link #remoteCommand} with 'resultInfo', then with <br>
	 * {@link SoloMessage#RESULT_INFO_GENERAL_FAIL}<br> and set this string to remoteresultinfo<br>
	 * 
	 * set the 'resource bundle information', so that the remote-controller can log NLS message.<br>
	 * 
	 * @param props			The Properties object containing the in and out parameters
	 * @param message		ResourceMessageInfo, information about the resource bundle message
	 * @param detailMessage	ResourceMessageInfo, information about the resource bundle detail message
	 * 
	 * @see #setResourceBundleInformation(Properties, ResourceMessageInfo, ResourceMessageInfo))
	 */
	protected void setGeneralErrorWithBundleMessage(Properties props, 
			                                        ResourceMessageInfo message, 
			                                        ResourceMessageInfo detailMessage){
		String debugPrefix = TAG+".setGeneralErrorWithBundleMessage() ";
		
		setGeneralError(props, "");

		if(!setResourceBundleInformation(props, message, detailMessage)){
			debug(debugPrefix+" Failed to set resource bundle's information!");
		}
	}
	
	/**
	 * This method will set the following parameters to props<br>
	 * {@link SAFSMessage#RESOURCE_BUNDLE_KEY_FOR_MSG}<br>
	 * {@link SAFSMessage#RESOURCE_BUNDLE_NAME_FOR_MSG}<br>
	 * {@link SAFSMessage#RESOURCE_BUNDLE_PARAMS_FOR_MSG}<br>
	 * {@link SAFSMessage#RESOURCE_BUNDLE_KEY_FOR_DETAIL_MSG}<br>
	 * {@link SAFSMessage#RESOURCE_BUNDLE_NAME_FOR_DETAIL_MSG}<br>
	 * {@link SAFSMessage#RESOURCE_BUNDLE_PARAMS_FOR_DETAIL_MSG}<br>
	 * The remote-controller will get these values to retrieve an NLS message from resource bundle.<br>
	 * 
	 * This method will be called internally by<br>
	 * {@link #setGeneralErrorWithBundleMessage(Properties, ResourceMessageInfo, ResourceMessageInfo))}<br>
	 * {@link #setGeneralSuccessWithBundleMessage(Properties, ResourceMessageInfo, ResourceMessageInfo))<br>
	 * 
	 * @param props			Properties,	contains the parameters to be sent back to remote-controller
	 * @param message		ResourceMessageInfo (required), information about the resource bundle message
	 * @param detailMessage	ResourceMessageInfo (optional), information about the resource bundle detail message
	 * 
	 * @see #setResourceBundleInformation(Properties, ResourceMessageInfo, boolean)
	 */
	private boolean setResourceBundleInformation(Properties props, 
			                                     ResourceMessageInfo message /*required*/,
			                                     ResourceMessageInfo detailMessage /*optional*/){
		String debugPrefix = TAG+".setResourceBundleInformation() ";
		
		//Maybe we can pass a list of ResourceMessageInfo
		//props.setProperty(SAFSMessage.MESSAGE_SUFFIX, "Message");
		//props.setProperty(SAFSMessage.RESOURCE_BUNDLE_NAME+"Message", bundldName);

		//props.setProperty(SAFSMessage.MESSAGE_SUFFIX, "DetailMessage");
		//props.setProperty(SAFSMessage.RESOURCE_BUNDLE_NAME+"DetailMessage", detailBundldName);

		//At remote-controller side
		//String suffix = props.getProperty(SAFSMessage.SAFSMessage.MESSAGE_SUFFIX);
		//String bundleName = props.getProperty(SAFSMessage.RESOURCE_BUNDLE_NAME+suffix);
		
		//set the optional 'detail message'
		if(detailMessage!=null){
			debug(debugPrefix + " set the 'detail message'");				
			if(!setResourceBundleInformation(props, detailMessage, true)){
				debug(debugPrefix + " fail to set the 'detail message'");				
			}
		}
		
		return setResourceBundleInformation(props, message, false);
	}
	
	/**
	 * This method will set the following parameters to props<br>
	 * {@link SAFSMessage#RESOURCE_BUNDLE_KEY_FOR_MSG}<br>
	 * {@link SAFSMessage#RESOURCE_BUNDLE_NAME_FOR_MSG}<br>
	 * {@link SAFSMessage#RESOURCE_BUNDLE_PARAMS_FOR_MSG}<br>
	 * {@link SAFSMessage#RESOURCE_BUNDLE_KEY_FOR_DETAIL_MSG}<br>
	 * {@link SAFSMessage#RESOURCE_BUNDLE_NAME_FOR_DETAIL_MSG}<br>
	 * {@link SAFSMessage#RESOURCE_BUNDLE_PARAMS_FOR_DETAIL_MSG}<br>
	 * The remote-controller will get these values to retrieve an NLS message from resource bundle.<br>
	 * 
	 * This method will be called internally by<br>
	 * {@link #setResourceBundleInformation(Properties, ResourceMessageInfo, ResourceMessageInfo)<br>
	 * 
	 * @param props				Properties,	contains the parameters to be sent back to remote-controller
	 * @param message			ResourceMessageInfo, information about the resource bundle message
	 * @param isDetailMessage	boolean, if the message is 'detail message'
	 */
	private boolean setResourceBundleInformation(Properties props,
												 ResourceMessageInfo message,
												 boolean isDetailMessage) {
		String debugPrefix = TAG + ".setResourceBundleInformation(Properties, ResourceMessageInfo, boolean) ";

		if(message==null){
			debug(debugPrefix + " the parameter ResourceMessageInfo 'message' is null!");
			return false;
		}
		
		String bundldname = message.getResourceBundleName();
		String key = message.getKey();
		List<String> params = message.getParams();
		String alttext = message.getAltText();

		// Set required resource key
		if (key == null) {
			debug(debugPrefix + " The resource key is null, you MUST provide one.");
			return false;
		}
		if(isDetailMessage){
			props.setProperty(SAFSMessage.RESOURCE_BUNDLE_KEY_FOR_DETAIL_MSG, key);			
		}else{
			props.setProperty(SAFSMessage.RESOURCE_BUNDLE_KEY_FOR_MSG, key);
		}

		// Set possible resource bundle name
		if (bundldname != null) {
			debug(debugPrefix + " Get resource message from bundle '" + bundldname + "'");
			if(isDetailMessage){
				props.setProperty(SAFSMessage.RESOURCE_BUNDLE_NAME_FOR_DETAIL_MSG, bundldname);				
			}else{
				props.setProperty(SAFSMessage.RESOURCE_BUNDLE_NAME_FOR_MSG, bundldname);								
			}
		}

		// Set possible alternative text
		if (alttext != null) {
			debug(debugPrefix + " The alternative message is '" + alttext + "'");
			if(isDetailMessage){
				props.setProperty(SAFSMessage.RESOURCE_BUNDLE_ALTTEXT_FOR_DETAIL_MSG, alttext);
			}else{
				props.setProperty(SAFSMessage.RESOURCE_BUNDLE_ALTTEXT_FOR_MSG, alttext);
			}
		}
		
		// Set possible parameters
		if (params == null) {
			debug("You didn't set any parameter for resource key '" + key + "'.");
		} else {
			try {
				String delimitedParams = SoloMessage.convertToDelimitedString(params.toArray(new String[0]));
				debug(debugPrefix + " delimitedParams=" + delimitedParams);
				if(isDetailMessage){
					props.setProperty(SAFSMessage.RESOURCE_BUNDLE_PARAMS_FOR_DETAIL_MSG, delimitedParams);					
				}else{
					props.setProperty(SAFSMessage.RESOURCE_BUNDLE_PARAMS_FOR_MSG, delimitedParams);					
				}
			} catch (IllegalThreadStateException e) {
				debug(debugPrefix + " During convert a list of parameters to a delimited string: Exception=" + e.getMessage());
			}
		}

		return true;
	}	
}
