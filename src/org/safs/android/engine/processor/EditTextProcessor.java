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
import org.safs.text.FAILKEYS;
import org.safs.tools.stringutils.StringUtilities;

import android.widget.EditText;
import android.widget.TextView;

/**
 * 
 * @author Carl Nagle, SAS Institute, Inc
 * 25 MAR, 2012		LeiWang	Add first implementation.
 */
public class EditTextProcessor extends  TestStepProcessor{

	private boolean tempSuccess = false;
	
	public EditTextProcessor(DSAFSTestRunner testrunner) {
		super(testrunner);
	}

	public void processComponentFunction(Properties props) {
		String dbPrefix = debugPrefix +".processComponentFunction(): ";		
		if(!checkSolo(props)){
			debug(dbPrefix+"CANNOT get the Solo object.");
			return;
		}		
		if(!((compobj instanceof EditText)||compobj instanceof TextView)){
			// not handled here, maybe in a chained ViewProcessor?
			debug(dbPrefix+"skipped for object not instanceof TextView or EditText.");
			return;
		}
		try{
			
			if(((compobj instanceof EditText)||compobj instanceof TextView) &&
			   ( SAFSMessage.cf_comprouting_settextcharacters.equalsIgnoreCase(remoteCommand) ||
			     SAFSMessage.cf_comprouting_settextvalue.equalsIgnoreCase(remoteCommand) ||
			     SAFSMessage.cf_comprouting_setunverifiedtextcharacters.equalsIgnoreCase(remoteCommand) ||
			     SAFSMessage.cf_comprouting_setunverifiedtextvalue.equalsIgnoreCase(remoteCommand))
			   ){
				_setTextCommands(props);
			}else{
				debug(dbPrefix+"command '"+remoteCommand+"' is not processed by EditTextProcessor.");
				return;
			}
			// route Results to the controller side CFEditTextFunctions library
			if(commandNotExecuted(props)){ /*ignore*/ }
			else props.setProperty(SAFSMessage.KEY_TARGET, SAFSMessage.target_safs_edittext);

		}catch(Throwable x){
			String stackout = getStackTrace(x);
			debug(dbPrefix+ "\n"+ stackout);
			setGenericError(props, stackout);
		}		
	}
	
	void _setTextCommands(Properties props){
		String dbPrefix = debugPrefix +"_setTextCommands(): ";

		//These keywords need one required parameter--but it can be an empty string.
		if(!checkParameterSize(1, props)){
			debug(dbPrefix+" the parameters are not engough.");
			return;
		}
		String text = params.iterator().next();
		TextView textView = (TextView) compobj;
		//preset success
		boolean success = true;
		
		if(SAFSMessage.cf_comprouting_settextcharacters.equalsIgnoreCase(remoteCommand)){
			success = setText(textView, text, true, true);
		}else if(SAFSMessage.cf_comprouting_settextvalue.equalsIgnoreCase(remoteCommand)){
			success = setText(textView, text, false, true);
		}else if(SAFSMessage.cf_comprouting_setunverifiedtextcharacters.equalsIgnoreCase(remoteCommand)){
			success = setText(textView, text, true, false);
		}else if(SAFSMessage.cf_comprouting_setunverifiedtextvalue.equalsIgnoreCase(remoteCommand)){
			success = setText(textView, text, false, false);
		}else{
			debug(dbPrefix+"command '"+remoteCommand+"' has not been properly implemented in EditTextProcessor!");
			return;
		}
		//If success is false, setText should have set the global resourceDetailMsg error already
		if(success){
			setGeneralSuccessWithBundle(props);
		}else{
			setGeneralErrorWithDetailBundle(props, resourceDetailMsg);
		}			
	}
	
	public boolean setText(final TextView textView, final String text, boolean isCharacter, boolean verify){
		final String dbPrefix = debugPrefix + ".setText(): ";
		
		debug(dbPrefix+ " Try to set string '"+text+"' to "+textView.getClass().getSimpleName());
		tempSuccess = true;
		if(isCharacter){
			if(textView instanceof EditText){
				debug(dbPrefix+"trying with Solo ...");
				EditText editText = (EditText) textView;
				solo.clearEditText(editText);
				solo.enterText(editText, text);
			}else{
				try {
					debug(dbPrefix + "trying with Instrumentation ...");
					
					inst.runOnMainSync(new Runnable() {
						public void run() {
							try {
								textView.requestFocus();
								textView.setText(text);
							} catch (Exception e) {
								debug(dbPrefix + " Met Exception: " + e.getMessage() + ".");
								tempSuccess = false;
							}
						}
					});

				} catch (Throwable x) {
					debug(dbPrefix + " Met Exception: " + x.getMessage() + ".");
					tempSuccess = false;
				}				
			}
		}else{
			
			try {
				debug(dbPrefix + "set focus and clear with Instrumentation ...");
				
				inst.runOnMainSync(new Runnable() {
					public void run() {
						try {
							//Request the focus and clear the text
							textView.requestFocus();
							textView.setText("");
						} catch (Exception e) {
							debug(dbPrefix + " Met Exception: " + e.getMessage() + ".");
							tempSuccess = false;
						}
					}
				});

				debug(dbPrefix+"trying with SAFS Robot...");
				robot.inputKeys(text);
			} catch (Throwable x) {
				debug(dbPrefix + " Met Exception: " + x.getMessage() + ".");
				tempSuccess = false;
			}
		}
		
		if(tempSuccess){
			if(verify){
				//If the text is all characters, or it is keys but doesn't contain special keys
				//we will verify.
				if(isCharacter || !StringUtilities.containsSepcialKeys(text)){
					String currentText = textView.getText().toString();
					if(!text.equals(currentText)){
						debug(dbPrefix+"verification error: '"+text+"'!='"+currentText+"'");
						resourceDetailMsg.reset();
						resourceDetailMsg.setKey(FAILKEYS.TEXT_DIFFERENT);
						resourceDetailMsg.addParameter(text);
						tempSuccess = false;
					}else{
						debug(dbPrefix+"verification passed.");
					}					
				}
			}
		}else{
			debug(dbPrefix+"Fail to set text '"+text+"'");
			resourceDetailMsg.reset();
			resourceDetailMsg.setKey(FAILKEYS.GENERIC_ERROR);
			resourceDetailMsg.addParameter("Fail to set text '"+text+"'");
		}
		
		return tempSuccess;
	}
}
