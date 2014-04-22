/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher.log4j;

import org.mule.api.MuleRuntimeException;
import org.mule.config.i18n.CoreMessages;
import org.mule.module.launcher.DirectoryResourceLocator;
import org.mule.module.launcher.LocalResourceLocator;
import org.mule.module.launcher.application.ApplicationClassLoader;
import org.mule.module.launcher.artifact.ArtifactClassLoader;
import org.mule.module.launcher.artifact.ShutdownListener;
import org.mule.module.reboot.MuleContainerBootstrapUtils;
import org.mule.module.reboot.MuleContainerSystemClassLoader;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.log4j.Appender;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.DailyRollingFileAppender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Hierarchy;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.helpers.LogLog;
import org.apache.log4j.spi.LoggerRepository;
import org.apache.log4j.spi.RepositorySelector;
import org.apache.log4j.spi.RootLogger;
import org.apache.log4j.xml.DOMConfigurator;

public class ArtifactAwareRepositorySelector implements RepositorySelector
{
    protected static final String PATTERN_LAYOUT = "%-5p %d [%t] %c: %m%n";

    protected static final Integer NO_CCL_CLASSLOADER = 0;

    public static final String MULE_APP_LOG_FILE_TEMPLATE = "mule-app-%s.log";

    protected LoggerRepositoryCache cache = new LoggerRepositoryCache();

    // note that this is a direct log4j logger declaration, not a clogging one
    protected Logger logger = Logger.getLogger(getClass());

    protected final ThreadLocal<LoggerRepository> repositoryUnderConstruction = new ThreadLocal<LoggerRepository>();

    @Override
    public LoggerRepository getLoggerRepository()
    {
        final ClassLoader ccl = Thread.currentThread().getContextClassLoader();

        LoggerRepository repository = repositoryUnderConstruction.get();
        if (repository != null)
        {
            return repository;
        }

        repository = cache.getLoggerRepository(ccl);
        if (repository == null)
        {
            final RootLogger root = new RootLogger(Level.INFO);
            repository = new Hierarchy(root);

            repositoryUnderConstruction.set(repository);

            try
            {
                ConfigWatchDog configWatchDog = null;
                if (ccl instanceof ArtifactClassLoader)
                {
                    String logFileNamePatter;
                    if (ccl instanceof ApplicationClassLoader)
                    {
                        logFileNamePatter = MULE_APP_LOG_FILE_TEMPLATE;
                    }
                    else
                    {
                        logFileNamePatter = "mule-domain-%s.log";
                    }
                    configWatchDog = configureLoggerAndRetrieveWatchdog((ArtifactClassLoader) ccl, repository, root, configWatchDog, logFileNamePatter);
                }
                else
                {
                    // this is not an app init, use the top-level defaults
                    if (MuleContainerBootstrapUtils.getMuleConfDir() != null)
                    {
                        URL rootLogConfig = getLogConfig(new DirectoryResourceLocator(MuleContainerBootstrapUtils.getMuleConfDir().getAbsolutePath()));
                        configureFrom(rootLogConfig, repository);

                        // only start a watchdog for the Mule container class loader. Other class loaders
                        // (e.g. Jetty's WebAppClassLoader) should not start a watchdog
                        if (ccl instanceof MuleContainerSystemClassLoader)
                        {
                            configWatchDog = new ConfigWatchDog(ccl, rootLogConfig.getFile(), repository);
                            configWatchDog.setName("Mule.system.log4j.config.monitor");
                        }
                    }
                    else
                    {
                        addDefaultAppender(root, "mule-main.log", null);
                    }
                }

                final LoggerRepository previous = cache.storeLoggerRepository(ccl, repository);

                // Checks if repository was already initialized in a different thread
                if (previous != null)
                {
                    repository = previous;
                }
                else
                {
                    if (configWatchDog != null)
                    {
                        configWatchDog.start();
                    }
                }
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
            finally
            {
                repositoryUnderConstruction.remove();
            }
        }

        return repository;
    }

    private ConfigWatchDog configureLoggerAndRetrieveWatchdog(ArtifactClassLoader artifactClassLoader, LoggerRepository repository, RootLogger root, ConfigWatchDog configWatchDog, String logFileNameTemplate) throws IOException
    {
        URL artifactLogConfig = getArtifactLoggingConfig(artifactClassLoader);
        final String artifactName = artifactClassLoader.getArtifactName();
        if (artifactLogConfig == null)
        {
            addDefaultAppender(root, logFileNameTemplate, artifactName);
        }
        else
        {
            configureFrom(artifactLogConfig, repository);
            if (artifactLogConfig.toExternalForm().startsWith("file:"))
            {
                // if it's not a file, no sense in monitoring it for changes
                configWatchDog = new ConfigWatchDog(artifactClassLoader.getClassLoader(), artifactLogConfig.getFile(), repository);
                configWatchDog.setName(String.format("[%s].log4j.config.monitor", artifactName));
            }
            else
            {
                if (logger.isInfoEnabled())
                {
                    logger.info(String.format("Logging config %s is not an external file, will not be monitored for changes", artifactLogConfig));
                }
            }

            // If the artifact logging is configured using the global config file and there is no file appender for the artifact, then configure a default one
            if (MuleContainerBootstrapUtils.getMuleConfDir() != null && artifactLogConfig.toExternalForm().contains(MuleContainerBootstrapUtils.getMuleConfDir().getAbsolutePath()))
            {
                if (!hasFileAppender(root, artifactName))
                {
                    addDefaultAppender(root, logFileNameTemplate, artifactName);
                    removeConsoleAppenders(root);
                }
            }
        }
        return configWatchDog;
    }

    private void removeConsoleAppenders(RootLogger root)
    {
        Collection<Appender> appendersToRemove = new ArrayList<Appender>();
        Enumeration appenders = root.getAllAppenders();
        while (appenders.hasMoreElements())
        {
            Appender appender = (Appender) appenders.nextElement();
            if (appender instanceof ConsoleAppender)
            {
                appendersToRemove.add(appender);
            }
        }
        for(Appender appender : appendersToRemove)
        {
            root.removeAppender(appender);
        }
    }

    private boolean hasFileAppender(RootLogger root, String artifactName)
    {
        Enumeration appenders = root.getAllAppenders();
        while (appenders.hasMoreElements())
        {
            Appender appender = (Appender) appenders.nextElement();
            if (appender instanceof FileAppender)
            {
                if (((FileAppender) appender).getFile().contains(artifactName))
                {
                    return true;
                }
            }
        }
        return false;
    }

    private void addDefaultAppender(RootLogger root, String logFileNameTemplate, String artifactName) throws IOException
    {
        String logName = String.format(logFileNameTemplate, (artifactName != null ? artifactName : ""));
        File logDir = new File(MuleContainerBootstrapUtils.getMuleHome(), "logs");
        File logFile = new File(logDir, logName);
        DailyRollingFileAppender fileAppender = new DailyRollingFileAppender(new PatternLayout(PATTERN_LAYOUT), logFile.getAbsolutePath(), "'.'yyyy-MM-dd");
        fileAppender.setName("defaultFileAppender");
        fileAppender.setAppend(true);
        fileAppender.activateOptions();
        root.addAppender(fileAppender);
    }

    private URL getArtifactLoggingConfig(ArtifactClassLoader muleCL)
    {
        // Checks if there's an app-specific logging configuration available,
        // scope the lookup to this classloader only, as getResource() will delegate to parents
        // locate xml config first, fallback to properties format if not found
        URL appLogConfig = getLogConfig(muleCL);

        if (appLogConfig != null && logger.isInfoEnabled())
        {
            logger.info(String.format("Found logging config for application '%s' at '%s'", muleCL.getArtifactName(), appLogConfig));
        }

        return appLogConfig;
    }

    private URL getLogConfig(LocalResourceLocator localResourceLocator)
    {
        URL appLogConfig = localResourceLocator.findLocalResource("log4j.xml");

        if (appLogConfig == null)
        {
            appLogConfig = localResourceLocator.findLocalResource("log4j.properties");
        }
        return appLogConfig;
    }

    protected void configureFrom(URL url, LoggerRepository repository)
    {
        if (url.toExternalForm().endsWith(".xml"))
        {
            new DOMConfigurator().doConfigure(url, repository);
        }
        else
        {
            new PropertyConfigurator().doConfigure(url, repository);
        }
    }

    protected static class LoggerRepositoryCache
    {
        protected ConcurrentMap<Integer, LoggerRepository> repositories = new ConcurrentHashMap<Integer, LoggerRepository>();

        public LoggerRepository getLoggerRepository(ClassLoader classLoader)
        {
            return repositories.get(computeKey(classLoader));
        }

        public LoggerRepository storeLoggerRepository(ClassLoader classLoader, LoggerRepository repository)
        {
            return repositories.putIfAbsent(computeKey(classLoader), repository);
        }

        public void remove(ClassLoader classLoader)
        {
            repositories.remove(computeKey(classLoader));
        }

        protected Integer computeKey(ClassLoader classLoader)
        {
            return classLoader == null ? NO_CCL_CLASSLOADER : classLoader.hashCode();
        }
    }

    // TODO: MULE-7421 rewrite using a single-threaded scheduled executor and terminate on undeploy/redeploy
    // this is a modified and unified version from log4j to better fit Mule's app lifecycle
    protected class ConfigWatchDog extends Thread
    {
        protected LoggerRepository repository;
        protected File file;
        protected long lastModif = 0;
        protected boolean warnedAlready = false;
        protected volatile boolean interrupted = false;

        /**
         * The default delay between every file modification check, set to 60
         * seconds.
         */
        static final public long DEFAULT_DELAY = 60000;
        /**
         * The name of the file to observe  for changes.
         */
        protected String filename;

        /**
         * The delay to observe between every check. By default set {@link
         * #DEFAULT_DELAY}.
         */
        protected long delay = DEFAULT_DELAY;

        public ConfigWatchDog(final ClassLoader classLoader, String filename, LoggerRepository repository)
        {
            if (classLoader instanceof ArtifactClassLoader)
            {
                ((ArtifactClassLoader) classLoader).addShutdownListener(new ShutdownListener()
                {
                    @Override
                    public void execute()
                    {
                        final ClassLoader ccl = Thread.currentThread().getContextClassLoader();
                        ArtifactAwareRepositorySelector.this.cache.remove(ccl);
                        ConfigWatchDog.this.interrupt();
                    }
                });
            }
            else if (!(classLoader instanceof MuleContainerSystemClassLoader))
            {
                throw new MuleRuntimeException(CoreMessages.createStaticMessage("Can't create a ConfigWatchDog thread for " +
                                           "current class loader of type %s", classLoader.getClass().getName()));
            }

            this.filename = filename;
            this.file = new File(filename);
            this.lastModif = file.lastModified();
            setDaemon(true);
            this.repository = repository;
            this.delay = 10000; // 10 secs
        }

        public void doOnChange()
        {
            if (logger.isInfoEnabled())
            {
                logger.info("Reconfiguring logging from: " + filename);
            }
            if (filename.endsWith(".xml"))
            {
                new DOMConfigurator().doConfigure(filename, repository);
            }
            else
            {
                new PropertyConfigurator().doConfigure(filename, repository);
            }
        }

        /**
         * Set the delay to observe between each check of the file changes.
         */
        public void setDelay(long delay)
        {
            this.delay = delay;
        }

        protected void checkAndConfigure()
        {
            boolean fileExists;
            try
            {
                fileExists = file.exists();
            }
            catch (SecurityException e)
            {
                LogLog.warn("Was not allowed to read check file existence, file:[" + filename + "].");
                interrupted = true; // there is no point in continuing
                return;
            }

            if (fileExists)
            {
                long l = file.lastModified(); // this can also throw a SecurityException
                if (l > lastModif)
                {           // however, if we reached this point this
                    lastModif = l;              // is very unlikely.
                    doOnChange();
                    warnedAlready = false;
                }
            }
            else
            {
                if (!warnedAlready)
                {
                    LogLog.debug("[" + filename + "] does not exist.");
                    warnedAlready = true;
                }
            }
        }

        @Override
        public void run()
        {
            while (!interrupted)
            {
                try
                {
                    Thread.sleep(delay);
                }
                catch (InterruptedException e)
                {
                    interrupted = true;
                    Thread.currentThread().interrupt();
                    break;
                }
                checkAndConfigure();
            }
            if (logger.isDebugEnabled())
            {
                logger.debug(getName() + " terminated successfully");
            }
        }

    }
}

