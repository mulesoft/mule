/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extensions.internal.runtime.processor;

import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.processor.MessageProcessor;
import org.mule.config.i18n.MessageFactory;
import org.mule.extensions.introspection.Operation;
import org.mule.extensions.introspection.OperationContext;
import org.mule.module.extensions.internal.runtime.DefaultOperationContext;
import org.mule.module.extensions.internal.runtime.resolver.ResolverSet;
import org.mule.module.extensions.internal.runtime.resolver.ResolverSetResult;
import org.mule.module.extensions.internal.runtime.resolver.ValueResolver;
import org.mule.module.extensions.internal.util.GroupValueSetter;
import org.mule.module.extensions.internal.util.ValueSetter;

import java.util.List;
import java.util.concurrent.Future;

/**
 * A {@link MessageProcessor} capable of executing extension operations.
 * It obtains a configuration, evaluate all the operation parameters
 * and executes a {@link Operation}. This message processor is capable
 * of serving the execution of any operation in any extension.
 *
 * @since 3.7.0
 */
public final class OperationMessageProcessor implements MessageProcessor
{

    private final ValueResolver<Object> configuration;
    private final Operation operation;
    private final ResolverSet resolverSet;
    private final List<ValueSetter> instanceLevelGroupValueSetters;

    public OperationMessageProcessor(ValueResolver<Object> configuration, Operation operation, ResolverSet resolverSet)
    {
        this.configuration = configuration;
        this.operation = operation;
        this.resolverSet = resolverSet;
        instanceLevelGroupValueSetters = GroupValueSetter.settersFor(operation);
    }

    @Override
    public MuleEvent process(MuleEvent event) throws MuleException
    {
        Object configInstance = configuration.resolve(event);
        ResolverSetResult parameters = resolverSet.resolve(event);

        Future<Object> future = executeOperation(event, configInstance, parameters);
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

    private Future<Object> executeOperation(MuleEvent event, Object configInstance, ResolverSetResult parameters) throws MuleException
    {
        OperationContext context = new DefaultOperationContext(configInstance, parameters, event, instanceLevelGroupValueSetters);

        try
        {
            return operation.getImplementation().execute(context);
        }
        catch (Exception e)
        {
            throw handledException(String.format("Operation %s threw exception", operation.getName()), event, e);
        }
    }

    private MuleException handledException(String message, MuleEvent event, Exception e)
    {
        return new MessagingException(MessageFactory.createStaticMessage(message), event, e, this);
    }
}
