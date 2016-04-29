/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.transport;

import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.context.WorkManager;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.api.transaction.TransactionConfig;
import org.mule.runtime.core.api.transport.MessageReceiver;
import org.mule.runtime.core.execution.MessageProcessContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Generic {@link org.mule.execution.MessageProcessContext} implementations for transports.
 */
public class TransportMessageProcessContext implements MessageProcessContext
{

    protected transient Log logger = LogFactory.getLog(getClass());

    private final MessageReceiver messageReceiver;
    private WorkManager flowExecutionWorkManager;

    /**
     * Creates an instance that executes the flow in the current thread.
     * Calling #supportsAsynchronousProcessing method will always return false since
     * there's not work manager specified for the flow execution.
     *
     * @param messageReceiver receiver of the message
     */
    public TransportMessageProcessContext(MessageReceiver messageReceiver)
    {
        this.messageReceiver = messageReceiver;
    }

    /**
     * Creates an instance that executes the flow using the supplied WorkManager.
     * Calling #supportsAsynchronousProcessing method will always return true since
     * there's a WorkManager available to execute the flow.
     *
     * @param messageReceiver receiver of the message
     * @param flowExecutionWorkManager the work manager to use for the flow execution
     */
    public TransportMessageProcessContext(MessageReceiver messageReceiver, WorkManager flowExecutionWorkManager)
    {
        this.messageReceiver = messageReceiver;
        this.flowExecutionWorkManager = flowExecutionWorkManager;
    }

    @Override
    public MessageSource getMessageSource()
    {
        return this.messageReceiver.getEndpoint();
    }

    protected MessageSource getMessageReceiver()
    {
        return this.messageReceiver;
    }

    @Override
    public FlowConstruct getFlowConstruct()
    {
        return this.messageReceiver.getFlowConstruct();
    }

    @Override
    public boolean supportsAsynchronousProcessing()
    {
        if (flowExecutionWorkManager != null)
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    @Override
    public WorkManager getFlowExecutionWorkManager()
    {
        return flowExecutionWorkManager;
    }

    @Override
    public TransactionConfig getTransactionConfig()
    {
        return messageReceiver.getEndpoint().getTransactionConfig();
    }

    @Override
    public ClassLoader getExecutionClassLoader()
    {
        return messageReceiver.getEndpoint().getMuleContext().getExecutionClassLoader();
    }

}

