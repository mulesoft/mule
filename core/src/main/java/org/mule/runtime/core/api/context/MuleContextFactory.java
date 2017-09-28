/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.context;

import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.api.context.notification.MuleContextListener;

import java.util.List;

/**
 * A MuleContextFactory is used to create instances of {@link MuleContext}. The instances of {@link MuleContext} returned by this
 * factory are initialised but not started.
 */
public interface MuleContextFactory {

  /**
   * Creates a new MuleContext using the given configurationBuilder
   * 
   * @param configurationBuilder
   * @throws InitialisationException
   * @throws ConfigurationException
   */
  MuleContext createMuleContext(ConfigurationBuilder... configurationBuilders)
      throws InitialisationException, ConfigurationException;

  /**
   * Creates a new MuleContext using the {@link MuleContextBuilder} provided and configures it with the list of configuration
   * builder and c onfigures it with configurationBuilder
   * 
   * @param configurationBuilder
   * @throws InitialisationException
   * @throws ConfigurationException
   */
  MuleContext createMuleContext(ConfigurationBuilder configurationBuilder, MuleContextBuilder muleContextBuilder)
      throws InitialisationException, ConfigurationException;

  /**
   * Creates a new MuleContext using the {@link MuleContextBuilder} provided and configures it with the list of configuration
   * builders. Configuration builders will be invoked in the same or as provided in the List.
   * 
   * @throws InitialisationException
   * @throws ConfigurationException
   */
  MuleContext createMuleContext(List<ConfigurationBuilder> configurationBuilders, MuleContextBuilder muleContextBuilder)
      throws InitialisationException, ConfigurationException;

  /**
   * Adds a listener to be notified of each event related to the creation of a new {@link MuleContext}
   *
   * @param listener listener to add. Must be non null.
   */
  void addListener(MuleContextListener listener);

  /**
   * Removes a listener from the factory.
   *
   *
   * @param listener the listener to remove. After this call, the listener won't receive any notifications from this factory.
   * @return true if the listener was registered in the factory, false otherwise
   */
  boolean removeListener(MuleContextListener listener);
}
