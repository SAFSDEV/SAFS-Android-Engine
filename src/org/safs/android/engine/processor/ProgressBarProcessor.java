/** 
 ** Copyright (C) SAS Institute, All rights reserved.
 ** General Public License: http://www.opensource.org/licenses/gpl-license.php
 **/
package org.safs.android.engine.processor;

import java.text.NumberFormat;
import java.util.Iterator;
import java.util.Properties;

import org.safs.android.engine.DSAFSTestRunner;
import org.safs.android.remotecontrol.SAFSMessage;
import org.safs.text.FAILKEYS;
import org.safs.text.ResourceMessageInfo;
import org.safs.tools.stringutils.StringUtilities;

import android.widget.ProgressBar;
import android.widget.RatingBar;

/**
 * 
 * @author Lei Wang, SAS Institute, Inc
 * 
 */
public class ProgressBarProcessor extends  TestStepProcessor{
	private ProgressBar progressbar = null;
	private boolean tempSuccess = false;
	
	public ProgressBarProcessor(DSAFSTestRunner testrunner) {
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
		//android.widget.ProgressBar and android.widget.SeekBar
		if(!(compobj instanceof ProgressBar)){
			// not handled here, maybe in a chained ViewProcessor?
			debug(dbPrefix+"skipped for object not instanceof ProgressBar: it is "+compobj.getClass().getSimpleName());
			return;
		}else{
			progressbar = (ProgressBar) compobj;
		}
		
		try{
			iter = params.iterator();
			String param1 = "";
			String param2 = "";
			//Progress bar has 2 indicator, primary and secondary. isPrimaryBar tell which one to use.
			boolean isPrimaryBar = true;
			
			debug(dbPrefix+"Trying to process command '"+remoteCommand+"' on ProgressBar object.");
			
			if(SAFSMessage.cf_progressbar_getprogress.equalsIgnoreCase(remoteCommand)||
			   SAFSMessage.cf_progressbar_setprogress.equalsIgnoreCase(remoteCommand)){
				//Check the parameters
				//The progress parameter is required for setprogress
				//The variable parameter is required for getprogress
				if(!checkParameterSize(1, props)){
					debug(dbPrefix+" the parameters are not enough.");
					return;
				}
				param1 = iter.next();
				debug(dbPrefix+"The first parameter is '"+param1+"' ");
				if(iter.hasNext()){
					param2 = iter.next();
					debug(dbPrefix+"The second parameter is '"+param2+"' ");
					isPrimaryBar = StringUtilities.convertBool(param2);
				}
				
				//Process each keyword
				if(SAFSMessage.cf_progressbar_setprogress.equalsIgnoreCase(remoteCommand)){
					detailErrMsg = setProgress(progressbar, param1, isPrimaryBar);
					
				}else if(SAFSMessage.cf_progressbar_getprogress.equalsIgnoreCase(remoteCommand)){
					StringBuffer progress = new StringBuffer();
					detailErrMsg = getProgress(progressbar, progress, isPrimaryBar);
					
					props.setProperty(SAFSMessage.PARAM_9, progress.toString());
				}
			}else if(SAFSMessage.cf_progressbar_getrating.equalsIgnoreCase(remoteCommand)||
					 SAFSMessage.cf_progressbar_setrating.equalsIgnoreCase(remoteCommand)){
			
				//android.widget.RatingBar
				if(!(progressbar instanceof RatingBar)){
					debug(dbPrefix+"skipped for object not instanceof RatingBar: it is "+compobj.getClass().getSimpleName());
					return;
				}
				RatingBar ratingbar = (RatingBar) progressbar;
				
				//Check the parameters
				//The rating parameter is required for setrating
				//The variable parameter is required for getrating
				if(!checkParameterSize(1, props)){
					debug(dbPrefix+" the parameters are not enough.");
					return;
				}
				param1 = iter.next();
				debug(dbPrefix+"The first parameter is '"+param1+"' ");
				
				//Process each keyword
				if(SAFSMessage.cf_progressbar_setrating.equalsIgnoreCase(remoteCommand)){
					detailErrMsg = setRating(ratingbar, param1);
					
				}else if(SAFSMessage.cf_progressbar_getrating.equalsIgnoreCase(remoteCommand)){
					StringBuffer rating = new StringBuffer();
					detailErrMsg = getRating(ratingbar, rating);
					
					props.setProperty(SAFSMessage.PARAM_9, rating.toString());
				}
								
			}else{
				debug(dbPrefix+" keyword '"+remoteCommand+"' can't be processed here.");
				return;
			}
			
			if(detailErrMsg==null){
				setGeneralSuccessWithBundle(props);
			}else{
				debug(dbPrefix+"command '"+remoteCommand+"' failed.");
				setGeneralError3(props, detailErrMsg);
			}
			
			// route Results to the controller side CFScrollBarFunctions library
			if(commandNotExecuted(props)){ /*ignore*/ }
			else props.setProperty(SAFSMessage.KEY_TARGET, SAFSMessage.target_safs_progressbar);
			
		}catch(Throwable x){
			String stackout = getStackTrace(x);
			debug(dbPrefix+ "\n"+ stackout);
			setGenericError(props, stackout);
			return;
		}
	}

	/**
	 * Set the progress to a ProgressBar.
	 * 
	 * @param progressbar,			ProgressBar, the ProgressBar object
	 * @param progress,				String, the progress to set to ProgressBar, it should be a number between 0 and 1.
	 * @param isPrimary,			Boolean, Android progress bar has 2 indicators, primary and secondary.
	 *                                       this parameter is used to tell which one will be operated. 
	 * 
	 * @return	ResourceMessageInfo, if the action is executed successfully, return null.
	 * 								 otherwise, the returned ResourceMessageInfo contains the fail reason.
	 */
	private ResourceMessageInfo setProgress(final ProgressBar progressbar, final String progress, final boolean isPrimary){
		final String dbPrefix = debugPrefix +".setProgress(): ";
		ResourceMessageInfo detailErrMsg = null;
		
		try{
			debug(dbPrefix+ " Try to set progress '"+progress+"' to ProgressBar.");
			tempSuccess = false;
			
			inst.runOnMainSync(new Runnable(){
				
				public void run(){
					try{
						boolean setok = true;
						
//						if(progressbar.isIndeterminate()){
//							progressbar.setIndeterminate(false);
//							debug(dbPrefix+ " indeterminat="+progressbar.isIndeterminate());
//						}
						
						//Only non indeterminate progress bar can be operated
						if(!progressbar.isIndeterminate()){
							//convert the progress percentage to a number
							int tick = 0;
							try{
								tick = (int) (progressbar.getMax()*Float.parseFloat(progress));
							}catch(NumberFormatException e){
								NumberFormat nf = NumberFormat.getPercentInstance();
								tick = (int) (progressbar.getMax()*nf.parse(progress).floatValue());
							}
							
							debug(dbPrefix+ " Set progress '"+tick+"' to ProgressBar.");
							if(isPrimary){
								progressbar.setProgress(tick);
							}else{
								progressbar.setSecondaryProgress(tick);
							}
							
							tempSuccess = setok;
						}else{
							debug(dbPrefix+ " progress bar is indeterminat, it can not be operated");
						}
						
					}catch(Exception e){
						debug(dbPrefix+ " Met Exception: "+ e.getMessage()+".");
						tempSuccess = false;
					}
				}
			});
			
			if(!tempSuccess){
				detailErrMsg = new ResourceMessageInfo();
				detailErrMsg.setKey(FAILKEYS.GENERIC_ERROR);
				detailErrMsg.addParameter("Fail to set '"+progress+"' to ProgressBar.");
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
	 * Get the progress from a ProgressBar.
	 * 
	 * @param progressbar,			ProgressBar, the ProgressBar object
	 * @param progress,				StringBuffer, the progress got from ProgressBar, it is be a number between 0 and 1.
	 * @param isPrimary,			Boolean, Android progress bar has 2 indicators, primary and secondary.
	 *                                       this parameter is used to tell which one will be operated. 
	 * 
	 * @return	ResourceMessageInfo, if the action is executed successfully, return null.
	 * 								 otherwise, the returned ResourceMessageInfo contains the fail reason.
	 */
	private ResourceMessageInfo getProgress(final ProgressBar progressbar, final StringBuffer progress, final boolean isPrimary){
		final String dbPrefix = debugPrefix +".getProgress(): ";
		ResourceMessageInfo detailErrMsg = null;
		
		try{
			debug(dbPrefix+ " Try to get progress from ProgressBar.");
			tempSuccess = false;
			
			inst.runOnMainSync(new Runnable(){
				public void run(){
					try{
//						if(progressbar.isIndeterminate()){
//							progressbar.setIndeterminate(false);
//							debug(dbPrefix+ " indeterminat="+progressbar.isIndeterminate());
//						}
						
						if(!progressbar.isIndeterminate()){
							float progressValue = 0.0f;
							float maxValue = progressbar.getMax();
							
							if(isPrimary){
								progressValue = progressbar.getProgress();
							}else{
								progressValue = progressbar.getSecondaryProgress();
							}
							
							NumberFormat nf = NumberFormat.getPercentInstance();
							String percentage = nf.format(progressValue/maxValue);
							debug(dbPrefix+ " progressValue="+progressValue+" maxValue="+maxValue);
							debug(dbPrefix+ " Got progress from ProgressBar: "+percentage);
							
							progress.append(percentage);
							tempSuccess = true;
						}else{
							debug(dbPrefix+ " progress bar is indeterminat, it can not be operated");
						}
						
					}catch(Exception e){
						debug(dbPrefix+ " Met Exception: "+ e.getMessage()+".");
						tempSuccess = false;
					}
				}
			});
			
			if(!tempSuccess){
				detailErrMsg = new ResourceMessageInfo();
				detailErrMsg.setKey(FAILKEYS.GENERIC_ERROR);
				detailErrMsg.addParameter("Fail to get progress from ProgressBar.");
			}

		}catch(Throwable x){
			debug(dbPrefix+ " Met Exception: "+ x.getClass().getSimpleName()+":"+ x.getMessage()+".");
			detailErrMsg = new ResourceMessageInfo();
			detailErrMsg.setKey(FAILKEYS.GENERIC_ERROR);
			detailErrMsg.addParameter(" Met Exception: "+ x.getClass().getSimpleName()+":"+ x.getMessage()+".");
		}
		
		return detailErrMsg;
	}
	

	/**
	 * Set the rating to a RatingBar.
	 * 
	 * @param ratingbar,			RatingBar, the RatingBar object
	 * @param rating,				String, the rating to set to RatingBar, it should be a number.
	 * 
	 * @return	ResourceMessageInfo, if the action is executed successfully, return null.
	 * 								 otherwise, the returned ResourceMessageInfo contains the fail reason.
	 */
	private ResourceMessageInfo setRating(final RatingBar ratingbar, final String rating){
		final String dbPrefix = debugPrefix +".setRating(): ";
		ResourceMessageInfo detailErrMsg = null;
		
		try{
			debug(dbPrefix+ " Try to set rating '"+rating+"' to RatingBar.");
			tempSuccess = false;
			
			inst.runOnMainSync(new Runnable(){
				
				public void run(){
					try{
						boolean setok = true;
						//Only non indeterminate rating bar can be operated
						if(!ratingbar.isIndeterminate()){
							//convert the rating percentage to a number
							float tick = 0;
							try{
								tick = Float.parseFloat(rating);
								debug(dbPrefix+ " Set rating '"+tick+"' to RatingBar.");
								ratingbar.setRating(tick);

								tempSuccess = setok;
							}catch(NumberFormatException e){
								debug(dbPrefix+ " Can't convert '"+rating+"' to float number.");
							}
						}else{
							debug(dbPrefix+ " rating bar is indeterminat, it can not be operated");
						}
					}catch(Exception e){
						debug(dbPrefix+ " Met Exception: "+ e.getMessage()+".");
						tempSuccess = false;
					}
				}
			});
			
			if(!tempSuccess){
				detailErrMsg = new ResourceMessageInfo();
				detailErrMsg.setKey(FAILKEYS.GENERIC_ERROR);
				detailErrMsg.addParameter("Fail to set '"+rating+"' to RatingBar.");
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
	 * Get the rating from a RatingBar.
	 * 
	 * @param ratingbar,			RatingBar, the RatingBar object
	 * @param rating,				StringBuffer, the rating got from RatingBar, it is a number.
	 * 
	 * @return	ResourceMessageInfo, if the action is executed successfully, return null.
	 * 								 otherwise, the returned ResourceMessageInfo contains the fail reason.
	 */
	private ResourceMessageInfo getRating(final RatingBar ratingbar, final StringBuffer rating){
		final String dbPrefix = debugPrefix +".getRating(): ";
		ResourceMessageInfo detailErrMsg = null;
		
		try{
			debug(dbPrefix+ " Try to get rating from RatingBar.");
			tempSuccess = false;
			
			inst.runOnMainSync(new Runnable(){
				public void run(){
					try{
						
						if(!ratingbar.isIndeterminate()){
							float ratingValue = 0.0f;
							
							ratingValue = ratingbar.getRating();

							debug(dbPrefix+ " Got rating from RatingBar: "+ratingValue);
							
							rating.append(ratingValue);
							tempSuccess = true;
						}else{
							debug(dbPrefix+ " rating bar is indeterminat, it can not be operated");
						}
						
					}catch(Exception e){
						debug(dbPrefix+ " Met Exception: "+ e.getMessage()+".");
						tempSuccess = false;
					}
				}
			});
			
			if(!tempSuccess){
				detailErrMsg = new ResourceMessageInfo();
				detailErrMsg.setKey(FAILKEYS.GENERIC_ERROR);
				detailErrMsg.addParameter("Fail to get rating from RatingBar.");
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
