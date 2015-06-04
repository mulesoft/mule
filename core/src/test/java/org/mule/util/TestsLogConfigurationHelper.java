/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;

/**
 * A helper class for configuring logging
 * on test cases
 *
 * @since 3.7.0
 */
public class TestsLogConfigurationHelper
{

    private static final ThreadLocal<String> LOGGING_CONFIG_CACHE = new ThreadLocal<>();
    private static final String LOG4J_CONFIGURATION_FILE = "log4j.configurationFile";
    private static final String NULL_CONFIG_FILE = "NULL_CONFIG_FILE";

    /**
     * Looks for a logging config file on the directory tree
     * where the binaries for {@code testClass} resides.
     * If found, that logging config file is used, otherwise
     * Log4j2 is allowed to search for the config file using
     * its own logic.
     * <p/>
     * A file called {@code log4j2-test.xml} will be searched first.
     * If not found, will try with {@code log4j2.xml}.
     * <p/>
     * In either case, this method ends by reconfiguring all
     * loggers in the current {@link LoggerContext}
     *
     * @param testClass the class holding the tests to be executed
     */
    public static void configureLoggingForTest(Class<?> testClass)
    {
        String logConfigPath = LOGGING_CONFIG_CACHE.get();

        if (logConfigPath == null)
        {
            logConfigPath = findLogConfigurationPath(testClass);
        }

        forceConfigFile(logConfigPath);
    }

    /**
     * Clears the configurations generated by
     * {@link #configureLoggingForTest(Class)}
     */
    public static void clearLoggingConfig()
    {
        LOGGING_CONFIG_CACHE.remove();
        System.clearProperty(LOG4J_CONFIGURATION_FILE);
    }

    private static String findLogConfigurationPath(Class<?> testClass)
    {
        String encodedFolder = testClass.getClassLoader().getResource("").getPath().toString();

        String folderPath ;
        try {
            folderPath = URLDecoder.decode(encodedFolder, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            folderPath = encodedFolder;
        }

        File folder = new File(folderPath);

        if (folder != null && "target".equals(folder.getParentFile().getName()))
        {
            folder = folder.getParentFile();
        }

        File logConfigFile = FileUtils.findFileByName(folder, "log4j2-test.xml", true);
        if (logConfigFile == null)
        {
            logConfigFile = FileUtils.findFileByName(folder, "log4j2.xml", true);
        }

        String logConfigPath = logConfigFile != null && !logConfigFile.isDirectory() ? logConfigFile.getAbsolutePath() : NULL_CONFIG_FILE;
        LOGGING_CONFIG_CACHE.set(logConfigPath);
        return logConfigPath;
    }

    private static void forceConfigFile(String logConfigFile)
    {
        if (!NULL_CONFIG_FILE.equals(logConfigFile))
        {
            System.setProperty(LOG4J_CONFIGURATION_FILE, logConfigFile);
        }

        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        ctx.reconfigure();
    }
}
