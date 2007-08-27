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

import org.jbpm.graph.exe.ExecutionContext;

/**
 * Sends a Mule message to the specified endpoint and continues execution to the next
 * state. This class assumes the current state has only one leaving transition.
 * 
 * @param url - the Mule endpoint
 * @param transformers - any transformers to be applied
 * @param payload - specify the payload as a string directly in the jPDL
 * @param payloadSource - process variable from which to generate the message
 *            payload, defaults to {@link ProcessConnector.PROCESS_VARIABLE_DATA}
 * @param messageProperties - any properties to be applied to the message
 */
public class SendMuleEventAndContinue extends SendMuleEvent
{

    private static final long serialVersionUID = 1L;

    public void execute(ExecutionContext executionContext) throws Exception
    {
        super.execute(executionContext);
        executionContext.leaveNode();
    }

}
