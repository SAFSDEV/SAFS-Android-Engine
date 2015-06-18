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
