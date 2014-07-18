/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher.log4j2;

import java.net.URI;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.core.LifeCycle;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.xml.XmlConfigurationFactory;
import org.apache.logging.log4j.core.impl.ContextAnchor;
import org.apache.logging.log4j.core.selector.ContextSelector;
import org.apache.logging.log4j.spi.LoggerContextFactory;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * Implementation of {@link org.apache.logging.log4j.spi.LoggerContextFactory} which
 * acts as the bootstrap for mule's logging mechanism.
 * <p/>
 * It forces {@link org.mule.module.launcher.log4j2.ArtifactAwareContextSelector} as
 * the only selector, {@link org.apache.logging.log4j.core.config.xml.XmlConfigurationFactory}
 * as the only available {@link org.apache.logging.log4j.core.config.ConfigurationFactory},
 * and sets {@link org.mule.module.launcher.log4j2.AsyncLoggerExceptionHandler} as the
 * {@link com.lmax.disruptor.ExceptionHandler} for failing async loggers.
 * <p/>
 * Other than that, it's pretty much a copy paste of {@link org.apache.logging.log4j.core.impl.Log4jContextFactory},
 * due to that classes' lack of extensibility.
 * <p/>
 * By forcing {@link org.apache.logging.log4j.core.config.xml.XmlConfigurationFactory} as the only available
 * {@link org.apache.logging.log4j.core.config.ConfigurationFactory} we're disabling log4j2's ability to
 * take json and yaml configurations. This is so because those configuration factories
 * depend on versions of the jackson libraries which would cause conflict with the ones in mule.
 * TODO: Upgrade the jackson libraries bundled with mule so that this restriction can be lifted off
 *
 * @since 3.6.0
 */
public final class MuleLog4jContextFactory implements LoggerContextFactory
{

    private static final StatusLogger LOGGER = StatusLogger.getLogger();

    private static final String LOG_CONFIGURATION_FACTORY_PROPERTY = "log4j.configurationFactory";
    private static final String DEFAULT_LOG_CONFIGURATION_FACTORY = XmlConfigurationFactory.class.getName();
    private static final String ASYNC_LOGGER_EXCEPTION_HANDLER_PROPERTY = "AsyncLoggerConfig.ExceptionHandler";
    private static final String DEFAULT_ASYNC_LOGGER_EXCEPTION_HANLDER = AsyncLoggerExceptionHandler.class.getName();

    private final ContextSelector selector;

    /**
     * Initializes the ContextSelector.
     */
    public MuleLog4jContextFactory()
    {
        setupConfigurationFactory();
        setupAsyncLoggerExceptionHandler();
        selector = new ArtifactAwareContextSelector();
    }

    private void setupConfigurationFactory()
    {
        System.setProperty(LOG_CONFIGURATION_FACTORY_PROPERTY, DEFAULT_LOG_CONFIGURATION_FACTORY);
    }

    private void setupAsyncLoggerExceptionHandler()
    {
        String handler = System.getProperty(ASYNC_LOGGER_EXCEPTION_HANDLER_PROPERTY);
        if (StringUtils.isBlank(handler))
        {
            System.setProperty(ASYNC_LOGGER_EXCEPTION_HANDLER_PROPERTY, DEFAULT_ASYNC_LOGGER_EXCEPTION_HANLDER);
        }
    }

    /**
     * Loads the LoggerContext using the ContextSelector.
     *
     * @param fqcn            The fully qualified class name of the caller.
     * @param loader          The ClassLoader to use or null.
     * @param currentContext  If true returns the current Context, if false returns the Context appropriate
     *                        for the caller if a more appropriate Context can be determined.
     * @param externalContext An external context (such as a ServletContext) to be associated with the LoggerContext.
     * @return The LoggerContext.
     */
    @Override
    public LoggerContext getContext(final String fqcn, final ClassLoader loader, final Object externalContext,
                                    final boolean currentContext)
    {
        final LoggerContext ctx = selector.getContext(fqcn, loader, currentContext);
        ctx.setExternalContext(externalContext);
        if (ctx.getState() == LifeCycle.State.INITIALIZED)
        {
            ctx.start();
        }
        return ctx;
    }

    /**
     * Loads the LoggerContext using the ContextSelector.
     *
     * @param fqcn            The fully qualified class name of the caller.
     * @param loader          The ClassLoader to use or null.
     * @param externalContext An external context (such as a ServletContext) to be associated with the LoggerContext.
     * @param currentContext  If true returns the current Context, if false returns the Context appropriate
     *                        for the caller if a more appropriate Context can be determined.
     * @param source          The configuration source.
     * @return The LoggerContext.
     */
    public LoggerContext getContext(final String fqcn, final ClassLoader loader, final Object externalContext,
                                    final boolean currentContext, final ConfigurationSource source)
    {
        final LoggerContext ctx = selector.getContext(fqcn, loader, currentContext, null);
        if (externalContext != null && ctx.getExternalContext() == null)
        {
            ctx.setExternalContext(externalContext);
        }
        if (ctx.getState() == LifeCycle.State.INITIALIZED)
        {
            if (source != null)
            {
                ContextAnchor.THREAD_CONTEXT.set(ctx);
                final Configuration config = ConfigurationFactory.getInstance().getConfiguration(source);
                LOGGER.debug("Starting LoggerContext[name={}] from configuration {}", ctx.getName(), source);
                ctx.start(config);
                ContextAnchor.THREAD_CONTEXT.remove();
            }
            else
            {
                ctx.start();
            }
        }
        return ctx;
    }

    /**
     * Loads the LoggerContext using the ContextSelector.
     *
     * @param fqcn            The fully qualified class name of the caller.
     * @param loader          The ClassLoader to use or null.
     * @param externalContext An external context (such as a ServletContext) to be associated with the LoggerContext.
     * @param currentContext  If true returns the current Context, if false returns the Context appropriate
     *                        for the caller if a more appropriate Context can be determined.
     * @param configLocation  The location of the configuration for the LoggerContext.
     * @return The LoggerContext.
     */
    @Override
    public LoggerContext getContext(final String fqcn, final ClassLoader loader, final Object externalContext,
                                    final boolean currentContext, final URI configLocation, final String name)
    {
        final LoggerContext ctx = selector.getContext(fqcn, loader, currentContext, configLocation);
        if (externalContext != null && ctx.getExternalContext() == null)
        {
            ctx.setExternalContext(externalContext);
        }
        if (ctx.getState() == LifeCycle.State.INITIALIZED)
        {
            if (configLocation != null || name != null)
            {
                ContextAnchor.THREAD_CONTEXT.set(ctx);
                final Configuration config = ConfigurationFactory.getInstance().getConfiguration(name, configLocation);
                LOGGER.debug("Starting LoggerContext[name={}] from configuration at {}", ctx.getName(), configLocation);
                ctx.start(config);
                ContextAnchor.THREAD_CONTEXT.remove();
            }
            else
            {
                ctx.start();
            }
        }
        return ctx;
    }

    /**
     * Removes knowledge of a LoggerContext.
     *
     * @param context The context to remove.
     */
    @Override
    public void removeContext(final org.apache.logging.log4j.spi.LoggerContext context)
    {
        if (context instanceof LoggerContext)
        {
            selector.removeContext((LoggerContext) context);
        }
    }
}
