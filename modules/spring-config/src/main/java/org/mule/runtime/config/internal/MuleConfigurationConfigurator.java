/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.util.MuleSystemProperties.GRACEFUL_SHUTDOWN_DEFAULT_TIMEOUT;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_TIME_SUPPLIER;

import static java.lang.Long.parseLong;
import static java.lang.System.getProperty;
import static java.lang.System.getenv;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.serialization.ObjectSerializer;
import org.mule.runtime.api.time.TimeSupplier;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.api.config.ConfigurationExtension;
import org.mule.runtime.core.api.config.DefaultMuleConfiguration;
import org.mule.runtime.core.api.config.DynamicConfigExpiration;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.internal.config.ExpressionCorrelationIdGenerator;
import org.mule.runtime.core.internal.context.DefaultMuleContext;
import org.mule.runtime.dsl.api.component.AbstractComponentFactory;

import java.util.List;
import java.util.OptionalLong;

import javax.inject.Inject;

import org.springframework.beans.factory.SmartFactoryBean;

/**
 * This class is a "SmartFactoryBean" which allows a few XML attributes to be set on the otherwise read-only MuleConfiguration. It
 * looks up the MuleConfiguration from the MuleContext and does some class-casting to be able to modify it. Note that this will
 * only work if the MuleContext has not yet been started, otherwise the modifications will be ignored (and warnings logged).
 */
// TODO MULE-9638 remove usage of SmartFactoryBean
public class MuleConfigurationConfigurator extends AbstractComponentFactory<MuleConfiguration>
    implements SmartFactoryBean<MuleConfiguration> {

  @Inject
  private MuleContext muleContext;

  @Inject
  private Registry registry;

  // We instantiate DefaultMuleConfiguration to make sure we get the default values for
  // any properties not set by the user.
  private final DefaultMuleConfiguration config = new DefaultMuleConfiguration();

  private boolean explicitDynamicConfigExpiration;

  private OptionalLong shutdownTimeout = OptionalLong.empty();

  @Override
  public boolean isEagerInit() {
    return true;
  }

  @Override
  public boolean isPrototype() {
    return false;
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

  public void setDefaultResponseTimeout(int responseTimeout) {
    config.setDefaultResponseTimeout(responseTimeout);
  }

  public void setDefaultTransactionTimeout(int defaultTransactionTimeout) {
    config.setDefaultTransactionTimeout(defaultTransactionTimeout);
  }

  public void setShutdownTimeout(long shutdownTimeout) {
    this.shutdownTimeout = OptionalLong.of(shutdownTimeout);
  }

  public void setDefaultErrorHandlerName(String defaultErrorHandlerName) {
    config.setDefaultErrorHandlerName(defaultErrorHandlerName);
  }

  public void setDefaultObjectSerializer(ObjectSerializer objectSerializer) {
    config.setDefaultObjectSerializer(objectSerializer);
  }

  public void setMaxQueueTransactionFilesSize(int queueTransactionFilesSizeInMegabytes) {
    config.setMaxQueueTransactionFilesSize(queueTransactionFilesSizeInMegabytes);
  }

  public void setDynamicConfigExpiration(DynamicConfigExpiration dynamicConfigExpiration) {
    explicitDynamicConfigExpiration = true;
    config.setDynamicConfigExpiration(dynamicConfigExpiration);
  }

  public void setInheritIterableRepeatability(String inheritIterableRepeatability) {
    config.setInheritIterableRepeatability(inheritIterableRepeatability);
  }

  public void setCorrelationIdGeneratorExpression(String correlationIdGeneratorExpression) {
    config.setDefaultCorrelationIdGenerator(new ExpressionCorrelationIdGenerator(muleContext, correlationIdGeneratorExpression));
  }

  public void setExtensions(List<ConfigurationExtension> extensions) {
    config.addExtensions(extensions);
  }

  @Override
  public MuleConfiguration doGetObject() throws Exception {
    MuleConfiguration configuration = muleContext.getConfiguration();
    if (configuration instanceof DefaultMuleConfiguration) {
      DefaultMuleConfiguration defaultConfig = (DefaultMuleConfiguration) configuration;

      defaultConfig.setDefaultResponseTimeout(config.getDefaultResponseTimeout());
      defaultConfig.setDefaultTransactionTimeout(config.getDefaultTransactionTimeout());
      defaultConfig.setShutdownTimeout(shutdownTimeout
          .orElseGet(() -> {
            final String defaultValue = getProperty(GRACEFUL_SHUTDOWN_DEFAULT_TIMEOUT,
                                                    getenv(GRACEFUL_SHUTDOWN_DEFAULT_TIMEOUT.toUpperCase()
                                                        .replaceAll("\\.", "_")));

            if (defaultValue == null) {
              return 5000L;
            }

            return parseLong(defaultValue);
          }));
      defaultConfig.setDefaultErrorHandlerName(config.getDefaultErrorHandlerName());
      defaultConfig.addExtensions(config.getExtensions());
      defaultConfig.setMaxQueueTransactionFilesSize(config.getMaxQueueTransactionFilesSizeInMegabytes());
      defaultConfig.setDynamicConfigExpiration(resolveDynamicConfigExpiration());
      defaultConfig.setInheritIterableRepeatability(config.isInheritIterableRepeatability());
      config.getDefaultCorrelationIdGenerator().ifPresent(generator -> defaultConfig.setDefaultCorrelationIdGenerator(generator));
      applyDefaultIfNoObjectSerializerSet(defaultConfig);

      return configuration;
    } else {
      throw new ConfigurationException(createStaticMessage("Unable to set properties on read-only MuleConfiguration: "
          + configuration.getClass()));
    }
  }

  // This has to be done because the mule context object supplier has to be
  // used as a default if not dynamic configuration expiration is set in the app.
  // But this is accessible only after the mule context is started
  // (after the defaults for this class are resolved)
  private DynamicConfigExpiration resolveDynamicConfigExpiration() {
    if (explicitDynamicConfigExpiration) {
      return config.getDynamicConfigExpiration();
    }

    return registry.lookupByName(OBJECT_TIME_SUPPLIER)
        .map(ts -> DynamicConfigExpiration.getDefault((TimeSupplier) ts)).orElse(config.getDynamicConfigExpiration());
  }

  public void setMuleContext(MuleContext muleContext) {
    this.muleContext = muleContext;
  }
  
  public void setRegistry(Registry registry) {
    this.registry = registry;
  }
}
