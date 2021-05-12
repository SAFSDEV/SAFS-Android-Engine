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

import java.util.Properties;

import org.safs.android.engine.DSAFSTestRunner;
import org.safs.android.remotecontrol.SAFSMessage;

import android.widget.CheckBox;

/**
 * 
 * @author Carl Nagle, SAS Institute, Inc
 * 29 AUG, 2012		LeiWang	Implement for keyword 'check' and 'uncheck' 
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
