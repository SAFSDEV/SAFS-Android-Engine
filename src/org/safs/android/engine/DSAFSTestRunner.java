/** 
 ** Copyright (C) SAS Institute, All rights reserved.
 ** General Public License: http://www.opensource.org/licenses/gpl-license.php
 **/
package org.safs.android.engine;

import java.io.InputStream;
import java.lang.reflect.Field;

import org.safs.android.engine.processor.*;
import org.safs.android.remotecontrol.SAFSMessage;
import org.safs.android.engine.R;

import android.util.Log;

import com.jayway.android.robotium.remotecontrol.client.RobotiumTestRunner;

/**
 * @author Carl Nagle, SAS Institute, Inc.
 * <p>
 * FEB 07, 2013 (Lei Wang)	Override method {@link #beforeStart()} to initialize DGuiClassData.<br>
 */
public class DSAFSTestRunner extends RobotiumTestRunner {

	public static String TAG = DSAFSTestRunner.class.getSimpleName();
	/**
	 * 
	 */
	public DSAFSTestRunner() {
		super(); // superclass adds the target_solo Processor for us
		
		addProcessor(SAFSMessage.target_safs_engine, new EngineProcessor(this));
		addProcessor(SAFSMessage.target_safs_driver, new DriverProcessor(this));
		addProcessor(SAFSMessage.target_safs_comprouting, new RoutingViewProcessor(this));

		// build the hierarchy of chained processors according to component types
		
		ViewProcessor view = new ViewProcessor(this);
		addProcessor(SAFSMessage.target_safs_view, view);
		
		addProcessor(SAFSMessage.target_safs_checkbox, new CheckBoxProcessor(this));
		addProcessor(SAFSMessage.target_safs_checkbox, view);

		addProcessor(SAFSMessage.target_safs_edittext, new EditTextProcessor(this));
		addProcessor(SAFSMessage.target_safs_edittext, view);

		addProcessor(SAFSMessage.target_safs_gridview, new GridViewProcessor(this));
		addProcessor(SAFSMessage.target_safs_gridview, view);

		addProcessor(SAFSMessage.target_safs_listview, new ListViewProcessor(this));
		addProcessor(SAFSMessage.target_safs_listview, view);
		
		addProcessor(SAFSMessage.target_safs_combobox, new ComboBoxProcessor(this));
		addProcessor(SAFSMessage.target_safs_combobox, view);
		
		addProcessor(SAFSMessage.target_safs_tab, new TabControlProcessor(this));
		addProcessor(SAFSMessage.target_safs_tab, view);
		
		addProcessor(SAFSMessage.target_safs_scrollbar, new ScrollBarProcessor(this));
		addProcessor(SAFSMessage.target_safs_scrollbar, view);
		
		addProcessor(SAFSMessage.target_safs_datepicker, new DataPickerProcessor(this));
		addProcessor(SAFSMessage.target_safs_datepicker, view);
		
		addProcessor(SAFSMessage.target_safs_timepicker, new TimePickerProcessor(this));
		addProcessor(SAFSMessage.target_safs_timepicker, view);

		addProcessor(SAFSMessage.target_safs_progressbar, new ProgressBarProcessor(this));
		addProcessor(SAFSMessage.target_safs_progressbar, view);

		DGuiObjectVector.setSAFSTestRunner(this);
		DGuiObjectRecognition.setSAFSTestRunner(this);
		DGuiObjectDefinition.setSAFSTestRunner(this);
		DGuiObjectIndices.setSAFSTestRunner(this);
	}

	/**
	 * <p>
	 * In this method, we will initialize the static field DGuiClassData in
	 * SAFSProcessor, and load the class-type/type-library mapping file.
	 * <p>
	 * As we need the Context object of Instrument to load resource, so
	 * {@link #getContext()} should return a valid object. In this method,
	 * we are sure that the context object has been created properly.
	 * 
	 * @see DGuiClassData#loadMappingFile()
	 * @sse {@link #readRawFile(String)}
	 */
	public boolean beforeStart(){
		debug(TAG+" Initialize DGuiClassData and load mapping files.");
		DGuiClassData dgcd = new DGuiClassData(this);
		dgcd.loadMappingFile();
		//Set the DGuiClassData to other users.
		SAFSProcessor.setDgcd(dgcd);
		DGuiObjectVector.setGuiClassData(dgcd);
		DGuiObjectDefinition.setGuiClassData(dgcd);

		return super.beforeStart();
	}
	
	/**
	 * <p>
	 * Prerequisite: The Instrument's context must be initialized.
	 * {@link #getContext()} must return a non-null value.
	 * <p>
	 * When program comes to {@link #onCreate(android.os.Bundle)}, {@link #getContext()} will
	 * return a valid object. While {@link #beforeStart()} is called in {@link #onCreate(android.os.Bundle)},
	 * so in method {@link #beforeStart()} or after it, we can safely call this method.
	 * 
	 * @param filenameWithoutSuffix, String, the name of file stored in project folder res/raw
	 * 
	 * @return	InputStream, the InputStream of the file.
	 * @see #beforeStart()
	 */
	public InputStream readRawFile(String filenameWithoutSuffix){
		Field field = null;
		InputStream in = null;
		
		try {
			field = R.raw.class.getDeclaredField(filenameWithoutSuffix);
			if(field != null){
				debug(TAG+" loading raw file: "+filenameWithoutSuffix);
				debug(TAG+" context= "+getContext());
				debug(TAG+" getResources= "+getContext().getResources());
				in = getContext().getResources().openRawResource(field.getInt(R.raw.class));
				if(in!=null) debug(TAG+" got InputStream. ");
				else debug(TAG+" InputStream is null!!! ");
			}
		} catch (Exception e) {
			debug(TAG+" Met "+e.getClass().getSimpleName()+": "+e.getMessage());
		}
		
		return in;
	}
	
	/**
	 * Send a debug message to our remote TCP client (if enabled).
	 * If our messageRunner is null, or did not successfully send the message, 
	 * then we will send to the Android debug Log.d.
	 */
	@Override
	public void debug(String message){
		if(isDebugEnabled()){
			try{ if(messageRunner.sendDebug(message)) return;}
			catch(Exception x){} 
			Log.d(TAG, message);  		
		}
	}
}
