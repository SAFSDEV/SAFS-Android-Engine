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
package com.jayway.android.robotium.remotecontrol.client;

import com.robotium.solo.RCSolo;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;

/**
 * Robotium requires the use of an ActivityInstrumentationTestCase2 and not the 
 * InstrumentationTestRunner of the test package.
 * <p>
 * In a general-purpose framework like SAFS this may be all that this class ever does--
 * instantiate a working version of Robotium Solo.
 * @author Carl Nagle, SAS Institute, Inc.
 * @since JAN 28, 2012
 * <br>May 21, 2013		(LeiWang)	Use RCSolo instead of Solo.
 */
public class RobotiumTestCase extends ActivityInstrumentationTestCase2{

	RCSolo solo = null;
	Activity activity = null;
	Intent intent = null;

	/**
	 * Constructor for the class merely calling the superclass constructor.
	 * Prepares the instance with the targetPackage and launch Activity Class.
	 * These items are deduced elsewhere thru the Android PackageManager.
	 * @param String targetPackage
	 * @param Class targetClass
	 * @see ActivityInstrumentationTestCase2#ActivityInstrumentationTestCase2(String, Class)
	 */
	public RobotiumTestCase(String targetPackage, Class targetClass){
		super(targetPackage, targetClass);
	}

	/**
	 * Wrapper to preferred output/debug logging.
	 * @param text
	 */
	void setStatus(String text){
		Log.d(AbstractTestRunner.TAG, text);
	}
	
	/**
	 *  Acquires the launch Activity with a call to getActivity() and then creates the 
	 *  Robotium Solo instance from that Activity.
	 *  @see #getActivity()
	 *  @see com.robotium.solo.RCSolo#RCSolo(Instrumentation, Activity)
	 */
	public void setUp(){
		if(activity == null){
			activity = getActivity();
			intent = new Intent(activity, activity.getClass());
		}else{
			intent.setAction(Intent.ACTION_MAIN);
			intent.addCategory(Intent.CATEGORY_LAUNCHER);
			intent.addFlags(Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY);
			activity.startActivity(intent);
		}
		solo = new RCSolo(getInstrumentation(), activity);
	}

	/**
	 * Retrieve the Solo instance after it has been created.
	 * @return Solo instance
	 */
	public RCSolo getRobotium() { return solo; }
	
	/**
	 * Currently, simply calls solo.finishOpenActivities()
	 * @see RCSolo#finishOpenedActivities()
	 */
	public void tearDown(){
		solo.finishOpenedActivities();
	}
}
