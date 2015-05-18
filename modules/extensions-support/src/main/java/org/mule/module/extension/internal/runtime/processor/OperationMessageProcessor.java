/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.runtime.processor;

import org.mule.api.MessagingException;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.context.MuleContextAware;
import org.mule.api.processor.MessageProcessor;
import org.mule.config.i18n.MessageFactory;
import org.mule.extension.introspection.Operation;
import org.mule.extension.runtime.OperationContext;
import org.mule.extension.runtime.OperationExecutor;
import org.mule.module.extension.internal.runtime.DefaultOperationContext;
import org.mule.module.extension.internal.runtime.OperationContextAdapter;
import org.mule.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.module.extension.internal.runtime.resolver.ResolverSetResult;

import java.util.concurrent.Future;

import org.apache.commons.lang.StringUtils;

/**
 * A {@link MessageProcessor} capable of executing extension operations.
 * It obtains a configuration, evaluate all the operation parameters
 * and executes a {@link Operation}. This message processor is capable
 * of serving the execution of any operation in any extension.
 *
 * @since 3.7.0
 */
public final class OperationMessageProcessor implements MessageProcessor, MuleContextAware
{

    private final String configurationInstanceProviderName;
    private final Operation operation;
    private final ResolverSet resolverSet;

    private MuleContext muleContext;

    public OperationMessageProcessor(Operation operation,
                                     String configurationInstanceProviderName,
                                     ResolverSet resolverSet)
    {
        this.operation = operation;
        this.configurationInstanceProviderName = configurationInstanceProviderName;
        this.resolverSet = resolverSet;
    }

    private OperationContext createOperationContext(MuleEvent event) throws MuleException {
        ResolverSetResult parameters = resolverSet.resolve(event);
        return new DefaultOperationContext(operation, parameters, event);
    }

    @Override
    public MuleEvent process(MuleEvent event) throws MuleException
    {
        OperationContext operationContext = createOperationContext(event);

        Future<Object> future = executeOperation(operationContext);
        Object result = extractResult(event, future);

        if (result instanceof MuleEvent)
        {
            return (MuleEvent) result;
        }
        else if (result instanceof MuleMessage)
        {
            event.setMessage((MuleMessage) result);
        }
        else
        {
            event.getMessage().setPayload(result);
        }

        return event;
    }

    private Object extractResult(MuleEvent event, Future<Object> future) throws MuleException
    {
        try
        {
            // for now this is fine because the execution engine is blocking. When we move
            // to a non-blocking engine, this future needs to be handled differently
            return future.get();
        }
        catch (Exception e)
        {
            throw handledException("Could not execute operation " + operation.getName(), event, e);
        }
    }

    private Future<Object> executeOperation(OperationContext operationContext) throws MuleException
    {
        OperationExecutor executor = StringUtils.isBlank(configurationInstanceProviderName)
                                     ? muleContext.getExtensionManager().getOperationExecutor(operationContext)
                                     : muleContext.getExtensionManager().getOperationExecutor(configurationInstanceProviderName, operationContext);

        try
        {
            return executor.execute(operationContext);
        }
        catch (Exception e)
        {
            throw handledException(String.format("Operation %s threw exception", operation.getName()),
                                   ((OperationContextAdapter) operationContext).getEvent(),
                                   e);
        }
    }

    private MuleException handledException(String message, MuleEvent event, Exception e)
    {
        return new MessagingException(MessageFactory.createStaticMessage(message), event, e, this);
    }

    @Override
    public void setMuleContext(MuleContext muleContext)
    {
        this.muleContext = muleContext;
    }
}
