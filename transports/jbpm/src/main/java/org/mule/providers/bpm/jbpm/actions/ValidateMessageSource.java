/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.bpm.jbpm.actions;

import org.mule.providers.bpm.ProcessConnector;

import org.jbpm.JbpmException;
import org.jbpm.graph.exe.ExecutionContext;

/**
 * Throws an exception if the message's source is not as expected.
 * 
 *  <action class="org.mule.providers.bpm.jbpm.actions.ValidateSource">
 *     <expectedSource>ERPGateway</expectedSource> 
 *  </action> 
 *  <action class="org.mule.providers.bpm.jbpm.actions.ValidateSource">
 *     <expectedSource>http://localhost:8080/incoming</expectedSource>
 *  </action>
 *             
 * @param expectedSource can be the expected endpoint's "name" or "address"
 * @throws JbpmException 
 */
public class ValidateMessageSource extends LoggingActionHandler
{

    private static final long serialVersionUID = 1L;

    protected String expectedSource;

    public void execute(ExecutionContext executionContext) throws Exception
    {
        super.execute(executionContext);
        String messageSource = (String)executionContext
            .getVariable(ProcessConnector.PROCESS_VARIABLE_INCOMING_SOURCE);
        logger.debug("Validating message source = " + messageSource + ", expected = " + expectedSource);
        if (expectedSource.equalsIgnoreCase(messageSource) == false)
        {
            throw new JbpmException("Incoming message source is " + messageSource + ", expected source is "
                            + expectedSource);
        }
    }

}
