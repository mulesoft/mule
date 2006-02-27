package org.mule.tools.config.graph.util;

import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.log.LogSystem;
import org.mule.tools.config.graph.config.GraphEnvironment;

public class VelocityLogger implements LogSystem {

    private GraphEnvironment environment = null;

    public VelocityLogger(GraphEnvironment environment) {
        this.environment = environment;
    }

		public void init(RuntimeServices arg0) throws Exception {

		}

		public void logVelocityMessage(int arg0, String arg1) {
			if(environment!=null) environment.log(arg1);
		}

    public GraphEnvironment getEnvironment() {
        return environment;
    }

    public void setEnvironment(GraphEnvironment environment) {
        this.environment = environment;
    }

}
