/*
 * $Id: PlexusContainerContext.java 2179 2006-06-04 22:51:52Z holger $
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.extras.plexus;

import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.embed.Embedder;
import org.mule.config.ConfigurationException;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.impl.container.AbstractContainerContext;
import org.mule.impl.container.ContainerKeyPair;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.lifecycle.RecoverableException;
import org.mule.umo.manager.ContainerException;
import org.mule.umo.manager.ObjectNotFoundException;
import org.mule.util.FileUtils;

import java.io.Reader;
import java.net.URL;

/**
 * <code>PlexusContainerContext</code> integrate the plexus container with
 * Mule so that Mule objects can be constructed using Plexus-managed objects
 * 
 * @author Brian Topping
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision: 2179 $
 */
public class PlexusContainerContext extends AbstractContainerContext
{
    protected Embedder container;
    protected String configFile;

    public PlexusContainerContext()
    {
        this(new Embedder());
    }

    public PlexusContainerContext(Embedder container)
    {
        super("plexus");
        this.container = container;
    }

    public Object getComponent(Object key) throws ObjectNotFoundException
    {
        if (key == null) {
            throw new ObjectNotFoundException("Component not found for null key");
        }
        if(key instanceof ContainerKeyPair) {
            key = ((ContainerKeyPair)key).getKey();
        }
        try {
            String compKey = (key instanceof Class ? ((Class) key).getName() : key.toString());
            return container.lookup(compKey);
        } catch (ComponentLookupException e) {
            throw new ObjectNotFoundException("could not load component", e);
        }
    }

    public void configure(Reader configuration) throws ContainerException
    {
        try {
            container.setConfiguration(configuration);
            container.start();
        } catch (Exception e) {
            throw new ContainerException(new Message(Messages.FAILED_TO_CONFIGURE_CONTAINER), e);
        }
    }

    public String getConfigFile()
    {
        return configFile;
    }

    /**
     * @param configFile The configFile to set.
     */
    public void setConfigFile(String configFile) throws ConfigurationException
    {
        this.configFile = configFile;
    }

    public void initialise() throws InitialisationException, RecoverableException
    {
        if (configFile == null) {
            return;
        }
        try {
            URL url = FileUtils.getResource(configFile, getClass());
            if (url == null) {
                throw new ConfigurationException(new Message(Messages.CANT_LOAD_X_FROM_CLASSPATH_FILE, configFile));
            }
            container.setConfiguration(url);
            container.start();
        } catch (Exception e) {
            throw new InitialisationException(new Message(Messages.FAILED_TO_CREATE_X_WITH_X,
                                                          "Plexus container",
                                                          this.configFile), this);
        }
    }

    public void dispose()
    {
        if (container != null) {
            try {
                container.stop();
            } catch (Exception e) {
                logger.info("Plexus container", e);
            }
        }
    }
}
