/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.bpm.jbpm.actions;

import org.mule.transport.bpm.ProcessConnector;

import org.jbpm.JbpmException;
import org.jbpm.graph.exe.ExecutionContext;

/**
 * Throws an exception if the message's source is not as expected.
 * 
 *  &lt;action class="org.mule.transport.bpm.jbpm.actions.ValidateSource"&gt;
 *     &lt;expectedSource&gt;ERPGateway&lt;/expectedSource&gt; 
 *  &lt;/action&gt; 
 *  &lt;action class="org.mule.transport.bpm.jbpm.actions.ValidateSource"&gt;
 *     &lt;expectedSource&gt;http://localhost:8080/incoming&lt;/expectedSource&gt;
 *  &lt;/action&gt;
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
        if (!expectedSource.equalsIgnoreCase(messageSource))
        {
            throw new JbpmException("Incoming message source is " + messageSource + ", expected source is "
                            + expectedSource);
        }
    }

}
