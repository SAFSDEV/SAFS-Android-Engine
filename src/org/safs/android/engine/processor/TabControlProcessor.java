/** 
 ** Copyright (C) SAS Institute, All rights reserved.
 ** General Public License: http://www.opensource.org/licenses/gpl-license.php
 **/
package org.safs.android.engine.processor;

import java.util.Iterator;
import java.util.Properties;

import org.safs.android.engine.DSAFSTestRunner;
import org.safs.android.remotecontrol.SAFSMessage;
import org.safs.text.FAILKEYS;
import org.safs.text.ResourceMessageInfo;

import android.R;
import android.view.View;
import android.widget.TabHost;
import android.widget.TabWidget;
import android.widget.TextView;

/**
 * 
 * @author Lei Wang, SAS Institute, Inc
 * 
 */
public class TabControlProcessor extends  TestStepProcessor{
	private TabHost tabhost = null;
	private TabWidget tabwidget = null;
	private boolean tempSuccess = false;
	
	public TabControlProcessor(DSAFSTestRunner testrunner) {
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
		if(!(compobj instanceof TabHost)){
			// not handled here, maybe in a chained ViewProcessor?
			debug(dbPrefix+"skipped for object not instanceof TabHost.");
			return;
		}
		try{
			String param1 = "";
			iter = params.iterator();
			tabhost = (TabHost) compobj;
			tabwidget = tabhost.getTabWidget();
			
			debug(dbPrefix+"Trying to process command '"+remoteCommand+"' on TabHost object.");
			
			//These keywords need one required parameter
			if(SAFSMessage.cf_tab_clicktab.equalsIgnoreCase(remoteCommand)||
			   SAFSMessage.cf_tab_clicktabcontains.equalsIgnoreCase(remoteCommand)  ||
			   SAFSMessage.cf_tab_selecttab.equalsIgnoreCase(remoteCommand) ||
			   SAFSMessage.cf_tab_selecttabindex.equalsIgnoreCase(remoteCommand) ||
			   SAFSMessage.cf_tab_unverifiedclicktab.equalsIgnoreCase(remoteCommand) ||
			   SAFSMessage.cf_tab_makeselection.equalsIgnoreCase(remoteCommand)){

				if(!checkParameterSize(1, props)){
					debug(dbPrefix+" the parameters are not enough.");
					return;
				}
				param1 = iter.next();
				debug(dbPrefix+"The first parameter is '"+param1+"' ");
				
				if(SAFSMessage.cf_tab_clicktab.equalsIgnoreCase(remoteCommand) ||
				   SAFSMessage.cf_tab_selecttab.equalsIgnoreCase(remoteCommand) ||
				   SAFSMessage.cf_tab_makeselection.equalsIgnoreCase(remoteCommand)){
					detailErrMsg = _selectTabText(param1, false, true, true);
				}else if(SAFSMessage.cf_tab_clicktabcontains.equalsIgnoreCase(remoteCommand)){
					detailErrMsg = _selectTabText(param1, true, true, true);
				}else if(SAFSMessage.cf_tab_selecttabindex.equalsIgnoreCase(remoteCommand)){
					//We should convert the index from 1-based to 0-based.
					detailErrMsg = _selectTabIndex(Integer.parseInt(param1)-1);
				}else if(SAFSMessage.cf_tab_unverifiedclicktab.equalsIgnoreCase(remoteCommand)){
					detailErrMsg = _selectTabText(param1, false, true, false);
				}
				
			}else{
				detailErrMsg = new ResourceMessageInfo();
				detailErrMsg.setKey(FAILKEYS.GENERIC_ERROR);
				detailErrMsg.addParameter("The command '"+remoteCommand+"' is not supported by TabControlProcessor.");
			}
			
			if(detailErrMsg==null){
				setGeneralSuccessWithBundle(props);
			}else{
				debug(dbPrefix+"command '"+remoteCommand+"' failed.");
				setGeneralError3(props, detailErrMsg);
			}
			
			// route Results to the controller side CFCheckBoxFunctions library
			if(commandNotExecuted(props)){ /*ignore*/ }
			else props.setProperty(SAFSMessage.KEY_TARGET, SAFSMessage.target_safs_tab);
			
		}catch(Throwable x){
			String stackout = getStackTrace(x);
			debug(dbPrefix+ "\n"+ stackout);
			setGenericError(props, stackout);
			return;
		}
	}

	/**
	 * Select a tab by text.
	 * 
	 * @param text, 		String, the item's text to select
	 * @param partial		boolean, if the provided text is part of the item to be clicked
	 * @param caseSensitive boolean, if the provided text is case sensitive
	 * @param toVerify,		boolean, if the verification is needed to make sure the text is selected.
	 * 
	 * @return	ResourceMessageInfo, if the item is selected successfully, return null.
	 * 								 otherwise, the returned ResourceMessageInfo contains the fail reason.
	 */
	private ResourceMessageInfo _selectTabText(final String text, boolean partial, boolean caseSensitive, boolean toVerify){
		String dbPrefix = debugPrefix +"._selectTabText(): ";
		ResourceMessageInfo detailErrMsg = null;
		
		try{
			debug(dbPrefix+ " Try to select '"+text+"' for TabControl.");
			tempSuccess = false;
			
			//TODO Did NOT find an API to select a tab by text. So try to select by index.
			//If there is one, we can try to select by text first. If not succeed, we can try index.
			int index = getTabIndex(text, partial, caseSensitive);
			debug("Get the index '"+index+"' for tab text '"+text+"'");
			detailErrMsg = _selectTabIndex(index);
			if(detailErrMsg!=null) tempSuccess = false;
			
			if(tempSuccess){
				if(toVerify){
					View indicatorView = tabhost.getCurrentTabView();

					String itemText = getTabText(indicatorView);
					if(!stringIsMatched(itemText, text, partial, caseSensitive)){
						detailErrMsg = new ResourceMessageInfo();
						detailErrMsg.setKey(FAILKEYS.GENERIC_ERROR);
						detailErrMsg.addParameter("The selected item '"+itemText+"' doesn't match to expected item '"+text+"'");					
					}				
				}
			}else{
				detailErrMsg = new ResourceMessageInfo();
				detailErrMsg.setKey(FAILKEYS.GENERIC_ERROR);
				detailErrMsg.addParameter("Fail to select tab item '"+text+"'");
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
	 * Select a tab by index.
	 * 
	 * @param index, 		int, the index to select, 0-based.
	 * 
	 * @return	ResourceMessageInfo, if the item is selected successfully, return null.
	 * 								 otherwise, the returned ResourceMessageInfo contains the fail reason.
	 */
	private ResourceMessageInfo _selectTabIndex(final int index){
		String dbPrefix = debugPrefix +"._selectTabIndex(): ";
		ResourceMessageInfo detailErrMsg = null;
		
		try{
			debug(dbPrefix+ " Try to select index '"+index+"' for TabControl.");
			tempSuccess = false;
			
			inst.runOnMainSync(new Runnable(){
				public void run(){
					if(index!=-1){
						tabhost.setCurrentTab(index);
						tempSuccess = true;
					}else{
						debug("Can't get the index -1.");
					}
				}
			});
			
			if(!tempSuccess){
				detailErrMsg = new ResourceMessageInfo();
				detailErrMsg.setKey(FAILKEYS.GENERIC_ERROR);
				detailErrMsg.addParameter("Fail to select tab index '"+index+"'");
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
	 * @param text, 		String, the item's text to set
	 * @param partial		boolean, if the provided text is part of the item to be clicked
	 * @param caseSensitive boolean, if the provided text is case sensitive
	 * 
	 * @return	int, the index of the item matching with the provided text
	 */
	private int getTabIndex(String text, boolean partial, boolean caseSensitive){
		int count = tabwidget.getTabCount();
		View indicatorView = null;
		int index = -1;
		
		for(int i=0;i<count;i++){
			indicatorView = tabwidget.getChildTabViewAt(i);
			if(stringIsMatched(text, getTabText(indicatorView), partial, caseSensitive)){
				index = i;
				break;
			}
		}
		
		return index;
	}
	
	/**
	 * Try to get the text of the Tab's item.<br>
	 * This method accept a View as parameter, this View is generated by TabHost$IndicatorStrategy.<br>
	 * TabHost defined 2 Indicator Strategies: LabelIndicatorStrategy and LabelAndIconIndicatorStrategy,<br>
	 * both of them can generate a View containing a TextView, which hold the tab item's label.<br>
	 * 
	 * But developer can freely provide their own IndicatorStrategy, for that case we need to modify<br>
	 * this method.
	 * 
	 * @param indicatorView, View returned by TabHost$IndicatorStrategy (LabelIndicatorStrategy, LabelAndIconIndicatorStrategy)
	 * 
	 */
	protected String getTabText(View indicatorView){
		String dbPrefix = debugPrefix +".getTabText(): ";
		String text = "";
		if(indicatorView!=null){
			View textView = indicatorView.findViewById(R.id.title);
			debug(dbPrefix+" textView is "+textView);
			if(textView instanceof TextView){
				text = ((TextView)textView).getText().toString();
			}
		}
		
		return text;
	}

}
