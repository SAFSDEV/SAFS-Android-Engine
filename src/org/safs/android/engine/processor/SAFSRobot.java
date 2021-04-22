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

import org.safs.android.engine.DSAFSTestRunner;
import org.safs.robot.Robot;

public class SAFSRobot extends Robot{
	
	private DSAFSTestRunner runner = null;
	
	public SAFSRobot(DSAFSTestRunner runner){
		super(runner);
		this.runner = runner;
	}

	public void debug(String message){
		if(runner!=null){
			runner.debug(message);
		}else{
			super.debug(message);
		}
	}
}
