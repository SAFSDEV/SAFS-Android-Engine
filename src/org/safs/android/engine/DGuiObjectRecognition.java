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
package org.safs.android.engine;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.safs.android.remotecontrol.SAFSMessage;
import org.safs.sockets.RemoteException;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Application;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.SparseBooleanArray;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.AbsListView;
import android.widget.AbsSpinner;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.robotium.solo.RCSolo;

/**
 * Primary means to interrogate GUI Objects on the Android Device or Emulator.<br>
 * (CarlNagle) May 10, 2012  Fixed getTopLevelWindows to properly extract the topmost focused 
 *                        View even if it is a Dialog or PopupWindow and not a child of 
 *                        the "current" Activity.<br>
 * (LeiWang) SEP 07, 2012  Add method getPropertyByReflection().
 * 						  Modify method getObjectProperty(): at the end, call getPropertyByReflection() to
 * 						  get property for some simple properties easily.<br>
 * (LeiWang) FEB 21, 2013  Add "TYPE=WINDOW" to array {@link #TOPLEVEL_WINDOW_RECS_UC}<br>
 * (LeiWang) JUN 05, 2013  Return view's id name as the object's name.<br>
 */
public class DGuiObjectRecognition{

	static String TAG = "DGOR";	

    /*
     * Used to get at Android objects via Solo or OS Widgets and Views
     */
    static DSAFSTestRunner testrunner = null;

    /**
     * Must be set prior to calling debug or any of the static "get" functions.
     * Currently this is set by the DSAFSTestRunner itself during initialization.
     * @param _testrunner
     */
    public static void setSAFSTestRunner(DSAFSTestRunner _testrunner){
    	testrunner = _testrunner;
    }        
    
    static void debug(String message){
    	try{ testrunner.debug(message);}catch(Exception x){}
    }
    
    /**
     * Retrieve the testrunner.getSolo() object.  If null, we will run testrunner.launchApplication() 
     * in an attempt to create the Solo object.
     * @return testrunner.getSolo() with a valid Solo or a RemoteException is thrown.
     * @throws RemoteException if testrunner is null, or an attempt to testrunner.getSolo() 
     * after a testrunner.launchApplication() still returns null.
     */
    private static RCSolo autostartMainLauncher() throws RemoteException{
		if(testrunner == null){	// should never happen?
			throw new RemoteException("DGuiObjectRecognition invalid or missing DSAFSTestRunner (null).");
		}
		if(testrunner.getSolo()== null){
			testrunner.launchApplication();			
		}
		if(testrunner.getSolo()== null){
			throw new RemoteException("DGuiObjectRecognition invalid or missing Robotium Solo object (null).");
		}
		return testrunner.getSolo();
    }

    /**
     * @return The currently active Application.
     * @throws RemoteException if no static testrunner or Solo object has been set.
     * @see #setSAFSTestRunner(DSAFSTestRunner)
     * @see android.app.Application
     */
	public static Application getForegroundApplication()throws RemoteException{
		return autostartMainLauncher().getCurrentActivity().getApplication();	    
	}

	/**
	 * Returns the one topmost parent View of the View that is currently focused.
	 * This may not be the same as any View in the "current" Activity if there is a 
	 * Dialog or PopupWindow being displayed in front of the "current" Activity.
	 * @return the topmost parent View of the View currently focused.
     * @throws RemoteException if no static testrunner or Solo object has been set or 
     * if Solo cannot seem to get the currently focused View of the Current Window.
     * @see #setSAFSTestRunner(DSAFSTestRunner)
	 */
	public static Object[] getTopLevelWindows()throws RemoteException{
		// getCurrentFocus can return null!
		RCSolo solo = autostartMainLauncher();
		solo.finishInactiveActivities();
		View view = null;
		ArrayList<View> topwins = new ArrayList<View>();
		List<Activity> activities = solo.getAllOpenedActivities();
		for(int i=0; i < activities.size();i++){
			Activity act = activities.get(i);
			view = act.getCurrentFocus();// TouchMode may remove focus!
			if(view == null) {
				try{ view = act.getWindow().peekDecorView(); }catch(Exception ignore){}
				if(view != null) view = solo.getTopParent(view);
				if(view != null){
					if(! topwins.contains(view) && view.isShown()) 
						topwins.add(view);
				}
			}else{
				view = solo.getTopParent(view);
				if(! topwins.contains(view) && view.isShown())
					topwins.add(0, view);
			}
		}
		view = null;
		try{ view = solo.getViews().get(0).getRootView();}catch(Exception ignore){}
		if(view == null)
			throw new RemoteException("Solo does not have the most current Activity or Dialog with Window focus at this time!");
		view = solo.getTopParent(view);
		if(view != null){
			if(! topwins.contains(view) && view.isShown()) 
				topwins.add(0,view);
		}
		debug("TopParent Views: "+ topwins.size());
		return topwins.toArray();
	}

	/**
	 * @return true if the View is a ViewGroup or an AdapterView that has children.
	 * false otherwise.
	 */
	public static boolean hasChildren(Object view){
		if(view instanceof AdapterView){
			return (((AdapterView)view).getChildCount() > 0);
		}else
		return (view instanceof ViewGroup)            ? 
			   (((ViewGroup)view).getChildCount() > 0):
			   false;
	}

	/**
	 * @param aparent ViewGroup or AdapterView from which to seek children.
	 * Only a ViewGroup is checked for child Views.
	 * @return Array of 0 or more View children.
	 */
	public static Object[] getChildren(Object aparent){
		ArrayList<View> views = new ArrayList<View>();
		Object[] children = new Object[0];
		if(aparent instanceof AdapterView){
			AdapterView avparent = (AdapterView) aparent;
			Adapter adapter = avparent.getAdapter();
			View aview = null;
			int firstVisible = avparent.getFirstVisiblePosition();
			int lastVisible = avparent.getLastVisiblePosition();
			for(int i = 0;i < avparent.getCount();i++){				
				//try{views.add(adapter.getView(i, null, gparent));}
				//catch(Exception ignore){}
				if(i < firstVisible || i > lastVisible){
					debug("DGOR.getChildren attempting to retrieve offscreen child["+i+"] info from AdapterView...");
					aview = adapter.getView(i, null, avparent);
				}else{
					debug("DGOR.getChildren attempting to retrieve onscreen child["+(i-firstVisible)+"] info from AdapterView...");
					aview = avparent.getChildAt(i - firstVisible);
				}
				views.add(aview);
			}
			try{views.remove(avparent);}catch(Exception ignore){ }
			if (views.size() > 0) children = views.toArray(children);
		}else
		if(aparent instanceof ViewGroup){
			ViewGroup gparent = (ViewGroup) aparent;
			for(int i = 0;i < gparent.getChildCount();i++){
				views.add(gparent.getChildAt(i));
			}
			views.remove(aparent);
			if (views.size() > 0) children = views.toArray(children);
		}
		return children;
	}

	/**
	 * Array of known top-level window recognition strings
	 */
	public static final String[] TOPLEVEL_WINDOW_RECS_UC = {
		"CURRENTWINDOW",
		"CLASS=COM.ANDROID.INTERNAL.POLICY.IMPL.PHONEWINDOW$DECORVIEW",
		"CLASS=ANDROID.WIDGET.POPUPWINDOW$POPUPVIEWCONTAINER",//For PopupWindow
		"TYPE=WINDOW"
	};
	
	/**
	 * Initial implementation to detect that a recognition string snippet is likely pointing to 
	 * the top-level window itself and not a child of the top-level window.
	 * @param objstring
	 * @return true if the recognition string is for a topmost Window, Activity, or View
	 */
	public static boolean isTopLevelWindowRecognition(String objstring){
		String tag = TAG+".isTopLevelWindowRecognition ";
		debug(tag+" objstring = "+ objstring);
		if (objstring == null) {
			debug(tag+" == false");
			return false;
		}
		String uc_objectstring =objstring.toUpperCase();
		for(int i=0;i< TOPLEVEL_WINDOW_RECS_UC.length; i++){
			if(uc_objectstring.startsWith(TOPLEVEL_WINDOW_RECS_UC[i])) {
				debug(tag+" == true");
				return true;
			}
		}
		debug(tag+" == false");
		return false;
	}
	
	/**
	 * Currently not implemented...
	 * @param objstring
	 * @return true if the Object is an instanceof or a class of a popup type of container
	 */
	public static boolean isTopLevelPopupContainer(Object obj){
		// TODO:
	    //return true if the obj is instanceof or class of such a thing;
		return false;
	}
	
	/**
	 * @param obj
	 * @return full classname of the object. ex: org.safs.android.view.CustomView
	 * @throws NullPointerException on null obj reference.
	 */
	public static String getObjectClassName(Object obj)throws NullPointerException{
		return obj.getClass().getName();	    
	}

	/**
	 * Get the full class hierarchy of superclass classnames, begin from java.lang.Object.
	 * @param obj
	 * @return String[] of superclass classname hierarchy. Class String java.lang.Object is the first in the Array.
	 * @throws NullPointerException on null obj reference. 
	 */
	public static String[] getObjectSuperclassNames(Object obj){
		String[] rc = new String[0];
		ArrayList<String> list = new ArrayList<String>();
		Class oClass = obj.getClass();
		Class sClass = oClass;
		Class cClass = null;
		while(sClass != null){
			cClass = sClass.getSuperclass();
			sClass = cClass;
			try{ list.add(cClass.getName());}catch(Exception x){}
		}
		int size = list.size();
		rc = new String[size];
		for(int i=0;i < size;i++) rc[i]=(String)list.get(size-i-1);
	    return rc;
	}

	/**
	 * Android Views do not support Names, apparently.<br>
	 * <p>
	 * So we will try to see if the contentDescription is worth using.
	 * <p>
	 * We will try to get the view's id name defined in the xml file, for example, we have a 
	 * view definition like &lt;TextView android:id="@+id/text2" .../&gt;, then 'text2' will be returned.
	 * 
	 * @param obj
	 * @return
     * @throws NullPointerException if null obj reference.
     * @see #setSAFSTestRunner(DSAFSTestRunner)
     * @see #getObjectId(Object)(android.view.View)
	 */
	public static String getObjectName(Object obj) throws NullPointerException{
		String aname = null;
		try{
			AccessibilityNodeInfo info = ((View)obj).createAccessibilityNodeInfo();
			if(info != null){
				try{ aname = info.getContentDescription().toString();}catch(Exception x){}
				info.recycle();
			}
		}catch(Throwable nm){ /* API Level 14 and above. */ }

		if(aname == null){
			try{
				//Try to get the id's name from the layout XML file
				View view = (View) obj;
				Resources res = view.getResources();
				int ID = view.getId();
				if(ID!=View.NO_ID) aname = res.getResourceEntryName(ID);
			}catch(Throwable ignore){}
		}
		
		return aname;
	}

	/**
	 * @param obj
	 * @return view.getId()
     * @throws NullPointerException if null obj reference.
     * @see #setSAFSTestRunner(DSAFSTestRunner)
     * @see #getObjectIdString(Object)
	 */
	public static int getObjectId(Object obj) throws NullPointerException{
		return ((View)obj).getId();
	}
	
	/**
	 * @param obj
	 * @return String, the string value of object's id.<br>
	 *                 null, if the object is not an instance of View; or the id is {@value View#NO_ID}.<br>
	 * @see #getObjectId(Object)               
	 */
	public static String getObjectIdString(Object obj){
		String idString = null;
		
		try{
			if(obj instanceof View){
				int id = getObjectId(obj);
				if(id!=View.NO_ID){
					idString = String.valueOf(id);
				}
			}
		}catch(Throwable ignore){}
		
		return idString;
	}

	/**
	 * @param obj
	 * @return view.isEnabled()
     * @throws NullPointerException if null obj reference.
     * @see #setSAFSTestRunner(DSAFSTestRunner)
	 */
	public static boolean getObjectIsEnabled(Object obj) throws NullPointerException{
		return ((View)obj).isEnabled();
	}

	/**
	 * @param obj
	 * @return view.isShown()
     * @throws NullPointerException if null obj reference.
     * @see #setSAFSTestRunner(DSAFSTestRunner)
	 */
	public static boolean getObjectIsShowing(Object obj) throws NullPointerException{
		return ((View)obj).isShown();
	}

	/**
	 * @param obj
	 * @return view.createAccessibilityNodeInfo().getContentDescription() != null
     * @throws NullPointerException if null obj reference.
     * @see #setSAFSTestRunner(DSAFSTestRunner)
	 */
	public static boolean getObjectIsValid(Object obj) throws NullPointerException{
		boolean rc = false;
		try{
			AccessibilityNodeInfo info = ((View)obj).createAccessibilityNodeInfo();
			if(info != null){
				try{rc  = info.getContentDescription() != null;}catch(Exception x){}
				info.recycle();
			}
		}catch(Throwable nm){ /* API level 14 and above */
			rc = (((View)obj).getLayoutParams() != null);
		}
		return rc;
	}

	/**
	 * @param obj View subclass like TextView, or Adapter subclass.
	 * @return ((TextView)view).getText().toString().  Can be null if we 
	 * don't yet know how to get the text.
     * @throws NullPointerException if null obj reference.
     * of View provided is NOT one we know how to handle yet.
     * @see #setSAFSTestRunner(DSAFSTestRunner)
	 */
	public static String getObjectText(Object obj)throws NullPointerException{
		if(obj instanceof TextView){
			return ((TextView)obj).getText().toString();
		}
		if(obj instanceof View){
			String val = null;
			try{
				AccessibilityNodeInfo info = ((View)obj).createAccessibilityNodeInfo();
				if(info != null){
					try{val = info.getText().toString();}catch(Exception x){}
					info.recycle();
				}
			}
			catch(Throwable nm){ /* API Level 14 or higher */ }
			if (val == null) val = obj.toString();
			return val;
		}
		if(obj instanceof Intent){
			// extract any data with getExtras()
			Bundle bundle = ((Intent)obj).getExtras();			
			if(bundle == null) {
				debug("DGOR.getObjectText cannot yet handle Intent with no Extras() Bundle: "+ obj.getClass().getName());		
				return obj.toString();
			}
			if(bundle.isEmpty()){
				debug("DGOR.getObjectText cannot yet handle Intent with empty Extras() Bundle: "+ obj.getClass().getName());		
				return obj.toString();
			}
			Set keys = bundle.keySet();
			// currently only process the first one
			Iterator ikeys = keys.iterator();			
			String key = (String) ikeys.next();
			Object item = bundle.get(key);
			return String.valueOf(item);
		}
		if(obj instanceof HashMap){
			Collection items = ((HashMap)obj).values();
			// currently only process the first String
			for(Object item: items){
				if(item instanceof String) return String.valueOf(item);
			}
			debug("DGOR.getObjectText cannot yet handle HashMap with no String values..");		
			return obj.toString();
		}
		debug("DGOR.getObjectText cannot yet handle instanceof "+ obj.getClass().getName());		
		return obj.toString();
	}

	/**
	 * getTitle if Object is an Activity.  Otherwise, we try getObjectText.
	 * @param obj
	 * @return Activity.getTitle or TextView.getText or null if we don't know how 
	 * to get the text.
     * @throws NullPointerException if null obj reference.
     * @throws RemoteException if no static testrunner or Solo object has been set or if the subclass 
     * of Object provided is NOT one we know how to handle yet.
     * @throws NullPointerException if the Object provided was null.
     * @see #setSAFSTestRunner(DSAFSTestRunner)
	 */
	public static String getObjectCaption(Object obj) throws RemoteException, NullPointerException{
		if(obj instanceof Activity){
			return ((Activity)obj).getTitle().toString();
		}else if(obj instanceof TextView){
			return getObjectText((TextView)obj);
		}else{
			String val = null;
			try{
				AccessibilityNodeInfo info = ((View)obj).createAccessibilityNodeInfo();
				if(info != null){
					try{ val = info.getText().toString();}catch(Exception x){}
				    info.recycle();
				}
			}catch(Throwable nm){ /* API Level 14 */ }
			return val;
		}
	}

	
	/************************************************************
	*
	* GuiObjectRecognition.getObjectProperty (obj, propname)
	*
	*@return A String.  Arrays of values will generally be returned from {@link #convertArrayToDelimitedString(ArrayList)} 
	*************************************************************/
	public static String getObjectProperty(Object obj, String propname){
		//String rc = null;
	    if(obj == null || propname==null) return SAFSMessage.NULL_VALUE;
	    
		if (PROP_CLASSNAME.equalsIgnoreCase(propname))return getObjectClassName(obj);
	    try {
			if(obj instanceof Window)
			{	
				if (PROP_LAYOUTPARAMS.equalsIgnoreCase(propname)){
					WindowManager.LayoutParams attr = ((Window)obj).getAttributes();
					return attr.debug("");
				}
			}
		} 
	    catch (Throwable e) {	debug("Ignoring "+ e.getClass().getSimpleName()+": "+ e.getMessage()); }
		try{
		    if(obj instanceof Activity)
		    {
		    	if (PROP_CALLINGACTIVITY.equalsIgnoreCase(propname))         return ((Activity)obj).getCallingActivity().flattenToString();
		    	if (PROP_COMPONENTNAME.equalsIgnoreCase(propname))           return ((Activity)obj).getComponentName().flattenToString();
		    }    
		} 
		catch (Throwable e) {	debug("Ignoring "+ e.getClass().getSimpleName()+": "+ e.getMessage()); }
   		    
	    if(obj instanceof View)
	    {
	    	if (propname.length()> PROP_ACCESSIBLEPREFIX.length() && propname.startsWith(PROP_ACCESSIBLEPREFIX)){
	    		StringBuffer	sb = new StringBuffer(propname.substring(PROP_ACCESSIBLEPREFIX.length()));
   				sb.setCharAt(0, Character.toLowerCase(sb.charAt(0)));
	    		String realProp = sb.toString();
	    		String value = null;
	        	Object ani = null;
		    	try{ 
		    		ani = ((View)obj).createAccessibilityNodeInfo(/* API Level 14 and above */);
		    		AccessibilityNodeInfo info = (AccessibilityNodeInfo) ani;
		    		if(info == null) throw new NoSuchMethodException("createAccessibilityNodeInfo not supported.") ;
		    		if (PROP_ACCESSIBLEBOUNDSINPARENT.equalsIgnoreCase(propname)){
		    			Rect bounds = new Rect();
		    			info.getBoundsInParent(bounds);
		    			value = bounds.flattenToString();
			    	    try{info.recycle();}catch(Exception x){}
			    	    return value;
		    		}
		    		if (PROP_ACCESSIBLEBOUNDSINSCREEN.equalsIgnoreCase(propname)){
		    			Rect bounds = new Rect();
		    			info.getBoundsInScreen(bounds);
		    			value = bounds.flattenToString();
			    	    try{info.recycle();}catch(Exception x){}
			    	    return value;
		    		}
		    		value = getPropertyByReflection(info, realProp, true);
		    	    try{info.recycle();}catch(Exception x){}
		    	    return value;
				} 
		    	catch (Throwable e) { debug("Ignoring "+ e.getClass().getSimpleName()+": "+ e.getMessage()); }
	    	    try{((AccessibilityNodeInfo)ani).recycle();}catch(Throwable x){}
	    	}
	    	
    		if (PROP_CANSCROLLLEFT.equalsIgnoreCase(propname)){	    		
    			try{return String.valueOf(((View)obj).canScrollHorizontally(-1));}
				catch (Throwable e) { debug("Ignoring "+ e.getClass().getSimpleName()+": "+ e.getMessage()); }
				return SAFSMessage.NULL_VALUE;
    		}
    		if (PROP_CANSCROLLRIGHT.equalsIgnoreCase(propname)){	    		
    			try { return String.valueOf(((View)obj).canScrollHorizontally(1));}
				catch (Throwable e) { debug("Ignoring "+ e.getClass().getSimpleName()+": "+ e.getMessage());}
				return SAFSMessage.NULL_VALUE;
    		}
    		if (PROP_CANSCROLLUP.equalsIgnoreCase(propname)){	    		
    			try{ return String.valueOf(((View)obj).canScrollVertically(-1));}
				catch (Throwable e) { debug("Ignoring "+ e.getClass().getSimpleName()+": "+ e.getMessage()); }
				return SAFSMessage.NULL_VALUE;
    		}
    		if (PROP_CANSCROLLDOWN.equalsIgnoreCase(propname)){	    		
    			try{ return String.valueOf(((View)obj).canScrollVertically(1));}
				catch (Throwable e) { debug("Ignoring "+ e.getClass().getSimpleName()+": "+ e.getMessage()); }
				return SAFSMessage.NULL_VALUE;
    		}
    		if (PROP_DRAWINGRECT.equalsIgnoreCase(propname)){
    			Rect rect = new Rect();
    			((View)obj).getDrawingRect(rect);
    			return rect.flattenToString();
    		}
    		if (PROP_GLOBALVISIBLERECT.equalsIgnoreCase(propname)){
    			Rect rect = new Rect();
    			if(((View)obj).getGlobalVisibleRect(rect))
    				 return rect.flattenToString();
    			else return SAFSMessage.NULL_VALUE;    			
    		}
    		if (PROP_HITRECT.equalsIgnoreCase(propname)){
    			Rect rect = new Rect();
    			((View)obj).getHitRect(rect);
    			return rect.flattenToString();
    		}
    		if (PROP_LOCALVISIBLERECT.equalsIgnoreCase(propname)){
    			Rect rect = new Rect();
    			if(((View)obj).getLocalVisibleRect(rect))
        		     return rect.flattenToString();
    			else return SAFSMessage.NULL_VALUE; 
    		}
    		if (PROP_LOCATIONINWINDOW.equalsIgnoreCase(propname)){
    			int[] loc = new int[2];
    			((View)obj).getLocationInWindow(loc);
       			return String.valueOf(loc[0])+" "+ String.valueOf(loc[1]);
    		}
    		if (PROP_LOCATIONONSCREEN.equalsIgnoreCase(propname)){
    			int[] loc = new int[2];
    			((View)obj).getLocationOnScreen(loc);
       			return String.valueOf(loc[0])+" "+ String.valueOf(loc[1]);
    		}
    		if (PROP_WINDOWVISIBLEDISPLAYFRAME.equalsIgnoreCase(propname)){	    		
    			Rect rect = new Rect();
    			((View)obj).getWindowVisibleDisplayFrame(rect);
       			return rect.flattenToString();
    		}
	    }
	    try{
	    	if(obj instanceof ViewGroup){
				if(PROP_VISIBLEITEMCOUNT.equalsIgnoreCase(propname)){
					return String.valueOf(((ViewGroup)obj).getChildCount());
				}
				if(PROP_VISIBLEITEMS.equalsIgnoreCase(propname)){
					ViewGroup gparent = (ViewGroup) obj;
					ArrayList views = new ArrayList();
					for(int i = 0;i < gparent.getChildCount();i++){
						try{ views.add(gparent.getChildAt(i));}catch(Exception ignore){}
					}
					views.remove(gparent);
					ArrayList texts = new ArrayList();
					for(Object item:views){
						texts.add(getObjectText(item));
					}
	    			return convertArrayToDelimitedString(texts);
				}
	    	}
		} catch (Throwable e) {
			debug("Ignoring "+ e.getClass().getSimpleName()+": "+ e.getMessage());
		}	    

		try{ 
	    	if(obj instanceof AbsListView)
		    {	    	
	    		if(PROP_CHECKEDITEMIDS.equalsIgnoreCase(propname)){	    			
	    			ArrayList items = new ArrayList();
	    			long[] ids = ((AbsListView)obj).getCheckedItemIds();
	    			if(ids.length == 0) return SAFSMessage.NULL_VALUE;
	    			for(int i=0;i < ids.length;){
	    				items.add(String.valueOf(ids[i++]));
	    			}	    			
	    			return convertArrayToDelimitedString(items);
	    		}
	    		if(PROP_CHECKEDITEMPOSITIONS.equalsIgnoreCase(propname)){
	    			ArrayList items = new ArrayList();
	    			SparseBooleanArray ids = ((AbsListView)obj).getCheckedItemPositions();
	    			if(ids == null) return SAFSMessage.NULL_VALUE;
	    			for(int i=0;i < ids.size();i++){
	    				items.add(String.valueOf(ids.get(i)));
	    			}	    			
	    			return convertArrayToDelimitedString(items);
	    		}	    		
		    }
		} catch (Throwable e) {
			debug("Ignoring "+ e.getClass().getSimpleName()+": "+ e.getMessage());
		}	    
	    
	    try{ 
	    	if(obj instanceof ListView)
		    {	    	
	    		if(PROP_CHECKITEMIDS.equalsIgnoreCase(propname)){
	    			ArrayList items = new ArrayList();
    				long[] ids = ((ListView)obj).getCheckItemIds();
	    			for(int i=0;i < ids.length;){
	    				items.add(String.valueOf(ids[i++]));
	    			}	    			
	    			return convertArrayToDelimitedString(items);
	    		}
	    		if(PROP_ITEMS.equalsIgnoreCase(propname)){
	    			ListView list = (ListView) obj;
	    			Object[] children = getChildren(list);
	    			if(children.length > 0){    
		    			ArrayList items = new ArrayList();
		    			for(int i=0; i < children.length;i++){
		    				items.add(getObjectText(children[i]));
		    			}
		    			return convertArrayToDelimitedString(items);
	    			}
	    			// otherwise, try to get items via the adapter below
	    		}
	    		ListAdapter adapter = ((ListView)obj).getAdapter();
	    		String rv = getAdapterProperty(adapter, propname);
	    		if(rv != null) return rv;

	    		// remaining ListView properties handled by reflection
		    }
		} catch (Throwable e) {
			debug("Ignoring "+ e.getClass().getSimpleName()+": "+ e.getMessage());
		}	    

		try{
			if(obj instanceof TextView){
				if(PROP_FOCUSEDRECT.equalsIgnoreCase(propname)){
					Rect rect = new Rect();
					((TextView) obj).getFocusedRect(rect);
					return String.valueOf(rect);
				}
	    		// remaining TextView properties handled by reflection
			}
		}catch(Throwable e){
			debug("Ignoring "+ e.getClass().getSimpleName()+": "+ e.getMessage());				
		}
		
		try{
			if(obj instanceof AbsSpinner){
				SpinnerAdapter adapter = ((AbsSpinner)obj).getAdapter();
				String rv = getAdapterProperty(adapter, propname);
				if(rv != null) return rv;
				// remaining AbsSpinner properties handled by reflection
			}
		}catch(Throwable e){
			debug("Ignoring "+ e.getClass().getSimpleName()+": "+ e.getMessage());				
		}
		
		// Window         -- handled above and by Reflection
		// Activity       -- handled entirely by Reflection
		// ActionBar      -- handled entirely by Reflection
		// ActionBar.Tab  -- handled entirely by Reflection
		// View           -- handled above and by Reflection -- including AccessibleNodeInfo
		// ViewGroup      -- handled entirely by Reflection		
		// AbsListView    -- handled above and by Reflection
		// AdapterView    -- handled entirely by Reflection		
		// CompoundButton -- handled entirely by Reflection
		// Switch         -- handled entirely by Reflection
		// ToggleButton   -- handled entirely by Reflection
		// Display        -- handled entirely by Reflection
		// ProgressDialog -- handled entirely by Reflection
		// Spinner        -- handled entirely by Reflection
		
	    String value = getPropertyByReflection(obj, propname, true);
	    if(value!=null){
	    	debug(TAG+" received property value '"+value+"' for '"+propname+"'");
	    	return value;
	    }	    
	    return SAFSMessage.NULL_VALUE;
	}	

	/** The possible prefix for the name of accessory method*/
	public static final String PREFIX_PROP_GET 		= "get";
	public static final String PREFIX_PROP_IS 		= "is";
	public static final String PREFIX_PROP_HAS 		= "has";
	public static final String[] PREFIX_PROP_ARRAY 	= {PREFIX_PROP_GET, PREFIX_PROP_IS, PREFIX_PROP_HAS};	
	
	//Shared property Names
	public static final String PROP_CLASSNAME 			= "class";
	public static final String PROP_CONTENTDESCRIPTION 	= "contentDescription";
	public static final String PROP_GRAVITY 			= "Gravity";
	public static final String PROP_HASWINDOWFOCUS 		= "hasWindowFocus";
	public static final String PROP_HEIGHT 				= "height";
	public static final String PROP_LAYOUTPARAMS 		= "layoutParams";
	public static final String PROP_ROTATION 			= "rotation";
	public static final String PROP_TEXT 				= "text";
	public static final String PROP_TITLE 				= "title";
	public static final String PROP_WIDTH 				= "width";
	
	//Window
	public static final String PROP_HASCHILDREN 		= "hasChildren";
	public static final String PROP_ISFLOATING 			= "isFloating";
	public static final String PROP_ISACTIVE 			= "isActive";
	/** ARRAY_PROP_WINDOW contains all property names should be returned for Window object.*/
	public static final String[] ARRAY_PROP_WINDOW = {
		PROP_LAYOUTPARAMS, PROP_HASCHILDREN, PROP_ISFLOATING, PROP_ISACTIVE
	};
	
	//Activity	
	public static final String PROP_APPLICATION = "application";
	public static final String PROP_CALLINGACTIVITY = "callingActivity";
	public static final String PROP_CALLINGPACKAGE = "callingPackage";
	public static final String PROP_COMPONENTNAME = "componentName";
	public static final String PROP_LOCALCLASSNAME = "localClassName";
	public static final String PROP_REQUESTEDORIENTATION = "requestedOrientation";
	public static final String PROP_TASKID = "taskId";
	public static final String PROP_TITLECOLOR = "titleColor";
	public static final String PROP_ISCHANGINGCONFIGURATIONS = "isChangingConfigurations";
	public static final String PROP_ISCHILD = "isChild";
	public static final String PROP_ISFINISHING = "isFinishing";
	public static final String PROP_ISTASKROOT = "isTaskRoot";
	/** ARRAY_PROP_ACTIVITY contains all property names should be returned for Activity object.*/
	public static final String[] ARRAY_PROP_ACTIVITY = {
		PROP_APPLICATION,PROP_CALLINGACTIVITY,PROP_CALLINGPACKAGE,PROP_COMPONENTNAME
		,PROP_LOCALCLASSNAME,PROP_REQUESTEDORIENTATION,PROP_TASKID,PROP_TITLECOLOR
		,PROP_ISCHANGINGCONFIGURATIONS,PROP_ISCHILD,PROP_ISFINISHING,PROP_ISTASKROOT
		,PROP_HASWINDOWFOCUS,PROP_TITLE/*Shared with other object*/
	};
	
	//ActionBar
	public static final String PROP_DISPLAYOPTIONS = "displayOptions";
	public static final String PROP_ISSHOWING = "isShowing";
	public static final String PROP_NAVIGATIONITEMCOUNT = "navigationItemCount";
	public static final String PROP_NAVIGATIONMODE = "navigationMode";
	public static final String PROP_SELECTEDTAB = "selectedTab";
	public static final String PROP_SELECTEDNAVIGATIONINDEX = "selectedNavigationIndex";
	public static final String PROP_SUBTITLE = "subtitle";
	public static final String PROP_TABCOUNT = "tabCount";
	/** ARRAY_PROP_ACTIONBAR contains all property names should be returned for ActionBar object.*/
	public static final String[] ARRAY_PROP_ACTIONBAR = {
		PROP_DISPLAYOPTIONS,PROP_ISSHOWING,PROP_NAVIGATIONITEMCOUNT,PROP_NAVIGATIONMODE
		,PROP_SELECTEDTAB,PROP_SELECTEDNAVIGATIONINDEX,PROP_SUBTITLE,PROP_TABCOUNT,
		PROP_HEIGHT,PROP_TITLE/*Shared with other object*/
	};
	
	//ActionBar.Tab
	public static final String PROP_POSITION = "position";
	/** ARRAY_PROP_ACTIONBAR_TAB contains all property names should be returned for ActionBar.Tab object.*/
	public static final String[] ARRAY_PROP_ACTIONBAR_TAB = {
		PROP_POSITION
		,PROP_TEXT,PROP_CONTENTDESCRIPTION/*Shared with other object*/
	};
	
	//Display
	public static final String PROP_DISPLAYID = "displayId";
	public static final String PROP_ORIENTATION = "orientation";
	public static final String PROP_PIXELFORMAT = "pixelFormat";
	public static final String PROP_REFRESHRATE = "refreshRate";
	/** ARRAY_PROP_DISPLAY contains all property names should be returned for Display object.*/
	public static final String[] ARRAY_PROP_DISPLAY = {
		PROP_DISPLAYID,PROP_ORIENTATION,PROP_PIXELFORMAT,PROP_REFRESHRATE
		,PROP_HEIGHT,PROP_ROTATION,PROP_WIDTH/*Shared with other object*/
	};
	
	//ProgressDialog
	public static final String PROP_MAX = "max";
	public static final String PROP_PROGRESS = "progress";
	public static final String PROP_SECONDARYPROGRESS = "secondaryProgress";
	public static final String PROP_ISINDETERMINATE = "isIndeterminate";
	/** ARRAY_PROP_DISPLAY contains all property names should be returned for Display object.*/
	public static final String[] ARRAY_PROP_PROGRESSDIALOG = {
		PROP_MAX,PROP_PROGRESS,PROP_SECONDARYPROGRESS,PROP_ISINDETERMINATE	
	};
	
	//AccessibilityNodeInfo
	public static final String PROP_ACCESSIBLEPREFIX = "accessible";
	public static final String PROP_ACCESSIBLEISACCESSIBILITYFOCUSED = "accessibleIsAccessibilityFocused";/* API Level 16 and above */
	public static final String PROP_ACCESSIBLEBOUNDSINPARENT = "accessibleBoundsInParent";
	public static final String PROP_ACCESSIBLEBOUNDSINSCREEN = "accessibleBoundsInScreen";
	public static final String PROP_ACCESSIBLECHILDCOUNT = "accessibleChildCount";
	public static final String PROP_ACCESSIBLECONTENTDESCRIPTION = "accessibleContentDescription";
	public static final String PROP_ACCESSIBLEPACKAGENAME = "accessiblePackageName";
	public static final String PROP_ACCESSIBLETEXT = "accessibleText";
	public static final String PROP_ACCESSIBLEWINDOWID = "accessibleWindowId";
	public static final String PROP_ACCESSIBLEISCHECKABLE = "accessibleIsCheckable";
	public static final String PROP_ACCESSIBLEISCHECKED = "accessibleIsChecked";
	public static final String PROP_ACCESSIBLEISCLICKABLE = "accessibleIsClickable";
	public static final String PROP_ACCESSIBLEISENABLED = "accessibleIsEnabled";
	public static final String PROP_ACCESSIBLEISFOCUSABLE = "accessibleIsFocusable";
	public static final String PROP_ACCESSIBLEISFOCUSED = "accessibleIsFocused";
	public static final String PROP_ACCESSIBLEISLONGCLICKABLE = "accessibleIsLongClickable";
	public static final String PROP_ACCESSIBLEISPASSWORD = "accessibleIsPassword";
	public static final String PROP_ACCESSIBLEISSCROLLABLE = "accessibleIsScrollable";
	public static final String PROP_ACCESSIBLEISSELECTED = "accessibleIsSelected";
	public static final String PROP_ACCESSIBLEISVISIBLETOUSER = "accessibleIsVisibleToUser";/* API Level 16 and above */
	/** ARRAY_PROP_ACCESSIBLITY contains all property names should be returned for object containing AccessibilityNodeInfo.*/
	public static final String[] ARRAY_PROP_ACCESSIBLITY = {
		PROP_ACCESSIBLEISACCESSIBILITYFOCUSED,PROP_ACCESSIBLEBOUNDSINPARENT,PROP_ACCESSIBLEBOUNDSINSCREEN
		,PROP_ACCESSIBLECHILDCOUNT,PROP_ACCESSIBLECONTENTDESCRIPTION,PROP_ACCESSIBLEPACKAGENAME
		,PROP_ACCESSIBLETEXT,PROP_ACCESSIBLEWINDOWID,PROP_ACCESSIBLEISCHECKABLE,PROP_ACCESSIBLEISCHECKED
		,PROP_ACCESSIBLEISCLICKABLE,PROP_ACCESSIBLEISENABLED,PROP_ACCESSIBLEISFOCUSABLE
		,PROP_ACCESSIBLEISFOCUSED,PROP_ACCESSIBLEISLONGCLICKABLE,PROP_ACCESSIBLEISPASSWORD
		,PROP_ACCESSIBLEISSCROLLABLE,PROP_ACCESSIBLEISSELECTED,PROP_ACCESSIBLEISVISIBLETOUSER		
	};

	// ViewGroup (extends from View)
	public static final String PROP_CHILDCOUNT = "childCount";
	public static final String PROP_ISALWAYSDRAWNWITHCACHEENABLED = "isAlwaysDrawnWithCacheEnabled";
	public static final String PROP_ISANIMATIONCACHEENABLED = "isAnimationCacheEnabled";
	public static final String PROP_ISMOTIONEVENTSPLITTINGENABLED = "isMotionEventSplittingEnabled";
	public static final String PROP_SHOULDDELAYCHILDPRESSEDSTATE = "shouldDelayChildPressedState";
	/** ARRAY_PROP_VIEWGROUP contains all property names should be returned for ViewGroup object.*/
	public static final String[] ARRAY_PROP_VIEWGROUP = {
		PROP_CHILDCOUNT,PROP_ISALWAYSDRAWNWITHCACHEENABLED,PROP_ISANIMATIONCACHEENABLED
		,PROP_ISMOTIONEVENTSPLITTINGENABLED,PROP_SHOULDDELAYCHILDPRESSEDSTATE		
	};
	
	// AdapterView (extends from ViewGroup)
	public static final String PROP_COUNT = "count";
	public static final String PROP_FIRSTVISIBLEPOSITION = "firstVisiblePosition";
	public static final String PROP_LASTVISIBLEPOSITION = "lastVisiblePosition";
	public static final String PROP_VISIBLEITEMCOUNT = "visibleItemCount";
	public static final String PROP_VISIBLEITEMS = "visibleItems";
	public static final String PROP_SELECTEDITEMID = "selectedItemId";
	public static final String PROP_SELECTEDITEMPOSITION = "selectedItemPosition";
	/** ARRAY_PROP_ADAPTERVIEW contains all property names should be returned for AdapterView object.*/
	public static final String[] ARRAY_PROP_ADAPTERVIEW = {
		PROP_COUNT,PROP_FIRSTVISIBLEPOSITION,PROP_LASTVISIBLEPOSITION
		,PROP_SELECTEDITEMID,PROP_SELECTEDITEMPOSITION, PROP_VISIBLEITEMCOUNT, PROP_VISIBLEITEMS		
	};
	
	// AbsListView (extends from AdapterView)
	public static final String PROP_CACHECOLORHINT = "cacheColorHint";
	public static final String PROP_CHECKEDITEMCOUNT = "checkedItemCount";
	public static final String PROP_CHECKEDITEMIDS = "checkedItemIds";
	public static final String PROP_CHECKEDITEMPOSITION = "checkedItemPosition";
	public static final String PROP_CHECKEDITEMPOSITIONS = "checkedItemPositions";
	public static final String PROP_CHOICEMODE = "choiceMode";
	public static final String PROP_LISTPADDINGBOTTOM = "listPaddingBottom";
	public static final String PROP_LISTPADDINGLEFT = "listPaddingLeft";
	public static final String PROP_LISTPADDINGRIGHT = "listPaddingRight";
	public static final String PROP_LISTPADDINGTOP = "listPaddingTop";
	public static final String PROP_TRANSCRIPTMODE = "transcriptMode";
	public static final String PROP_TEXTFILTER = "textFilter";
	public static final String PROP_HASTEXTFILTER = "hasTextFilter";
	public static final String PROP_ISFASTSCROLLALWAYSVISIBLE = "isFastScrollAlwaysVisible";
	public static final String PROP_ISFASTSCROLLENABLED = "isFastScrollEnabled";
	public static final String PROP_ISSCROLLINGCACHEENABLED = "isScrollingCacheEnabled";
	public static final String PROP_ISSMOOTHSCROLLBARENABLED = "isSmoothScrollbarEnabled";
	public static final String PROP_ISSTACKFROMBOTTOM = "isStackFromBottom";
	public static final String PROP_ISTEXTFILTERENABLED = "isTextFilterEnabled";
	/** ARRAY_PROP_ABSLISTVIEW contains all property names should be returned for AbsListView object.*/
	public static final String[] ARRAY_PROP_ABSLISTVIEW = {
		PROP_CACHECOLORHINT,PROP_CHECKEDITEMCOUNT,PROP_CHECKEDITEMIDS,PROP_CHECKEDITEMPOSITION
		,PROP_CHECKEDITEMPOSITIONS,PROP_CHOICEMODE,PROP_LISTPADDINGBOTTOM,PROP_LISTPADDINGLEFT
		,PROP_LISTPADDINGRIGHT,PROP_LISTPADDINGTOP,PROP_TRANSCRIPTMODE,PROP_TEXTFILTER
		,PROP_HASTEXTFILTER,PROP_ISFASTSCROLLALWAYSVISIBLE,PROP_ISFASTSCROLLENABLED
		,PROP_ISSCROLLINGCACHEENABLED,PROP_ISSMOOTHSCROLLBARENABLED,PROP_ISSTACKFROMBOTTOM
		,PROP_ISTEXTFILTERENABLED
	};
	
	// ListView (extends from AbsListView)
	public static final String PROP_CHECKITEMIDS = "checkItemIds";
	public static final String PROP_DIVIDERHEIGHT = "deviderHeight";
	public static final String PROP_FOOTERVIEWSCOUNT = "footerViewsCount";
	public static final String PROP_HEADERVIEWSCOUNT = "headerViewsCount";
	public static final String PROP_ITEMSCANFOCUS = "itemsCanFocus";
	public static final String PROP_MAXSCROLLAMOUNT = "maxScrollAmount";
	/** ARRAY_PROP_LISTVIEW contains all property names should be returned for ListView object.*/
	public static final String[] ARRAY_PROP_LISTVIEW = {
		PROP_CHECKITEMIDS,PROP_DIVIDERHEIGHT,PROP_FOOTERVIEWSCOUNT,PROP_HEADERVIEWSCOUNT
		,PROP_ITEMSCANFOCUS,PROP_MAXSCROLLAMOUNT		
	};	
	
	//AbsSpinner (extends from AdapterView)
	/**
	 * PROP_ABSSPINNER_COUNT is redundancy with AdapterView's PROP_COUNT, 
	 * as AbsSpinner is subclass of AdapterView, so we don't need to add this 
	 * property name to {@link #getObjectPropertyNames(Object)} again for AbsSpinner
	 */
	//public static final String PROP_ABSSPINNER_COUNT		 	= PROP_COUNT
	//public static final String PROP_POINTTOPOSITION 			= "pointToPosition";
	
	//Spinner (extends from AbsSpinner)
	public static final String PROP_DROPDOWNHORIZONTALOFFSET 	= "DropDownHorizontalOffset";
	public static final String PROP_DROPDOWNVERTICALOFFSET 		= "DropDownVerticalOffset";
	public static final String PROP_DROPDOWNWIDTH				= "DropDownWidth";
	public static final String PROP_PROMPT 						= "Prompt";
	public static final String[] ARRAY_PROP_SPINNER	= {
		PROP_DROPDOWNHORIZONTALOFFSET,PROP_DROPDOWNVERTICALOFFSET,
		PROP_DROPDOWNWIDTH,PROP_PROMPT,
		PROP_GRAVITY/*name shared with other object*/
	};
	
	//TextView (extends from View)
	public static final String PROP_DIDTOUCHFOCUSSELECT 	= "didTouchFocusSelect";
	public static final String PROP_HASSELECTION 			= "hasSelection";
	public static final String PROP_HASOVERLAPPINGRENDERING	= "hasOverlappingRendering";
	public static final String PROP_AUTOLINKMASK 			= "AutoLinkMask";
	public static final String PROP_COMPOUNDDRAWABLEPADDING = "CompoundDrawablePadding";
	public static final String PROP_COMPOUNDPADDINGBOTTOM 	= "CompoundPaddingBottom";
	public static final String PROP_COMPOUNDPADDINGLEFT 	= "CompoundPaddingLeft";
	public static final String PROP_COMPOUNDPADDINGRIGHT 	= "CompoundPaddingRight";
	public static final String PROP_COMPOUNDPADDINGTOP 		= "CompoundPaddingTop";
	public static final String PROP_CURRENTHINTTEXTCOLOR	= "CurrentHintTextColor";
	public static final String PROP_CURRENTTEXTCOLOR 		= "CurrentTextColor";
	public static final String PROP_EXTENDEDPADDINGBOTTOM 	= "ExtendedPaddingBottom";
	public static final String PROP_EXTENDEDPADDINGTOP 		= "ExtendedPaddingTop";
	public static final String PROP_FOCUSEDRECT 			= "FocusedRect";
	public static final String PROP_FREEZETEXT 				= "FreezesText";
	public static final String PROP_HIGHLIGHTCOLOR 			= "HighlightColor";
	public static final String PROP_HINT 					= "Hint";
	public static final String PROP_IMEACTIONID 			= "ImeActionId";
	public static final String PROP_IMEACTIONLABEL 			= "ImeActionLabel";
	public static final String PROP_IMEOPTIONS	 			= "ImeOptions";
	public static final String PROP_INCLUDEDFONTPADDING 	= "IncludeFontPadding";
	public static final String PROP_INPUTTYPE 				= "InputType";
	public static final String PROP_LINECOUNT 				= "LineCount";
	public static final String PROP_LINEHEIGHT 				= "LineHeight";
	public static final String PROP_LINESPACINGEXTRA		= "LineSpacingExtra";
	public static final String PROP_LINESPACINGMULTIPLIER	= "LineSpacingMultiplier";
	public static final String PROP_LINKSCLICKABLE			= "LinksClickable";
	public static final String PROP_MARQUEEREPEATLIMIT		= "MarqueeRepeatLimit";
	public static final String PROP_MAXHEIGHT 				= "MaxHeight";
	public static final String PROP_MAXLINES 				= "MaxLines";
	public static final String PROP_MAXWIDTH 				= "MaxWidth";
	public static final String PROP_MINHEIGHT 				= "MinHeight";
	public static final String PROP_MINLINES 				= "MinLines";
	public static final String PROP_MINWIDTH 				= "MinWidth";
	public static final String PROP_TEXTSCALEX 				= "TextScaleX";
	public static final String PROP_TEXTSIZE				= "TextSize";
	public static final String PROP_TOTALPADDINGBOTTOM		= "TotalPaddingBottom";
	public static final String PROP_TOTALPADDINGLEFT 		= "TotalPaddingLeft";
	public static final String PROP_TOTALPADDINGRIGHT 		= "TotalPaddingRight";
	public static final String PROP_TOTALPADDINGTOP 		= "TotalPaddingTop";
	public static final String PROP_SELECTIONEND 			= "SelectionEnd";
	public static final String PROP_SELECTIONSTART 			= "SelectionStart";
	public static final String PROP_CURSORVISIBLE 		= "CursorVisible";/*Since: API Level 16*/
	public static final String PROP_INPUTMETHODTARGET 	= "InputMethodTarget";
	public static final String PROP_SUGGESTIONSENABLED 	= "SuggestionsEnabled";
	public static final String PROP_TEXTSELECTABLE 		= "TextSelectable";
	public static final String PROP_LENGTH 				= "length";
	/** ARRAY_PROP_TEXTVIEW contains all property names should be returned for TextView object.*/
	public static final String[] ARRAY_PROP_TEXTVIEW = {
		PROP_DIDTOUCHFOCUSSELECT, PROP_HASSELECTION, PROP_HASOVERLAPPINGRENDERING,
		PROP_AUTOLINKMASK,/* PROP_TEXTVIEW_BASELINE,*/
		PROP_COMPOUNDDRAWABLEPADDING, PROP_COMPOUNDPADDINGBOTTOM, PROP_COMPOUNDPADDINGLEFT,
		PROP_COMPOUNDPADDINGRIGHT, PROP_COMPOUNDPADDINGTOP, PROP_CURRENTHINTTEXTCOLOR,
		PROP_CURRENTTEXTCOLOR, PROP_EXTENDEDPADDINGBOTTOM, PROP_EXTENDEDPADDINGTOP,
		PROP_FOCUSEDRECT, PROP_FREEZETEXT, PROP_HIGHLIGHTCOLOR, PROP_HINT,
		PROP_IMEACTIONID, PROP_IMEACTIONLABEL, PROP_IMEOPTIONS, 
		PROP_INCLUDEDFONTPADDING, PROP_INPUTTYPE, PROP_LINECOUNT,
		PROP_LINEHEIGHT, PROP_LINESPACINGEXTRA,PROP_LINESPACINGMULTIPLIER,
		PROP_LINKSCLICKABLE,PROP_MARQUEEREPEATLIMIT,PROP_MAXHEIGHT,
		PROP_MAXLINES,PROP_MAXWIDTH,PROP_MINHEIGHT,PROP_MINLINES,
		PROP_MINWIDTH,PROP_TEXTSCALEX,PROP_TEXTSIZE,PROP_TOTALPADDINGBOTTOM,
		PROP_TOTALPADDINGLEFT,PROP_TOTALPADDINGRIGHT,PROP_TOTALPADDINGTOP,		
		PROP_SELECTIONEND, PROP_SELECTIONSTART, 
		PROP_CURSORVISIBLE, PROP_INPUTMETHODTARGET, PROP_SUGGESTIONSENABLED,
		PROP_TEXTSELECTABLE,PROP_LENGTH,
		PROP_GRAVITY, PROP_TEXT,/*name shared with other object*/
	};
	
	//Button (extends from TextView), has AccessibilityNodeInfo
	//It doesn't have too much properties defined by itself, /* API Level 16 and below */
	
	//CompoundButton (extends from Button)
	public static final String PROP_ISCHECKED = "isChecked";
	
	//CheckBox (extends from CompoundButton)
	//It doesn't have too much properties defined by itself, /* API Level 16 and below */
	
	//RadioButton (extends from CompoundButton)
	//It doesn't have too much properties defined by itself, /* API Level 16 and below */
	
	//Switch (extends from CompoundButton)/*Since: API Level 14*/
	public static final String PROP_SWITCH_COMPOUNDPADDINGRIGHT = PROP_COMPOUNDPADDINGRIGHT;/*Since: API Level 14*/
	public static final String PROP_SWITCHMINWIDTH = "switchMinWidth";/*Since: API Level 16*/
	public static final String PROP_SWITCHPADDING = "switchPadding";/*Since: API Level 16*/	
	public static final String PROP_TEXTOFF = "textOff";/*Since: API Level 14*/
	public static final String PROP_TEXTON = "textOn";/*Since: API Level 14*/
	public static final String PROP_THUMBTEXTPADDING = "thumbTextPadding";/*Since: API Level 16*/
	public static final String[] ARRAY_PROP_SWITCH = {
		PROP_SWITCH_COMPOUNDPADDINGRIGHT,PROP_SWITCHMINWIDTH,
		PROP_SWITCHPADDING,PROP_TEXTOFF,PROP_TEXTON,PROP_THUMBTEXTPADDING
	};
	
	//ToggleButton (extends from CompoundButton)
	public static final String PROP_TOGGLEBUTTON_TEXTOFF = PROP_TEXTOFF;
	public static final String PROP_TOGGLEBUTTON_TEXTON = PROP_TEXTON;	
	public static final String[] ARRAY_PROP_TOGGLEBUTTON = {PROP_TOGGLEBUTTON_TEXTOFF,PROP_TOGGLEBUTTON_TEXTON};
	
	//View
	public static final String PROP_ALPHA = "alpha";
	public static final String PROP_BASELINE = "baseline";
	public static final String PROP_BOTTOM = "bottom";
	public static final String PROP_CANSCROLLLEFT = "canScrollLeft";
	public static final String PROP_CANSCROLLRIGHT = "canScrollRight";
	public static final String PROP_CANSCROLLUP = "canScrollUp";
	public static final String PROP_CANSCROLLDOWN = "canScrollDown";	
	public static final String PROP_DRAWINGCACHEBACKGROUNDCOLOR = "drawingCacheBackgroundColor";
	public static final String PROP_DRAWINGCACHEQUALITY = "drawingCacheQuality";
	public static final String PROP_DRAWINGRECT = "drawingRect";
	public static final String PROP_DRAWINGTIME = "drawingTime";
	public static final String PROP_FILTERTOUCHESWHENOBSCURED = "filterTouchesWhenObscured";
	public static final String PROP_GLOBALVISIBLERECT = "globalVisibleRect";
	public static final String PROP_HITRECT = "hitRect";
	public static final String PROP_HORIZONTALFADINGEDGELENGTH = "horizontalFadingEdgeLength";
	public static final String PROP_ID = "id";
	public static final String PROP_KEEPSCREENON = "keepScreenOn";
	public static final String PROP_LAYERTYPE = "layerType";
	public static final String PROP_LEFT = "left";
	public static final String PROP_LOCALVISIBLERECT = "localVisibleRect";
	public static final String PROP_LOCATIONINWINDOW = "locationInWindow";
	public static final String PROP_LOCATIONONSCREEN = "locationOnScreen";
	public static final String PROP_NEXTFOCUSDOWNID = "nextFocusDownId";
	public static final String PROP_NEXTFOCUSFORWARDID = "nextFocusForwardId";
	public static final String PROP_NEXTFOCUSLEFTID = "nextFocusLeftId";
	public static final String PROP_NEXTFOCUSRIGHTID = "nextFocusRightId";
	public static final String PROP_NEXTFOCUSUPID = "nextFocusUpId";
	public static final String PROP_OVERSCROLLMODE = "overScrollMode";
	public static final String PROP_PADDINGBOTTOM = "paddingBottom";
	public static final String PROP_PADDINGLEFT = "paddingLeft";
	public static final String PROP_PADDINGRIGHT = "paddingRight";
	public static final String PROP_PADDINGTOP = "paddingTop";
	public static final String PROP_PIVOTX = "pivotX";
	public static final String PROP_PIVOTY = "pivotY";
	public static final String PROP_RIGHT = "right";
	public static final String PROP_ROTATIONX = "rotationX";
	public static final String PROP_ROTATIONY = "rotationY";
	public static final String PROP_SCALEX = "scaleX";
	public static final String PROP_SCALEY = "scaleY";
	public static final String PROP_SCROLLBARSTYLE = "scrollBarStyle";
	public static final String PROP_SCROLLX = "scrollX";
	public static final String PROP_SCROLLY = "scrollY";
	public static final String PROP_SOLIDCOLOR = "solidColor";
	public static final String PROP_SYSTEMUIVISIBILITY = "systemUiVisibility";
	public static final String PROP_TAG = "tag";
	public static final String PROP_TOP = "top";
	public static final String PROP_TRANSLATIONX = "translationX";
	public static final String PROP_TRANSLATIONY = "translationY";
	public static final String PROP_VERTICALFADINGEDGELENGTH = "verticalFadingEdgeLength";
	public static final String PROP_VERTICALSCROLLBARPOSITION = "verticalScrollbarPosition";
	public static final String PROP_VERTICALSCROLLBARWIDTH = "verticalScrollbarWidth";
	public static final String PROP_VISIBILITY = "visibility";
	public static final String PROP_WINDOWVISIBILITY = "windowVisibility";
	public static final String PROP_WINDOWVISIBLEDISPLAYFRAME = "windowVisibleDisplayFrame";
	public static final String PROP_X = "x";
	public static final String PROP_Y = "y";
	public static final String PROP_HASFOCUS = "hasFocus";
	public static final String PROP_HASFOCUSABLE = "hasFocusable";
	public static final String PROP_ISACTIVATED = "isActivated";
	public static final String PROP_ISCLICKABLE = "isClickable";
	public static final String PROP_ISDIRTY = "isDirty";
	public static final String PROP_ISDRAWINGCACHEENABLED = "isDrawingCacheEnabled";
	public static final String PROP_ISDUPLICATEPARENTSTATEENABLED = "isDuplicateParentStateEnabled";
	public static final String PROP_ISENABLED = "isEnabled";
	public static final String PROP_ISFOCUSABLE = "isFocusable";
	public static final String PROP_ISFOCUSABLEINTOUCHMODE = "isFocusableInTouchMode";
	public static final String PROP_ISFOCUSED = "isFocused";
	public static final String PROP_ISHAPTICFEEDBACKENABLED = "isHapticFeedbackEnabled";
	public static final String PROP_ISHARDWAREACCELERATED = "isHardwareAccelerated";
	public static final String PROP_ISHORIZONTALFADINGEDGEENABLED = "isHorizontalFadingEdgeEnabled";
	public static final String PROP_ISHORIZONTALSCROLLBARENABLED = "isHorizontalScrollBarEnabled";
	public static final String PROP_ISHOVERED = "isHovered";
	public static final String PROP_ISINEDITMODE = "isInEditMode";
	public static final String PROP_ISINTOUCHMODE = "isInTouchMode";
	public static final String PROP_ISLAYOUTREQUESTED = "isLayoutRequested";
	public static final String PROP_ISLONGCLICKABLE = "isLongClickable";
	public static final String PROP_ISOPAQUE = "isOpaque";
	public static final String PROP_ISPRESSED = "isPressed";
	public static final String PROP_ISSAVEENABLED = "isSaveEnabled";
	public static final String PROP_ISSAVEFROMPARENTENABLED = "isSaveFromParentEnabled";
	public static final String PROP_ISSCROLLBARFADINGENABLED = "isScrollbarFadingEnabled";	
	public static final String PROP_ISSELECTED = "isSelected";
	public static final String PROP_ISSHOWN = "isShown";
	public static final String PROP_ISSOUNDEFFECTSENABLED = "isSoundEffectsEnabled";
	public static final String PROP_ISVERTICALFADINGEDGEENABLED = "isVerticalFadingEdgeEnabled";
	public static final String PROP_ISVERTICALSCROLLBARENABLED = "isVerticalScrollBarEnabled";	
	public static final String PROP_WILLNOTCACHEDRAWING = "willNotCacheDrawing";
	public static final String PROP_WILLNOTDRAW = "willNotDraw";
	/** 
	 * Don't put PROP_HASONCLICKLISTENERS in ARRAY_PROP_VIEW, whether return 
	 * PROP_HASONCLICKLISTENERS as View's property name will depend on whether
	 * View object has method "hasOnClickListeners()" or not.
	 */
	public static final String PROP_HASONCLICKLISTENERS = "hasOnClickListeners";
	/** ARRAY_PROP_VIEW contains all property names should be returned for View object. */
	public static final String[] ARRAY_PROP_VIEW = {
		PROP_ALPHA,PROP_BASELINE,PROP_BOTTOM,PROP_DRAWINGCACHEBACKGROUNDCOLOR
		,PROP_DRAWINGCACHEQUALITY,PROP_DRAWINGRECT,PROP_DRAWINGTIME,PROP_FILTERTOUCHESWHENOBSCURED
		,PROP_GLOBALVISIBLERECT,PROP_HITRECT,PROP_HORIZONTALFADINGEDGELENGTH,PROP_ID
		,PROP_KEEPSCREENON,PROP_LAYERTYPE,PROP_LEFT,PROP_LOCALVISIBLERECT,PROP_LOCATIONINWINDOW
		,PROP_LOCATIONONSCREEN,PROP_NEXTFOCUSDOWNID,PROP_NEXTFOCUSFORWARDID,PROP_NEXTFOCUSLEFTID
		,PROP_NEXTFOCUSRIGHTID,PROP_NEXTFOCUSUPID,PROP_OVERSCROLLMODE,PROP_PADDINGBOTTOM
		,PROP_PADDINGLEFT,PROP_PADDINGRIGHT,PROP_PADDINGTOP,PROP_PIVOTX,PROP_PIVOTY,PROP_RIGHT
		,PROP_ROTATION,PROP_ROTATIONX,PROP_ROTATIONY,PROP_SCALEX,PROP_SCALEY,PROP_SCROLLBARSTYLE
		,PROP_SCROLLX,PROP_SCROLLY,PROP_SOLIDCOLOR,PROP_SYSTEMUIVISIBILITY,PROP_TAG,PROP_TOP
		,PROP_TRANSLATIONX,PROP_TRANSLATIONY,PROP_VERTICALFADINGEDGELENGTH,PROP_VERTICALSCROLLBARPOSITION
		,PROP_VERTICALSCROLLBARWIDTH,PROP_VISIBILITY,PROP_WIDTH,PROP_WINDOWVISIBILITY
		,PROP_WINDOWVISIBLEDISPLAYFRAME,PROP_X,PROP_Y,PROP_HASFOCUS,PROP_HASFOCUSABLE
		,PROP_ISACTIVATED,PROP_ISCLICKABLE,PROP_ISDIRTY,PROP_ISDRAWINGCACHEENABLED
		,PROP_ISDUPLICATEPARENTSTATEENABLED,PROP_ISENABLED,PROP_ISFOCUSABLE,PROP_ISFOCUSABLEINTOUCHMODE
		,PROP_ISFOCUSED,PROP_ISHAPTICFEEDBACKENABLED,PROP_ISHARDWAREACCELERATED
		,PROP_ISHORIZONTALFADINGEDGEENABLED,PROP_ISHORIZONTALSCROLLBARENABLED,PROP_ISHOVERED
		,PROP_ISINEDITMODE,PROP_ISINTOUCHMODE,PROP_ISLAYOUTREQUESTED,PROP_ISLONGCLICKABLE
		,PROP_ISOPAQUE,PROP_ISPRESSED,PROP_ISSAVEENABLED,PROP_ISSAVEFROMPARENTENABLED
		,PROP_ISSCROLLBARFADINGENABLED,PROP_ISSELECTED,PROP_ISSHOWN
		,PROP_ISSOUNDEFFECTSENABLED,PROP_ISVERTICALFADINGEDGEENABLED,PROP_ISVERTICALSCROLLBARENABLED	
		,PROP_WILLNOTCACHEDRAWING,PROP_WILLNOTDRAW
		,PROP_CANSCROLLLEFT,PROP_CANSCROLLRIGHT,PROP_CANSCROLLUP,PROP_CANSCROLLDOWN
		,PROP_CONTENTDESCRIPTION,PROP_HEIGHT, PROP_LAYOUTPARAMS, PROP_HASWINDOWFOCUS /*names shared with other object*/
	};
	
	// Adapter
	public static final String PROP_ITEMCOUNT = "itemCount";
	public static final String PROP_ITEMIDS = "itemIds";
	public static final String PROP_ITEMS = "items";
	public static final String PROP_VIEWTYPECOUNT = "viewTypeCount";
	public static final String PROP_HASSTABLEIDS = "hasStableIds";
	public static final String PROP_ISEMPTY = "isEmpty";
	/** ARRAY_PROP_ADAPTER contains all property names should be returned for Adapter object.*/
	public static final String[] ARRAY_PROP_ADAPTER = {
		PROP_ITEMCOUNT,PROP_ITEMIDS,PROP_ITEMS,PROP_VIEWTYPECOUNT
		,PROP_HASSTABLEIDS,PROP_ISEMPTY		
	};	

	// ListAdapter
	public static final String PROP_AREALLITEMSENABLED = "areAllItemsEnabled";
	
	//SpinnerAdapter
	
	/** 
	 * Add Adapter subclass property names to the list of available object property names.
	 * We do this to expose the Adapter properties as if they were properties of the actual View.
	 * <p>
	 * Some property names exposed for certain Adapters:
	 * <ul>
	 * <li>{@value #PROP_AREALLITEMSENABLED}
	 * <li>{@value #PROP_HASSTABLEIDS}
	 * <li>{@value #PROP_ISEMPTY}
	 * <li>{@value #PROP_ITEMCOUNT}
	 * <li>{@value #PROP_ITEMIDS}
	 * <li>{@value #PROP_ITEMS}
	 * <li>{@value #PROP_VIEWTYPECOUNT}
	 * </ul>
	 */
	public static Vector<String> addAdapterPropertyNames(Vector<String> names, Adapter adapter){		
		for(int i=0;i<ARRAY_PROP_ADAPTER.length;i++){
			names.add(ARRAY_PROP_ADAPTER[i]);
		}

		if(adapter instanceof ListAdapter)
		{
			names.add(PROP_AREALLITEMSENABLED);
		}		
		return names;
	}

	/**
	 * Returning a String of ArrayList items using String.valueOf(Object) following these guidelines:
	 * <p><ul>
	 *    <li>no items in list or array or a null array will return an empty string.
	 *    <li>one or more items in list will return a string in the following format:
	 *    <ul>
	 *        <li>First character is the delimiter used to delimit items.  
	 *        This can potentially be different for different types of data.  
	 *        <li>followed by each item separated by that character delimiter.<br>
	 *        example: ",item 1,item 2,item 3,item 4"<br>
	 *        <li>a null (missing) item will be indicated by no characters between delimiters, 
	 *        or no characters following the last delimiter<br>
	 *        example: ",item 1,,item 3,item 4,," (items #2, #5, and #6 are missing)
	 *    </ul>
	 * </ul>
	 * @param list
	 * @return
	 */
	public static String convertArrayToDelimitedString(ArrayList list){
		if (list == null || list.isEmpty()) return "";
		char r ='\r';
		StringBuffer sb = new StringBuffer();
		for(Object item:list){
			sb.append(r);
			if(item == null) continue;
			sb.append(String.valueOf(item));
		}
		return sb.toString();
	}
	
	/** 
	 * Add Adapter subclass property names to the list of available object property names.
	 * We do this to expose the Adapter properties as if they were properties of the actual View.
	 * <p>
	 * @param adapter Adapter retrieved from the View using the Adapter
	 * @param propname String property name to extract from the Adapter.
	 * @return A String.  Arrays of values will generally be returned from {@link #convertArrayToDelimitedString(ArrayList)} 
	 * @see #addAdapterPropertyNames(Vector, Adapter)
	 */
	public static String getAdapterProperty(Adapter adapter, String propname){	
		if(adapter == null || propname == null) return null;
		if (PROP_ITEMCOUNT.equalsIgnoreCase(propname))	    		
			return String.valueOf(adapter.getCount());    		
		if (PROP_VIEWTYPECOUNT.equalsIgnoreCase(propname))
			return String.valueOf(adapter.getViewTypeCount());
		if (PROP_HASSTABLEIDS.equalsIgnoreCase(propname))
			return String.valueOf(adapter.hasStableIds());
		if (PROP_ISEMPTY.equalsIgnoreCase(propname))
			return String.valueOf(adapter.isEmpty());
		if (PROP_ITEMIDS.equalsIgnoreCase(propname)){
			long id = -1;
			int count = adapter.getCount();
			ArrayList ids = new ArrayList();
			for(int i=0;i < count;i++){
				try{ 
					id = adapter.getItemId(i);
					ids.add(String.valueOf(id));
				}catch(Exception x){
					ids.add(null);
				}
			}
			return convertArrayToDelimitedString(ids);    		
		}
		if (PROP_ITEMS.equalsIgnoreCase(propname)){
			int count = adapter.getCount();
			ArrayList items = new ArrayList();
			for(int i=0;i < count;i++){
				try{ 
					Object obj = adapter.getItem(i);
					items.add(getObjectText(obj));
				}catch(Exception x){
					items.add(null);
				}
			}
			return convertArrayToDelimitedString(items);    		
		}
		if(adapter instanceof ListAdapter){
			ListAdapter la = (ListAdapter) adapter;
			if(PROP_AREALLITEMSENABLED.equalsIgnoreCase(propname)){
				return String.valueOf(la.areAllItemsEnabled());
			}
		}
		return null;
	}
	
	/**
	 * @param obj
	 * @return String[] of 0 or more property names for the object.
	 */
	public static String[] getObjectPropertyNames(Object obj){	

		Vector<String> names = new Vector<String>(100);

		names.add(PROP_CLASSNAME);
		
		if(obj instanceof View)
		{
			try{
	        	AccessibilityNodeInfo info = null;
	    		info = ((View)obj).createAccessibilityNodeInfo();/* API Level 14 and above */
	    		if(info != null){
		    		try{info.recycle();}catch(Exception x){}
					for(int i=0;i<ARRAY_PROP_ACCESSIBLITY.length;i++) names.add(ARRAY_PROP_ACCESSIBLITY[i]);
	    		}
			} catch (Throwable e) {
				debug("Ignoring "+ e.getClass().getSimpleName()+": "+ e.getMessage());
			}
			
			for(int i=0;i<ARRAY_PROP_VIEW.length;i++) names.add(ARRAY_PROP_VIEW[i]);

			try{ 
				Method method =((View)obj).getClass().getMethod(PROP_HASONCLICKLISTENERS, new Class[0]);
				if(method != null)
					names.add(PROP_HASONCLICKLISTENERS);			
			}catch(Exception x){}			
		}

		try{
			if(obj instanceof ViewGroup)
			{	
				for(int i=0;i<ARRAY_PROP_VIEWGROUP.length;i++) names.add(ARRAY_PROP_VIEWGROUP[i]);
			}
		} catch (Throwable e) {
			debug("Ignoring "+ e.getClass().getSimpleName()+": "+ e.getMessage());
		}
		
		try{
			if(obj instanceof AdapterView)
			{
				for(int i=0;i<ARRAY_PROP_ADAPTERVIEW.length;i++) names.add(ARRAY_PROP_ADAPTERVIEW[i]);
			}
		} catch (Throwable e) {
			debug("Ignoring "+ e.getClass().getSimpleName()+": "+ e.getMessage());
		}
		
		try{
			if(obj instanceof AbsListView)
			{
				for(int i=0;i<ARRAY_PROP_ABSLISTVIEW.length;i++) names.add(ARRAY_PROP_ABSLISTVIEW[i]);
			}
		} catch (Throwable e) {
			debug("Ignoring "+ e.getClass().getSimpleName()+": "+ e.getMessage());
		}

		try{
			if(obj instanceof ListView)
			{	
				for(int i=0;i<ARRAY_PROP_LISTVIEW.length;i++) names.add(ARRAY_PROP_LISTVIEW[i]);
				
				ListAdapter adapter = ((ListView)obj).getAdapter();
				names = addAdapterPropertyNames(names, adapter);
			}
		} catch (Throwable e) {
			debug("Ignoring "+ e.getClass().getSimpleName()+": "+ e.getMessage());
		}
		
		try{
			if(obj instanceof AbsSpinner)
			{	
				SpinnerAdapter adapter = ((AbsSpinner)obj).getAdapter();
				names = addAdapterPropertyNames(names, adapter);				
			}
		} catch (Throwable e) {
			debug("Ignoring "+ e.getClass().getSimpleName()+": "+ e.getMessage());
		}
		
		try{
			if(obj instanceof Spinner)
			{	
				for(int i=0;i<ARRAY_PROP_SPINNER.length;i++) names.add(ARRAY_PROP_SPINNER[i]);
			}
		} catch (Throwable e) {
			debug("Ignoring "+ e.getClass().getSimpleName()+": "+ e.getMessage());
		}
		
		try{
			if(obj instanceof TextView){
				for(int i=0;i<ARRAY_PROP_TEXTVIEW.length;i++) names.add(ARRAY_PROP_TEXTVIEW[i]);
			}
		} catch (Throwable e) {
			debug("Ignoring "+ e.getClass().getSimpleName()+": "+ e.getMessage());
		}
		
		try{
			if(obj instanceof Switch){
				for(int i=0;i<ARRAY_PROP_SWITCH.length;i++) names.add(ARRAY_PROP_SWITCH[i]);
			}
		} catch (Throwable e) {
			debug("Ignoring "+ e.getClass().getSimpleName()+": "+ e.getMessage());
		}
		
		try{
			if(obj instanceof CompoundButton){
				names.add(PROP_ISCHECKED);
			}
		} catch (Throwable e) {
			debug("Ignoring "+ e.getClass().getSimpleName()+": "+ e.getMessage());
		}
		
		try{
			if(obj instanceof ToggleButton){
				for(int i=0;i<ARRAY_PROP_TOGGLEBUTTON.length;i++) names.add(ARRAY_PROP_TOGGLEBUTTON[i]);
			}
		} catch (Throwable e) {
			debug("Ignoring "+ e.getClass().getSimpleName()+": "+ e.getMessage());
		}
		
		try{
			if(obj instanceof Window)
			{
				for(int i=0;i<ARRAY_PROP_WINDOW.length;i++) names.add(ARRAY_PROP_WINDOW[i]);
			}
		} catch (Throwable e) {
			debug("Ignoring "+ e.getClass().getSimpleName()+": "+ e.getMessage());
		}
		
		try{
			if(obj instanceof Activity)
			{
				for(int i=0;i<ARRAY_PROP_ACTIVITY.length;i++) names.add(ARRAY_PROP_ACTIVITY[i]);
			}
		} catch (Throwable e) {
			debug("Ignoring "+ e.getClass().getSimpleName()+": "+ e.getMessage());
		}
		
		try{
			if(obj instanceof ActionBar) /* Android 3.0 and later only */		
			{
				for(int i=0;i<ARRAY_PROP_ACTIONBAR.length;i++) names.add(ARRAY_PROP_ACTIONBAR[i]);
			}
		} catch (Throwable e) {
			debug("Ignoring "+ e.getClass().getSimpleName()+": "+ e.getMessage());
		}

		try{
			if(obj instanceof ActionBar.Tab) 		
			{
				for(int i=0;i<ARRAY_PROP_ACTIONBAR_TAB.length;i++) names.add(ARRAY_PROP_ACTIONBAR_TAB[i]);
			}
		} catch (Throwable e) {
			debug("Ignoring "+ e.getClass().getSimpleName()+": "+ e.getMessage());
		}
		
		try{
			if(obj instanceof Display)
			{	
				for(int i=0;i<ARRAY_PROP_DISPLAY.length;i++) names.add(ARRAY_PROP_DISPLAY[i]);
			}
		} catch (Throwable e) {
			debug("Ignoring "+ e.getClass().getSimpleName()+": "+ e.getMessage());
		}

		try{
			if(obj instanceof ProgressDialog)
			{	
				for(int i=0;i<ARRAY_PROP_PROGRESSDIALOG.length;i++) names.add(ARRAY_PROP_PROGRESSDIALOG[i]);
			}
		} catch (Throwable e) {
			debug("Ignoring "+ e.getClass().getSimpleName()+": "+ e.getMessage());
		}
		
		return (String[])names.toArray(new String[0]);
	}
	
	/**
	 * Purpose: Get the name of the possible property accessory methods, for example isXXX, getXXX, hasXXX<br>
	 * 	        and return them within a List.<br>
	 * 
	 * @param	propertyName, String, the property name
	 * @return  List, a list of possible property accessory method names.
	 * Minimally, the propertyName itself is returned as a possible accessorMethod 
	 */
	public static List<String> getPossiblePropertyAccessorMethodsName(String propertyName){
		List<String> possibleAccessorMethods = new ArrayList<String>();
		StringBuffer sb = null;		
		if(propertyName!=null){
			sb = new StringBuffer(propertyName.trim());
			if(sb.length()>0){
				possibleAccessorMethods.add(sb.toString());
				//Make the first character upper case
				sb.setCharAt(0, Character.toUpperCase(sb.charAt(0)));
				for(int i=0;i<PREFIX_PROP_ARRAY.length;i++){
					possibleAccessorMethods.add(PREFIX_PROP_ARRAY[i]+sb.toString());
				}
			}
		}		
		return possibleAccessorMethods;
	}
	
	/**
	 * Purpose: Try to get the value of a property for an object.<br>
	 * 
	 * @param obj, 					Object, the object from which to get the property value
	 * @param property, 			String, the property name to get value
	 * @param includingSuperClass, 	boolean, if we need to get value from super class.
	 * 
	 * @return String, the value of the property. Or SAFSMessage.NULL_VALUE if not found.
	 */
	public static String getPropertyByReflection(Object obj, String property, boolean includingSuperClass){
		
		if(obj==null || property==null) return null;		
		Class clazz = obj.getClass();
		Field field = null;
		Method method = null;
		
		//First, try to get value from public field
		try {
			if(includingSuperClass)
				field = clazz.getField(property);
			else
				field = clazz.getDeclaredField(property);			
			return String.valueOf(field.get(obj));
		} catch (Exception e) {
			debug("Can't get value for property '"+property+"' by Field, met "+ e.getClass().getSimpleName()+": "+e.getMessage());
		}
		
		//If not found, try specific no-arg accessory methods
		List<String> possibleAccessorMethods = getPossiblePropertyAccessorMethodsName(property);
		String methodName = null;
		for(int i=0;method==null && i<possibleAccessorMethods.size();i++){
			methodName = possibleAccessorMethods.get(i);
			try{
				if(includingSuperClass)
					method = clazz.getMethod(methodName, new Class[0]);
				else
					method = clazz.getDeclaredMethod(methodName, new Class[0]);
			} catch (Exception e) {
				debug("Can't get value for property '"+property+"' by Method, met "+ e.getClass().getSimpleName()+": "+e.getMessage());
			}
		}
		if(method != null){
			try { return String.valueOf(method.invoke(obj, new Object[0]));} 
			catch (Exception e) {
				debug("Can't invoke Method for property '"+property+"', met "+ e.getClass().getSimpleName()+": "+e.getMessage());
			}
		}		
		return SAFSMessage.NULL_VALUE;
	}

}
