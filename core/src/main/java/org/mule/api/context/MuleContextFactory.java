/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.context;

import org.mule.api.MuleContext;
import org.mule.api.config.ConfigurationBuilder;
import org.mule.api.config.ConfigurationException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.config.builders.DefaultsConfigurationBuilder;

import java.util.List;

/**
 * A MuleContextFactory is used to create instances of {@link MuleContext}. The instances of 
 * {@link MuleContext} returned by this factory are initialised but not started.
 */
public interface MuleContextFactory
{

    /**
     * Returns an existing instance of {@link MuleContext} is one exists, otherwise a
     * new {@link MuleContext} instance is created with defaults.
     * 
     * @throws InitialisationException
     * @throws ConfigurationException
     * @see DefaultsConfigurationBuilder
     */
    MuleContext createMuleContext() throws InitialisationException, ConfigurationException;

    /**
     * Creates a new MuleContext using the {@link MuleContextBuilder} provided.
     * 
     * @throws InitialisationException
     * @throws ConfigurationException
     */
    MuleContext createMuleContext(MuleContextBuilder muleContextBuilder)
        throws InitialisationException, ConfigurationException;

    /**
     * Creates a new MuleContext using the given configurationBuilder
     * 
     * @param configurationBuilder
     * @throws InitialisationException
     * @throws ConfigurationException
     */
    MuleContext createMuleContext(ConfigurationBuilder configurationBuilder)
        throws InitialisationException, ConfigurationException;

    /**
     * Creates a new MuleContext using the {@link MuleContextBuilder} provided and
     * configures it with the list of configuration builder and c onfigures it with
     * configurationBuilder
     * 
     * @param configurationBuilder
     * @throws InitialisationException
     * @throws ConfigurationException
     */
    MuleContext createMuleContext(ConfigurationBuilder configurationBuilder,
                                  MuleContextBuilder muleContextBuilder)
        throws InitialisationException, ConfigurationException;

    /**
     * Creates a new MuleContext using the {@link MuleContextBuilder} provided and
     * configures it with the list of configuration builders. Configuration builders
     * will be invoked in the same or as provided in the List.
     * 
     * @throws InitialisationException
     * @throws ConfigurationException
     */
    MuleContext createMuleContext(List<ConfigurationBuilder> configurationBuilders, MuleContextBuilder muleContextBuilder)
        throws InitialisationException, ConfigurationException;

}
