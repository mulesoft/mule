/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
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
import org.mule.config.i18n.MessageFactory;
import org.mule.transport.AbstractMessageDispatcher;
import org.mule.transport.NullPayload;
import org.mule.util.StringUtils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Initiates or advances a workflow process from an outgoing Mule event.
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
    public MuleMessage doSend(MuleEvent event) throws Exception
    {
        Object process = processAction(event);

        if (process != null)
        {
            MuleMessage msg = new DefaultMuleMessage(process);
            msg.setProperty(ProcessConnector.PROPERTY_PROCESS_ID, connector.getBpms().getId(process));
            return msg;
        }
        else
        {
            throw new DispatchException(MessageFactory
                .createStaticMessage("Synchronous process invocation must return the new process state."),
                event.getMessage(), event.getEndpoint());
        }
    }

    /**
     * Performs an asynchronous action on the BPMS.
     */
    public void doDispatch(MuleEvent event) throws Exception
    {
        processAction(event);
    }

    protected Object processAction(MuleEvent event) throws Exception
    {
        // An object representing the new state of the process
        Object process = null;

        // Create a map of process variables based on the message properties.
        Map processVariables = new HashMap();
        if (event != null)
        {
            String propertyName;
            for (Iterator iterator = event.getMessage().getPropertyNames().iterator(); iterator.hasNext();)
            {
                propertyName = (String)iterator.next();
                processVariables.put(propertyName, event.getMessage().getProperty(propertyName));
            }

            Object payload = event.transformMessage();
            if (payload != null && !(payload instanceof NullPayload))
            {
                // Store the message's payload as a process variable.
                processVariables.put(ProcessConnector.PROCESS_VARIABLE_INCOMING, payload);

                // Store the endpoint on which the message was received as a process
                // variable.
                String originatingEndpoint = event.getMessage().getStringProperty(
                    MuleProperties.MULE_ORIGINATING_ENDPOINT_PROPERTY, null);
                if (StringUtils.isNotEmpty(originatingEndpoint))
                {
                    processVariables.put(ProcessConnector.PROCESS_VARIABLE_INCOMING_SOURCE,
                        originatingEndpoint);
                }
            }
        }

        // Retrieve the parameters
        Object processType = event.getProperty(ProcessConnector.PROPERTY_PROCESS_TYPE);
        processVariables.remove(ProcessConnector.PROPERTY_PROCESS_TYPE);

        // TODO MULE-1220 The processId for BPM is sort of like a session and so we could probably use
        // Mule's SessionHandler interface for managing this.  
        Object processId;
        String processIdField = connector.getProcessIdField();

        //TODO this is redundent but I'm not sure what the correct behaviour is
        if (StringUtils.isNotEmpty(processIdField))
        {
            processId = event.getProperty(processIdField);
        }
        // If processId is explicitly set for the message, this overrides the
        // processIdField.
        processId = event.getProperty(ProcessConnector.PROPERTY_PROCESS_ID);
        processVariables.remove(ProcessConnector.PROPERTY_PROCESS_ID);

        // Default action is "advance"
        String action = event.getMessage().getStringProperty(ProcessConnector.PROPERTY_ACTION,
            ProcessConnector.ACTION_ADVANCE);
        processVariables.remove(ProcessConnector.PROPERTY_ACTION);

        Object transition = event.getMessage().getProperty(ProcessConnector.PROPERTY_TRANSITION);
        processVariables.remove(ProcessConnector.PROPERTY_TRANSITION);

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

        // Start a new process.
        if (processId == null || action.equals(ProcessConnector.ACTION_START))
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
        else if (action.equals(ProcessConnector.ACTION_UPDATE))
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
        else if (action.equals(ProcessConnector.ACTION_ABORT))
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
}
