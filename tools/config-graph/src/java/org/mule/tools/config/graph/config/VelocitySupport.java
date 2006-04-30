package org.mule.tools.config.graph.config;

import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.log.LogSystem;
import org.mule.tools.config.graph.util.VelocityLogger;

public abstract class VelocitySupport {

    protected VelocityEngine ve;

    protected GraphEnvironment env = null;
    
    protected static LogSystem logSystem;


    protected VelocitySupport(GraphEnvironment env) throws Exception {
        this.env = env;
        logSystem = new VelocityLogger(env);
        ve = new VelocityEngine();
        ve.setProperty(VelocityEngine.RUNTIME_LOG_LOGSYSTEM, logSystem);
        ve.init();
    }
}
