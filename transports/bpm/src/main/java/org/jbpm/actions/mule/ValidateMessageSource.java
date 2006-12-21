package org.jbpm.actions.mule;

import org.jbpm.JbpmException;
import org.jbpm.actions.LoggingActionHandler;
import org.jbpm.graph.exe.ExecutionContext;
import org.mule.providers.bpm.ProcessConnector;

/**
 * Throws an exception if the message's source is not as expected.
 * @param expectedSource can be the expected endpoint's "name" or "address"
 * @throws JbpmException
 *
 * <action class="org.jbpm.actions.mule.ValidateSource">
 *      <expectedSource>ERPGateway</expectedSource>
 * </action>
 *
 * <action class="org.jbpm.actions.mule.ValidateSource">
 *      <expectedSource>http://localhost:8080/incoming</expectedSource>
 * </action>
 */
public class ValidateMessageSource extends LoggingActionHandler {

    protected String expectedSource;

    public void execute(ExecutionContext executionContext) throws Exception {
        super.execute(executionContext);
        String messageSource =
            (String) executionContext.getVariable(ProcessConnector.PROCESS_VARIABLE_INCOMING_SOURCE);
        logger.debug("Validating message source = " + messageSource + ", expected = " + expectedSource);
        if (expectedSource.equalsIgnoreCase(messageSource) == false) {
            throw new JbpmException("Incoming message source is " + messageSource + ", expected source is " + expectedSource);
        }
    }
}
