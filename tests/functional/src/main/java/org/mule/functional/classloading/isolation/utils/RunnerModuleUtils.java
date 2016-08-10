/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.functional.classloading.isolation.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Utility class for runnner.
 */
public final class RunnerModuleUtils
{
    public static final String EXCLUDED_PROPERTIES_FILE = "excluded.properties";

    private RunnerModuleUtils()
    {
    }

    /**
     * Loads the {@link RunnerModuleUtils#EXCLUDED_PROPERTIES_FILE} file.
     *
     * @return a {@link Properties} loaded with the content of the file.
     * @throws IOException if the properties couldn't load the file.
     * @throws IllegalStateException if the file couldn't be found.
     */
    public static final Properties getExcludedProperties() throws IllegalStateException, IOException
    {
        try (InputStream excludedPropertiesUrl = RunnerModuleUtils.class.getClassLoader().getResourceAsStream(EXCLUDED_PROPERTIES_FILE))
        {
            if (excludedPropertiesUrl == null)
            {
                throw new IllegalStateException("Couldn't find file: " + EXCLUDED_PROPERTIES_FILE + " in classpath, at least one should be defined");
            }
            Properties excludedProperties = new Properties();
            excludedProperties.load(excludedPropertiesUrl);
            return excludedProperties;
        }
    }
}
