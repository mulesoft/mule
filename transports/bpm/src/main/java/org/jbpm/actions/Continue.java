package org.jbpm.actions;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.exe.ExecutionContext;

/**
 * Simply continues the process execution (moves on to the next state).
 */
public class Continue implements ActionHandler  {

    public void execute(ExecutionContext executionContext) throws Exception {
        executionContext.leaveNode();
    }

    private static Log log = LogFactory.getLog(Continue.class);
}
