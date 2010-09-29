/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.bpm;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.transport.DispatchException;
import org.mule.api.transport.PropertyScope;
import org.mule.config.i18n.MessageFactory;
import org.mule.module.bpm.Process;
import org.mule.transport.AbstractMessageDispatcher;
import org.mule.transport.NullPayload;
import org.mule.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Initiates or advances a workflow process from an outgoing Mule event.
 * 
 * @deprecated It is recommended to configure BPM as a component rather than a transport for 3.x
 */
public class ProcessMessageDispatcher extends AbstractMessageDispatcher
{
    private ProcessConnector connector;

    public ProcessMessageDispatcher(OutboundEndpoint endpoint)
    {
        super(endpoint);
        this.connector = (ProcessConnector)endpoint.getConnector();
    }

    /**
     * Performs a synchronous action on the BPMS.
     * 
     * @return an object representing the new state of the process
     */
    @Override
    public MuleMessage doSend(MuleEvent event) throws Exception
    {
        Object process = processAction(event);

        if (process != null)
        {
            MuleMessage msg = new DefaultMuleMessage(process, connector.getMuleContext());
            msg.setProperty(Process.PROPERTY_PROCESS_ID, connector.getBpms().getId(process), PropertyScope.SESSION);
            return msg;
        }
        else
        {
            throw new DispatchException(
                MessageFactory.createStaticMessage("Synchronous process invocation must return the new process state."),
                event, this);
        }
    }

    /**
     * Performs an asynchronous action on the BPMS.
     */
    @Override
    public void doDispatch(MuleEvent event) throws Exception
    {
        processAction(event);
    }

    protected Object processAction(MuleEvent event) throws Exception
    {
        // An object representing the new state of the process
        Object process;

        // Create a map of process variables based on the message properties.
        Map processVariables = new HashMap();
        if (event != null)
        {
            populateProcessVariables(event, processVariables, PropertyScope.INVOCATION);
            populateProcessVariables(event, processVariables, PropertyScope.OUTBOUND);

            Object payload = event.getMessage().getPayload();
            if (payload != null && !(payload instanceof NullPayload))
            {
                // Store the message's payload as a process variable.
                processVariables.put(Process.PROCESS_VARIABLE_INCOMING, payload);

                // Store the endpoint on which the message was received as a process variable.
                String originatingEndpoint = event.getMessage().getInboundProperty(MuleProperties.MULE_ORIGINATING_ENDPOINT_PROPERTY);
                if (StringUtils.isNotEmpty(originatingEndpoint))
                {
                    processVariables.put(Process.PROCESS_VARIABLE_INCOMING_SOURCE, originatingEndpoint);
                }
            }
        }

        // Retrieve the parameters
        Object processType = event.getMessage().getSessionProperty(Process.PROPERTY_PROCESS_TYPE);
        if (processType == null)
        {
            processType = event.getMessage().getInvocationProperty(Process.PROPERTY_PROCESS_TYPE);
        }
        if (processType == null)
        {
            processType = event.getMessage().getInboundProperty(Process.PROPERTY_PROCESS_TYPE);
        }
        processVariables.remove(Process.PROPERTY_PROCESS_TYPE);

        String processIdField = connector.getProcessIdField();
        if (StringUtils.isEmpty(processIdField))
        {
            processIdField = Process.PROPERTY_PROCESS_ID;
        }

        Object processId;
        processId = event.getMessage().getSessionProperty(processIdField);
        if (processId == null)
        {
            processId = event.getMessage().getInvocationProperty(processIdField); 
        }
        if (processId == null)
        {
            processId = event.getMessage().getInboundProperty(processIdField); 
        }
        processVariables.remove(processIdField);

        // Default action is "advance"
        String action = event.getMessage().getInvocationProperty(Process.PROPERTY_ACTION, Process.ACTION_ADVANCE);
        processVariables.remove(Process.PROPERTY_ACTION);

        Object transition = event.getMessage().getInvocationProperty(Process.PROPERTY_TRANSITION);
        processVariables.remove(Process.PROPERTY_TRANSITION);

        // Decode the URI, for example:
        // bpm://testProcess/4561?action=advance
        String temp;
        temp = event.getEndpoint().getEndpointURI().getHost();
        if (StringUtils.isNotEmpty(temp))
        {
            processType = temp;
        }
        temp = event.getEndpoint().getEndpointURI().getPath();
        if (StringUtils.isNotEmpty(temp))
        {
            // Strip the leading "/" from the path.
            if (temp.startsWith("/"))
            {
                temp = StringUtils.right(temp, temp.length() - 1);
            }
            // If there are any remaining "/", we don't know what to do with them.
            if (temp.indexOf("/") != -1)
            {
                throw new IllegalArgumentException("Unexpected format in the path of the URL: " + temp);
            }
            processId = temp;
        }

        // //////////////////////////////////////////////////////////////////////

        logger.debug("Message received: payload = " + event.getMessage().getPayload().getClass().getName() + " processType = " + processType + " processId = " + processId + " action = " + action);
        
        // Start a new process.
        if (processId == null || action.equals(Process.ACTION_START))
        {
            if (processType != null)
            {
                process = connector.getBpms().startProcess(processType, transition, processVariables);
                if ((process != null) && logger.isInfoEnabled())
                {
                    logger.info("New process started, ID = " + connector.getBpms().getId(process));
                }
            }
            else
            {
                throw new IllegalArgumentException("Process type is missing, cannot start a new process.");
            }
        }

        // Don't advance the process, just update the process variables.
        else if (action.equals(Process.ACTION_UPDATE))
        {
            if (processId != null)
            {
                process = connector.getBpms().updateProcess(processId, processVariables);
                if ((process != null) && logger.isInfoEnabled())
                {
                    logger.info("Process variables updated, ID = " + connector.getBpms().getId(process));
                }
            }
            else
            {
                throw new IllegalArgumentException("Process ID is missing, cannot update process.");
            }
        }

        // Abort the running process (end abnormally).
        else if (action.equals(Process.ACTION_ABORT))
        {
            if (processId != null)
            {
                connector.getBpms().abortProcess(processId);
                process = NullPayload.getInstance();
                logger.info("Process aborted, ID = " + processId);
            }
            else
            {
                throw new IllegalArgumentException("Process ID is missing, cannot abort process.");
            }
        }

        // Advance the already-running process one step.
        else
        {
            if (processId != null)
            {
                process = connector.getBpms().advanceProcess(processId, transition, processVariables);
                if ((process != null) && logger.isInfoEnabled())
                {
                    logger.info("Process advanced, ID = " + connector.getBpms().getId(process)
                                    + ", new state = " + connector.getBpms().getState(process));
                }
            }
            else
            {
                throw new IllegalArgumentException("Process ID is missing, cannot advance process.");
            }
        }

        return process;
    }

    protected void populateProcessVariables(MuleEvent event, Map processVariables, PropertyScope propertyScope)
    {
        for (String propertyName : event.getMessage().getPropertyNames(propertyScope))
        {
            // The session property can become rather large and causes problems with DB persistence.
            if (!propertyName.equals(MuleProperties.MULE_SESSION_PROPERTY))
            {
                processVariables.put(propertyName, event.getMessage().getProperty(propertyName, propertyScope));
            }
        }
    }
}
