package org.jbpm.actions;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.exe.ExecutionContext;

public abstract class LoggingActionHandler implements ActionHandler {
    protected transient Log logger = LogFactory.getLog(getClass());

    public void execute(ExecutionContext executionContext) throws Exception {
        if (logger.isDebugEnabled()) {
            String currentNode = "???";
            if (executionContext.getNode() != null) {
                currentNode = executionContext.getNode().getFullyQualifiedName();
            }
            logger.debug("Executing action " + this.getClass().getName() + " from state \"" + currentNode + "\"");
        }
    }
}
