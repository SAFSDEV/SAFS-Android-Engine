/** 
 ** Copyright (C) SAS Institute, All rights reserved.
 ** General Public License: http://www.opensource.org/licenses/gpl-license.php
 **/
package org.safs.android.engine.processor;

import java.util.Properties;

import org.safs.android.engine.DSAFSTestRunner;
import org.safs.android.remotecontrol.SAFSMessage;

import android.widget.CheckBox;

/**
 * 
 * @author Carl Nagle, SAS Institute, Inc
 * 29 AUG, 2012		Lei Wang	Implement for keyword 'check' and 'uncheck' 
 */
public class CheckBoxProcessor extends  TestStepProcessor{
	private CheckBox checkbox = null;
	
	public CheckBoxProcessor(DSAFSTestRunner testrunner) {
		super(testrunner);
	}

	public void processComponentFunction(Properties props) {
		String dbPrefix = debugPrefix +".processComponentFunction(): ";
		boolean success = false;
		
		if(!checkSolo(props)){
			debug(dbPrefix+"CANNOT get the Solo object.");
			return;
		}		
		if(!(compobj instanceof CheckBox)){
			// not handled here, maybe in a chained ViewProcessor?
			debug(dbPrefix+"skipped for object not instanceof CheckBox.");
			return;
		}
		try{
			checkbox = (CheckBox) compobj;
			
			String text = checkbox.getText().toString();
			debug(dbPrefix+"Trying to process command '"+remoteCommand+"' on checkbox '"+text+"'");
			
			if(SAFSMessage.cf_comprouting_check.equalsIgnoreCase(remoteCommand)){
				success = setCheck(true);
			}else if(SAFSMessage.cf_comprouting_uncheck.equalsIgnoreCase(remoteCommand)){
				success = setCheck(false);
			}else{
				// not handled here, maybe in a chained ViewProcessor?
				debug(dbPrefix+"command '"+remoteCommand+"' is not processed by CheckBoxProcessor.");
				return;
			}
			
			if(success){
				setGeneralSuccessWithBundle(props);
			}else{
				debug(dbPrefix+"command '"+remoteCommand+"' failed.");
				setGeneralError3(props, null);
			}
			// route Results to the controller side CFCheckBoxFunctions library
			if(commandNotExecuted(props)){ /*ignore*/ }
			else props.setProperty(SAFSMessage.KEY_TARGET, SAFSMessage.target_safs_checkbox);
			
		}catch(Throwable x){
			String stackout = getStackTrace(x);
			debug(dbPrefix+ "\n"+ stackout);
			setGenericError(props, stackout);
			return;
		}
	}

	/**
	 * Set the checkbox's status to checked or un-checked.
	 * 
	 * @param 	boolean, the status to set to check box.
	 * @return	boolean, if the status is set successfully, return true.
	 */
	private boolean setCheck(final boolean checked){
		String dbPrefix = debugPrefix +".setCheck(): ";
		boolean success = false;
		
		try{
			if(checkbox.isChecked()!=checked){
				solo.clickOnView(checkbox);
			}
			success = true;
		}catch(Throwable x){
			debug(dbPrefix+ " Met Exception '"+ x.getMessage()+"'.");
			if(inst!=null){
				debug(dbPrefix+ " Try to use Instrumentation to do the work directly.");
				inst.runOnMainSync(new Runnable(){
					public void run(){
						checkbox.setChecked(checked);
					}
				});
				success = true;
			}
		}
		
		return success;
	}
}
