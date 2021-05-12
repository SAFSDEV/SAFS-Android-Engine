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

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

import org.safs.android.engine.DGuiObjectRecognition;
import org.safs.android.engine.DGuiObjectVector;
import org.safs.android.engine.DSAFSTestRunner;
import org.safs.android.remotecontrol.SAFSMessage;
import org.safs.text.FAILKEYS;
import org.safs.text.GENKEYS;
import org.safs.text.ResourceMessageInfo;

import android.view.View;

import com.jayway.android.robotium.remotecontrol.client.SoloMessage;
import com.jayway.android.robotium.remotecontrol.client.processor.ProcessorException;

/**
 * Abstract class used to process <a href="http://safsdev.sourceforge.net/sqabasic2000/SAFSReference.php?rt=T&lib=GenericMasterFunctions"  
 * target="_blank" title="SAFS Component Functions Reference" alt="SAFS ComponentFunctions Reference">ComponentFunctions</a> 
 * in the remote (on-device) SAFSTestRunner.
 * <p>
 * Each concrete subclass will extend this class and implement the required processComponentFunction method.
 * <p>
 * The SAFSTestRunner should expect to execute from Properties with the following settings:
 * <p>
 * <ul>
 * SAFSMessage.KEY_TARGET=SAFSMessage.target_safs_&lt;cf_processor><br>
 * SAFSMessage.KEY_WINNAME=&lt;window name><br>
 * SAFSMessage.KEY_WINREC=&lt;window recognition string><br>
 * SAFSMessage.KEY_COMPNAME=&lt;child component name><br>
 * SAFSMessage.KEY_COMPREC=&lt;child component recognition string><br>
 * SAFSMessage.KEY_COMMAND=&lt;cf_action><br>
 * SAFSMessage.PARAM_TIMEOUT=&lt;seconds><br>
 * </ul>
 * <p>
 * Upon a valid and complete execution the SAFSTestRunner is expected to return:
 * <p>
 * <ul>
 * SAFSMessage.KEY_ISREMOTERESULT=true<br>
 * SAFSMessage.KEY_REMOTERESULTCODE=int<br>
 * SAFSMessage.KEY_REMOTERESULTINFO=String<br>
 * </ul>
 * <p>
 * Any command returning a REMOTERESULTINFO should format it in the exact manner documented in the 
 * SAFS Keyword Reference for that command.  This processor will simply forward that info into the 
 * testRecordData used by the engine using this processor.
 * <p>
 * As specific commands require additional parameters they are sent as:
 * <p>
 * <ul>
 * SAFSMessage.PARAM_1=val<br>
 * SAFSMessage.PARAM_2=val<br>
 * etc...<br>
 * SAFSMessage.PARAM_9=val<br>
 * </ul>
 * <p>
 * @author Carl Nagle, SAS Institute, Inc.
 * 26 APR, 2012 	(LeiWang)	Add some methods to treat the keyword's parameters; 
 *                              get embedded solo, instrumentation object; log general success and failure.
 * 19 APR, 2013 	(LeiWang)	Get testObject from cache by windowName and componentName before looking by search-algorithm.<br>
 */
public abstract class TestStepProcessor extends SAFSProcessor {

	
	String debugPrefix = getClass().getSimpleName();
	
	public TestStepProcessor(DSAFSTestRunner testrunner) {
		super(testrunner);
	}

	/**
	 * Implements Component Function handling initialization by first finding 
	 * the necessary Window and Component objects before routing the call to 
	 * processComponentFunction().  The routine will issue appropriate errors 
	 * if the Window or Component names are missing, or if the recognition strings 
	 * for the objects are missing.
	 * 
	 * It call {@link #retrieveParameters(Properties)} to initialize {@link #params}<br>
	 */
	public void processProperties(Properties props) {
		String dbPrefix = debugPrefix +".processProperties(): ";
		debug(dbPrefix +"processing "+remoteCommand+" ...");

		// no search has been done in this processor chain ?
		if(!props.containsKey(KEY_COMPOBJ)){ 
			
			if(! extractComponentProperties(props)) return;
			
			dgov = new DGuiObjectVector(winrec, comprec, null);
			
			debug(dbPrefix+"preset default routing command to "+SAFSMessage.target_safs_view);
			props.setProperty(SAFSMessage.KEY_TARGET, SAFSMessage.target_safs_view);			

			int wintime = getSecsWaitForWindow();
			int cmptime = getSecsWaitForComponent();
			
			/* **************************************************************
			 * START CHANGE_TIMEOUT TESTING                                 *
			 * comment out intervening code when not debugging this feature.*			
			 ****************************************************************/
//			props.setProperty(SAFSMessage.KEY_ISREMOTERESULT, String.valueOf(true));
//			props.setProperty(SAFSMessage.KEY_CHANGETIMEOUT, String.valueOf(wintime+cmptime+command_timeout));
//			
//			if( testRunner.sendServiceResult(props)){
//				debug(dbPrefix +"CHANGE_TIMEOUT was successfully sent.");
//			}else{
//				debug(dbPrefix +"CHANGE_TIMEOUT was NOT successfully sent.");
//			}
//			props.setProperty(SAFSMessage.KEY_ISREMOTERESULT, String.valueOf(false));
//			props.remove(SAFSMessage.KEY_CHANGETIMEOUT);			
			/* **************************************************************
			 * END CHANGE_TIMEOUT TESTING                                   *
			 ****************************************************************/
				
			dgov.SINGLE_LOOP_SEARCH = false;
			try{
				if(SAFSMessage.cf_view_guidoesexist.equalsIgnoreCase(remoteCommand)||
				   SAFSMessage.cf_view_guidoesnotexist.equalsIgnoreCase(remoteCommand)){
					wintime = command_timeout;
					dgov.SINGLE_LOOP_SEARCH = true;
				}
				//TODO For guidoesexist and guidoesnotexist, should we get the object from cache?
				winobj = getTestObject(winname, winname);
				if(winobj==null){
					winobj = dgov.getMatchingParentObject(wintime);
					if(winobj!=null) setTestObject(winname, winname, winobj);
				}else{
					debug(dbPrefix + " Got window object from cache.");			
				}
				if(winobj instanceof View){
					debug(dbPrefix + " Window object ID: "+DGuiObjectRecognition.getObjectId(winobj));			
				}
			}
			catch(Exception rx){
				String stack = getStackTrace(rx);
				debug(dbPrefix + stack);			
				resourceMsg.reset();
				resourceMsg.setResourceBundleName(ResourceMessageInfo.BUNDLENAME_FAILEDTEXT);
				resourceMsg.setKey(FAILKEYS.ERROR_PERFORMING_1);
				resourceMsg.setParams(new String[]{remoteCommand});
				resourceMsg.setAltText("%1% error.","%");
				resourceDetailMsg.reset();
				resourceDetailMsg.setResourceBundleName(ResourceMessageInfo.BUNDLENAME_FAILEDTEXT);
				resourceDetailMsg.setKey(FAILKEYS.GENERIC_ERROR);
				resourceDetailMsg.setParams(new String[]{rx.getMessage()});
				resourceDetailMsg.setAltText("*** ERROR *** %1%","%");
				setGeneralErrorWithBundleMessage(props, resourceMsg, resourceDetailMsg);
				return;
			}
			if(winobj == null){
				if( ! SAFSMessage.cf_view_guidoesnotexist.equalsIgnoreCase(remoteCommand)){
					debug(dbPrefix + "window not found.");			
					resourceMsg.reset();
					resourceMsg.setResourceBundleName(ResourceMessageInfo.BUNDLENAME_FAILEDTEXT);
					resourceMsg.setKey(FAILKEYS.ERROR_PERFORMING_1);
					resourceMsg.setParams(new String[]{remoteCommand});
					resourceMsg.setAltText("%1% error.","%");
					resourceDetailMsg.reset();
					resourceDetailMsg.setResourceBundleName(ResourceMessageInfo.BUNDLENAME_FAILEDTEXT);
					resourceDetailMsg.setKey(FAILKEYS.NOT_FOUND_ON_SCREEN);
					resourceDetailMsg.setParams(new String[]{winname});
					resourceDetailMsg.setAltText("%1% was not found on screen","%");
					setGeneralErrorWithBundleMessage(props, resourceMsg, resourceDetailMsg);
					return;
				}
			}
			
			if(winname.equalsIgnoreCase(compname)){// or winrec.equalsIgnoreCase(comprec)?
				debug(dbPrefix+"sought child same as parent for "+ remoteCommand);			
				compobj = winobj;
			}else{
				try{ 
					if(SAFSMessage.cf_view_guidoesexist.equalsIgnoreCase(remoteCommand)||
					   SAFSMessage.cf_view_guidoesnotexist.equalsIgnoreCase(remoteCommand)) 
						cmptime = command_timeout;
					//TODO For guidoesexist and guidoesnotexist, should we get the object from cache?
					compobj = getTestObject(winname, compname);
					if(compobj==null){
						compobj = dgov.getMatchingChild(cmptime);
						if(compobj!=null) setTestObject(winname, compname, compobj);
					}else{
						debug(dbPrefix + " Got component object from cache.");			
					}
					if(compobj instanceof View){
						debug(dbPrefix + " Component object ID: "+DGuiObjectRecognition.getObjectId(compobj));			
					}
				}
				catch(Exception rx){
					String stack = getStackTrace(rx);
					debug(dbPrefix + stack);			
					resourceMsg.reset();
					resourceMsg.setResourceBundleName(ResourceMessageInfo.BUNDLENAME_FAILEDTEXT);
					resourceMsg.setKey(FAILKEYS.ERROR_PERFORMING_1);
					resourceMsg.setParams(new String[]{remoteCommand});
					resourceMsg.setAltText("%1% error.","%");
					resourceDetailMsg.reset();
					resourceDetailMsg.setResourceBundleName(ResourceMessageInfo.BUNDLENAME_FAILEDTEXT);
					resourceDetailMsg.setKey(FAILKEYS.GENERIC_ERROR);
					resourceDetailMsg.setParams(new String[]{rx.getMessage()});
					resourceDetailMsg.setAltText("*** ERROR *** %1%","%");
					setGeneralErrorWithBundleMessage(props, resourceMsg, resourceDetailMsg);
					return;
				}
				if(compobj == null){
					if( ! SAFSMessage.cf_view_guidoesnotexist.equalsIgnoreCase(remoteCommand)){
						debug(dbPrefix + "child not found.");			
						resourceMsg.reset();
						resourceMsg.setResourceBundleName(ResourceMessageInfo.BUNDLENAME_FAILEDTEXT);
						resourceMsg.setKey(FAILKEYS.ERROR_PERFORMING_1);
						resourceMsg.setParams(new String[]{remoteCommand});
						resourceMsg.setAltText("%1% error.","%");
						resourceDetailMsg.reset();
						resourceDetailMsg.setResourceBundleName(ResourceMessageInfo.BUNDLENAME_FAILEDTEXT);
						resourceDetailMsg.setKey(FAILKEYS.NOT_FOUND_ON_SCREEN);
						resourceDetailMsg.setParams(new String[]{compname});
						resourceDetailMsg.setAltText("%1% was not found on screen","%");
						setGeneralErrorWithBundleMessage(props, resourceMsg, resourceDetailMsg);
						return;
					}
				}
			}
			// component search has been successfully completed and 
			// should NOT be done again in this processor chain
			props.setProperty(KEY_COMPOBJ, KEY_COMPOBJ);
		}

		//Set to "Not Executed" before starting the execution
		props.setProperty(SAFSMessage.KEY_REMOTERESULTCODE, SAFSMessage.STATUS_REMOTE_NOT_EXECUTED_STRING);
		
		//Get the keyword's parameters from Properties if exist
		retrieveParameters(props);
		
		processComponentFunction(props);		
	}
	
	/**
	 * Test if the command has been executed or not.<br>
	 * 
	 * @return boolean, true if the command has not been executed.
	 */
	protected boolean commandNotExecuted(Properties props) throws ProcessorException{
		boolean notExecuted = false;
		String debugmsg = debugPrefix + ".commandNotExecuted(): ";
		
		if(props.containsKey(SAFSMessage.KEY_REMOTERESULTCODE)){
			int rc = SoloMessage.getInteger(props, SAFSMessage.KEY_REMOTERESULTCODE);
			notExecuted = (rc==SAFSMessage.STATUS_REMOTE_NOT_EXECUTED);
		}else{
			throw new ProcessorException(debugmsg+" props doesn't contain KEY_REMOTERESULTCODE");
		}
		
		return notExecuted;
	}
	
	/**
	 * Retrieve the parameters from the properties and put them in the global variable {@link #params}<br>
	 * The parameters are stored in a properties object, the key is:<br>
	 * {@link SAFSMessage#PARAM_1}<br>
	 * {@link SAFSMessage#PARAM_2}<br>
	 * and so on.<br>
	 * 
	 * @param props	Properties contain the parameters sent from 'remote control side'
	 */
	protected void retrieveParameters(Properties props){
		String debugmsg = debugPrefix + ".retrieveParameters(): ";

		if(params!=null){
			params.clear();
		}else{
			params = new ArrayList<String>();
		}
		
		debug(debugmsg+" try to get parameters from properties.");
		//keys are "param1", "param2", "param3" etc.
		int i = 1;
		String key = SAFSMessage.PARAM_1.substring(0, SAFSMessage.PARAM_1.length()-1);
		String tmpkey = key+(String.valueOf(i++).trim());
		String val = null;
		
		while(props.containsKey(tmpkey)){
			try {
//				debug(debugmsg+" try to get parameter for '"+tmpkey+"'");
				val = SoloMessage.getString(props, tmpkey);
			} catch (ProcessorException e) {
				debug(debugmsg+" fail to get parameter for '"+tmpkey+"'");
				val = "";
			}
//			debug(debugmsg+" got parameter '"+val+"' for '"+tmpkey+"'");
			params.add(val);
			tmpkey = key+(String.valueOf(i++).trim());
		}
		
		debug(debugmsg+" got "+params.size()+" parameters from properties.");
	}
	
	/**
	 * To check if the parameter's size is enough.<br>
	 * If the parameter size if not enough, we set general error with resource message.<br>
	 * and return false. When user call this method and get a false, user is happy to
	 * return without setting the error in the properties.<br>
	 * 
	 * @param size, int, the minimum number of parameters
	 * @param props Properties, contains the output results
	 * @return boolean, true if {@link #params} contain engough parameters.
	 */
	protected boolean checkParameterSize(int size, Properties props){
		String debugmsg = debugPrefix + ".checkParameterSize(): ";
		
		int realSize = 0;
		if(params!=null) realSize = params.size();
		
		if(realSize < size ){
			debug(debugmsg + " '"+realSize+"' parameters are not enough. Should be at least '"+size+"'");
			
		    List<String> messageParams = new ArrayList<String>();
		    messageParams.add(remoteCommand);
		    messageParams.add(Integer.toString(realSize));
		
		    String key = FAILKEYS.PARAMSIZE_3;
			if (winname !=null && compname != null) {
				messageParams.add(winname+":"+compname);
			} else {
				messageParams.add(getClass().getName());
			}
			
			resourceMsg.reset();
			resourceMsg.setResourceBundleName(ResourceMessageInfo.BUNDLENAME_FAILEDTEXT);
			resourceMsg.setKey(key);
			resourceMsg.setParams(messageParams);
			resourceMsg.setAltText("%1%, wrong number of parameters: %2%, %3%","%");
			setGeneralErrorWithBundleMessage(props, resourceMsg, null);
			return false;
		}
		
		return true;
	}
	
	/**
	 * To check if the Solo object can be got or not.<br>
	 * If the Solo object can be got, we set general error with resource message.<br>
	 * and return false. When user call this method and get a false, user is happy to
	 * return without setting the error in the properties.<br>
	 * 
	 * @param props Properties, contains the output results
	 * @return boolean, true if {@link #solo} is got correclty.
	 */
	protected boolean checkSolo(Properties props){
		String debugmsg = debugPrefix + ".checkSolo(): ";
		if(solo!=null) return true;
		
		solo = getSolo();
		
		if(solo==null){
			debug(debugmsg+" Can't get Solo object.");
			// %1% support may not be properly initialized!
			resourceMsg.reset();
			resourceMsg.setResourceBundleName(ResourceMessageInfo.BUNDLENAME_FAILEDTEXT);
			resourceMsg.setKey(FAILKEYS.SUPPORT_NOT_INITIALIZED);
			resourceMsg.addParameter("Messenger/Solo");
			resourceMsg.setAltText("%1% support may not be properly initialized!","%");
			setGeneralErrorWithBundleMessage(props, resourceMsg, null);
			return false;
		}
		
		return true;
	}
	
	/**
	 * Set a success result and send back success message as following:<br>
	 * winname:compname remoteCommand successful.<br>
	 * 
	 * @param props Properties, contains the output results
	 */
	protected void setGeneralSuccessWithBundle(Properties props){
		resourceMsg.reset();
		resourceMsg.setResourceBundleName(ResourceMessageInfo.BUNDLENAME_GENERICTEXT);
		resourceMsg.setKey(GENKEYS.SUCCESS_3);
		resourceMsg.addParameter(winname);
		resourceMsg.addParameter(compname);
		resourceMsg.addParameter(remoteCommand);
		resourceMsg.setAltText("%1%:%2% %3% successful.","%");
		setGeneralSuccessWithBundleMessage(props, resourceMsg, null);
	}
	
	/**
	 * Set a error result and send back error message as following:<br>
	 * winname:compname remoteCommand processed with a negative result.<br>
	 * The parameter 'detail' of this method will contain the detail error<br>
	 * message, and it will be also sent back<br>
	 * 
	 * @param props Properties, contains the output results
	 * @param detail ResourceMessageInfo, contain the detail error message.
	 */
	protected void setGeneralErrorWithDetailBundle(Properties props, ResourceMessageInfo detail){
		resourceMsg.reset();
		resourceMsg.setKey(FAILKEYS.EXECUTED_WITH_NEGATIVERESULT);
		resourceMsg.addParameter(winname);
		resourceMsg.addParameter(compname);
		resourceMsg.addParameter(remoteCommand);
		resourceMsg.setAltText("%1%:%2% %3% processed with a negative result.","%");
		setGeneralErrorWithBundleMessage(props, resourceMsg, detail);
	}
	
	/**
	 * Set a error result and send back error message as following:<br>
	 * Unable to perform remoteCommand on compname in winname.<br>
	 * The parameter 'detail' of this method will contain the detail error<br>
	 * message, and it will be also sent back<br>
	 * 
	 * @param props Properties, contains the output results
	 * @param detail ResourceMessageInfo, contain the detail error message.
	 */
	protected void setGeneralError3(Properties props, ResourceMessageInfo detail){
		resourceMsg.reset();
		resourceMsg.setKey(FAILKEYS.FAILURE_3);
		resourceMsg.addParameter(winname);
		resourceMsg.addParameter(compname);
		resourceMsg.addParameter(remoteCommand);
		resourceMsg.setAltText("Unable to perform %3% on %2% in %1%.","%");
		setGeneralErrorWithBundleMessage(props, resourceMsg, detail);
	}
	
	/**
	 * Set a error result and send back error message as following:<br>
	 * *** ERROR *** Error Message.<br>
	 * 
	 * @param props 	Properties, contains the output results
	 * @param errorMsg 	String, the generic error message.
	 */
	protected void setGenericError(Properties props, String errorMsg){
		resourceMsg.reset();
		resourceMsg.setKey(FAILKEYS.GENERIC_ERROR);
		resourceMsg.addParameter(errorMsg);
		resourceMsg.setAltText("*** ERROR *** %1%","%");
		setGeneralErrorWithBundleMessage(props, resourceMsg, null);
	}
	
	/**
	 * Calls {@link #setGeneralErrorWithBundleMessage(Properties, ResourceMessageInfo, ResourceMessageInfo)} 
	 * issuing the general error with the additional detail that the compobj is NOT of a supported type 
	 * for the action requested.
	 * @param props
	 */
	protected void setGeneralErrorUnsupportedObjectType(Properties props){
		String compClassName = compobj.getClass().getName();
		resourceDetailMsg.reset();
		resourceDetailMsg.setResourceBundleName(ResourceMessageInfo.BUNDLENAME_FAILEDTEXT);
		resourceDetailMsg.setKey(FAILKEYS.NO_ACTION_FOR_TYPE);
		resourceDetailMsg.addParameter(remoteCommand);
		resourceDetailMsg.addParameter(getClass().getSimpleName());
		resourceDetailMsg.addParameter(compClassName);
		resourceDetailMsg.setAltText("%1% not supported in %2% for %3%.","%");
		setGeneralErrorWithDetailBundle(props, resourceDetailMsg);
	}
	
    /** 
     * Convert coordinates string of the formats:
     * <ul>
     * <li>"x;y"
	 * <li>"x,y"
	 * <li>"x y"
	 * <li>"Coords=x;y"
	 * <li>"Coords=x,y"
	 * <li>"Coords=x y"
	 * </ul> 
	 * into a java.awt.Point object.
	 * <p>
     * Subclasses may override to convert alternative values, such 
     * as Row and Col values as is done in org.safs.rational.CFTable
     * 
     * @param   coords, String x;y or x,y or Coords=x;y  or Coords=x,y
     * @return  String[]{x,y}, or String[0]{} if invalid
     **/
    public String[] convertCoords(String coords) {
    	String debugmsg = debugPrefix + ".convertCoords(): ";    	
	    try {
	    	String ncoords = new String(coords);
    		int coordsindex = coords.indexOf("=");
    		if(coordsindex > 0) ncoords = ncoords.substring(coordsindex+1);
    		ncoords=ncoords.trim();
    		debug(debugmsg+"working with coords: "+ coords +" prefix stripped to: "+ncoords);
    		
    		int sindex = ncoords.indexOf(";");
      		if (sindex < 0) sindex = ncoords.indexOf(",");
      		boolean isspace = false;
      		if(sindex < 0){
      			sindex = ncoords.indexOf(" ");
      			isspace = (sindex > 0);
	    	}
      		if (sindex < 0){
      			debug(debugmsg+"invalid coords: "+ ncoords +"; no separator detected.");
      			return new String[0];
      		}

			// properly handles case where coordsindex = -1 (not found)
		    String xS = null;
      		String yS = null;
      		if(isspace){
      			debug(debugmsg+"converting space-delimited coords: "+ ncoords);
      			StringTokenizer toker = new StringTokenizer(ncoords, " ");
      			if(toker.countTokens() < 2) {
      				debug(debugmsg+"invalid space-delimited coords: "+ ncoords);
          			return new String[0];
      			}
      			xS = toker.nextToken();
      			yS = toker.nextToken();
      		}else{
      			xS = ncoords.substring(0, sindex).trim();
      			yS = ncoords.substring(sindex+1).trim();
      		}
      		if ((xS.length()==0)||(yS.length()==0)){
      			debug(debugmsg+"invalid coordinate substrings  "+ xS +","+ yS);
      			return new String[0];
      		}

      		debug(debugmsg+"x: "+xS);
      		debug(debugmsg+"y: "+yS);

      		int y = (int) Float.parseFloat(yS);
      		int x = (int) Float.parseFloat(xS);
      		
      		debug(debugmsg+"converted coords: x: "+x+", y: "+y);
        	return new String[]{String.valueOf(x), String.valueOf(y)};

	    } catch (Exception ee) {
	    	debug(debugmsg+"bad coords format: "+ coords);
  			return new String[0];
    	}
    }


	/**
	 * Called internally by processProperties after the DGOV, WIN, and COMP components 
	 * have been found and are ready for action.
	 * 
	 * @param props
	 */
	protected abstract void processComponentFunction(Properties props);
}
