/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) Cubis Limited. All rights reserved.
 * http://www.cubis.co.uk
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.extras.plexus;

import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.embed.Embedder;
import org.mule.config.ConfigurationException;
import org.mule.umo.model.ComponentNotFoundException;
import org.mule.umo.model.ComponentResolverException;
import org.mule.umo.model.UMOContainerContext;
import org.mule.util.Utility;

import java.io.Reader;
import java.net.URL;
import java.util.Map;
/**
 * <code>PlexusContainerContext</code> integrate the plexus container with Mule so that Mule
 * objects can be constructed using Plexus-managed objects
 *
 * @author Brian Topping
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public class PlexusContainerContext implements UMOContainerContext
{
    protected Embedder container;
    protected URL configFile;

    public PlexusContainerContext()
    {
        this(new Embedder());
    }

    public PlexusContainerContext(Embedder container)
    {
        this.container = container;
    }

    public Object getComponent(Object key) throws ComponentNotFoundException
    {
        if (key == null)
        {
            throw new ComponentNotFoundException("Component not found for null key");
        }
        try
        {
            String compKey = (key instanceof Class ? ((Class)key).getName() : key.toString());
            return container.lookup(compKey);
        } catch (ComponentLookupException e)
        {
            throw new ComponentNotFoundException("could not load component", e);
        }
    }

    public void configure(Reader configuration, Map configurationProperties) throws ComponentResolverException
    {
        try
        {
            container.setConfiguration(configuration);
            container.start();
        } catch (Exception e)
        {
            throw new ComponentResolverException("problem configuring and starting container", e);
        }
    }

    public URL getConfigFile()
    {
        return configFile;
    }

    public void setConfigFile(URL configFile) throws ConfigurationException
    {
        try
        {
            this.configFile = configFile;
            container.setConfiguration(configFile);
            container.start();
        } catch (Exception e)
        {
            throw new ConfigurationException("Failed to initialise plexus container using URL: " + this.configFile);
        }
    }

    /**
     * @param configFile The configFile to set.
     */
    public void setConfigFile(String configFile) throws ConfigurationException
    {
        URL url = Utility.getResource(configFile, getClass());
        if (url == null)
        {
            throw new ConfigurationException("Failed to load config from file or classpath: " + configFile);
        }
        setConfigFile(url);
    }
}
