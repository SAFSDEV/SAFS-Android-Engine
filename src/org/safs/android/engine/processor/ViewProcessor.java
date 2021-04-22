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

import java.io.ByteArrayOutputStream;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.util.Properties;

import org.safs.android.engine.DGuiObjectRecognition;
import org.safs.android.engine.DSAFSTestRunner;
import org.safs.android.remotecontrol.SAFSMessage;
import org.safs.text.FAILKEYS;
import org.safs.text.GENKEYS;
import org.safs.text.ResourceMessageInfo;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.util.Base64;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;

import com.jayway.android.robotium.remotecontrol.client.SoloMessage;


/**
 * 
 * @author Carl Nagle, SAS Institute, Inc
 */
public class ViewProcessor extends  TestStepProcessor{

	public ViewProcessor(DSAFSTestRunner testrunner) {
		super(testrunner);
	}

	/**
	 * If the command is processed here it sets the KEY_TARGET property to {@value SAFSMessage#target_safs_view} 
	 * in order to route Results processing to the CFViewFunctions on the controller.
	 */
	public void processComponentFunction(Properties props) {
		String dbPrefix = debugPrefix+".processComponentFunction(): ";
		debug(dbPrefix +"processing... '"+remoteCommand+"'");
		if(!checkSolo(props)){ // typically already done in RoutingViewProcessor
			debug(dbPrefix+"CANNOT get the Solo object.");
			return;
		}
		
		//don't test if the command supports a null or missing object.
		if( ! SAFSMessage.cf_view_guidoesnotexist.equalsIgnoreCase(remoteCommand)){
			if(!(compobj instanceof View)){
			    debug(dbPrefix +"matched child object is NOT instanceof View!!!");
		    	setGeneralErrorUnsupportedObjectType(props);
		    	return;
			}
		}
		
		try{
			if(SAFSMessage.cf_view_click.equalsIgnoreCase(remoteCommand)||
			   SAFSMessage.cf_view_tap.equalsIgnoreCase(remoteCommand)||
			   SAFSMessage.cf_view_press.equalsIgnoreCase(remoteCommand)){
				_clickCommands(props);
			}else
			if(SAFSMessage.cf_view_getguiimage.equalsIgnoreCase(remoteCommand)){
				_getGuiImage(props);
			}else
			if(SAFSMessage.cf_view_guidoesexist.equalsIgnoreCase(remoteCommand)){
				_guiExists(props, true);
			}else
			if(SAFSMessage.cf_view_guidoesnotexist.equalsIgnoreCase(remoteCommand)){
				_guiExists(props, false);
			}else
			if(SAFSMessage.cf_view_inputcharacters.equalsIgnoreCase(remoteCommand) ||
			   SAFSMessage.cf_view_typechars.equalsIgnoreCase(remoteCommand)){
				_inputCharacters(props);
			}else
			if(SAFSMessage.cf_view_inputkeys.equalsIgnoreCase(remoteCommand) ||
			   SAFSMessage.cf_view_typekeys.equalsIgnoreCase(remoteCommand)){
				_inputKeys(props);
			}				
			else
			if(SAFSMessage.cf_view_capturepropertiestofile.equalsIgnoreCase(remoteCommand)||
			   SAFSMessage.cf_view_verifypropertiestofile.equalsIgnoreCase(remoteCommand)){
				_captureProperties(props);
			}
			// route Results to the controller side CFViewFunctions library
			if(commandNotExecuted(props)){ /*ignore*/ }
			else props.setProperty(SAFSMessage.KEY_TARGET, SAFSMessage.target_safs_view);
			
		}catch(Throwable x){
			String stackout = getStackTrace(x);
			debug(dbPrefix+ "\n"+ stackout);
			resourceMsg.reset();
			resourceMsg.setKey(FAILKEYS.GENERIC_ERROR);
			resourceMsg.addParameter(stackout);		
			setGeneralErrorWithBundleMessage(props, resourceMsg, null);
			return;
		}
	}
	
	// ****************************************************************************
	void _clickCommands(Properties props){
		String dbPrefix = debugPrefix +"_clickCommands(): ";
		
		if(SAFSMessage.cf_view_click.equalsIgnoreCase(remoteCommand)||
		    SAFSMessage.cf_view_tap.equalsIgnoreCase(remoteCommand)){
			String coords = props.getProperty(SAFSMessage.PARAM_1);
			boolean coordsvalid = false;
			float x = 0;
			float y = 0;
			if (coords == null) coords = "";
			if(coords.length() > 2){
				String[] xy = convertCoords(coords); 
				if(xy.length < 2){
				    debug(dbPrefix +"ignoring XY coords value -- appears to be invalid: "+ coords);
				}else{
					try{
					    debug(dbPrefix +"extracted appMapSubKey offset of "+ coords);
						x = Integer.valueOf(xy[0]).floatValue();
						y = Integer.valueOf(xy[1]).floatValue();						
						int[] lxy = {0,0};
						((View)compobj).getLocationOnScreen(lxy);
					    debug(dbPrefix +"extracted component location of "+ lxy[0] +", "+ lxy[1]);
						int w =  ((View)compobj).getWidth();
						int h =  ((View)compobj).getHeight();
					    debug(dbPrefix +"extracted component size of "+ w +", "+ h);
						x = lxy[0] + x;
						y = lxy[1] + y;
					    debug(dbPrefix +"calculated desired screen hitpoint of "+ x +", "+ y);
						Rect display = new Rect();
						((View)compobj).getWindowVisibleDisplayFrame(display);
					    debug(dbPrefix +"retrieved working window frame of "+ display.width() +", "+ display.height());
					    Rect screen = getDisplayRect((View)compobj);
					    debug(dbPrefix +"retrieved working screen size of "+ screen.width() +", "+ screen.height());
					    if(x >= screen.left && x <= screen.width()-1 &&
					       y >= screen.top && y <= screen.height()-1){
						    debug(dbPrefix +"deduced desired screen hitpoint is valid.");
					    	coordsvalid = true;
					    }else{
						    debug(dbPrefix +"deduced desired screen hitpoint is NOT valid. Using default Click coords.");
					    }
					}catch(NumberFormatException e){
					    debug(dbPrefix +"ignoring XY coords value -- integers appears to be invalid: "+ coords);
					}catch(Exception e){
					    debug(dbPrefix +"ignoring "+e.getClass().getSimpleName()+": "+e.getMessage());
					}
				}
			}
		    try{
				if(coordsvalid){				
				    debug(dbPrefix +"attempting Solo.clickOnScreen "+ x +", "+ y);
					solo.clickOnScreen(x, y);
				    setGeneralSuccessWithSpecialInfo(props, null);
				}else{
				    debug(dbPrefix +"attempting Solo.clickOnView...");
			    	solo.clickOnView((View)compobj);
				    setGeneralSuccessWithSpecialInfo(props, null);
				}
		    }catch(Throwable t){			    	
		    	debug(dbPrefix + t.getClass().getSimpleName()+", "+ t.getMessage());
				resourceMsg.reset();
				resourceMsg.setResourceBundleName(ResourceMessageInfo.BUNDLENAME_FAILEDTEXT);
				resourceMsg.setKey(FAILKEYS.GENERIC_ERROR);
				resourceMsg.setParams(new String[]{t.getClass().getSimpleName()+" "+ t.getMessage()});		
				setGeneralErrorWithBundleMessage(props, resourceMsg, null);
		    }
		}
		else if(SAFSMessage.cf_view_press.equalsIgnoreCase(remoteCommand)){
		    try{
			    debug(dbPrefix +"attempting Solo.clickLongOnView...");
			    solo.clickLongOnView((View)compobj);
			    setGeneralSuccessWithSpecialInfo(props, null);
		    }catch(Throwable t){			    	
		    	debug(dbPrefix + t.getClass().getSimpleName()+", "+ t.getMessage());
				resourceMsg.reset();
				resourceMsg.setResourceBundleName(ResourceMessageInfo.BUNDLENAME_FAILEDTEXT);
				resourceMsg.setKey(FAILKEYS.GENERIC_ERROR);
				resourceMsg.setParams(new String[]{t.getClass().getSimpleName()+" "+ t.getMessage()});		
				setGeneralErrorWithBundleMessage(props, resourceMsg, null);
		    }
		}
	}
	
	// ****************************************************************************
	void _inputCharacters(Properties props){
	    String dbPrefix = debugPrefix +"_inputCharacters(): ";
	    View cobj = (View)compobj;
	    try{ cobj.requestFocus();}catch(Exception x){
	    	debug(dbPrefix +"ignoring "+x.getClass().getSimpleName()); }
		String text = props.getProperty(SAFSMessage.PARAM_1);
		
		try{
			if(compobj instanceof EditText){
		    	debug(dbPrefix +"entering text into instanceof Android EditText widget.");
		    	if(text == null || text.length() == 0){
		    		solo.clearEditText((EditText)compobj);
		    	}else{
		    		solo.enterText((EditText)compobj, text);
		    	}
			    setGeneralSuccessWithSpecialInfo(props, null);
		    }else{
		    	debug(dbPrefix +"object is NOT instanceof Android EditText widget.");
		    	debug(dbPrefix +"Try to use the instrumentation to input characters.");
		    	robot.inputChars(text);
		    }
		}catch(Exception e){
			String errormsg = "met Exception "+e.getClass().getSimpleName()+":"+e.getMessage();
			debug(dbPrefix +errormsg);
			resourceMsg.reset();
			resourceMsg.setResourceBundleName(ResourceMessageInfo.BUNDLENAME_FAILEDTEXT);
			resourceMsg.setKey(FAILKEYS.GENERIC_ERROR);
			resourceMsg.setParams(new String[]{errormsg});		
			setGeneralErrorWithBundleMessage(props, resourceMsg, null);
		}
		
	}

	void _inputKeys(Properties props){
	    String dbPrefix = debugPrefix +"_inputKeys(): ";
	    View cobj = (View)compobj;
	    try{ cobj.requestFocus();}catch(Exception x){
	    	debug(dbPrefix +"ignoring "+x.getClass().getSimpleName()); }
	    
		String param = props.getProperty(SAFSMessage.PARAM_1);

		debug(dbPrefix +"entering '"+param+"' into '"+cobj.getClass().getSimpleName()+"'");
		
		try{
			robot.inputKeys(param);
			setGeneralSuccessWithSpecialInfo(props, null);

		}catch(Exception e){
			String errormsg = "met Exception "+e.getClass().getSimpleName()+":"+e.getMessage();
			debug(dbPrefix +errormsg);
			resourceMsg.reset();
			resourceMsg.setResourceBundleName(ResourceMessageInfo.BUNDLENAME_FAILEDTEXT);
			resourceMsg.setKey(FAILKEYS.GENERIC_ERROR);
			resourceMsg.setParams(new String[]{errormsg});		
			setGeneralErrorWithBundleMessage(props, resourceMsg, null);
		}

	}
	
	/**
	 * If successful, the resulting image is passed in the resultInfo.
	 * @param props
	 */
	void _getGuiImage(Properties props){
	    String dbPrefix = debugPrefix +"_getGuiImage(): ";
	    View cobj = (View)compobj;
	    Rect v = new Rect();
	    boolean isvisible = cobj.getGlobalVisibleRect(v);
	    if(isvisible){
		    View root = cobj.getRootView();		   
		    root.setDrawingCacheEnabled(true);
		    Bitmap bitmap = Bitmap.createBitmap(root.getDrawingCache(), v.left, v.top, v.width(), v.height());
		    root.setDrawingCacheEnabled(false);
		    ByteArrayOutputStream outputstream = new ByteArrayOutputStream();
		    boolean success = bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputstream);
		    if(success){
			    //props.setProperty(SAFSMessage.PARAM_1, Base64.encodeToString(outputstream.toByteArray(), Base64.DEFAULT));
			    //setGeneralSuccessWithSpecialInfo(props, null);
			    setGeneralSuccessWithSpecialInfo(props, Base64.encodeToString(outputstream.toByteArray(), Base64.DEFAULT));
		    }else{
			    debug(dbPrefix +"did not successfully compress Bitmap to PNG format.");
				resourceMsg.reset();
				resourceMsg.setResourceBundleName(ResourceMessageInfo.BUNDLENAME_FAILEDTEXT);
				resourceMsg.setKey(FAILKEYS.FAILURE_2);
				resourceMsg.setParams(new String[]{"Bitmap.compress()", winname+":"+compname});		
				setGeneralErrorWithBundleMessage(props, resourceMsg, null);
		    }
	    }else{
		    debug(dbPrefix +"object is not visible on screen.");
			resourceMsg.reset();
			resourceMsg.setResourceBundleName(ResourceMessageInfo.BUNDLENAME_FAILEDTEXT);
			resourceMsg.setKey(FAILKEYS.NOT_FOUND_ON_SCREEN);
			resourceMsg.addParameter(winname+":"+compname);		
			setGeneralErrorWithBundleMessage(props, resourceMsg, null);
	   }
	}
	
	// ****************************************************************************
	void _guiExists(Properties props, boolean doesExist){
	    String dbPrefix = debugPrefix;
	    dbPrefix += doesExist? "_guiExists(true): " : "_guiExists(false): ";
	    Rect v = new Rect();
	    boolean isvisible = false;
	    try{ isvisible = (compobj == null) ? false : ((View)compobj).getGlobalVisibleRect(v); }
	    catch(Exception ignore){ }
	    if(isvisible){
	    	if(doesExist){ 
	    		// is visible and should be visible
				resourceMsg.reset();
				resourceMsg.setResourceBundleName(ResourceMessageInfo.BUNDLENAME_GENERICTEXT);
				resourceMsg.setKey(GENKEYS.EXISTS);
				resourceMsg.addParameter(winname+":"+compname);		
				setGeneralSuccessWithBundleMessage(props, resourceMsg, null);
	    	}else{ 
	    		// is visible but should NOT be visible
				resourceMsg.reset();
				resourceMsg.setResourceBundleName(ResourceMessageInfo.BUNDLENAME_GENERICTEXT);
				resourceMsg.setKey(GENKEYS.EXISTS);
				resourceMsg.addParameter(winname+":"+compname);		
				setGeneralErrorWithBundleMessage(props, resourceMsg, null);
	    	}
	    }else{
	    	if(! doesExist){ 
	    		// is NOT visible and should not be visible
				resourceMsg.reset();
				resourceMsg.setResourceBundleName(ResourceMessageInfo.BUNDLENAME_GENERICTEXT);
				resourceMsg.setKey(GENKEYS.NOT_EXIST);
				resourceMsg.addParameter(winname+":"+compname);		
				setGeneralSuccessWithBundleMessage(props, resourceMsg, null);
	    	}else{ 
	    		// actually should have been handled already in TestStepProcessor
	    		// is NOT visible but should be visible
				resourceMsg.reset();
				resourceMsg.setResourceBundleName(ResourceMessageInfo.BUNDLENAME_FAILEDTEXT);
				resourceMsg.setKey(FAILKEYS.NOT_FOUND_ON_SCREEN);
				resourceMsg.addParameter(winname+":"+compname);		
				setGeneralErrorWithBundleMessage(props, resourceMsg, null);
	    	}
	    }
	}
	
	/**
	 * Returns the captured properties as a Properties.store suitable for Properties.load 
	 * in the resultInfo.
	 * @param props
	 */
	void _captureProperties(Properties props){
	    String dbPrefix = debugPrefix;
	    dbPrefix += "_captureProperties()";
	    debug(dbPrefix +"attempting to grab property values...");
	    String[] names = DGuiObjectRecognition.getObjectPropertyNames(compobj);
	    if(names == null || names.length == 0){
		    debug(dbPrefix +"failed to retrieve ANY property values!");
			resourceMsg.reset();
			resourceMsg.setResourceBundleName(ResourceMessageInfo.BUNDLENAME_FAILEDTEXT);
			resourceMsg.setKey(FAILKEYS.FAIL_EXTRACT_KEY);
			resourceMsg.addParameter("Properties");		
			setGeneralErrorWithBundleMessage(props, resourceMsg, null);
			return;
	    }
	    Properties store = new Properties();
	    String value = null;
	    for(String name : names){
	    	value = DGuiObjectRecognition.getObjectProperty(compobj, name);
	    	if (value == null) value = SoloMessage.NULL_VALUE;
	    	store.setProperty(name, value);
	    }
		StringBuffer buffer = new StringBuffer();
		CharArrayWriter writer = new CharArrayWriter();
		try{ store.store(writer, "Properties"); }
		catch(IOException x){
		    debug(dbPrefix +"IOException writing Properties for exchange!");
			resourceMsg.reset();
			resourceMsg.setResourceBundleName(ResourceMessageInfo.BUNDLENAME_FAILEDTEXT);
			resourceMsg.setKey(FAILKEYS.FAIL_EXTRACT_KEY);
			resourceMsg.addParameter("Properties");		
			setGeneralErrorWithBundleMessage(props, resourceMsg, null);
			return;
		}
		buffer.append(writer.toCharArray());
	    setGeneralSuccessWithSpecialInfo(props, buffer.toString());
	}

	/**
	 * Return a RECT containing, primarily, the Width and Height of our virtual Display.
	 * @param aview
	 * @return Rect of the Default Display via the Window Manager.
	 */
	public static Rect getDisplayRect(View aview){
		WindowManager winmgr = (WindowManager) aview.getContext().getSystemService("window");
		Display display = winmgr.getDefaultDisplay();
		return new Rect(0,0,display.getWidth(), display.getHeight());
	}
}
