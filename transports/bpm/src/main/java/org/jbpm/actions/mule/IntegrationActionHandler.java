package org.jbpm.actions.mule;

import org.jbpm.actions.LoggingActionHandler;
import org.jbpm.graph.exe.ExecutionContext;
import org.mule.providers.bpm.ProcessConnector;

public abstract class IntegrationActionHandler extends LoggingActionHandler {

    private Object incoming;

    public void execute(ExecutionContext executionContext) throws Exception {
        super.execute(executionContext);
        incoming = executionContext.getVariable(ProcessConnector.PROCESS_VARIABLE_INCOMING);
    }

    protected Object getIncomingMessage() {
        return incoming;
    }
}
