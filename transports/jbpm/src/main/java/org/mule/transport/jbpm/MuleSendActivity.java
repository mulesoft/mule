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

import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.transport.bpm.MessageService;
import org.mule.transport.bpm.ProcessConnector;
import org.mule.util.ClassUtils;

import java.util.HashMap;
import java.util.Map;

import org.jbpm.api.JbpmException;
import org.jbpm.api.activity.ActivityExecution;
import org.jbpm.api.listener.EventListener;
import org.jbpm.api.listener.EventListenerExecution;
import org.jbpm.api.model.OpenExecution;
import org.jbpm.internal.log.Log;
import org.jbpm.jpdl.internal.activity.JpdlActivity;
import org.jbpm.pvm.internal.env.EnvironmentImpl;
import org.jbpm.pvm.internal.model.ExecutionImpl;
import org.jbpm.pvm.internal.script.ScriptManager;

public class MuleSendActivity extends JpdlActivity implements EventListener
{
    private boolean synchronous;
    private String endpoint;

    // Expected response type in the case of a synchronous call; if the response payload is not assignable to this class, an exception will be thrown.
    private Class responsePayloadClass;

    // Variable into which the synchronous response will be stored.  If null, the response will not be stored at all.
    private String responseVariableName;
    
    // payloadSource may be a literal value or it may be an expression which references process variables.
    private String payloadExpression;

    // The actual payload (as an object) will be stored here.
    private Object payloadObject;
    
    private static final Log log = Log.getLog(MuleSendActivity.class.getName());

    public void execute(ActivityExecution execution) throws Exception
    {
        perform(execution);
        ((ExecutionImpl) execution).historyAutomatic();
    }

    public void notify(EventListenerExecution execution) throws Exception
    {
        perform(execution);
    }

    public void perform(OpenExecution execution) throws Exception
    {
        MessageService mule = EnvironmentImpl.getCurrent().get(MuleMessageService.class);
        if (mule == null)
        {
            throw new JbpmException("The Mule MessageService is not available from the ProcessEngine, you may need to add it to your jbpm.cfg.xml file");
        }

        if (payloadExpression == null)
        {
            payloadObject = execution.getVariable(ProcessConnector.PROCESS_VARIABLE_DATA);
            if (payloadObject == null)
            {
                payloadObject = execution.getVariable(ProcessConnector.PROCESS_VARIABLE_INCOMING);
            }
        }
        else
        {
            // The payloadSource may be specified using an expression (e.g.,
            // #{myObject.myStuff.myField} would first retrieve the process
            // variable "myObject" and then call .getMyStuff().getMyField()
            payloadObject = ScriptManager.getScriptManager().evaluateExpression(payloadExpression, null);
        }
        if (payloadObject == null)
        {
            throw new IllegalArgumentException("Payload for message is null.  Payload source is \""
                                               + payloadExpression + "\"");
        }

        Map props = new HashMap();

        // TODO: this probably isn't the best. I'm casting to an Impl because it's
        // the only way I could see to get the name of the process definition
        props.put(ProcessConnector.PROPERTY_PROCESS_TYPE, ((ExecutionImpl) execution).getProcessDefinition().getName());
        props.put(ProcessConnector.PROPERTY_PROCESS_ID, execution.getId());
        String state = Jbpm.getState(execution.getProcessInstance());
        props.put("MULE_BPM_PROCESS_STATE", state);
        log.debug("process state: " + state);        

        // Set process vars as properties on outgoing Mule messages.
        for (Map.Entry<String, Object> var : execution.getVariables().entrySet())
        {
            if (!var.getKey().startsWith(MuleProperties.PROPERTY_PREFIX))
            {
                log.debug("process var: " + var.getKey() + " = " + var.getValue());
                props.put(var.getKey(), var.getValue());
            }
        }

        // Just in case the endpoint itself is an expression
        endpoint = (String) ScriptManager.getScriptManager().evaluateExpression(endpoint, null);
        MuleMessage response = mule.generateMessage(endpoint, payloadObject, props, synchronous);

        if (synchronous && response != null)
        {
            Object responsePayload = response.getPayload();
    
            // Validate expected response type
            if (responsePayloadClass != null)
            {
                log.debug("Validating response type = " + responsePayload.getClass() + ", expected = " + responsePayloadClass);
                if (!responsePayloadClass.isAssignableFrom(responsePayload.getClass()))
                {
                    throw new JbpmException("Response message is of type " + responsePayload.getClass() + " but expected type is " + responsePayloadClass);
                }
            }
            
            if (responseVariableName != null)
            {
                if (responsePayload != null)
                {
                    execution.setVariable(responseVariableName, responsePayload);
                }
                else
                {
                    log.info("Synchronous message was sent to endpoint " + endpoint + ", but no response was returned.");
                }
            }
        }
    }
    
    public boolean isSynchronous()
    {
        return synchronous;
    }

    public void setSynchronous(boolean synchronous)
    {
        this.synchronous = synchronous;
    }

    public String getEndpoint()
    {
        return endpoint;
    }

    public void setEndpoint(String endpoint)
    {
        this.endpoint = endpoint;
    }

    public String getPayloadExpression()
    {
        return payloadExpression;
    }

    public void setPayloadExpression(String payloadExpression)
    {
        this.payloadExpression = payloadExpression;
    }

    public String getResponseVariableName()
    {
        return responseVariableName;
    }

    public void setResponseVariableName(String responseVariableName)
    {
        this.responseVariableName = responseVariableName;
    }

    public void setResponsePayloadClass(String className)
    {
        if (className != null)
        {
            try
            {
                responsePayloadClass = ClassUtils.loadClass(className, this.getClass());
            }
            catch (ClassNotFoundException e)
            {
                log.error("Expected message type not valid: " + e.getMessage());
            }
        }
    }

    public Class getResponsePayloadClass()
    {
        return responsePayloadClass;
    }
}
