/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring;

import static org.mule.runtime.core.api.config.MuleProperties.MULE_DEFAULT_PROCESSING_STRATEGY;
import static org.mule.runtime.core.internal.util.ProcessingStrategyUtils.parseProcessingStrategy;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.i18n.I18nMessageFactory;
import org.mule.runtime.core.DefaultMuleContext;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.api.config.ConfigurationExtension;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.exception.MessagingExceptionHandler;
import org.mule.runtime.core.api.exception.MessagingExceptionHandlerAcceptor;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategyFactory;
import org.mule.runtime.core.api.serialization.ObjectSerializer;
import org.mule.runtime.core.config.DefaultMuleConfiguration;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.dsl.api.component.AbstractAnnotatedObjectFactory;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.SmartFactoryBean;

/**
 * This class is a "SmartFactoryBean" which allows a few XML attributes to be set on the otherwise read-only MuleConfiguration. It
 * looks up the MuleConfiguration from the MuleContext and does some class-casting to be able to modify it. Note that this will
 * only work if the MuleContext has not yet been started, otherwise the modifications will be ignored (and warnings logged).
 */
// TODO MULE-9638 remove usage of SmartFactoryBean
public class MuleConfigurationConfigurator extends AbstractAnnotatedObjectFactory implements MuleContextAware, SmartFactoryBean {

  private MuleContext muleContext;

  // We instantiate DefaultMuleConfiguration to make sure we get the default values for
  // any properties not set by the user.
  private DefaultMuleConfiguration config = new DefaultMuleConfiguration();

  protected transient Logger logger = LoggerFactory.getLogger(MuleConfigurationConfigurator.class);

  @Override
  public void setMuleContext(MuleContext context) {
    this.muleContext = context;
  }

  @Override
  public boolean isEagerInit() {
    return true;
  }

  @Override
  public boolean isPrototype() {
    return false;
  }

  private void determineDefaultProcessingStrategy(DefaultMuleConfiguration defaultConfig) {
    if (config.getDefaultProcessingStrategyFactory() != null) {
      defaultConfig.setDefaultProcessingStrategyFactory(config.getDefaultProcessingStrategyFactory());
    } else {
      String processingStrategyFromSystemProperty = System.getProperty(MULE_DEFAULT_PROCESSING_STRATEGY);
      if (!StringUtils.isBlank(processingStrategyFromSystemProperty)) {
        defaultConfig.setDefaultProcessingStrategyFactory(parseProcessingStrategy(processingStrategyFromSystemProperty));
      }
    }
  }

  private void validateDefaultErrorHandler() {
    String defaultErrorHandler = config.getDefaultErrorHandlerName();
    if (defaultErrorHandler != null) {
      MessagingExceptionHandler messagingExceptionHandler = muleContext.getRegistry().lookupObject(defaultErrorHandler);
      if (messagingExceptionHandler == null) {
        throw new MuleRuntimeException(CoreMessages.createStaticMessage(String
            .format("No global error handler defined with name %s.", defaultErrorHandler)));
      }
      if (messagingExceptionHandler instanceof MessagingExceptionHandlerAcceptor) {
        MessagingExceptionHandlerAcceptor messagingExceptionHandlerAcceptor =
            (MessagingExceptionHandlerAcceptor) messagingExceptionHandler;
        if (!messagingExceptionHandlerAcceptor.acceptsAll()) {
          throw new MuleRuntimeException(CoreMessages
              .createStaticMessage("Default exception strategy must not have expression attribute. It must accept any message."));
        }
      }
    }
  }

  private void applyDefaultIfNoObjectSerializerSet(DefaultMuleConfiguration configuration) {
    ObjectSerializer configuredSerializer = config.getDefaultObjectSerializer();

    if (configuredSerializer != null) {
      configuration.setDefaultObjectSerializer(configuredSerializer);
      if (muleContext instanceof DefaultMuleContext) {
        ((DefaultMuleContext) muleContext).setObjectSerializer(configuredSerializer);
      }
    }
  }

  @Override
  public Class<?> getObjectType() {
    return MuleConfiguration.class;
  }

  @Override
  public boolean isSingleton() {
    return true;
  }

  public void setDefaultSynchronousEndpoints(boolean synchronous) {
    config.setDefaultSynchronousEndpoints(synchronous);
  }

  public void setDefaultResponseTimeout(int responseTimeout) {
    config.setDefaultResponseTimeout(responseTimeout);
  }

  public void setDefaultTransactionTimeout(int defaultTransactionTimeout) {
    config.setDefaultTransactionTimeout(defaultTransactionTimeout);
  }

  public void setShutdownTimeout(int shutdownTimeout) {
    config.setShutdownTimeout(shutdownTimeout);
  }

  public void setDefaultErrorHandlerName(String defaultErrorHandlerName) {
    config.setDefaultErrorHandlerName(defaultErrorHandlerName);
  }

  public void setDefaultObjectSerializer(ObjectSerializer objectSerializer) {
    config.setDefaultObjectSerializer(objectSerializer);
  }

  public void setDefaultProcessingStrategy(ProcessingStrategyFactory processingStrategyFactory) {
    config.setDefaultProcessingStrategyFactory(processingStrategyFactory);
  }

  public void setMaxQueueTransactionFilesSize(int queueTransactionFilesSizeInMegabytes) {
    config.setMaxQueueTransactionFilesSize(queueTransactionFilesSizeInMegabytes);
  }

  public void setExtensions(List<ConfigurationExtension> extensions) {
    config.addExtensions(extensions);
  }

  @Override
  public Object doGetObject() throws Exception {
    MuleConfiguration configuration = muleContext.getConfiguration();
    if (configuration instanceof DefaultMuleConfiguration) {
      DefaultMuleConfiguration defaultConfig = (DefaultMuleConfiguration) configuration;
      defaultConfig.setDefaultResponseTimeout(config.getDefaultResponseTimeout());
      defaultConfig.setDefaultTransactionTimeout(config.getDefaultTransactionTimeout());
      defaultConfig.setShutdownTimeout(config.getShutdownTimeout());
      defaultConfig.setDefaultErrorHandlerName(config.getDefaultErrorHandlerName());
      defaultConfig.addExtensions(config.getExtensions());
      defaultConfig.setMaxQueueTransactionFilesSize(config.getMaxQueueTransactionFilesSizeInMegabytes());
      determineDefaultProcessingStrategy(defaultConfig);
      validateDefaultErrorHandler();
      applyDefaultIfNoObjectSerializerSet(defaultConfig);

      return configuration;
    } else {
      throw new ConfigurationException(I18nMessageFactory
          .createStaticMessage("Unable to set properties on read-only MuleConfiguration: " + configuration.getClass()));
    }
  }
}
