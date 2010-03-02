/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.jbpm;

import org.mule.transport.bpm.ProcessConnector;
import org.mule.util.ClassUtils;

import java.util.Map;

import org.jbpm.api.JbpmException;
import org.jbpm.internal.log.Log;
import org.jbpm.jpdl.internal.activity.StateActivity;
import org.jbpm.pvm.internal.model.ExecutionImpl;

public class MuleReceiveActivity extends StateActivity
{
    // Expected incoming endpoint; if a message was received from a different endpoint, an exception will be thrown.
    private String endpoint;

    // Expected incoming message type; if the payload received is not assignable to this class, an exception will be thrown.
    private Class payloadClass;

    // Variable into which the incoming message payload will be stored. If null, the payload will not
    // be stored at all.
    private String variableName;

    private static final Log log = Log.getLog(MuleReceiveActivity.class.getName());

    @Override
    public void signal(ExecutionImpl execution, String signalName, Map<String, ?> parameters)
        throws Exception
    {
        Object payload = execution.getVariable(ProcessConnector.PROCESS_VARIABLE_INCOMING);

        // Validate expected inbound endpoint
        if (endpoint != null)
        {
            String messageSource = (String) execution.getVariable(ProcessConnector.PROCESS_VARIABLE_INCOMING_SOURCE);
            log.debug("Validating message source = " + messageSource + ", expected = " + endpoint);
            if (!endpoint.equalsIgnoreCase(messageSource))
            {
                throw new JbpmException("Incoming message source is " + messageSource + " but expected source is " + endpoint);
            }
        }

        // Validate expected message type
        if (payloadClass != null)
        {
            log.debug("Validating message type = " + payload.getClass() + ", expected = " + payloadClass);
            if (!payloadClass.isAssignableFrom(payload.getClass()))
            {
                throw new JbpmException("Incoming message is of type " + payload.getClass() + " but expected type is " + payloadClass);
            }
        }
        
        // Store message payload into variable
        if (variableName != null)
        {
            if (payload != null)
            {
                log.debug("Storing incoming message to variable " + variableName + ", payload = " + payload);
                execution.setVariable(variableName, payload);
            }
            else
            {
                log.info("Synchronous message was sent to endpoint " + endpoint + ", but no response was returned.");
            }
        }

        super.signal(execution, signalName, parameters);
    }

    public String getEndpoint()
    {
        return endpoint;
    }

    public void setEndpoint(String endpoint)
    {
        this.endpoint = endpoint;
    }

    public String getVariableName()
    {
        return variableName;
    }

    public void setVariableName(String variableName)
    {
        this.variableName = variableName;
    }

    public void setPayloadClass(String className)
    {
        if (className != null)
        {
            try
            {
                payloadClass = ClassUtils.loadClass(className, this.getClass());
            }
            catch (ClassNotFoundException e)
            {
                log.error("Expected message type not valid: " + e.getMessage());
            }
        }
    }

    public Class getPayloadClass()
    {
        return payloadClass;
    }
}
