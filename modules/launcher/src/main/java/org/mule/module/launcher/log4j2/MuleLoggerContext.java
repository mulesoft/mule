/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher.log4j2;

import java.net.URI;

import org.apache.logging.log4j.core.LoggerContext;

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
 *
 * @since 3.6.0
 */
class MuleLoggerContext extends LoggerContext
{

    private final ClassLoader ownerClassLoader;
    private final URI configFile;
    private final boolean standlone;


    /**
     * Constructor taking only a name.
     *
     * @param name The context name.
     */
    public MuleLoggerContext(String name, boolean standalone)
    {
        this(name, null, null, standalone);
    }

    /**
     * Constructor taking a name and a configuration URI.
     *
     * @param name       The context name.
     * @param configLocn The location of the configuration as a URI.
     */
    MuleLoggerContext(String name, URI configLocn, ClassLoader ownerClassLoader, boolean standalone)
    {
        super(name, null, configLocn);
        this.ownerClassLoader = ownerClassLoader;
        configFile = configLocn;
        this.standlone = standalone;
    }

    @Override
    public synchronized void reconfigure()
    {
        super.reconfigure();
        new LoggerContextConfigurer().configure(this);
    }

    protected ClassLoader getOwnerClassLoader()
    {
        return ownerClassLoader;
    }

    protected URI getConfigFile()
    {
        return configFile;
    }

    protected boolean isStandlone()
    {
        return standlone;
    }
}
