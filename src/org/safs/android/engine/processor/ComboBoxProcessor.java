/** 
 ** Copyright (C) SAS Institute, All rights reserved.
 ** General Public License: http://www.opensource.org/licenses/gpl-license.php
 **/
package org.safs.android.engine.processor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.safs.android.engine.DGuiObjectRecognition;
import org.safs.android.engine.DSAFSTestRunner;
import org.safs.android.remotecontrol.SAFSMessage;
import org.safs.text.FAILKEYS;
import org.safs.text.ResourceMessageInfo;

import android.view.View;
import android.widget.Spinner;
import android.widget.TextView;

/**
 * 
 * @author Carl Nagle, SAS Institute, Inc
 * 29 AUG, 2012		Lei Wang	Implement for keywords.
 */
public class ComboBoxProcessor extends  TestStepProcessor{
	private Spinner spinner = null;
	static boolean tempSuccess = false;
	
	public ComboBoxProcessor(DSAFSTestRunner testrunner) {
		super(testrunner);
	}

	public void processComponentFunction(Properties props) {
		String dbPrefix = debugPrefix +".processComponentFunction(): ";
		ResourceMessageInfo detailErrMsg = null;
		Iterator<String> iter = null;
		
		if(!checkSolo(props)){
			debug(dbPrefix+"CANNOT get the Solo object.");
			return;
		}		
		if(!(compobj instanceof Spinner)){
			// not handled here, maybe in a chained ViewProcessor?
			debug(dbPrefix+"skipped for object not instanceof Spinner.");
			return;
		}
		try{
			String param1 = "";
			String param2 = "";
			iter = params.iterator();
			spinner = (Spinner) compobj;			
			debug(dbPrefix+"Trying to process command '"+remoteCommand+"' on combobox");
			
			//These keywords need one required parameter
			if(SAFSMessage.cf_comprouting_captureitemstofile.equalsIgnoreCase(remoteCommand)||
			   SAFSMessage.cf_comprouting_select.equalsIgnoreCase(remoteCommand) ||
			   SAFSMessage.cf_comprouting_selectindex.equalsIgnoreCase(remoteCommand) ||
			   SAFSMessage.cf_comprouting_selectpartialmatch.equalsIgnoreCase(remoteCommand) ||
			   SAFSMessage.cf_comprouting_selectunverified.equalsIgnoreCase(remoteCommand) ||
			   SAFSMessage.cf_comprouting_settextvalue.equalsIgnoreCase(remoteCommand) ||
			   SAFSMessage.cf_comprouting_setunverifiedtextvalue.equalsIgnoreCase(remoteCommand) ||
			   SAFSMessage.cf_comprouting_verifyselected.equalsIgnoreCase(remoteCommand)){


				if(!checkParameterSize(1, props)){
					debug(dbPrefix+" the parameters are not enough.");
					return;
				}
				param1 = iter.next();
				debug(dbPrefix+"The first parameter is '"+param1+"' ");
				
				if(SAFSMessage.cf_comprouting_captureitemstofile.equalsIgnoreCase(remoteCommand)){
					if(iter.hasNext()){
						param2= iter.next();
						debug(dbPrefix+" save to file with encoding '"+param2+"'");
					}
					
					detailErrMsg = _captureItems(props);
					
				}else if(SAFSMessage.cf_comprouting_select.equalsIgnoreCase(remoteCommand)){
					detailErrMsg = _selectItemText(param1, false, true, true);
					
				}else if(SAFSMessage.cf_comprouting_selectindex.equalsIgnoreCase(remoteCommand)){
					try{
						//the passed in index is 1-based, convert it to 0-based
						detailErrMsg = _selectItemIndex(Integer.parseInt(param1)-1);
						
					}catch(NumberFormatException e){
						detailErrMsg = new ResourceMessageInfo();
						detailErrMsg.setKey(FAILKEYS.GENERIC_ERROR);
						detailErrMsg.addParameter("The index parameter '"+param1+"' is not a number");
					}
					
				}else if(SAFSMessage.cf_comprouting_selectpartialmatch.equalsIgnoreCase(remoteCommand)){
					detailErrMsg = _selectItemText(param1, true, true, true);
					
				}else if(SAFSMessage.cf_comprouting_selectunverified.equalsIgnoreCase(remoteCommand)){
					detailErrMsg = _selectItemText(param1, false, true, false);
					
				}else if(SAFSMessage.cf_comprouting_settextvalue.equalsIgnoreCase(remoteCommand)){
					detailErrMsg = _setItemText(param1, true);
					
				}else if(SAFSMessage.cf_comprouting_setunverifiedtextvalue.equalsIgnoreCase(remoteCommand)){
					detailErrMsg = _setItemText(param1, false);
					
				}else if(SAFSMessage.cf_comprouting_verifyselected.equalsIgnoreCase(remoteCommand)){
					detailErrMsg = _verifyItemText(param1, true, true);
					
				}
			}else{
				// not handled here, maybe in a chained ViewProcessor?
				debug(dbPrefix+"command '"+remoteCommand+"' is not processed by CheckBoxProcessor.");
				return;
			}
			
			if(detailErrMsg==null){
				setGeneralSuccessWithBundle(props);
			}else{
				debug(dbPrefix+"command '"+remoteCommand+"' failed.");
				setGeneralError3(props, detailErrMsg);
			}
			// route Results to the controller side CFComboBoxFunctions library
			if(commandNotExecuted(props)){ /*ignore*/ }
			else props.setProperty(SAFSMessage.KEY_TARGET, SAFSMessage.target_safs_combobox);
			
			
		}catch(Throwable x){
			String stackout = getStackTrace(x);
			debug(dbPrefix+ "\n"+ stackout);
			setGenericError(props, stackout);
			return;
		}
	}

	/**
	 * Set an item according to the item's index.
	 * 
	 * @param 	index, the index to select, 0-based.
	 * @return	ResourceMessageInfo, if the item is selected successfully, return null.
	 * 								 otherwise, the returned ResourceMessageInfo contains the fail reason.
	 */
	private ResourceMessageInfo _captureItems(Properties props){
		String dbPrefix = debugPrefix +"._captureItems(): ";
		ResourceMessageInfo detailErrMsg = null;
		String itemsString = "";
		
		try{
			debug(dbPrefix+ " Try to capture items of combobox.");
			List<View> itemList = getComboBoxItems(spinner);
			
			for(int i=0;i<itemList.size();i++){
				itemsString += DGuiObjectRecognition.getObjectText(itemList.get(i)) + SAFSMessage.cf_combobox_items_separator;
			}

			//Use the PARAM_9 to take back the items' value.
			props.setProperty(SAFSMessage.PARAM_9, itemsString);
			
		}catch(Throwable x){
			debug(dbPrefix+ " Met Exception: "+ x.getMessage()+".");
			detailErrMsg = new ResourceMessageInfo();
			detailErrMsg.setKey(FAILKEYS.GENERIC_ERROR);
			detailErrMsg.addParameter(" Met Exception: "+ x.getMessage()+".");
		}
		
		return detailErrMsg;
	}
	
	/**
	 * Set an item according to the item's index.
	 * 
	 * @param 	index, the index to select, 0-based.
	 * @return	ResourceMessageInfo, if the item is selected successfully, return null.
	 * 								 otherwise, the returned ResourceMessageInfo contains the fail reason.
	 */
	private ResourceMessageInfo _selectItemIndex(final int index){
		String dbPrefix = debugPrefix +"._selectItemIndex(): ";
		ResourceMessageInfo detailErrMsg = null;
		int selectedIndex = -1;
		
		try{
			debug(dbPrefix+ " Try to select index '"+index+"' from combobox.");
			if(!selectIndex(spinner, index)){
				//The exception's message will be logged to SAFS log,
				//convert the index to 1-based.
				throw new Exception("Fail to select '"+(index+1)+"' for combo box.");
			}
			
			//Verify the selected index is what we want
			selectedIndex = spinner.getSelectedItemPosition();
			if(index!=selectedIndex){
				detailErrMsg = new ResourceMessageInfo();
				detailErrMsg.setKey(FAILKEYS.GENERIC_ERROR);
				detailErrMsg.addParameter("The selected index '"+selectedIndex+"' doesn't equal to expected index '"+index+"'");
			}

		}catch(Throwable x){
			debug(dbPrefix+ " Met Exception: "+ x.getMessage()+".");
			detailErrMsg = new ResourceMessageInfo();
			detailErrMsg.setKey(FAILKEYS.GENERIC_ERROR);
			detailErrMsg.addParameter(" Met Exception: "+ x.getMessage()+".");
		}
		
		return detailErrMsg;
	}
	
	/**
	 * Set an item according to the item's text.
	 * 
	 * @param 	text, 			String, the item's text to select
	 * @param	partial,		boolean, if the parameter text is partial value of the item to select
	 * @param	caseSensitive,	boolean, if the parameter text is case-sensitive value of the item to select
	 * @param	toVerify,		boolean, if the verification is needed to make sure the text is selected.
	 * 
	 * @return	ResourceMessageInfo, if the item is selected successfully, return null.
	 * 								 otherwise, the returned ResourceMessageInfo contains the fail reason.
	 */
	private ResourceMessageInfo _selectItemText(final String text, boolean partial, boolean caseSensitive, boolean toVerify){
		String dbPrefix = debugPrefix +"._selectItemText(): ";
		ResourceMessageInfo detailErrMsg = null;
		int indexToSelect = -1;
		View selectedView = null;
		
		try{
			debug(dbPrefix+ " Try to select '"+text+"' from combobox.");
			//First, get the index for item text
			List<View> itemList = getComboBoxItems(spinner);
			String itemText = "";
			for(int i=0; i<itemList.size();i++){
				itemText = DGuiObjectRecognition.getObjectText(itemList.get(i));
				debug(dbPrefix+" Comparing with '"+itemText+"'");
				if(stringIsMatched(itemText, text, partial, caseSensitive)){
					indexToSelect = i;
					break;
				}
			}
			
			if(!selectIndex(spinner, indexToSelect)){
				throw new Exception("Fail to select '"+text+"' for combo box. MatchedIndex='"+indexToSelect+"'");
			}
			
			if(toVerify){
				//Verify the selected index is what we want
				selectedView = spinner.getSelectedView();
				itemText = DGuiObjectRecognition.getObjectText(selectedView);
				if(!stringIsMatched(itemText, text, partial, caseSensitive)){
					detailErrMsg = new ResourceMessageInfo();
					detailErrMsg.setKey(FAILKEYS.GENERIC_ERROR);
					detailErrMsg.addParameter("The selected item '"+itemText+"' doesn't match to expected item '"+text+"'");					
				}				
			}

		}catch(Throwable x){
			debug(dbPrefix+ " Met Exception: "+ x.getMessage()+".");
			detailErrMsg = new ResourceMessageInfo();
			detailErrMsg.setKey(FAILKEYS.GENERIC_ERROR);
			detailErrMsg.addParameter(" Met Exception: "+ x.getMessage()+".");
		}
		
		return detailErrMsg;
	}
	
	/**
	 * Set an item's text.
	 * 
	 * @param 	text, 			String, the item's text to set
	 * @param	toVerify,		boolean, if the verification is needed to make sure the text is selected.
	 * 
	 * @return	ResourceMessageInfo, if the item is set successfully, return null.
	 * 								 otherwise, the returned ResourceMessageInfo contains the fail reason.
	 */
	private ResourceMessageInfo _setItemText(final String text, boolean toVerify){
		String dbPrefix = debugPrefix +"._setItemText(): ";
		ResourceMessageInfo detailErrMsg = null;
		View selectedView = null;
		boolean cotainsSpecialKeys = false;
		
		try{
			debug(dbPrefix+ " Try to set '"+text+"' for combobox.");
			//TODO Need to treat the special characters like ~^+%{(
			//StringUtils.containsSepcialKeys(), can we import StringUtils???
			inst.runOnMainSync(new Runnable(){
				public void run(){
					View currentView = spinner.getSelectedView();
					if(currentView instanceof TextView)
						((TextView)currentView).setText(text);
				}
			});
			
			if(toVerify && !cotainsSpecialKeys){
				//Verify the selected index is what we want
				selectedView = spinner.getSelectedView();
				String itemText = DGuiObjectRecognition.getObjectText(selectedView);
				if(!stringIsMatched(itemText, text, false, true)){
					detailErrMsg = new ResourceMessageInfo();
					detailErrMsg.setKey(FAILKEYS.GENERIC_ERROR);
					detailErrMsg.addParameter("The selected item '"+itemText+"' doesn't match to expected item '"+text+"'");					
				}				
			}

		}catch(Throwable x){
			debug(dbPrefix+ " Met Exception: "+ x.getMessage()+".");
			detailErrMsg = new ResourceMessageInfo();
			detailErrMsg.setKey(FAILKEYS.GENERIC_ERROR);
			detailErrMsg.addParameter(" Met Exception: "+ x.getMessage()+".");
		}
		
		return detailErrMsg;
	}
	
	/**
	 * Verify if the selected item's text match the given parameter.
	 * 
	 * @param 	text, 			String, the item's text to select
	 * @param	partial,		boolean, if the parameter text is partial value of the item to select
	 * @param	caseSensitive,	boolean, if the parameter text is case-sensitive value of the item to select
	 * 
	 * @return	ResourceMessageInfo, if the item's text is matched, return null.
	 * 								 otherwise, the returned ResourceMessageInfo contains the fail reason.
	 */
	private ResourceMessageInfo _verifyItemText(final String text, boolean partial, boolean caseSensitive){
		String dbPrefix = debugPrefix +"._verifyItemText(): ";
		ResourceMessageInfo detailErrMsg = null;
		View selectedView = null;
		
		try{
			debug(dbPrefix+ " Try to verify '"+text+"' from combobox.");
					//Verify the selected index is what we want
				selectedView = spinner.getSelectedView();
				String itemText = DGuiObjectRecognition.getObjectText(selectedView);
				if(!stringIsMatched(itemText, text, partial, caseSensitive)){
					detailErrMsg = new ResourceMessageInfo();
					detailErrMsg.setKey(FAILKEYS.GENERIC_ERROR);
					detailErrMsg.addParameter("The selected item '"+itemText+"' doesn't match to expected item '"+text+"'");					
				}				

		}catch(Throwable x){
			debug(dbPrefix+ " Met Exception "+ x.getMessage()+".");
			detailErrMsg = new ResourceMessageInfo();
			detailErrMsg.setKey(FAILKEYS.GENERIC_ERROR);
			detailErrMsg.addParameter(" Met Exception: "+ x.getMessage()+".");
		}
		
		return detailErrMsg;
	}
	
	boolean selectIndex(final Spinner spinner, final int index){
		String dbPrefix = debugPrefix +".selectIndex(): ";
		tempSuccess = false;
		try{
			debug(dbPrefix+ " Try to select index '"+index+"' from combobox.");
			if(index<0){
				debug(dbPrefix+ "The index should NOT be less than 0.");				
				return false;
			}
			if(index > (spinner.getCount() -1)){
				debug(dbPrefix+ "The index should NOT be greater than the number of items.");				
				return false;
			}
			inst.runOnMainSync(new Runnable(){
				public void run(){
					//I saw the source code of Spinner, this will called if a click is performed on the popup of spinner
					try{
						spinner.setSelection(index);
						ComboBoxProcessor.tempSuccess = true;
					}catch(Throwable x){ }											
					//Not sure if the API below will invoke the OnClickListner of the Spinner!!!
					//spinner.setSelection(index, true);
				}
			});
		}catch(Throwable x){
			debug(dbPrefix+ " Met Exception: "+ x.getMessage()+".");
			//We may try other way to select an index, for example, using Solo?
		}
		
		return tempSuccess;
	}
	
	/**
	 * Currently only returns TextView items in the List.
	 * @param spinner
	 * @return
	 */
	List<View> getComboBoxItems(final Spinner spinner){
		String dbPrefix = debugPrefix +".getComboBoxItems(): ";
		List<View> items = new ArrayList<View>();
		int count = 0;
		View child = null;
		
		//Adapter is the model object of Spinner, BUT the items' order may NOT be the same as displayed!!!
//		spinner.getAdapter();
		count = spinner.getCount();//This will return the number of items in model
//		count = spinner.getChildCount();
		debug(dbPrefix+"There are "+count+" children in Spinner.");
		for(int i=0;i<count;i++){
			child = spinner.getAdapter().getView(i, null, spinner);
			//child = spinner.getChildAt(i);
			if(child instanceof TextView){
//				debug(dbPrefix+"Adding "+((TextView) child).getText().toString()+" to array.");
				items.add(child);
			}else{
//				debug(dbPrefix+"Ignoring view '"+child.getClass().getSimpleName()+"'.");
			}
		}
		
		//DGuiObjectRecognition.getObjectProperty(spinner, DGuiObjectRecognition.PROP_ITEMS);
		
		return items;
	}
	
}
