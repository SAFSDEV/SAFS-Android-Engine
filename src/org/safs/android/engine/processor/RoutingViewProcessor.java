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

import java.util.Properties;
import java.util.Vector;

import org.safs.android.engine.DGuiClassData;
import org.safs.android.engine.DSAFSTestRunner;
import org.safs.android.remotecontrol.SAFSMessage;
import org.safs.text.FAILKEYS;

import com.jayway.android.robotium.remotecontrol.client.processor.ProcessorInterface;


/**
 * This processor is a special processor used to route commands according to the type of object encountered.
 * While most commands are unique to a particular component type, there are some commands that are the same 
 * for different component types.  For example, "SelectIndex" might be a command for a ListView, or a Spinner.  
 * This routing processor is used to route commands according to the component type found by the superclasses 
 * prior to the invocation of processComponentFunction.
 * 
 * @author Carl Nagle, SAS Institute, Inc
 */
public class RoutingViewProcessor extends  TestStepProcessor{

	public RoutingViewProcessor(DSAFSTestRunner testrunner) {
		super(testrunner);
	}

	/**
	 * Given the set of properties for the command, and the found Window and Comp objects, 
	 * the processor attempts to route the command to the correct set of processors for the 
	 * particular component instance found by the search algorithm. 
	 * <p>
	 * The processors are extracted from the TestRunner provided during initialization of 
	 * this class instance.
	 */
	public void processComponentFunction(Properties props) {
		String dbPrefix = debugPrefix+".processProperties(): ";
		debug(dbPrefix +"processing...");
		if(!checkSolo(props)){
			debug(dbPrefix+"CANNOT get the Solo object.");
			return;
		}		
		try{
			String target = null;
			Vector<ProcessorInterface> processors = null;
			ProcessorInterface processor = null;
			String command = props.getProperty(SAFSMessage.KEY_COMMAND); // should never be null by this point

			if(compobj == null){
			   if( SAFSMessage.cf_view_guidoesexist.equalsIgnoreCase(remoteCommand)||
			       SAFSMessage.cf_view_guidoesnotexist.equalsIgnoreCase(remoteCommand)){
				   target = SAFSMessage.target_safs_view;
			   }
			}else{
				// must route according to deepest class hierarchy before lowest			
				target = dgcd.getMappedClassType(compobj.getClass().getName(), compobj);
				if(target==null){
					debug(dbPrefix +"cannot currently route for class "+ compobj.getClass().getName());
					return;
				}
				target = target.trim();
				debug(dbPrefix +"processing mapped class as type(s) "+ target);
				target = DGuiClassData.getGenericObjectType(target);
				if(target==null){
					debug(dbPrefix +"cannot determine library handler for class type "+ target);
					return;
				}
				target = target.trim();
			}
			debug(dbPrefix+"routing command to "+target);
			props.setProperty(SAFSMessage.KEY_TARGET, target);			
			processors = testrunner.getProcessors(target);
			if(processors == null || processors.isEmpty()){
				debug(dbPrefix +"no available processors found for target "+ target);
				return;
			}
			boolean processed = false;
			// cycle through chained target processors only until one of them handles the command
			for(int i=0; i<processors.size()&& !processed;i++){
				processor = processors.get(i);
				processor.setRemoteCommand(command);
				//processor.processProperties(props); // already done in this RoutingViewProcessor
				try{ 
					((SAFSProcessor)processor).setParams(getParams());
					((SAFSProcessor)processor).setSolo(solo);
					((TestStepProcessor)processor).processComponentFunction(props);}
				catch(ClassCastException cc){
					debug(dbPrefix+"ignoring ClassCastException for "+processor.getClass().getName());
				}
				try{processed = ! SAFSMessage.STATUS_REMOTE_NOT_EXECUTED_STRING.equals(props.getProperty(SAFSMessage.KEY_REMOTERESULTCODE));}
				catch(NullPointerException x){}
			}						
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
}
