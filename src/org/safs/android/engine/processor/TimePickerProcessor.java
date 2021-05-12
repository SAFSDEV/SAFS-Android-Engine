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

import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.Properties;

import org.safs.android.engine.DSAFSTestRunner;
import org.safs.android.remotecontrol.SAFSMessage;
import org.safs.text.FAILKEYS;
import org.safs.text.ResourceMessageInfo;
import org.safs.tools.stringutils.StringUtilities;

import android.widget.TimePicker;

/**
 * 
 * @author Lei Wang, SAS Institute, Inc
 * 
 */
public class TimePickerProcessor extends  TestStepProcessor{
	private TimePicker timepicker = null;
	private boolean tempSuccess = false;
	
	public TimePickerProcessor(DSAFSTestRunner testrunner) {
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
		if(!(compobj instanceof TimePicker)){
			// not handled here, maybe in a chained ViewProcessor?
			debug(dbPrefix+"skipped for object not instanceof TimePicker.");
			return;
		}else{
			timepicker = (TimePicker) compobj;
		}
		try{
			iter = params.iterator();
			String param1 = "";
			
			debug(dbPrefix+"Trying to process command '"+remoteCommand+"' on TimePicker object.");
			
			//The time parameter is required for settime
			//The variable parameter is required for gettime
			if(SAFSMessage.cf_timepicker_settime.equalsIgnoreCase(remoteCommand)||
			   SAFSMessage.cf_timepicker_gettime.equalsIgnoreCase(remoteCommand)){
				if(!checkParameterSize(1, props)){
					debug(dbPrefix+" the parameters are not enough.");
					return;
				}
			}
			
			if(SAFSMessage.cf_timepicker_settime.equalsIgnoreCase(remoteCommand)){
				param1 = iter.next();
				debug(dbPrefix+"The first parameter is '"+param1+"' ");
				detailErrMsg = setTime(timepicker, param1);
				
			}else if(SAFSMessage.cf_timepicker_gettime.equalsIgnoreCase(remoteCommand)){
				Calendar calendar = Calendar.getInstance();
				detailErrMsg = getTime(timepicker, calendar);
				
				//Use format "HH:mm:ss" to create a time string
				String calendarStr = StringUtilities.getTimeString(calendar.getTime(), true);
					
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
			else props.setProperty(SAFSMessage.KEY_TARGET, SAFSMessage.target_safs_timepicker);
			
		}catch(Throwable x){
			String stackout = getStackTrace(x);
			debug(dbPrefix+ "\n"+ stackout);
			setGenericError(props, stackout);
			return;
		}
	}

	/**
	 * Set a time to a TimePicker.
	 * 
	 * @param timepicker,		TimePicker, the TimePicker object
	 * @param time,				String, the time to set to TimePicker, it should be military time (24 hours)
	 * 
	 * @return	ResourceMessageInfo, if the action is executed successfully, return null.
	 * 								 otherwise, the returned ResourceMessageInfo contains the fail reason.
	 */
	private ResourceMessageInfo setTime(final TimePicker timepicker, final String time){
		final String dbPrefix = debugPrefix +".setTime(): ";
		ResourceMessageInfo detailErrMsg = null;
		
		try{
			debug(dbPrefix+ " Try to set date time '"+time+"' to TimePicker.");
			tempSuccess = false;
			
			inst.runOnMainSync(new Runnable(){
				
				public void run(){
					try{
						boolean setok = true;
						Date date = StringUtilities.getTime(time);
						Calendar calendar = Calendar.getInstance();
						calendar.setTime(date);

						//API setCurrentHour() accept parameter in 24-hour format, it will set the
						//TimePicker to correct time according to if the TimePicker is 24-hour view.
						timepicker.setCurrentHour(calendar.get(Calendar.HOUR_OF_DAY));
						timepicker.setCurrentMinute(calendar.get(Calendar.MINUTE));
						
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
				detailErrMsg.addParameter("Fail to set '"+time+"' to TimePicker.");
			}

		}catch(Throwable x){
			debug(dbPrefix+ " Met Exception: "+ x.getClass().getSimpleName()+":"+x.getMessage()+".");
			detailErrMsg = new ResourceMessageInfo();
			detailErrMsg.setKey(FAILKEYS.GENERIC_ERROR);
			detailErrMsg.addParameter(" Met Exception: "+ x.getClass().getSimpleName()+":"+ x.getMessage()+".");
		}
		
		return detailErrMsg;
	}
	
	/**
	 * Get a time from a TimePicker.
	 * 
	 * @param timepicker,		In,  TimePicker, the TimePicker object
	 * @param calendar, 		Out, Calendar, contains the returned time of the TimePicker, a military time (24-hour)
	 * 
	 * @return	ResourceMessageInfo, if the action is executed successfully, return null.
	 * 								 otherwise, the returned ResourceMessageInfo contains the fail reason.
	 */
	private ResourceMessageInfo getTime(final TimePicker timepicker, final Calendar calendar){
		final String dbPrefix = debugPrefix +".getTime(): ";
		ResourceMessageInfo detailErrMsg = null;
		
		try{
			debug(dbPrefix+ " Try to get time from TimePicker.");
			tempSuccess = false;
			
			inst.runOnMainSync(new Runnable(){
				public void run(){
					try{
						//getCurrentHour() will return an integer between 0 and 23
						int hour = timepicker.getCurrentHour();
						int minute = timepicker.getCurrentMinute();
						
						calendar.set(0, 0, 0, hour, minute);
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
				detailErrMsg.addParameter("Fail to get date from TimePicker.");
			}

		}catch(Throwable x){
			debug(dbPrefix+ " Met Exception: "+ x.getClass().getSimpleName()+":"+ x.getMessage()+".");
			detailErrMsg = new ResourceMessageInfo();
			detailErrMsg.setKey(FAILKEYS.GENERIC_ERROR);
			detailErrMsg.addParameter(" Met Exception: "+ x.getClass().getSimpleName()+":"+ x.getMessage()+".");
		}
		
		return detailErrMsg;
	}

}
