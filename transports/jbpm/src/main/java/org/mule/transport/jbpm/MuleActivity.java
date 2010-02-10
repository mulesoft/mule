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
import org.mule.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.jxpath.JXPathContext;
import org.jbpm.api.JbpmException;
import org.jbpm.api.activity.ActivityExecution;
import org.jbpm.api.listener.EventListener;
import org.jbpm.api.listener.EventListenerExecution;
import org.jbpm.api.model.OpenExecution;
import org.jbpm.internal.log.Log;
import org.jbpm.jpdl.internal.activity.JpdlActivity;
import org.jbpm.pvm.internal.env.EnvironmentImpl;
import org.jbpm.pvm.internal.model.ExecutionImpl;

public class MuleActivity extends JpdlActivity implements EventListener
{
    boolean synchronous;
    String endpoint;
    String transformers;
    Map properties;

    // Use "payload" to easily specify the payload as a string directly in the jPDL.
    // Use "payloadSource" to get the payload from a process variable.
    String payload;
    String payloadSource;

    // The name of the variable can be changed by setting field "var" in the
    // process definition
    String variableName;
    
    // The actual payload (as an object) will be stored here.
    private Object payloadObject;
    
    private static final Log log = Log.getLog(MuleActivity.class.getName());

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

        if (transformers != null)
        {
            endpoint += "?transformers=" + transformers;
        }

        if (payload == null)
        {
            if (payloadSource == null)
            {
                payloadObject = execution.getVariable(ProcessConnector.PROCESS_VARIABLE_DATA);
                if (payloadObject == null)
                {
                    payloadObject = execution.getVariable(ProcessConnector.PROCESS_VARIABLE_INCOMING);
                }
            }
            else
            {
                // The payloadSource may be specified using JavaBean notation (e.g.,
                // "myObject.myStuff.myField" would first retrieve the process
                // variable "myObject" and then call .getMyStuff().getMyField()
                String[] tokens = StringUtils.split(payloadSource, ".", 2);
                payloadObject = execution.getVariable(tokens[0]);
                if (tokens.length > 1)
                {
                    JXPathContext context = JXPathContext.newContext(payloadObject);
                    payloadObject = context.getValue(tokens[1].replaceAll("\\.", "/"));
                }
            }
        }
        else
        {
            payloadObject = payload;
        }
        if (payloadObject == null)
        {
            throw new IllegalArgumentException("Payload for message is null.  Payload source is \""
                                               + payloadSource + "\"");
        }

        Map props = new HashMap();

        // TODO: this probably isn't the best. I'm casting to an Impl because it's
        // the only way I could see to get the name of the process definition
        props.put(ProcessConnector.PROPERTY_PROCESS_TYPE, ((ExecutionImpl)execution).getProcessDefinition().getName());
        props.put(ProcessConnector.PROPERTY_PROCESS_ID, execution.getProcessInstance().getId());
        props.put(MuleProperties.MULE_CORRELATION_ID_PROPERTY, execution.getProcessInstance().getId());

        if (properties != null)
        {
            props.putAll(properties);
        }

        MuleMessage response = mule.generateMessage(endpoint, payloadObject, props, synchronous);
        if (synchronous)
        {
            if (response != null)
            {
                execution.setVariable(variableName, response.getPayload());
            }
            else
            {
                log.info("Synchronous message was sent to endpoint " + endpoint
                         + ", but no response was returned.");
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

    public String getTransformers()
    {
        return transformers;
    }

    public void setTransformers(String transformers)
    {
        this.transformers = transformers;
    }

    public Map getProperties()
    {
        return properties;
    }

    public void setProperties(Map properties)
    {
        this.properties = properties;
    }

    public String getPayload()
    {
        return payload;
    }

    public void setPayload(String payload)
    {
        this.payload = payload;
    }

    public String getPayloadSource()
    {
        return payloadSource;
    }

    public void setPayloadSource(String payloadSource)
    {
        this.payloadSource = payloadSource;
    }

    public String getVariableName()
    {
        return variableName;
    }

    public void setVariableName(String variableName)
    {
        this.variableName = variableName;
    }
}
