/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extras.picocontainer;

import java.io.Reader;
import java.io.StringReader;

import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.impl.container.AbstractContainerContext;
import org.mule.impl.container.ContainerKeyPair;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.lifecycle.RecoverableException;
import org.mule.umo.manager.ContainerException;
import org.mule.umo.manager.ObjectNotFoundException;
import org.mule.util.ClassUtils;
import org.mule.util.IOUtils;
import org.nanocontainer.integrationkit.ContainerBuilder;
import org.nanocontainer.integrationkit.PicoCompositionException;
import org.nanocontainer.script.ScriptedContainerBuilderFactory;
import org.picocontainer.MutablePicoContainer;
import org.picocontainer.defaults.SimpleReference;

/**
 * <code>PicoContainerContext</code> is a Pico Context that can expose
 * pico-managed components for use in the Mule framework.
 *
 * @author <a href="mailto:antonio.lopez@4clerks.com">Antonio Lopez</a>
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class PicoContainerContext extends AbstractContainerContext
{
    public static final String CONFIGEXTENSION = "CONFIG";

    private String extension = ScriptedContainerBuilderFactory.XML;
    /**
     * The url of the config file to use
     */
    protected String configFile;

    /**
     * the pico container that manages the components
     */
    private MutablePicoContainer container;

    public PicoContainerContext()
    {
        super("pico");
    }

    public String getExtension()
    {
        return extension;
    }

    public void setExtension(String extension)
    {
        this.extension = extension;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.mule.model.UMOContainerContext#getComponent(java.lang.Object)
     */
    public Object getComponent(Object key) throws ObjectNotFoundException
    {
        if (container == null) {
            throw new IllegalStateException("Pico container has not been set");
        }
        if (key == null) {
            throw new ObjectNotFoundException("Component not found for null key");
        }

        if(key instanceof ContainerKeyPair) {
            key = ((ContainerKeyPair)key).getKey();
        }

        Object component = null;
        if (key instanceof String) {
            try {
                Class keyClass = ClassUtils.loadClass((String) key, getClass());
                component = container.getComponentInstance(keyClass);
            } catch (ClassNotFoundException e) {
                component = container.getComponentInstance(key);
            }
        } else {
            component = container.getComponentInstance(key);
        }

        if (component == null) {
            throw new ObjectNotFoundException("Component not found for key: " + key.toString());
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

    }

    public void configure(Reader configuration) throws ContainerException
    {
        String className = ScriptedContainerBuilderFactory.getBuilderClassName(extension);
        doConfigure(configuration, className);
    }

    protected void doConfigure(Reader configReader, String builderClassName) throws ContainerException
    {
        org.picocontainer.defaults.ObjectReference containerRef = new SimpleReference();
        org.picocontainer.defaults.ObjectReference parentContainerRef = new SimpleReference();
        ScriptedContainerBuilderFactory scriptedContainerBuilderFactory = null;
        try {
            scriptedContainerBuilderFactory = new ScriptedContainerBuilderFactory(configReader,
                                                                                  builderClassName,
                                                                                  Thread.currentThread()
                                                                                        .getContextClassLoader());
        } catch (ClassNotFoundException e) {
            throw new ContainerException(new Message(Messages.FAILED_TO_CONFIGURE_CONTAINER), e);
        }

        ContainerBuilder builder = scriptedContainerBuilderFactory.getContainerBuilder();
        builder.buildContainer(containerRef, parentContainerRef, null, false);
        setContainer((MutablePicoContainer) containerRef.get());
    }

    private String getBuilderClassName(String scriptName)
    {
        String extension = scriptName.substring(scriptName.lastIndexOf('.'));
        return ScriptedContainerBuilderFactory.getBuilderClassName(extension);
    }

    public void initialise() throws InitialisationException, RecoverableException
    {
        if (configFile == null) {
            return;
        }
        try {
            String builderClassName = getBuilderClassName(configFile);
            String configString = IOUtils.getResourceAsString(configFile, getClass());
            StringReader configReader = new StringReader(configString);
            doConfigure(configReader, builderClassName);

        } catch (Exception e) {
            throw new PicoCompositionException(e);
        }
    }

    public void dispose()
    {
        if (container != null) {
            container.dispose();
        }
    }
}
