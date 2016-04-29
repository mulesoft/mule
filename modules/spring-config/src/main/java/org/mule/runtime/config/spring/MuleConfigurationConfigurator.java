/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring;

import org.mule.runtime.config.spring.util.ProcessingStrategyUtils;
import org.mule.runtime.core.DefaultMuleContext;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.config.MuleProperties;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.exception.MessagingExceptionHandler;
import org.mule.runtime.core.api.exception.MessagingExceptionHandlerAcceptor;
import org.mule.runtime.core.api.processor.ProcessingStrategy;
import org.mule.runtime.core.api.serialization.ObjectSerializer;
import org.mule.runtime.core.config.DefaultMuleConfiguration;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.config.i18n.MessageFactory;
import org.mule.runtime.core.serialization.internal.JavaObjectSerializer;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.SmartFactoryBean;

/**
 * This class is a "SmartFactoryBean" which allows a few XML attributes to be set on the 
 * otherwise read-only MuleConfiguration.  It looks up the MuleConfiguration from the 
 * MuleContext and does some class-casting to be able to modify it.  Note that this will
 * only work if the MuleContext has not yet been started, otherwise the modifications 
 * will be ignored (and warnings logged).
 */
public class MuleConfigurationConfigurator implements MuleContextAware, SmartFactoryBean
{
    private MuleContext muleContext;

    // We instantiate DefaultMuleConfiguration to make sure we get the default values for
    // any properties not set by the user.
    private DefaultMuleConfiguration config = new DefaultMuleConfiguration();

    protected transient Log logger = LogFactory.getLog(MuleConfigurationConfigurator.class);

    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
    }

    public boolean isEagerInit()
    {
        return true;
    }

    public boolean isPrototype()
    {
        return false;
    }

    public Object getObject() throws Exception
    {
        MuleConfiguration configuration = muleContext.getConfiguration();
        if (configuration instanceof DefaultMuleConfiguration)
        {
            DefaultMuleConfiguration defaultConfig = (DefaultMuleConfiguration) configuration;
            defaultConfig.setDefaultResponseTimeout(config.getDefaultResponseTimeout());
            defaultConfig.setDefaultTransactionTimeout(config.getDefaultTransactionTimeout());
            defaultConfig.setShutdownTimeout(config.getShutdownTimeout());
            defaultConfig.setUseExtendedTransformations(config.useExtendedTransformations());
            defaultConfig.setFlowEndingWithOneWayEndpointReturnsNull(config.isFlowEndingWithOneWayEndpointReturnsNull());
            defaultConfig.setDefaultExceptionStrategyName(config.getDefaultExceptionStrategyName());
            defaultConfig.setEnricherPropagatesSessionVariableChanges(config.isEnricherPropagatesSessionVariableChanges());
            defaultConfig.setExtensions(config.getExtensions());

            determineDefaultProcessingStrategy(defaultConfig);
            validateDefaultExceptionStrategy();
            applyDefaultIfNoObjectSerializerSet(defaultConfig);

            return configuration;
        }
        else
        {
            throw new ConfigurationException(MessageFactory.createStaticMessage("Unable to set properties on read-only MuleConfiguration: " + configuration.getClass()));
        }
    }

    private void determineDefaultProcessingStrategy(DefaultMuleConfiguration defaultConfig)
    {
        if (config.getDefaultProcessingStrategy() != null)
        {
            defaultConfig.setDefaultProcessingStrategy(config.getDefaultProcessingStrategy());
        }
        else
        {
            String processingStrategyFromSystemProperty = System.getProperty(MuleProperties.MULE_DEFAULT_PROCESSING_STRATEGY);
            if (!StringUtils.isBlank(processingStrategyFromSystemProperty))
            {
                defaultConfig.setDefaultProcessingStrategy(ProcessingStrategyUtils.parseProcessingStrategy(processingStrategyFromSystemProperty));
            }
        }
    }

    private void validateDefaultExceptionStrategy()
    {
        String defaultExceptionStrategyName = config.getDefaultExceptionStrategyName();
        if (defaultExceptionStrategyName != null)
        {
            MessagingExceptionHandler messagingExceptionHandler = muleContext.getRegistry().lookupObject(
                defaultExceptionStrategyName);
            if (messagingExceptionHandler == null)
            {
                throw new MuleRuntimeException(CoreMessages.createStaticMessage(String.format(
                    "No global exception strategy defined with name %s.", defaultExceptionStrategyName)));
            }
            if (messagingExceptionHandler instanceof MessagingExceptionHandlerAcceptor)
            {
                MessagingExceptionHandlerAcceptor messagingExceptionHandlerAcceptor = (MessagingExceptionHandlerAcceptor) messagingExceptionHandler;
                if (!messagingExceptionHandlerAcceptor.acceptsAll())
                {
                    throw new MuleRuntimeException(
                        CoreMessages.createStaticMessage("Default exception strategy must not have expression attribute. It must accept any message."));
                }
            }
        }
    }

    private void applyDefaultIfNoObjectSerializerSet(DefaultMuleConfiguration configuration)
    {
        ObjectSerializer configuredSerializer = config.getDefaultObjectSerializer();
        if (configuredSerializer == null)
        {
            configuredSerializer = new JavaObjectSerializer();
            ((MuleContextAware) configuredSerializer).setMuleContext(muleContext);
            config.setDefaultObjectSerializer(configuredSerializer);
        }

        configuration.setDefaultObjectSerializer(configuredSerializer);
        if (muleContext instanceof DefaultMuleContext)
        {
            ((DefaultMuleContext) muleContext).setObjectSerializer(configuredSerializer);
        }
    }

    public Class<?> getObjectType()
    {
        return MuleConfiguration.class;
    }

    public boolean isSingleton()
    {
        return true;
    }

    public void setDefaultSynchronousEndpoints(boolean synchronous)
    {
        config.setDefaultSynchronousEndpoints(synchronous);
    }

    public void setDefaultResponseTimeout(int responseTimeout)
    {
        config.setDefaultResponseTimeout(responseTimeout);
    }

    public void setDefaultTransactionTimeout(int defaultTransactionTimeout)
    {
        config.setDefaultTransactionTimeout(defaultTransactionTimeout);
    }

    public void setShutdownTimeout(int shutdownTimeout)
    {
        config.setShutdownTimeout(shutdownTimeout);
    }

    public void setDefaultExceptionStrategyName(String defaultExceptionStrategyName)
    {
        config.setDefaultExceptionStrategyName(defaultExceptionStrategyName);
    }

    public void setUseExtendedTransformations(boolean resolveNonDirectTransformations)
    {
        config.setUseExtendedTransformations(resolveNonDirectTransformations);
    }

    public void setFlowEndingWithOneWayEndpointReturnsNull(boolean flowEndingWithOneWayEndpointReturnsNull)
    {
        config.setFlowEndingWithOneWayEndpointReturnsNull(flowEndingWithOneWayEndpointReturnsNull);
    }

    public void setEnricherPropagatesSessionVariableChanges(boolean enricherPropagatesSessionVariableChanges)
    {
        config.setEnricherPropagatesSessionVariableChanges(enricherPropagatesSessionVariableChanges);
    }

    public void setDefaultObjectSerializer(ObjectSerializer objectSerializer)
    {
        config.setDefaultObjectSerializer(objectSerializer);
    }

    public void setDefaultProcessingStrategy(ProcessingStrategy processingStrategy)
    {
        config.setDefaultProcessingStrategy(processingStrategy);
    }

    public void setExtensions(List<Object> extensions)
    {
        config.setExtensions(extensions);
    }

}
