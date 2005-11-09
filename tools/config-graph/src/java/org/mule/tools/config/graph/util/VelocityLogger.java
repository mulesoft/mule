package org.mule.tools.config.graph.util;

import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.log.LogSystem;

public class VelocityLogger implements LogSystem {
		public void init(RuntimeServices arg0) throws Exception {
			// TODO Auto-generated method stub

		}

		public void logVelocityMessage(int arg0, String arg1) {
			// TODO Auto-generated method stub
			System.out.println(arg1);
		}
	
}
