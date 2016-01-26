/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher.log4j2;

import org.mule.logging.LogConfigChangeSubject;
import org.mule.module.launcher.application.ApplicationClassLoader;
import org.mule.module.launcher.artifact.ArtifactClassLoader;

import java.beans.PropertyChangeListener;
import java.net.URI;

import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.selector.ContextSelector;
import org.apache.logging.log4j.message.MessageFactory;

/**
 * Subclass of {@link org.apache.logging.log4j.core.LoggerContext}
 * which adds some information about the mule artifact being logged.
 * <p/>
 * The most important function of this class though is to override the
 * {@link #reconfigure()} method to to its inherited purpose plus
 * invoking {@link org.mule.module.launcher.log4j2.LoggerContextConfigurer#configure(MuleLoggerContext)}.
 * <p/>
 * The {@link org.mule.module.launcher.log4j2.LoggerContextConfigurer} needs to be invoked here so
 * that it's invoked each time the configuration is reloaded.
 * <p/>
 * This class must not hold any reference to a {@link java.lang.ClassLoader} since otherwise
 * {@link org.apache.logging.log4j.core.Logger} instances held on static fields will make
 * that class loader GC unreachable
 *
 * @since 3.6.0
 */
class MuleLoggerContext extends LoggerContext implements LogConfigChangeSubject
{

    private final URI configFile;
    private final boolean standlone;
    private final ContextSelector contextSelector;
    private final boolean artifactClassloader;
    private final boolean applicationClassloader;
    private final String artifactName;
    private final int ownerClassLoaderHash;

    MuleLoggerContext(String name, ContextSelector contextSelector, boolean standalone)
    {
        this(name, null, null, contextSelector, standalone);
    }

    MuleLoggerContext(String name, URI configLocn, ClassLoader ownerClassLoader, ContextSelector contextSelector, boolean standalone)
    {
        super(name, null, configLocn);
        configFile = configLocn;
        this.contextSelector = contextSelector;
        this.standlone = standalone;
        ownerClassLoaderHash = ownerClassLoader != null ? ownerClassLoader.hashCode() : getClass().getClassLoader().getSystemClassLoader().hashCode();

        if (ownerClassLoader instanceof ArtifactClassLoader)
        {
            artifactClassloader = true;
            artifactName = ((ArtifactClassLoader) ownerClassLoader).getArtifactName();
            applicationClassloader = ownerClassLoader instanceof ApplicationClassLoader;
        }
        else
        {
            artifactClassloader = false;
            applicationClassloader = false;
            artifactName = null;
        }
    }

    @Override
    public synchronized void reconfigure()
    {
        super.reconfigure();
        applyContainerConfiguration();
    }

    private void applyContainerConfiguration()
    {
        new LoggerContextConfigurer().configure(this);
    }

    /**
     * This is workaround for log4j2 issue
     * <a href="https://issues.apache.org/jira/browse/LOG4J2-998">
     * LOG4J2-998</a>. When we upgrade to a version which includes
     * that fix then there will no longer be a need
     * for this method since {@link DispatchingLogger}
     * will be able to override {@link Logger#updateConfiguration(Configuration)}
     * {@inheritDoc}
     */
    @Override
    public void updateLoggers(Configuration config)
    {
        applyContainerConfiguration();
        super.updateLoggers(config);
        for (Logger logger : getLoggers())
        {
            if (logger instanceof DispatchingLogger)
            {
                ((DispatchingLogger) logger).updateConfiguration(config);
            }
        }
    }

    @Override
    public void registerLogConfigChangeListener(PropertyChangeListener logConfigChangeListener)
    {
        addPropertyChangeListener(logConfigChangeListener);
    }

    @Override
    public void unregisterLogConfigChangeListener(PropertyChangeListener logConfigChangeListener)
    {
        removePropertyChangeListener(logConfigChangeListener);
    }

    /**
     * Override to return a {@link DispatchingLogger}
     * instead of a simple logger
     * {@inheritDoc}
     *
     * @return a {@link DispatchingLogger}
     */
    @Override
    protected Logger newInstance(LoggerContext ctx, final String name, final MessageFactory messageFactory)
    {

        return new DispatchingLogger(super.newInstance(ctx, name, messageFactory), ownerClassLoaderHash, this, contextSelector, messageFactory)
        {
            // force the name due to log4j2's cyclic constructor dependencies
            // aren't a friend of the wrapper pattern
            @Override
            public String getName()
            {
                return name;
            }
        };
    }

    protected URI getConfigFile()
    {
        return configFile;
    }

    protected boolean isStandlone()
    {
        return standlone;
    }

    protected boolean isArtifactClassloader()
    {
        return artifactClassloader;
    }

    protected boolean isApplicationClassloader()
    {
        return applicationClassloader;
    }

    protected String getArtifactName()
    {
        return artifactName;
    }
}
