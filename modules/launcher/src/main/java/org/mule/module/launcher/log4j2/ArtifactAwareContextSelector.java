/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher.log4j2;

import static org.mule.config.i18n.MessageFactory.createStaticMessage;
import org.mule.api.MuleRuntimeException;
import org.mule.api.lifecycle.Disposable;
import org.mule.module.launcher.DirectoryResourceLocator;
import org.mule.module.launcher.LocalResourceLocator;
import org.mule.module.launcher.artifact.ArtifactClassLoader;
import org.mule.module.launcher.artifact.ShutdownListener;
import org.mule.module.reboot.MuleContainerBootstrapUtils;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.selector.ContextSelector;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * Implementation of {@link org.apache.logging.log4j.core.selector.ContextSelector}
 * which is used to implement log separation based on provided or current
 * {@link java.lang.ClassLoader}
 * <p/>
 * This component is responsible for managing the {@link org.apache.logging.log4j.core.LoggerContext}
 * that corresponds to each artifact (aka applications, domains, container), using its classloader as
 * an identifier. The same classloader always gets the same {@link org.apache.logging.log4j.core.LoggerContext}
 * <p/>
 * This component also overrides log4j2's default algorithm for locating configuration files, although it
 * does it in a way consistent with the replaced behavior:
 * <ul>
 * <li>A file called log4j2-test.xml is fetched from the corresponding search path</li>
 * <li>If log4j2-test.xml is not found, then log4j2.xml is attempted</li>
 * <li>If not found, a default configuration consisting of a single rolling file appender is used</li>
 * <li>The search path is derived from the artifact for which a logging context is being requested, following
 * a child first strategy (artifact - domain - container). Each artifact starts looking in the phase that makes
 * sense for it</li>
 * </ul>
 * <p/>
 * If the classloader is an artifact one, then it adds a {@link org.mule.module.launcher.artifact.ShutdownListener}
 * to destroy the logging context when the app is undeployed, preventing memory leaks.
 * <p/>
 * If mule is running in embedded mode, then all of this logic described above is discarded and it simply logs
 * to a file called mule-main.log
 *
 * @since 3.6.0
 */
class ArtifactAwareContextSelector implements ContextSelector, Disposable
{

    private static final StatusLogger logger = StatusLogger.getLogger();

    private LoggerContextCache cache = new LoggerContextCache(this, getClass().getClassLoader());


    ArtifactAwareContextSelector()
    {
    }

    @Override
    public LoggerContext getContext(String fqcn, ClassLoader loader, boolean currentContext)
    {
        return getContext(fqcn, loader, currentContext, null);
    }

    @Override
    public LoggerContext getContext(String fqcn, ClassLoader classLoader, boolean currentContext, URI configLocation)
    {
        return cache.getLoggerContext(resolveLoggerContextClassLoader(classLoader));
    }

    @Override
    public List<LoggerContext> getLoggerContexts()
    {
        return cache.getAllLoggerContexts();
    }

    @Override
    public void removeContext(LoggerContext context)
    {
        cache.remove(context);
    }

    /**
     * Given a {@code classLoader}  this method will resolve which is the {@code classLoader}
     * associated with the logger context to use for this {@code classLoader} .
     * <p/>
     * When the provided {@code classLoader}  is from an application or a domain it will return
     * the {@code classLoader}  associated with the logger context of the application or domain.
     * So far the artifact (domain or app) {@code classLoader}  will be resolved to it self.
     * <p/>
     * If the {@code classLoader} belongs to the container or any other {@code classLoader}  created from a
     * library running outside the context of an artifact then the system {@code classLoader}  will be used.
     *
     * @param classLoader {@link ClassLoader} running the code where the logging was done
     * @return the {@link ClassLoader} owner of the logger context
     */
    static ClassLoader resolveLoggerContextClassLoader(ClassLoader classLoader)
    {
        ClassLoader loggerClassLoader = classLoader == null ? Thread.currentThread().getContextClassLoader() : classLoader;
        if (!(loggerClassLoader instanceof ArtifactClassLoader))
        {
            loggerClassLoader = loggerClassLoader.getSystemClassLoader();
        }
        return loggerClassLoader;
    }

    @Override
    public void dispose()
    {
        cache.dispose();
    }

    private void destroyLoggersFor(ClassLoader classLoader)
    {
        cache.remove(classLoader);
    }

    private NewContextParameters resolveContextParameters(ClassLoader classLoader)
    {
        if (classLoader instanceof ArtifactClassLoader)
        {
            ArtifactClassLoader artifactClassLoader = (ArtifactClassLoader) classLoader;
            return new NewContextParameters(getArtifactLoggingConfig(artifactClassLoader), artifactClassLoader.getArtifactName());
        }
        else
        {
            // this is not an app init, use the top-level defaults
            if (MuleContainerBootstrapUtils.getMuleConfDir() != null)
            {
                return new NewContextParameters(
                        getLogConfig(new DirectoryResourceLocator(MuleContainerBootstrapUtils.getMuleConfDir().getAbsolutePath())),
                        classLoader.toString());
            }
        }

        return null;
    }

    LoggerContext buildContext(final ClassLoader classLoader)
    {
        NewContextParameters parameters = resolveContextParameters(classLoader);
        if (parameters == null)
        {
            return getDefaultContext();
        }

        MuleLoggerContext loggerContext = new MuleLoggerContext(parameters.contextName,
                                                                parameters.loggerConfigFile,
                                                                classLoader,
                                                                this,
                                                                isStandalone());

        if (classLoader instanceof ArtifactClassLoader)
        {
            final ArtifactClassLoader artifactClassLoader = (ArtifactClassLoader) classLoader;

            artifactClassLoader.addShutdownListener(new ShutdownListener()
            {
                @Override
                public void execute()
                {
                    destroyLoggersFor(resolveLoggerContextClassLoader(classLoader));
                }
            });
        }

        return loggerContext;
    }


    private URI getArtifactLoggingConfig(ArtifactClassLoader muleCL)
    {
        // Checks if there's an app-specific logging configuration available,
        // scope the lookup to this classloader only, as getResource() will delegate to parents
        // locate xml config first, fallback to properties format if not found
        URI appLogConfig = getLogConfig(muleCL);

        if (appLogConfig != null && logger.isInfoEnabled())
        {
            logger.info("Found logging config for application '{}' at '{}'", muleCL.getArtifactName(), appLogConfig);
        }

        return appLogConfig;
    }

    private URI getLogConfig(LocalResourceLocator localResourceLocator)
    {
        URL appLogConfig = localResourceLocator.findLocalResource("log4j2-test.xml");

        if (appLogConfig == null)
        {
            appLogConfig = localResourceLocator.findLocalResource("log4j2.xml");
        }

        if (appLogConfig == null)
        {
            File defaultConfigFile = new File(MuleContainerBootstrapUtils.getMuleHome(), "conf");
            defaultConfigFile = new File(defaultConfigFile, "log4j2.xml");

            try
            {
                appLogConfig = defaultConfigFile.toURI().toURL();
            }
            catch (MalformedURLException e)
            {
                throw new MuleRuntimeException(createStaticMessage("Could not locate log config in MULE_HOME"), e);
            }
        }

        try
        {
            return appLogConfig.toURI();
        }
        catch (URISyntaxException e)
        {
            throw new MuleRuntimeException(createStaticMessage("Could not read log file " + appLogConfig), e);
        }
    }

    private LoggerContext getDefaultContext()
    {
        return new MuleLoggerContext("Default", this, isStandalone());
    }

    private boolean isStandalone()
    {
        return MuleContainerBootstrapUtils.getMuleConfDir() != null;
    }

    private class NewContextParameters
    {

        private final URI loggerConfigFile;
        private final String contextName;

        private NewContextParameters(URI loggerConfigFile, String contextName)
        {
            this.loggerConfigFile = loggerConfigFile;
            this.contextName = contextName;
        }
    }
}
