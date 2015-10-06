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
import static org.mule.module.extension.internal.ExtensionProperties.CONTENT_TYPE;
import static org.mule.module.extension.internal.ExtensionProperties.ENCODING_PARAMETER_NAME;
import static org.mule.module.extension.internal.ExtensionProperties.MIME_TYPE_PARAMETER_NAME;
import org.mule.api.MessagingException;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Lifecycle;
import org.mule.api.processor.MessageProcessor;
import org.mule.extension.api.ExtensionManager;
import org.mule.extension.api.introspection.ExtensionModel;
import org.mule.extension.api.introspection.OperationModel;
import org.mule.extension.api.runtime.ConfigurationInstance;
import org.mule.extension.api.runtime.ContentType;
import org.mule.extension.api.runtime.OperationContext;
import org.mule.extension.api.runtime.OperationExecutor;
import org.mule.module.extension.internal.runtime.DefaultExecutionMediator;
import org.mule.module.extension.internal.runtime.DefaultOperationContext;
import org.mule.module.extension.internal.runtime.OperationContextAdapter;
import org.mule.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.module.extension.internal.runtime.resolver.ResolverSetResult;
import org.mule.util.ExceptionUtils;
import org.mule.util.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link MessageProcessor} capable of executing extension operations.
 * <p>
 * It obtains a configuration instance, evaluate all the operation parameters
 * and executes a {@link OperationModel} by using a {@link #operationExecutor}. This message processor is capable
 * of serving the execution of any {@link OperationModel} of any {@link ExtensionModel}.
 * <p>
 * A {@link #operationExecutor} is obtained by invoking {@link OperationModel#getExecutor()}. That instance
 * will be use to serve all invokations of {@link #process(MuleEvent)} on {@code this} instance but
 * will not be shared with other instances of {@link OperationMessageProcessor}. All the {@link Lifecycle}
 * events that {@code this} instance receives will be propagated to the {@link #operationExecutor}.
 * <p>
 * The {@link #operationExecutor} is executed directly but by the means of a {@link DefaultExecutionMediator}
 *
 * @since 3.7.0
 */
public final class OperationMessageProcessor implements MessageProcessor, MuleContextAware, Lifecycle
{

    private static final Logger LOGGER = LoggerFactory.getLogger(OperationMessageProcessor.class);

    private final ExtensionModel extensionModel;
    private final String configurationProviderName;
    private final OperationModel operationModel;
    private final ResolverSet resolverSet;
    private final ExtensionManager extensionManager;
    private final DefaultExecutionMediator executionMediator = new DefaultExecutionMediator();

    private MuleContext muleContext;
    private OperationExecutor operationExecutor;

    public OperationMessageProcessor(ExtensionModel extensionModel,
                                     OperationModel operationModel,
                                     String configurationProviderName,
                                     ResolverSet resolverSet,
                                     ExtensionManager extensionManager)
    {
        this.extensionModel = extensionModel;
        this.operationModel = operationModel;
        this.configurationProviderName = configurationProviderName;
        this.resolverSet = resolverSet;
        this.extensionManager = extensionManager;
    }

    @Override
    public MuleEvent process(MuleEvent event) throws MuleException
    {
        ResolverSetResult resolverSetResult = resolverSet.resolve(event);
        final MuleMessage message = event.getMessage();
        ContentType contentType = getContentType(resolverSetResult, message);

        ConfigurationInstance<Object> configuration = getConfiguration(event);
        OperationContextAdapter operationContext = createOperationContext(configuration, event, contentType);
        message.setContentType(contentType);

        Object result = executeOperation(operationContext, event);

        return getResponseEvent(event, operationContext, result);
    }

    private ContentType getContentType(ResolverSetResult resolverSetResult, MuleMessage message)
    {
        String encoding = nullableString(resolverSetResult, ENCODING_PARAMETER_NAME);
        if (encoding == null)
        {
            encoding = message.getEncoding();
        }

        String mimeType = nullableString(resolverSetResult, MIME_TYPE_PARAMETER_NAME);
        if (mimeType == null)
        {
            mimeType = message.getMimeType();
        }

        return new ContentType(encoding, mimeType);
    }

    private String nullableString(ResolverSetResult resolverSetResult, String key)
    {
        Object value = resolverSetResult.get(key);
        if (value == null)
        {
            return null;
        }

        if (!(value instanceof String))
        {
            throw new IllegalArgumentException(String.format("'%s' was expected to be a String but type '%s' was found instead", key, value.getClass().getName()));
        }

        return (String) value;
    }

    private MuleEvent getResponseEvent(MuleEvent event, OperationContextAdapter operationContext, Object result)
    {
        if (result instanceof MuleEvent)
        {
            event = (MuleEvent) result;
        }
        else if (result instanceof MuleMessage)
        {
            event.setMessage((MuleMessage) result);
        }
        else
        {
            event.getMessage().setPayload(result);
        }

        ContentType contentType = operationContext.getVariable(CONTENT_TYPE);
        if (contentType != null)
        {
            event.getMessage().setContentType(contentType);
        }

        return event;
    }

    private Object executeOperation(OperationContext operationContext, MuleEvent event) throws MuleException
    {
        try
        {
            return executionMediator.execute(operationExecutor, operationContext);
        }
        catch (Exception e)
        {
            throw handledException(e, event);
        }
    }

    private MuleException handledException(Exception e, MuleEvent event)
    {
        Throwable root = ExceptionUtils.getRootCause(e);
        if (root == null)
        {
            root = e;
        }
        return new MessagingException(createStaticMessage(root.getMessage()), event, root, this);
    }

    private ConfigurationInstance<Object> getConfiguration(MuleEvent event)
    {
        return StringUtils.isBlank(configurationProviderName) ? extensionManager.getConfiguration(extensionModel, event)
                                                              : extensionManager.getConfiguration(configurationProviderName, event);
    }

    private OperationContextAdapter createOperationContext(ConfigurationInstance<Object> configuration, MuleEvent event, ContentType contentType) throws MuleException
    {
        OperationContextAdapter operationContext = new DefaultOperationContext(configuration, resolverSet.resolve(event), event);
        operationContext.setVariable(CONTENT_TYPE, contentType);

        return operationContext;
    }

    @Override
    public void initialise() throws InitialisationException
    {
        operationExecutor = operationModel.getExecutor();
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
