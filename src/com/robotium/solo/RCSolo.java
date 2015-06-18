package com.robotium.solo;

import java.util.List;

import android.app.Activity;
import android.app.Instrumentation;
import android.view.View;

/** 
 * From release4.0, Robotium has removed the API getAllOpenedActivities(). We want to keep supporting this API in
 * Robotium RemoteControl. As in class {@link Solo}, there is a protected field {@link Solo#activityUtils},
 * which provides the API getAllOpenedActivities(), so {@link RCSolo} is created as subclass of {@link Solo}.
 * But {@link ActivityUtils} is only visible within package {@link com.robotium.solo}
 * ({@link com.jayway.android.robotium.solo} is the old package name <b>before Robotium 5.0.1</b>), so the
 * class {@link RCSolo} is put in the same package.<br>
 * As from <b>Robotium 5.0.1</b>, the package name 'com.jayway.android.robotium.solo' has been changed to 
 * 'com.robotium.solo', we have to change the package for this class RCSolo.<br><br>
 * 
 * There are also some other protected fields, which are useful, such as:<br>
 * {@link Solo#checker}<br>
 * {@link Solo#asserter}<br>
 * {@link Solo#clicker}<br>
 * {@link Solo#dialogUtils}<br>
 * {@link Solo#getter}<br>
 * {@link Solo#presser}<br>
 * {@link Solo#screenshotTaker}<br>
 * {@link Solo#scroller}<br>
 * {@link Solo#searcher}<br>
 * {@link Solo#sender}<br>
 * {@link Solo#setter}<br>
 * {@link Solo#sleeper}<br>
 * {@link Solo#textEnterer}<br>
 * {@link Solo#viewFetcher}<br>
 * {@link Solo#waiter}<br>
 * {@link Solo#webUrl}<br>
 * {@link Solo#webUtils}<br>
 * <br>
 * But these fields can ONLY be used in this class or its subclass within package {@link com.robotium.solo}
 * ({@link com.jayway.android.robotium.solo} the old package name <b>before Robotium 5.0.1</b>).
 * 
 * @author Lei Wang, SAS Institute, Inc
 * @since  May 21, 2013
 * <br>    May 17, 2013		(Lei Wang)	Update to add removed method finishInactiveActivities() in Robotium 4.1<br>
 */

public class RCSolo extends Solo{

	public RCSolo(Instrumentation instrumentation) {
		super(instrumentation);
	}

	public RCSolo(Instrumentation instrumentation, Activity activity){
		super(instrumentation, activity);
	}
	
	/**
	 * This method is removed from {@link Solo} from Robotium4.1 release.<br>
	 * Expose it in {@link RCSolo} to keep the backward compatibility.<br> 
	 */
	public List<Activity> getAllOpenedActivities(){
		if(activityUtils==null) return null;
		return activityUtils.getAllOpenedActivities();
	}

	/**
	 * This method is removed from {@link Solo} from Robotium4.1 release.<br>
	 * <p>
	 * The Activity handling has changed since that method was introduced. 
	 * Only weak references of Activities are now stored and Activities are 
	 * now also removed as soon as new ones are opened. Due to these changes 
	 * finishInactiveActivities has lost its purpose.
	 * The old implementation introduced crashes as keeping references to 
	 * Activities resulted in memory not being freed.
	 * <p>
	 * Expose it in {@link RCSolo} as a do-nothing to keep the backward compatibility.<br> 
	 * @deprecated
	 */
	public void finishInactiveActivities() {
	}

	/**
	 * Contain the latest view that has been waited for.
	 */
	private View waitedView = null;
	/**
	 * Get the latest view that has been waited for.<br>
	 * <p>
	 * As this field {@link #waitedView} is set in the method {@link #waitForView(int, int, int, boolean)} or {@link #waitForView(Object, int, int, boolean)} ,
	 * so before we can get the View by this method, we need to call firstly {@link #waitForView(Object, int, int, boolean)}, {@link #waitForView(int, int, int, boolean)} or
	 * the methods waitForView() who call them.	 To avoid the thread-influence, we should call these 2 methods in a synchronized block. Such as following:
	 * <pre>
	 * {@code
	 *   boolean found = false;
	 *   View view = null;
	 *   synchronized(solo){
	 *     found = solo.waitForView(tag);
	 *     if(found) view = solo.getWaitedView();					
	 *   }
	 * }
	 * </pre>
	 * @see #waitForView(int)
	 * @see #waitForView(int, int, int)
	 * @see #waitForView(int, int, int, boolean)
	 * @see #waitForView(Object)
	 * @see #waitForView(Object, int, int)
	 * @see #waitForView(Object, int, int, boolean)
	 */
	public View getWaitedView(){ return waitedView;}
	/**
	 * Just copy the same method in super class and save the waited view to a local field.<br>
	 * Waits for a View matching the specified tag.<br>
	 *
	 * @param tag the {@link View#getTag() tag} of the {@link View} to wait for
	 * @param minimumNumberOfMatches the minimum number of matches that are expected to be found. {@code 0} means any number of matches
	 * @param timeout the amount of time in milliseconds to wait
	 * @param scroll {@code true} if scrolling should be performed
	 * @return {@code true} if the {@link View} is displayed and {@code false} if it is not displayed before the timeout
	 */
	public synchronized boolean waitForView(Object tag, int minimumNumberOfMatches, int timeout, boolean scroll){
		int index = minimumNumberOfMatches-1;

		if(index < 1) {
			index = 0;
		}

		waitedView = waiter.waitForView(tag, index, timeout, scroll);
		return (waitedView!=null);
	}
	
	/**
	 * Just copy the same method in super class and save the waited view to a local field.<br>
	 * Waits for a View matching the specified resource id. 
	 * 
	 * @param id the R.id of the {@link View} to wait for
	 * @param minimumNumberOfMatches the minimum number of matches that are expected to be found. {@code 0} means any number of matches
	 * @param timeout the amount of time in milliseconds to wait
	 * @param scroll {@code true} if scrolling should be performed
	 * @return {@code true} if the {@link View} is displayed and {@code false} if it is not displayed before the timeout
	 */
	public synchronized boolean waitForView(int id, int minimumNumberOfMatches, int timeout, boolean scroll){
		int index = minimumNumberOfMatches-1;

		if(index < 1)
			index = 0;

		waitedView = waiter.waitForView(id, index, timeout, scroll); 
		return (waitedView!=null);
	}
}
