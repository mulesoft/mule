/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.context;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static org.apache.commons.lang3.exception.ExceptionUtils.getMessage;
import static org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace;
import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.APP;

import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.api.config.DefaultMuleConfiguration;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.config.builders.SimpleConfigurationBuilder;
import org.mule.runtime.core.api.context.notification.MuleContextListener;
import org.mule.runtime.core.internal.config.builders.AutoConfigurationBuilder;
import org.mule.runtime.core.internal.context.DefaultMuleContextBuilder;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation that uses {@link DefaultMuleContextBuilder} to build new {@link MuleContext} instances.
 */
public class DefaultMuleContextFactory implements MuleContextFactory {

  protected static final Logger logger = LoggerFactory.getLogger(DefaultMuleContextFactory.class);

  private List<MuleContextListener> listeners = new LinkedList<>();

  /**
   * Creates a MuleContext using a default MuleContextBuilder
   */
  @Override
  public MuleContext createMuleContext(ConfigurationBuilder... configurationBuilders)
      throws InitialisationException, ConfigurationException {
    return createMuleContext(asList(configurationBuilders), MuleContextBuilder.builder(APP));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MuleContext createMuleContext(final List<ConfigurationBuilder> configurationBuilders,
                                       MuleContextBuilder muleContextBuilder)
      throws InitialisationException, ConfigurationException {
    return doCreateMuleContext(muleContextBuilder, new ContextConfigurator() {

      @Override
      public void configure(MuleContext muleContext) throws ConfigurationException {
        // Configure
        for (ConfigurationBuilder configBuilder : configurationBuilders) {
          configBuilder.configure(muleContext);
        }
      }
    });
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MuleContext createMuleContext(final ConfigurationBuilder configurationBuilder, MuleContextBuilder muleContextBuilder)
      throws InitialisationException, ConfigurationException {
    return doCreateMuleContext(muleContextBuilder, new ContextConfigurator() {

      @Override
      public void configure(MuleContext muleContext) throws ConfigurationException {
        configurationBuilder.configure(muleContext);
      }
    });
  }

  // Additional Factory methods provided by this implementation.

  /**
   * Creates a new {@link MuleContext} instance from the resource provided. Implementations of {@link MuleContextFactory} can
   * either use a default {@link ConfigurationBuilder} to implement this, or do some auto-detection to determine the
   * {@link ConfigurationBuilder} that should be used.
   *
   * @param resource comma seperated list of configuration resources.
   * @throws InitialisationException
   * @throws ConfigurationException
   */
  public MuleContext createMuleContext(String resource) throws InitialisationException, ConfigurationException {
    return createMuleContext(resource, null);
  }

  /**
   * Creates a new {@link MuleContext} instance from the resource provided. Implementations of {@link MuleContextFactory} can
   * either use a default {@link ConfigurationBuilder} to implement this, or do some auto-detection to determine the
   * {@link ConfigurationBuilder} that should be used. Properties if provided are used to replace "property placeholder" value in
   * configuration files.
   */
  public MuleContext createMuleContext(final String configResources, final Map<String, Object> properties)
      throws InitialisationException, ConfigurationException {
    return doCreateMuleContext(MuleContextBuilder.builder(APP), new ContextConfigurator() {

      @Override
      public void configure(MuleContext muleContext) throws ConfigurationException {
        // Configure with startup properties
        if (properties != null && !properties.isEmpty()) {
          new SimpleConfigurationBuilder(properties).configure(muleContext);
        }

        // Automatically resolve Configuration to be used and delegate configuration
        // to it.
        new AutoConfigurationBuilder(configResources, emptyMap(), APP).configure(muleContext);
      }
    });
  }

  /**
   * Creates a new MuleContext using the given configurationBuilder. Properties if provided are used to replace "property
   * placeholder" value in configuration files.
   */
  public MuleContext createMuleContext(ConfigurationBuilder configurationBuilder, Map<String, Object> properties)
      throws InitialisationException, ConfigurationException {
    return createMuleContext(configurationBuilder, properties, new DefaultMuleConfiguration());
  }

  /**
   * Creates a new MuleContext using the given configurationBuilder. Properties if provided are used to replace "property
   * placeholder" value in configuration files.
   */
  public MuleContext createMuleContext(List<ConfigurationBuilder> configurationBuilders, Map<String, Object> properties)
      throws InitialisationException, ConfigurationException {
    return createMuleContext(configurationBuilders, properties, new DefaultMuleConfiguration());
  }

  /**
   * Creates a new MuleContext using the given configurationBuilder and configuration. Properties if provided are used to replace
   * "property placeholder" value in configuration files.
   */
  public MuleContext createMuleContext(final ConfigurationBuilder configurationBuilder, final Map<String, Object> properties,
                                       MuleConfiguration configuration)
      throws InitialisationException, ConfigurationException {
    return createMuleContext(Collections.singletonList(configurationBuilder), properties, configuration);
  }

  /**
   * Creates a new MuleContext using the given configurationBuilder and configuration. Properties if provided are used to replace
   * "property placeholder" value in configuration files.
   */
  public MuleContext createMuleContext(final List<ConfigurationBuilder> configurationBuilders,
                                       final Map<String, Object> properties,
                                       MuleConfiguration configuration)
      throws InitialisationException, ConfigurationException {
    // Create MuleContext
    MuleContextBuilder contextBuilder = MuleContextBuilder.builder(APP);
    contextBuilder.setMuleConfiguration(configuration);
    return doCreateMuleContext(contextBuilder, new ContextConfigurator() {

      @Override
      public void configure(MuleContext muleContext) throws ConfigurationException {
        // Configure with startup properties
        if (properties != null && !properties.isEmpty()) {
          new SimpleConfigurationBuilder(properties).configure(muleContext);
        }

        // Configure with configurationBuilder
        for (ConfigurationBuilder configurationBuilder : configurationBuilders) {
          configurationBuilder.configure(muleContext);
        }
      }
    });
  }

  private MuleContext doCreateMuleContext(MuleContextBuilder muleContextBuilder, ContextConfigurator configurator)
      throws InitialisationException, ConfigurationException {
    MuleContext muleContext = buildMuleContext(muleContextBuilder);
    listeners.forEach(l -> l.onCreation(muleContext));

    try {
      configurator.configure(muleContext);
      muleContext.initialise();
    } catch (ConfigurationException e) {
      if (muleContext != null && !muleContext.isDisposed()) {
        try {
          muleContext.dispose();
        } catch (Exception e1) {
          logger.warn("Can not dispose context. " + getMessage(e1));
          if (logger.isDebugEnabled()) {
            logger.debug("Can not dispose context. " + getStackTrace(e1));
          }
        }
      }
      throw e;
    }
    return muleContext;
  }

  protected MuleContext buildMuleContext(MuleContextBuilder muleContextBuilder) {
    muleContextBuilder.setListeners(listeners);
    return muleContextBuilder.buildMuleContext();
  }

  @Override
  public void addListener(MuleContextListener listener) {
    listeners.add(listener);
  }

  @Override
  public boolean removeListener(MuleContextListener listener) {
    return listeners.remove(listener);
  }

  private abstract class ContextConfigurator {

    public abstract void configure(MuleContext muleContext) throws ConfigurationException;
  }
}
