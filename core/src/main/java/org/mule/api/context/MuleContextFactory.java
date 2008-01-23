/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.api.context;

import org.mule.api.MuleContext;
import org.mule.api.config.ConfigurationBuilder;
import org.mule.api.config.ConfigurationException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.config.builders.DefaultsConfigurationBuilder;

import java.util.List;

/**
 * A {@link CopyOfMuleContextFactory} is used to create instances of
 * {@link MuleContext}. The instances of {@link MuleContext} returned by this
 * factory are initialised but not started.
 */
public interface MuleContextFactory
{

    /**
     * Returns an existing instance of {@link MuleContext} is one exists, otherwise a
     * new {@link MuleContext} instance is created with defaults.
     * 
     * @return
     * @throws InitialisationException
     * @throws ConfigurationException
     * @see {@link DefaultsConfigurationBuilder}
     */
    MuleContext createMuleContext() throws InitialisationException, ConfigurationException;

    /**
     * Creates a new MuleContext using the {@link MuleContextBuilder} provided.
     * 
     * @param configurationBuilder
     * @return
     * @throws InitialisationException
     * @throws ConfigurationException
     */
    public MuleContext createMuleContext(MuleContextBuilder muleContextBuilder)
        throws InitialisationException, ConfigurationException;

    /**
     * Creates a new MuleContext using the given configurationBuilder
     * 
     * @param configurationBuilder
     * @return
     * @throws InitialisationException
     * @throws ConfigurationException
     */
    public MuleContext createMuleContext(ConfigurationBuilder configurationBuilder)
        throws InitialisationException, ConfigurationException;

    /**
     * Creates a new MuleContext using the {@link MuleContextBuilder} provided and
     * configures it with the list of configuration builder and c onfigures it with
     * configurationBuilder
     * 
     * @param configurationBuilder
     * @return
     * @throws InitialisationException
     * @throws ConfigurationException
     */
    public MuleContext createMuleContext(ConfigurationBuilder configurationBuilder,
                                         MuleContextBuilder muleContextBuilder)
        throws InitialisationException, ConfigurationException;

    /**
     * Creates a new MuleContext using the {@link MuleContextBuilder} provided and
     * configures it with the list of configuration builders. Configuration builders
     * will be invoked in the same or as provided in the List.
     * 
     * @param configurationBuilder
     * @return
     * @throws InitialisationException
     * @throws ConfigurationException
     */
    public MuleContext createMuleContext(List configurationBuilders, MuleContextBuilder muleContextBuilder)
        throws InitialisationException, ConfigurationException;

}
