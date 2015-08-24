/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.runtime.processor;

import static org.mule.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.config.i18n.MessageFactory.createStaticMessage;
import org.mule.api.MessagingException;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Lifecycle;
import org.mule.api.processor.MessageProcessor;
import org.mule.extension.introspection.Extension;
import org.mule.extension.introspection.Operation;
import org.mule.extension.runtime.OperationContext;
import org.mule.extension.runtime.OperationExecutor;
import org.mule.module.extension.internal.manager.ExtensionManagerAdapter;
import org.mule.module.extension.internal.runtime.DefaultOperationContext;
import org.mule.module.extension.internal.runtime.OperationContextAdapter;
import org.mule.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.module.extension.internal.runtime.resolver.ResolverSetResult;
import org.mule.util.ExceptionUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link MessageProcessor} capable of executing extension operations.
 * <p/>
 * It obtains a configuration instance, evaluate all the operation parameters
 * and executes a {@link Operation} by using a {@link #operationExecutor}. This message processor is capable
 * of serving the execution of any {@link Operation} of any {@link Extension}.
 * <p/>
 * A {@link #operationExecutor} is obtained by invoking {@link Operation#getExecutor()}. That instance
 * will be use to serve all invokations of {@link #process(MuleEvent)} on {@code this} instance but
 * will not be shared with other instances of {@link OperationMessageProcessor}. All the {@link Lifecycle}
 * events that {@code this} instace receives will be propagated to the {@link #operationExecutor}
 *
 * @since 3.7.0
 */
public final class OperationMessageProcessor implements MessageProcessor, MuleContextAware, Lifecycle
{

    private static final Logger LOGGER = LoggerFactory.getLogger(OperationMessageProcessor.class);

    private final Extension extension;
    private final String configurationInstanceProviderName;
    private final Operation operation;
    private final ResolverSet resolverSet;
    private final ExtensionManagerAdapter extensionManager;

    private MuleContext muleContext;
    private OperationExecutor operationExecutor;

    public OperationMessageProcessor(Extension extension,
                                     Operation operation,
                                     String configurationInstanceProviderName,
                                     ResolverSet resolverSet,
                                     ExtensionManagerAdapter extensionManager)
    {
        this.extension = extension;
        this.operation = operation;
        this.configurationInstanceProviderName = configurationInstanceProviderName;
        this.resolverSet = resolverSet;
        this.extensionManager = extensionManager;
    }

    @Override
    public MuleEvent process(MuleEvent event) throws MuleException
    {
        OperationContext operationContext = createOperationContext(event);
        Object result = executeOperation(operationContext);

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

    private Object executeOperation(OperationContext operationContext) throws MuleException
    {
        try
        {
            return operationExecutor.execute(operationContext);
        }
        catch (Exception e)
        {
            throw handledException(operationContext, e);
        }
    }

    private MuleException handledException(OperationContext operationContext, Exception e)
    {
        Throwable root = ExceptionUtils.getRootCause(e);
        if (root == null)
        {
            root = e;
        }
        return new MessagingException(createStaticMessage(root.getMessage()), ((OperationContextAdapter) operationContext).getEvent(), root, this);
    }

    private OperationContext createOperationContext(MuleEvent event) throws MuleException
    {
        ResolverSetResult parameters = resolverSet.resolve(event);
        return new DefaultOperationContext(extension, operation, configurationInstanceProviderName, parameters, event, extensionManager);
    }

    @Override
    public void initialise() throws InitialisationException
    {
        operationExecutor = operation.getExecutor();
        initialiseIfNeeded(operationExecutor, muleContext);
    }

    @Override
    public void start() throws MuleException
    {
        startIfNeeded(operationExecutor);
    }

    @Override
    public void stop() throws MuleException
    {
        stopIfNeeded(operationExecutor);
    }

    @Override
    public void dispose()
    {
        disposeIfNeeded(operationExecutor, LOGGER);
    }

    @Override
    public void setMuleContext(MuleContext muleContext)
    {
        this.muleContext = muleContext;
    }
}
