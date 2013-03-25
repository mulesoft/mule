/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.launcher.log4j;

import org.mule.module.launcher.MuleApplicationClassLoader;
import org.mule.module.launcher.application.ApplicationClassLoader;
import org.mule.module.reboot.MuleContainerBootstrapUtils;
import org.mule.module.reboot.MuleContainerSystemClassLoader;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.log4j.DailyRollingFileAppender;
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

public class ApplicationAwareRepositorySelector implements RepositorySelector
{
    protected static final String PATTERN_LAYOUT = "%-5p %d [%t] %c: %m%n";

    protected static final Integer NO_CCL_CLASSLOADER = 0;

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
                if (ccl instanceof ApplicationClassLoader)
                {
                    ApplicationClassLoader muleCL = (ApplicationClassLoader) ccl;
                    URL appLogConfig = getAppLoggingConfig(muleCL);
                    final String appName = muleCL.getAppName();
                    if (appLogConfig == null)
                    {
                        // fallback to defaults
                        String logName = String.format("mule-app-%s.log", appName);
                        File logDir = new File(MuleContainerBootstrapUtils.getMuleHome(), "logs");
                        File logFile = new File(logDir, logName);
                        DailyRollingFileAppender fileAppender = new DailyRollingFileAppender(new PatternLayout(PATTERN_LAYOUT), logFile.getAbsolutePath(), "'.'yyyy-MM-dd");
                        fileAppender.setAppend(true);
                        fileAppender.activateOptions();
                        root.addAppender(fileAppender);
                    }
                    else
                    {
                        configureFrom(appLogConfig, repository);
                        if (appLogConfig.toExternalForm().startsWith("file:"))
                        {
                            // if it's not a file, no sense in monitoring it for changes
                            configWatchDog = new ConfigWatchDog(ccl, appLogConfig.getFile(), repository);
                            configWatchDog.setName(String.format("[%s].log4j.config.monitor", appName));
                        }
                        else
                        {
                            if (logger.isInfoEnabled())
                            {
                                logger.info(String.format("Logging config %s is not an external file, will not be monitored for changes", appLogConfig));
                            }
                        }
                    }
                }
                else
                {
                    // this is not an app init, use the top-level defaults
                    File defaultSystemLog = new File(MuleContainerBootstrapUtils.getMuleHome(), "conf/log4j.xml");
                    if (!defaultSystemLog.exists() && !defaultSystemLog.canRead())
                    {
                        defaultSystemLog = new File(MuleContainerBootstrapUtils.getMuleHome(), "conf/log4j.properties");
                    }
                    configureFrom(defaultSystemLog.toURL(), repository);

                    // only start a watchdog for the Mule container class loader. Other class loaders
                    // (e.g. Jetty's WebAppClassLoader) should not start a watchdog
                    if (ccl instanceof MuleContainerSystemClassLoader)
                    {
                        configWatchDog = new ConfigWatchDog(ccl, defaultSystemLog.getAbsolutePath(), repository);
                        configWatchDog.setName("Mule.system.log4j.config.monitor");
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

    private URL getAppLoggingConfig(ApplicationClassLoader muleCL)
    {
        // Checks if there's an app-specific logging configuration available,
        // scope the lookup to this classloader only, as getResource() will delegate to parents
        // locate xml config first, fallback to properties format if not found
        URL appLogConfig = muleCL.findResource("log4j.xml");

        if (appLogConfig == null)
        {
            appLogConfig = muleCL.findResource("log4j.properties");
        }

        if (appLogConfig != null && logger.isInfoEnabled())
        {
            logger.info(String.format("Found logging config for application '%s' at '%s'", muleCL.getAppName(), appLogConfig));
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

    // TODO rewrite using a single-threaded scheduled executor and terminate on undeploy/redeploy
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
            if (classLoader instanceof MuleApplicationClassLoader)
            {
                ((MuleApplicationClassLoader) classLoader).addShutdownListener(new MuleApplicationClassLoader.ShutdownListener()
                {
                    @Override
                    public void execute()
                    {
                        final ClassLoader ccl = Thread.currentThread().getContextClassLoader();
                        ApplicationAwareRepositorySelector.this.cache.remove(ccl);
                        interrupted = true;
                    }
                });
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

