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
 *
 */
package org.mule.extras.picocontainer;

import org.mule.umo.model.ComponentNotFoundException;
import org.mule.umo.model.UMOContainerContext;
import org.mule.util.ClassHelper;
import org.mule.util.Utility;
import org.nanocontainer.integrationkit.ContainerBuilder;
import org.nanocontainer.integrationkit.PicoCompositionException;
import org.nanocontainer.script.ScriptedContainerBuilderFactory;
import org.picocontainer.MutablePicoContainer;
import org.picocontainer.defaults.SimpleReference;

import java.io.StringReader;

/**
 * <code>PicoContainerContext</code> is a Pico Context that can expose pico-managed
 * components for use in the Mule framework.
 *
 * @author <a href="mailto:antonio.lopez@4clerks.com">Antonio Lopez</a>
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public class PicoContainerContext implements UMOContainerContext
{
    /**
     * The url of the config file to use
     */
    protected String configFile;

    /**
     * the pico container that manages the components
     */
    private MutablePicoContainer container;

    /* (non-Javadoc)
     * @see org.mule.model.UMOContainerContext#getComponent(java.lang.Object)
     */
    public Object getComponent(Object key) throws ComponentNotFoundException
    {
        if (container == null)
        {
            throw new IllegalStateException("Pico container has not been set");
        }
        if (key == null)
        {
            throw new ComponentNotFoundException("Component not found for null key");
        }
        Object component = null;
        if (key instanceof String)
        {
            try
            {
                Class keyClass = ClassHelper.loadClass((String) key, getClass());
                component = container.getComponentInstance(keyClass);
            } catch (ClassNotFoundException e)
            {
                component = container.getComponentInstance(key);
            }
        } else
        {
            component = container.getComponentInstance(key);
        }

        if (component == null)
        {
            throw new ComponentNotFoundException("Component not found for key: " + key.toString());
        }
        return component;
    }

    /**
     * @return Returns the container.
     */
    public MutablePicoContainer getContainer()
    {
        return container;
    }

    /**
     * @param container The container to set.
     */
    public void setContainer(MutablePicoContainer container)
    {
        this.container = container;
    }

    /**
     * The config file can be a resource on the classpath on a file system.
     *
     * @param configFile The configFile to set.
     */
    public void setConfigFile(String configFile) throws PicoCompositionException
    {
        this.configFile = configFile;
        try
        {
            org.picocontainer.defaults.ObjectReference containerRef = new SimpleReference();
            org.picocontainer.defaults.ObjectReference parentContainerRef = new SimpleReference();

            String builderClassName = getBuilderClassName(configFile);
            String configString = Utility.loadResourceAsString(configFile, getClass());

            ScriptedContainerBuilderFactory
                    scriptedContainerBuilderFactory =
                    new ScriptedContainerBuilderFactory(new StringReader(configString), builderClassName, Thread.currentThread().getContextClassLoader());

            ContainerBuilder builder = scriptedContainerBuilderFactory.getContainerBuilder();


            builder.buildContainer(containerRef, parentContainerRef, null, false);
            setContainer((MutablePicoContainer) containerRef.get());

        } catch (Exception e)
        {
            throw new PicoCompositionException(e);
        }
    }

    private String getBuilderClassName(String scriptName)
    {
        String extension = scriptName.substring(scriptName.lastIndexOf('.'));
        return ScriptedContainerBuilderFactory.getBuilderClassName(extension);
    }
}
