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

import org.safs.android.engine.DGuiObjectRecognition;
import org.safs.android.engine.DSAFSTestRunner;
import org.safs.android.messenger.client.MessageResult;
import org.safs.android.remotecontrol.SAFSMessage;
import org.safs.text.FAILKEYS;
import org.safs.text.GENKEYS;
import org.safs.text.ResourceMessageInfo;

import android.view.View;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;


/**
 * 
 * @author Carl Nagle, SAS Institute, Inc
 */
public class ListViewProcessor extends  TestStepProcessor{

	protected ListView listview = null;
	static boolean tempSuccess = false;
	static String  tempError   = null;
	
	public ListViewProcessor(DSAFSTestRunner testrunner) {
		super(testrunner);
	}

	public void processComponentFunction(Properties props) {
		String dbPrefix = debugPrefix +".processComponentFunction(): ";		
		if(!checkSolo(props)){
			debug(dbPrefix+"CANNOT get the Solo object.");
			return;
		}		
		if(!(compobj instanceof ListView)){ // should never happen
			debug(dbPrefix+"skipped for object not instanceof ListView.");
	    	setGeneralErrorUnsupportedObjectType(props);
	    	return;
		}
		listview = (ListView) compobj;
		try{
			if(SAFSMessage.cf_comprouting_selecttextitem.equalsIgnoreCase(remoteCommand)){
				_selectTextCommands(props, true, true, false, false);
			}
			else if(SAFSMessage.cf_comprouting_activatetextitem.equalsIgnoreCase(remoteCommand)){
				_selectTextCommands(props, false, true, false, true);
			}
			else if(SAFSMessage.cf_comprouting_selectpartialmatch.equalsIgnoreCase(remoteCommand)){
				_selectTextCommands(props, false, true, true, false);
			}
			else if(SAFSMessage.cf_comprouting_activatepartialmatch.equalsIgnoreCase(remoteCommand)){
				_selectTextCommands(props, false, true, true, true);
			}
			else if(SAFSMessage.cf_comprouting_selectunverifiedtextitem.equalsIgnoreCase(remoteCommand)){
				_selectTextCommands(props, false, true, false, false);
			}
			else if(SAFSMessage.cf_comprouting_activateunverifiedtextitem.equalsIgnoreCase(remoteCommand)){
				_selectTextCommands(props, false, true, false, true);
			}
			else if(SAFSMessage.cf_comprouting_selectindexitem.equalsIgnoreCase(remoteCommand) ||
					SAFSMessage.cf_comprouting_selectindex.equalsIgnoreCase(remoteCommand)){
				_selectIndexCommands(props, true, false);
			}
			else if(SAFSMessage.cf_comprouting_activateindexitem.equalsIgnoreCase(remoteCommand) ||
					SAFSMessage.cf_comprouting_activateindex.equalsIgnoreCase(remoteCommand) ||
					SAFSMessage.cf_comprouting_clickindexitem.equalsIgnoreCase(remoteCommand) ||
					SAFSMessage.cf_comprouting_clickindex.equalsIgnoreCase(remoteCommand)){
				_selectIndexCommands(props, false, true);
			}
			else if(SAFSMessage.cf_comprouting_captureitemstofile.equalsIgnoreCase(remoteCommand)){
				_captureItems(props);
			}
			
			// route Results to the controller side CFListViewFunctions library
			if(commandNotExecuted(props)){ /*ignore*/ }
			else props.setProperty(SAFSMessage.KEY_TARGET, SAFSMessage.target_safs_listview);
			
		}catch(Throwable x){
			String stackout = getStackTrace(x);
			debug(dbPrefix+ "\n"+ stackout);
			resourceMsg.reset();
			resourceMsg.setKey(FAILKEYS.GENERIC_ERROR);
			resourceMsg.addParameter(stackout);		
			setGeneralErrorWithBundleMessage(props, resourceMsg, null);			
		}
	}

	/**
	 * @param props
	 * @param isVerified, true if this is a 'verified' command -- not 'Unverified'.
	 * @param useCase, true if text comparisons ARE case-sensitive.
	 * @param isPartial, true if text comparisons only need a substring match.
	 * @param activate, true if we need to click the item after its selection.
	 */
	protected void _selectTextCommands(Properties props, boolean isVerified, boolean useCase, boolean isPartial, boolean activate) {
		String dbPrefix = debugPrefix +"._selectTextCommands(isVerified="+ isVerified +"): ";
		String text = props.getProperty(SAFSMessage.PARAM_1);
		String nthstr = props.getProperty(SAFSMessage.PARAM_2);// OPTIONAL -- might be null
		int nth = 1;
		if(nthstr != null){
			try{
				int split = nthstr.indexOf("Index=");
				if(split > -1){
					try{ 
						tempError = nthstr.split("=")[1];
						if(tempError != null && tempError.length() > 0) nthstr = tempError;
						tempError = null;
					}
					catch(Exception x){
						debug(dbPrefix+"ignoring invalid param 'MatchIndex' format. Using MatchIndex="+nth);
					}
				}
				int newnth = Integer.parseInt(nthstr);
				if(newnth < 1){
					debug(dbPrefix+"ignoring invalid param MatchIndex < 1. Using MatchIndex="+nth);
				}else{
					if(newnth > listview.getCount()){
						debug(dbPrefix+"ignoring MatchIndex > items in the list. Using MatchIndex="+ nth);
					}else{
						nth = newnth;
						debug(dbPrefix+"using param MatchIndex="+ nth);
					}
				}
			}catch(NumberFormatException x){
				debug(dbPrefix+"ignoring invalid param 'MatchIndex' format. Using MatchIndex="+nth);
			}
		}else{
			debug(dbPrefix+"using default MatchIndex="+ nth);
		}
		// prepare for failure
		resourceMsg.reset();
		resourceMsg.setResourceBundleName(ResourceMessageInfo.BUNDLENAME_FAILEDTEXT);
		resourceMsg.setKey(FAILKEYS.FAILURE_3);
		resourceMsg.setParams(new String[]{compname, text, remoteCommand});
		if(text == null || text.length()==0){
			resourceDetailMsg.reset();
			resourceDetailMsg.setKey(FAILKEYS.BAD_PARAM);
			resourceDetailMsg.setParams(new String[]{"TEXTVALUE"});
			setGeneralErrorWithBundleMessage(props, resourceMsg, resourceDetailMsg);
			return;
		}
		
		int item = getListViewItemIndex(listview, text, useCase, isPartial, nth);
		if(item < 0){ // match NOT found
			resourceDetailMsg.reset();
			resourceDetailMsg.setKey(FAILKEYS.NO_MATCH_FOUND);
			resourceDetailMsg.setParams(new String[]{text});
			setGeneralErrorWithBundleMessage(props, resourceMsg, resourceDetailMsg);
			return;
		}else{ // match found
			String fulltext = text;
			if(isPartial) { //get full item text if originally only a substring
				fulltext = getListViewTextAtIndex(listview, item);
				if(fulltext==null) fulltext = text;
			}
			tempError = null;
			if(selectIndex(listview, item, activate)){ //success				
				if(isVerified){
					int selected = getListViewSelectedIndex(listview);
					if(!(selected == item)){ //verification failed
						resourceMsg.setParams(new String[]{compname, fulltext, remoteCommand});
						resourceDetailMsg.reset();
						resourceDetailMsg.setResourceBundleName(ResourceMessageInfo.BUNDLENAME_FAILEDTEXT);
						resourceDetailMsg.setKey(FAILKEYS.SELECTED_INDEX_NOT_MATCH);
						resourceDetailMsg.setParams(new String[]{String.valueOf(selected), String.valueOf(item)});
						setGeneralErrorWithBundleMessage(props, resourceMsg, resourceDetailMsg);
						return;
					}
				}
				if(activate){
					resourceMsg.reset();
					resourceMsg.setResourceBundleName(ResourceMessageInfo.BUNDLENAME_GENERICTEXT);
					resourceMsg.setKey(GENKEYS.SUCCESS_2);
					resourceMsg.setParams(new String[]{remoteCommand, fulltext});		
					setGeneralSuccessWithBundleMessage(props, resourceMsg, null);
				}else{
					resourceMsg.reset();
					resourceMsg.setResourceBundleName(ResourceMessageInfo.BUNDLENAME_GENERICTEXT);
					resourceMsg.setKey(GENKEYS.SOMETHING_SET);
					resourceMsg.setParams(new String[]{compname, fulltext});		
					setGeneralSuccessWithBundleMessage(props, resourceMsg, null);
				}
			}else{ // not successful
				resourceMsg.setParams(new String[]{compname, fulltext, remoteCommand});
				resourceDetailMsg.reset();
				resourceDetailMsg.setKey(FAILKEYS.GENERIC_ERROR);
				resourceDetailMsg.setParams(new String[]{tempError});
				setGeneralErrorWithBundleMessage(props, resourceMsg, resourceDetailMsg);
			}
		}
	}

	/**
	 * @param props
	 * @param verified, true if we need to verify the selected index is selected.
	 * @param activate, true if we need to click the item after its selection.
	 */
	protected void _selectIndexCommands(Properties props, boolean verified, boolean activate) {
		String dbPrefix = debugPrefix +"._selectIndexCommands(): ";
		String nthstr = props.getProperty(SAFSMessage.PARAM_1);
		// prepare for failure
		resourceMsg.reset();
		resourceMsg.setResourceBundleName(ResourceMessageInfo.BUNDLENAME_FAILEDTEXT);
		resourceMsg.setKey(FAILKEYS.FAILURE_3);
		resourceMsg.setParams(new String[]{compname, "null", remoteCommand});
		resourceDetailMsg.reset();
		resourceDetailMsg.setResourceBundleName(ResourceMessageInfo.BUNDLENAME_FAILEDTEXT);
		resourceDetailMsg.setKey(FAILKEYS.BAD_PARAM);
		resourceDetailMsg.setParams(new String[]{"ITEMINDEX"});		
		if(nthstr==null){
			setGeneralErrorWithBundleMessage(props, resourceMsg, resourceDetailMsg);
			return;
		}
		int nth = 1;
		resourceMsg.setParams(new String[]{compname, nthstr, remoteCommand});
		try{
			int newnth = Integer.parseInt(nthstr);
			if(newnth < 0 || (newnth > listview.getCount()-1)){
				resourceMsg.setParams(new String[]{compname, String.valueOf(newnth), remoteCommand});
				setGeneralErrorWithBundleMessage(props, resourceMsg, resourceDetailMsg);
				return;
			}else{
				nth = newnth;
				debug(dbPrefix+"using param ItemIndex="+ nth);
			}
		}catch(NumberFormatException x){
			debug(dbPrefix+"ignoring invalid param 'ItemIndex' format.");
			setGeneralErrorWithBundleMessage(props, resourceMsg, resourceDetailMsg);
			return;
		}		
		tempError = null;
		resourceMsg.setParams(new String[]{compname, String.valueOf(nth), remoteCommand});
		if(selectIndex(listview, nth, activate)){ //success	
			if(verified){
				int selected = getListViewSelectedIndex(listview);
				if(!(selected == nth)){ //verification failed
					resourceDetailMsg.reset();
					resourceDetailMsg.setResourceBundleName(ResourceMessageInfo.BUNDLENAME_FAILEDTEXT);
					resourceDetailMsg.setKey(FAILKEYS.SELECTED_INDEX_NOT_MATCH);
					resourceDetailMsg.setParams(new String[]{String.valueOf(selected), String.valueOf(nth)});
					setGeneralErrorWithBundleMessage(props, resourceMsg, resourceDetailMsg);
					return;
				}
			}
			if(activate){
				resourceMsg.reset();
				resourceMsg.setResourceBundleName(ResourceMessageInfo.BUNDLENAME_GENERICTEXT);
				resourceMsg.setKey(GENKEYS.SUCCESS_2);
				resourceMsg.setParams(new String[]{remoteCommand, String.valueOf(nth)});		
				setGeneralSuccessWithBundleMessage(props, resourceMsg, null);				
			}else{
				resourceMsg.reset();
				resourceMsg.setResourceBundleName(ResourceMessageInfo.BUNDLENAME_GENERICTEXT);
				resourceMsg.setKey(GENKEYS.SOMETHING_SET);
				resourceMsg.setParams(new String[]{compname, String.valueOf(nth)});		
				setGeneralSuccessWithBundleMessage(props, resourceMsg, null);
			}
		}else{ // not successful
			resourceDetailMsg.reset();
			resourceDetailMsg.setKey(FAILKEYS.GENERIC_ERROR);
			resourceDetailMsg.setParams(new String[]{tempError});
			setGeneralErrorWithBundleMessage(props, resourceMsg, resourceDetailMsg);
		}
	}

	/**************************************************************************
	 * @param 	index, the index to select, 0-based.
	 */
	void _captureItems(Properties props){
		String dbPrefix = debugPrefix +"._captureItems(): ";
		String itemsString = "";		
		try{
			debug(dbPrefix+ " Try to capture items of listview.");
			List<String> itemList = getListViewItems(listview);
			
			for(int i=0;i<itemList.size();i++){
				itemsString += DGuiObjectRecognition.getObjectText(itemList.get(i)) + SAFSMessage.cf_combobox_items_separator;
			}
			//Use the KEY_REMOTERESULTINFO to take back the items' value.
		    setGeneralSuccessWithSpecialInfo(props, itemsString);
		}catch(Throwable x){
			String stackout = getStackTrace(x);
			debug(dbPrefix+ "\n"+ stackout);
			resourceMsg.reset();
			resourceMsg.setKey(FAILKEYS.GENERIC_ERROR);
			resourceMsg.addParameter(stackout);		
			setGeneralErrorWithBundleMessage(props, resourceMsg, null);			
		}
	}
	
	
	/**************************************************************************
	 * @param view
	 * @param index, 0-based index of item to select.
	 * @param doClick, true to click the item after its selection.
	 * @return true if no error occurred during attempt to select.
	 * Sets tempError to an error message if the selection attempt threw a Throwable.
	 */
	boolean selectIndex(final ListView view, final int index, boolean doClick){
		String dbPrefix = debugPrefix +".selectIndex(): ";
		tempSuccess = false;
		try{
			debug(dbPrefix+ " Try to select index '"+index+"' from ListView.");
			if(index<0){
				debug(dbPrefix+ "The index should NOT be less than 0.");				
				return false;
			}
			if(index > (view.getCount() -1)){
				debug(dbPrefix+ "The index should NOT be greater than the number of items.");				
				return false;
			}
			boolean touchMode = view.isInTouchMode();
			debug(dbPrefix+"pre-selection touchMode = "+ touchMode);
			//selection won't work in touch mode
			inst.runOnMainSync(new Runnable(){
				public void run(){
					try{
						boolean touchMode = view.isInTouchMode();
						if(touchMode){
							touchMode = !view.requestFocusFromTouch();
						}
						if(touchMode){
							view.setSelectionFromTop(index, 0);
						}else{
							view.setSelection(index);
						}
						ListViewProcessor.tempSuccess = true;
					}catch(Throwable x){
						ListViewProcessor.tempError = x.getClass().getSimpleName()+": "+ x.getMessage();
					}											
				}
			});
			touchMode = view.isInTouchMode();
			debug(dbPrefix+"post-selection touchMode = "+ touchMode);
			if(tempSuccess && doClick){
				try{
					View selected = null;;
					if(touchMode){
						// uncertain if this will return correct and clickable view
						selected = view.getChildAt(index);
					}else{
						selected = view.getSelectedView();						
					}
					if(selected == null){
						debug(dbPrefix+"did not retrieve a valid selected View for clicking.");
						
						String text = getListViewTextAtIndex(view, index);
						if(text!=null){
							debug(dbPrefix+"try solo.clickOnText to click on '"+text+"'");
							solo.clickOnText(text, 1, true);
							tempSuccess = true;
						}
						// let any null force the error and log it
						if(!tempSuccess)
							solo.clickOnView(selected);
					}else{
						solo.clickOnView(selected);
					}
				}catch(Throwable x){
					tempError = x.getClass().getSimpleName()+": "+ x.getMessage();
					tempSuccess = false;
				}
			}
		}catch(Throwable x){
			debug(dbPrefix+ x.getClass().getSimpleName()+": "+ x.getMessage()+".");			
		}		
		return tempSuccess;
	}
	
	/**************************************************************************
	 * @param view
	 * @return 0-N for selected item. -1 (ListView.INVALID_POSITION) if no item selected.
	 */
	int getListViewSelectedIndex(final ListView view){
		return view.getSelectedItemPosition();
	}
	
	/**************************************************************************
	 * @param view
	 * @param index
	 * @return item text at the provided index, or null.
	 */
	String getListViewTextAtIndex(final ListView view, int index){
		String dbPrefix = debugPrefix +".getListViewTextAtIndex(): ";
		if(index < 0 || index >= view.getCount()){
			debug(dbPrefix+"index-out-of-bounds using index "+ index);
			return null;
		}
		List<String> items = getListViewItems(listview);
		return items.get(index);
	}
	
	/**************************************************************************
	 * Call this routine if you already have the List of items.
	 * @param view
	 * @param useCase -- true if matching is case-sensitive
	 * @param isPartial -- true if we only need a substring match
	 * @param nth -- return the nth matching index: 1 for the first matching index, 2 for the second, etc..
	 * @return 0-based index of matching item.  -1 if a match cannot be deduced.
	 */
	int getListViewItemIndex(final ListView view, List<String> items, String text, boolean useCase, boolean isPartial, int nth){
		String dbPrefix = debugPrefix +".getListViewItemIndex(): ";
		int nmatches = 0;
		int viewcount = view.getCount();
		if(items.size() < viewcount){
			debug(dbPrefix+"count of indexable items does not match count of total items!");
			return -1;
		}
		String item = null;
		boolean matched = false;
		for(int i=0; i< items.size();i++){
			item = items.get(i);
			matched = false;
			if(item.length()== 0){ // EMPTY String undiscernable text
				// it is possible a later index might have text
			}else{
				if(useCase){
					if(isPartial){
						matched = item.contains(text);
					}else{
						matched = item.equals(text);
					}
				}else{
					if(isPartial){
						matched= item.toLowerCase().contains(text.toLowerCase());
					}else{
						matched = item.equalsIgnoreCase(text);
					}
				}
			}
			if(matched){
				if(++nmatches == nth) return i;
			}			
		}
		return -1;
	}
	
	/**************************************************************************
	 * Call this routine if you do NOT already have the List of items.
	 * @param view
	 * @param useCase -- true if matching is case-sensitive
	 * @param isPartial -- true if we only need a substring match
	 * @param nth -- return the nth matching index: 1 for the first matching index, 2 for the second, etc..
	 * @return 0-based index of matching item.  -1 if a match cannot be deduced.
	 */
	int getListViewItemIndex(final ListView view, String text, boolean useCase, boolean isPartial, int nth){
		List<String> items = getListViewItems(view);
		return getListViewItemIndex(view, items, text, useCase, isPartial, nth);
	}
	
	/**************************************************************************
	 * Uses DGuiObjectRecognition.getObjectText to extract text of each Adapter item.
	 * Items with no discernable text will be returned as an EMPTY String.
	 * @param spinner
	 * @return
	 */
	List<String> getListViewItems(final ListView view){
		String dbPrefix = debugPrefix +".getListViewItems(): ";
		List<String> items = new ArrayList<String>();
		int count = 0;
		View child = null;
		String item = null;
		//Adapter is the model object of ListView, BUT the items' order may NOT be the same as displayed!!!
		count = view.getCount();//This will return the number of items in model
		debug(dbPrefix+"There are "+count+" items in ListView.");
		for(int i=0;i<count;i++){
			child = view.getAdapter().getView(i, null, view);
			item = DGuiObjectRecognition.getObjectText(child);
			if (item != null) items.add(item);
			else {
				debug(debugPrefix+" detected item '"+ i +"' without discernable text! Storing EMPTY String.");
				items.add("");
			}
		}
		debug(dbPrefix+"There are "+items.size()+" valid text items returned in our list.");
		return items;
	}
}
