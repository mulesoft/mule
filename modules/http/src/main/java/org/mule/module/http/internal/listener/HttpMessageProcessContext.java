/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal.listener;

import org.mule.api.construct.FlowConstruct;
import org.mule.api.context.WorkManager;
import org.mule.api.source.MessageSource;
import org.mule.api.transaction.TransactionConfig;
import org.mule.execution.MessageProcessContext;

public class HttpMessageProcessContext implements MessageProcessContext
{

    private final DefaultHttpListener listener;
    private final FlowConstruct flowConstruct;
    private final WorkManager workManager;
    private final ClassLoader executionClassLoader;

    HttpMessageProcessContext(final DefaultHttpListener listener, final FlowConstruct flowConstruct, final WorkManager workManager, final ClassLoader executionClassLoader)
    {
        this.listener = listener;
        this.flowConstruct = flowConstruct;
        this.workManager = workManager;
        this.executionClassLoader = executionClassLoader;
    }

    @Override
    public boolean supportsAsynchronousProcessing()
    {
        return true;
    }

    @Override
    public MessageSource getMessageSource()
    {
        return listener;
    }

    @Override
    public FlowConstruct getFlowConstruct()
    {
        return flowConstruct;
    }

    @Override
    public WorkManager getFlowExecutionWorkManager()
    {
        return workManager;
    }

    @Override
    public TransactionConfig getTransactionConfig()
    {
        return null;
    }

    @Override
    public ClassLoader getExecutionClassLoader()
    {
        return executionClassLoader;
    }
}
