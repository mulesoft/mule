/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.runtime.source;

import static org.mule.MessageExchangePattern.REQUEST_RESPONSE;
import static org.mule.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.module.extension.internal.util.MuleExtensionUtils.getInitialiserEvent;
import static org.mule.util.concurrent.ThreadNameHelper.getPrefix;

import org.mule.DefaultMuleEvent;
import org.mule.api.DefaultMuleException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleRuntimeException;
import org.mule.api.config.ThreadingProfile;
import org.mule.api.connection.ConnectionException;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.context.WorkManager;
import org.mule.api.execution.CompletionHandler;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Lifecycle;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.retry.RetryCallback;
import org.mule.api.retry.RetryContext;
import org.mule.api.retry.RetryPolicyTemplate;
import org.mule.api.source.MessageSource;
import org.mule.api.temporary.MuleMessage;
import org.mule.api.transaction.TransactionConfig;
import org.mule.execution.MessageProcessContext;
import org.mule.execution.MessageProcessingManager;
import org.mule.extension.api.introspection.config.RuntimeConfigurationModel;
import org.mule.extension.api.introspection.RuntimeExtensionModel;
import org.mule.extension.api.introspection.source.RuntimeSourceModel;
import org.mule.extension.api.runtime.ConfigurationProvider;
import org.mule.extension.api.runtime.ExceptionCallback;
import org.mule.extension.api.runtime.MessageHandler;
import org.mule.extension.api.runtime.source.Source;
import org.mule.extension.api.runtime.source.SourceContext;
import org.mule.extension.api.runtime.source.SourceFactory;
import org.mule.module.extension.internal.manager.ExtensionManagerAdapter;
import org.mule.module.extension.internal.runtime.ExtensionComponent;
import org.mule.module.extension.internal.runtime.exception.ExceptionEnricherManager;
import org.mule.module.extension.internal.runtime.processor.IllegalOperationException;
import org.mule.module.extension.internal.runtime.processor.IllegalSourceException;
import org.mule.util.ExceptionUtils;

import java.io.Serializable;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link MessageSource} which connects the Extensions API with the Mule runtime by
 * connecting a {@link Source} with a flow represented by a {@link #messageProcessor}
 * <p>
 * This class implements the {@link Lifecycle} interface and propagates all of its events to
 * the underlying {@link Source}. It will also perform dependency injection on it and will
 * responsible for properly invokin {@link Source#setSourceContext(SourceContext)}
 *
 * @since 4.0
 */
public class ExtensionMessageSource extends ExtensionComponent implements MessageSource,
        MessageHandler<Object, Serializable>,
        ExceptionCallback<Throwable>,
        Lifecycle
{

    private static final Logger LOGGER = LoggerFactory.getLogger(ExtensionMessageSource.class);

    private final RuntimeSourceModel sourceModel;
    private final SourceFactory sourceFactory;
    private final ThreadingProfile threadingProfile;
    private final RetryPolicyTemplate retryPolicyTemplate;
    private final ExceptionEnricherManager exceptionEnricherManager;

    private SourceWrapper source;
    private WorkManager workManager;

    public ExtensionMessageSource(RuntimeExtensionModel extensionModel,
                                  RuntimeSourceModel sourceModel,
                                  SourceFactory sourceFactory,
                                  String configurationProviderName,
                                  ThreadingProfile threadingProfile,
                                  RetryPolicyTemplate retryPolicyTemplate,
                                  ExtensionManagerAdapter managerAdapter)
    {
        super(extensionModel, sourceModel, configurationProviderName, managerAdapter);
        this.sourceModel = sourceModel;
        this.sourceFactory = sourceFactory;
        this.threadingProfile = threadingProfile;
        this.retryPolicyTemplate = retryPolicyTemplate;
        this.exceptionEnricherManager = new ExceptionEnricherManager(extensionModel, sourceModel);
    }

    private MessageProcessor messageProcessor;

    @Inject
    private MessageProcessingManager messageProcessingManager;

    @Override
    public void handle(MuleMessage<Object, Serializable> message, CompletionHandler<MuleMessage<Object, Serializable>, Exception> completionHandler)
    {
        MuleEvent event = new DefaultMuleEvent((org.mule.api.MuleMessage) message, REQUEST_RESPONSE, flowConstruct);
        messageProcessingManager.processMessage(new ExtensionFlowProcessingTemplate(event, messageProcessor, completionHandler), createProcessingContext());
    }

    @Override
    public void handle(MuleMessage<Object, Serializable> message)
    {
        MuleEvent event = new DefaultMuleEvent((org.mule.api.MuleMessage) message, REQUEST_RESPONSE, flowConstruct);
        messageProcessingManager.processMessage(new ExtensionFlowProcessingTemplate(event, messageProcessor, new NullCompletionHandler()), createProcessingContext());
    }

    @Override
    public void onException(Throwable exception)
    {
        exception = exceptionEnricherManager.processException(exception);
        Optional<ConnectionException> connectionException = ExceptionUtils.extractRootConnectionException(exception);
        if (connectionException.isPresent())
        {
            try
            {
                LOGGER.warn(String.format("Message source '%s' on flow '%s' threw exception. Restarting...", source.getName(), flowConstruct.getName()), exception);
                stopSource();
                disposeSource();
                startSource();
            }
            catch (Throwable e)
            {
                notifyExceptionAndShutDown(e);
            }
        }
        else
        {
            notifyExceptionAndShutDown(exception);
        }
    }

    @Override
    public void doInitialise() throws InitialisationException
    {
        try
        {
            createSource();
        }
        catch (InitialisationException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new InitialisationException(e, this);
        }
    }

    @Override
    public void start() throws MuleException
    {
        if (workManager == null)
        {
            workManager = createWorkManager();
            workManager.start();
        }

        startSource();
    }

    @Override
    public void stop() throws MuleException
    {
        try
        {
            stopSource();
        }
        finally
        {
            stopWorkManager();
        }
    }

    @Override
    public void dispose()
    {
        disposeSource();
    }

    private void shutdown()
    {
        try
        {
            stopIfNeeded(this);
        }
        catch (Exception e)
        {
            LOGGER.error(String.format("Failed to stop source '%s' on flow '%s'", source.getName(), flowConstruct.getName()), e);
        }
        disposeIfNeeded(this, LOGGER);
    }

    private void stopWorkManager()
    {
        if (workManager != null)
        {
            try
            {
                workManager.dispose();
            }
            finally
            {
                workManager = null;
            }
        }
    }

    private void stopSource() throws MuleException
    {
        if (source != null)
        {
            try
            {
                source.stop();
            }
            catch (Exception e)
            {
                throw new DefaultMuleException(String.format("Found exception stopping source '%s' of flow '%s'", source.getName(), flowConstruct.getName()), e);
            }
        }
    }

    private void disposeSource()
    {
        disposeIfNeeded(source, LOGGER);
    }

    private void createSource() throws Exception
    {
        source = new SourceWrapper(sourceFactory.createSource());
        source.setFlowConstruct(flowConstruct);
        source.setSourceContext(new ImmutableSourceContext(this, this, getConfiguration(getInitialiserEvent(muleContext))));

        initialiseIfNeeded(source, true, muleContext);
    }

    private void startSource()
    {
        try
        {
            final RetryContext execute = retryPolicyTemplate.execute(new SourceRetryCallback(), workManager);

            if (!execute.isOk())
            {
                throw execute.getLastFailure();
            }
        }
        catch (Throwable e)
        {
            throw new MuleRuntimeException(e);
        }
    }

    private class SourceRetryCallback implements RetryCallback
    {

        @Override
        public void doWork(RetryContext context) throws Exception
        {
            try
            {
                createSource();
                source.start();
            }
            catch (Exception e)
            {
                stopSource();
                disposeSource();
                Exception exception = exceptionEnricherManager.processException(e);
                Optional<ConnectionException> connectionException = ExceptionUtils.extractRootConnectionException(exception);
                if (connectionException.isPresent())
                {
                    throw exception;
                }
                else
                {
                    context.setFailed(exception);
                }
            }
        }

        @Override
        public String getWorkDescription()
        {
            return "Message Source Reconnection";
        }

        @Override
        public Object getWorkOwner()
        {
            return this;
        }
    }

    //TODO: MULE-9320
    private WorkManager createWorkManager()
    {
        return threadingProfile.createWorkManager(String.format("%s%s.worker", getPrefix(muleContext), flowConstruct.getName()),
                                                  muleContext.getConfiguration().getShutdownTimeout());
    }

    private MessageProcessContext createProcessingContext()
    {
        return new MessageProcessContext()
        {
            @Override
            public boolean supportsAsynchronousProcessing()
            {
                return true;
            }

            @Override
            public MessageSource getMessageSource()
            {
                return ExtensionMessageSource.this;
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
                return muleContext.getExecutionClassLoader();
            }
        };
    }

    @Override
    public void setListener(MessageProcessor listener)
    {
        messageProcessor = listener;
    }

    private void notifyExceptionAndShutDown(Throwable exception)
    {
        LOGGER.error(String.format("Message source '%s' on flow '%s' threw exception. Shutting down it forever...", source.getName(), flowConstruct.getName()), exception);
        shutdown();
    }

    /**
     * Validates if the current source is valid for the set configuration.
     * In case that the validation fails, the method will throw a {@link IllegalSourceException}
     */
    @Override
    protected void validateOperationConfiguration(ConfigurationProvider<Object> configurationProvider)
    {
        RuntimeConfigurationModel configurationModel = configurationProvider.getModel();
        if (!configurationModel.getSourceModel(sourceModel.getName()).isPresent() &&
            !configurationModel.getExtensionModel().getSourceModel(sourceModel.getName()).isPresent())
        {
            throw new IllegalOperationException(String.format("Flow '%s' defines an usage of operation '%s' which points to configuration '%s'. " +
                                                              "The selected config does not support that operation.",
                                                              flowConstruct.getName(),
                                                              sourceModel.getName(),
                                                              configurationProvider.getName()));
        }
    }
}
