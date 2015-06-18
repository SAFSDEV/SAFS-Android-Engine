/** 
 ** Copyright (C) SAS Institute, All rights reserved.
 ** General Public License: http://www.opensource.org/licenses/gpl-license.php
 **/
package org.safs.android.engine.processor;

import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.Properties;

import org.safs.android.engine.DSAFSTestRunner;
import org.safs.android.remotecontrol.SAFSMessage;
import org.safs.text.FAILKEYS;
import org.safs.text.ResourceMessageInfo;
import org.safs.tools.stringutils.StringUtilities;

import android.widget.DatePicker;

/**
 * 
 * @author Lei Wang, SAS Institute, Inc
 * 
 */
public class DataPickerProcessor extends  TestStepProcessor{
	private DatePicker datapicker = null;
	private boolean tempSuccess = false;
	
	public DataPickerProcessor(DSAFSTestRunner testrunner) {
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
		if(!(compobj instanceof DatePicker)){
			// not handled here, maybe in a chained ViewProcessor?
			debug(dbPrefix+"skipped for object not instanceof DatePicker.");
			return;
		}else{
			datapicker = (DatePicker) compobj;
		}
		try{
			iter = params.iterator();
			String param1 = "";
			
			debug(dbPrefix+"Trying to process command '"+remoteCommand+"' on DatePicker object.");
			
			//The date parameter is required for setdate
			//The variable parameter is required for getdate
			if(SAFSMessage.cf_datepicker_setdate.equalsIgnoreCase(remoteCommand)||
			   SAFSMessage.cf_datepicker_getdate.equalsIgnoreCase(remoteCommand)){
				if(!checkParameterSize(1, props)){
					debug(dbPrefix+" the parameters are not enough.");
					return;
				}
			}
			
			if(SAFSMessage.cf_datepicker_setdate.equalsIgnoreCase(remoteCommand)){
				param1 = iter.next();
				debug(dbPrefix+"The first parameter is '"+param1+"' ");
				detailErrMsg = setDate(datapicker, param1);
				
			}else if(SAFSMessage.cf_datepicker_getdate.equalsIgnoreCase(remoteCommand)){
				Calendar calendar = Calendar.getInstance();
				detailErrMsg = getDate(datapicker, calendar);
				
				//Use format "MM-dd-yyyy" to create a date string
				String calendarStr = StringUtilities.getDateString(calendar.getTime());
					
				props.setProperty(SAFSMessage.PARAM_9, calendarStr);
			}
			
			if(detailErrMsg==null){
				setGeneralSuccessWithBundle(props);
			}else{
				debug(dbPrefix+"command '"+remoteCommand+"' failed.");
				setGeneralError3(props, detailErrMsg);
			}
			
			// route Results to the controller side CFScrollBarFunctions library
			if(commandNotExecuted(props)){ /*ignore*/ }
			else props.setProperty(SAFSMessage.KEY_TARGET, SAFSMessage.target_safs_datepicker);
			
		}catch(Throwable x){
			String stackout = getStackTrace(x);
			debug(dbPrefix+ "\n"+ stackout);
			setGenericError(props, stackout);
			return;
		}
	}

	/**
	 * Set a date time to a DatePicker.
	 * 
	 * @param datapicker,		DatePicker, the DatePicker object
	 * @param dateTime,			String, the date to set to DatePicker
	 * 
	 * @return	ResourceMessageInfo, if the action is executed successfully, return null.
	 * 								 otherwise, the returned ResourceMessageInfo contains the fail reason.
	 */
	private ResourceMessageInfo setDate(final DatePicker datapicker, final String dateTime){
		final String dbPrefix = debugPrefix +".setDate(): ";
		ResourceMessageInfo detailErrMsg = null;
		
		try{
			debug(dbPrefix+ " Try to set date time '"+dateTime+"' to DatePicker.");
			tempSuccess = false;
			
			inst.runOnMainSync(new Runnable(){
				
				public void run(){
					try{
						boolean setok = true;
						Date date = StringUtilities.getDate(dateTime);
						Calendar calendar = Calendar.getInstance();
						calendar.setTime(date);
						
						int year = calendar.get(Calendar.YEAR);
						int month = calendar.get(Calendar.MONTH);
						int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);
						datapicker.updateDate(year, month, dayOfMonth);
						tempSuccess = setok;
						
					}catch(Exception e){
						debug(dbPrefix+ " Met Exception: "+ e.getMessage()+".");
						tempSuccess = false;
					}
				}
			});
			
			if(!tempSuccess){
				detailErrMsg = new ResourceMessageInfo();
				detailErrMsg.setKey(FAILKEYS.GENERIC_ERROR);
				detailErrMsg.addParameter("Fail to set '"+dateTime+"' to DatePicker.");
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
	 * Get a date time from a DatePicker.
	 * 
	 * @param datapicker,		In,  DatePicker, the DatePicker object
	 * @param calendar, 		Out, Calendar, contains the returned date of the DatePicker
	 * 
	 * @return	ResourceMessageInfo, if the action is executed successfully, return null.
	 * 								 otherwise, the returned ResourceMessageInfo contains the fail reason.
	 */
	private ResourceMessageInfo getDate(final DatePicker datapicker, final Calendar calendar){
		final String dbPrefix = debugPrefix +".getDate(): ";
		ResourceMessageInfo detailErrMsg = null;
		
		try{
			debug(dbPrefix+ " Try to get date from DatePicker.");
			tempSuccess = false;
			
			inst.runOnMainSync(new Runnable(){
				public void run(){
					try{
						int year = datapicker.getYear();
						int month = datapicker.getMonth();
						int dayOfMonth = datapicker.getDayOfMonth();
						calendar.set(year, month, dayOfMonth);
						tempSuccess = true;
					}catch(Exception e){
						debug(dbPrefix+ " Met Exception: "+ e.getMessage()+".");
						tempSuccess = false;
					}
				}
			});
			
			if(!tempSuccess){
				detailErrMsg = new ResourceMessageInfo();
				detailErrMsg.setKey(FAILKEYS.GENERIC_ERROR);
				detailErrMsg.addParameter("Fail to get date from DatePicker.");
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
