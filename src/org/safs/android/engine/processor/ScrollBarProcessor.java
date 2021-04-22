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

import java.util.Iterator;
import java.util.Properties;

import org.safs.android.engine.DSAFSTestRunner;
import org.safs.android.remotecontrol.SAFSMessage;
import org.safs.text.FAILKEYS;
import org.safs.text.ResourceMessageInfo;

import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.ScrollView;

/**
 * 
 * @author Lei Wang, SAS Institute, Inc
 * 
 */
public class ScrollBarProcessor extends  TestStepProcessor{
	private ScrollView scrollview = null;
	private HorizontalScrollView hScrollview = null;
	private boolean tempSuccess = false;
	
	public ScrollBarProcessor(DSAFSTestRunner testrunner) {
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
		if(!(compobj instanceof ScrollView || compobj instanceof HorizontalScrollView)){
			// not handled here, maybe in a chained ViewProcessor?
			debug(dbPrefix+"skipped for object not instanceof ScrollView/HorizontalScrollView.");
			return;
		}
		try{
			iter = params.iterator();
			String param1 = "";
			int actionTimes = 1;
			
			debug(dbPrefix+"Trying to process command '"+remoteCommand+"' on ScrollView/HorizontalScrollView object.");
			if(compobj instanceof ScrollView){
				scrollview = (ScrollView) compobj;
			}else if(compobj instanceof HorizontalScrollView){
				hScrollview = (HorizontalScrollView) compobj;
			}
			
			//The parameter is optional
			if(iter.hasNext()){
				param1 = iter.next();
				debug(dbPrefix+"The first parameter is '"+param1+"' ");
				try{
					actionTimes = Integer.parseInt(param1);
				}catch(NumberFormatException e){
					actionTimes = 1;
				}
			}
			
			if(scrollview!=null &&
			   (SAFSMessage.cf_scrollbar_onedown.equalsIgnoreCase(remoteCommand)||
			   SAFSMessage.cf_scrollbar_oneup.equalsIgnoreCase(remoteCommand)  ||
			   SAFSMessage.cf_scrollbar_pagedown.equalsIgnoreCase(remoteCommand) ||
			   SAFSMessage.cf_scrollbar_pageup.equalsIgnoreCase(remoteCommand))){
				
				if(SAFSMessage.cf_scrollbar_pagedown.equalsIgnoreCase(remoteCommand)){
					detailErrMsg = _scroll(scrollview, true, false, actionTimes);
				}else if(SAFSMessage.cf_scrollbar_pageup.equalsIgnoreCase(remoteCommand)){
					detailErrMsg = _scroll(scrollview, true, true, actionTimes);
				}else if(SAFSMessage.cf_scrollbar_onedown.equalsIgnoreCase(remoteCommand)){
					detailErrMsg = _scroll(scrollview, false, false, actionTimes);
				}else if(SAFSMessage.cf_scrollbar_oneup.equalsIgnoreCase(remoteCommand)){
					detailErrMsg = _scroll(scrollview, false, true, actionTimes);
				}
				
			}else if(hScrollview!=null &&
					 (SAFSMessage.cf_scrollbar_oneleft.equalsIgnoreCase(remoteCommand) ||
					 SAFSMessage.cf_scrollbar_oneright.equalsIgnoreCase(remoteCommand) ||
					 SAFSMessage.cf_scrollbar_pageleft.equalsIgnoreCase(remoteCommand) ||
					 SAFSMessage.cf_scrollbar_pageright.equalsIgnoreCase(remoteCommand))){
				
				if(SAFSMessage.cf_scrollbar_pageleft.equalsIgnoreCase(remoteCommand)){
					detailErrMsg = _scroll(hScrollview, true, true, actionTimes);
				}else if(SAFSMessage.cf_scrollbar_pageright.equalsIgnoreCase(remoteCommand)){
					detailErrMsg = _scroll(hScrollview, true, false, actionTimes);
				}else if(SAFSMessage.cf_scrollbar_oneleft.equalsIgnoreCase(remoteCommand)){
					detailErrMsg = _scroll(hScrollview, false, true, actionTimes);
				}else if(SAFSMessage.cf_scrollbar_oneright.equalsIgnoreCase(remoteCommand)){
					detailErrMsg = _scroll(hScrollview, false, false, actionTimes);
				}				
			}else{
				detailErrMsg = new ResourceMessageInfo();
				detailErrMsg.setKey(FAILKEYS.GENERIC_ERROR);
				detailErrMsg.addParameter("The command '"+remoteCommand+"' is not supported by "+this.getClass().getSimpleName()+".");
			}
			
			if(detailErrMsg==null){
				setGeneralSuccessWithBundle(props);
			}else{
				debug(dbPrefix+"command '"+remoteCommand+"' failed.");
				setGeneralError3(props, detailErrMsg);
			}
			
			// route Results to the controller side CFScrollBarFunctions library
			if(commandNotExecuted(props)){ /*ignore*/ }
			else props.setProperty(SAFSMessage.KEY_TARGET, SAFSMessage.target_safs_scrollbar);
			
		}catch(Throwable x){
			String stackout = getStackTrace(x);
			debug(dbPrefix+ "\n"+ stackout);
			setGenericError(props, stackout);
			return;
		}
	}

	/**
	 * Scroll a ScrollView vertically.
	 * 
	 * @param scroller,		ScrollView, the scrollView object
	 * @param page			boolean, true scroll pace is page; otherwise it is one arrow.
	 * @param up			boolean, true scroll up; otherwise scroll down.
	 * @param times 		int, how many times the action should be repeated
	 * 
	 * @return	ResourceMessageInfo, if the scroll action is executed successfully, return null.
	 * 								 otherwise, the returned ResourceMessageInfo contains the fail reason.
	 */
	private ResourceMessageInfo _scroll(final ScrollView scroller, final boolean page, final boolean up, final int times){
		String dbPrefix = debugPrefix +"._scroll(): ";
		ResourceMessageInfo detailErrMsg = null;
		
		try{
			debug(dbPrefix+ " Try to scroll "+(page?"page":"one")+" "+(up?"up":"down")+" for "+times+" times.");
			tempSuccess = false;
			
			inst.runOnMainSync(new Runnable(){
				boolean scrollok = true;
				public void run(){
					for(int i=0;i<times && scrollok;i++){
						if(page){
							if(up) scrollok = scroller.pageScroll(View.FOCUS_UP);
							else   scrollok = scroller.pageScroll(View.FOCUS_DOWN);
						}else{
							if(up) scrollok = scroller.arrowScroll(View.FOCUS_UP);
							else   scrollok = scroller.arrowScroll(View.FOCUS_DOWN);
						}
					}
					tempSuccess = scrollok;
				}
			});
			
			if(!tempSuccess){
				detailErrMsg = new ResourceMessageInfo();
				detailErrMsg.setKey(FAILKEYS.GENERIC_ERROR);
				detailErrMsg.addParameter("Fail to scroll");
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
	 * Scroll a ScrollView horizontally.
	 * 
	 * @param scroller,		HorizontalScrollView, the scrollView object
	 * @param page			boolean, true scroll pace is page; otherwise it is one arrow.
	 * @param left			boolean, true scroll to left; otherwise scroll to right.
	 * @param times 		int, how many times the action should be repeated
	 * 
	 * @return	ResourceMessageInfo, if the scroll action is executed successfully, return null.
	 * 								 otherwise, the returned ResourceMessageInfo contains the fail reason.
	 */
	private ResourceMessageInfo _scroll(final HorizontalScrollView scroller, final boolean page, final boolean left, final int times){
		String dbPrefix = debugPrefix +"._scroll(): ";
		ResourceMessageInfo detailErrMsg = null;
		
		try{
			debug(dbPrefix+ " Try to scroll "+(page?"page":"one")+" "+(left?"left":"right")+" for "+times+" times.");
			tempSuccess = false;
			
			inst.runOnMainSync(new Runnable(){
				boolean scrollok = true;
				public void run(){
					for(int i=0;i<times && scrollok;i++){
						if(page){
							if(left) scrollok = scroller.pageScroll(View.FOCUS_LEFT);
							else   scrollok = scroller.pageScroll(View.FOCUS_RIGHT);
						}else{
							if(left) scrollok = scroller.arrowScroll(View.FOCUS_LEFT);
							else   scrollok = scroller.arrowScroll(View.FOCUS_RIGHT);
						}
					}
					tempSuccess = scrollok;
				}
			});
			
			if(!tempSuccess){
				detailErrMsg = new ResourceMessageInfo();
				detailErrMsg.setKey(FAILKEYS.GENERIC_ERROR);
				detailErrMsg.addParameter("Fail to scroll");
			}

		}catch(Throwable x){
			debug(dbPrefix+ " Met Exception: "+ x.getMessage()+".");
			detailErrMsg = new ResourceMessageInfo();
			detailErrMsg.setKey(FAILKEYS.GENERIC_ERROR);
			detailErrMsg.addParameter(" Met Exception: "+ x.getMessage()+".");
		}
		
		return detailErrMsg;
	}

}
