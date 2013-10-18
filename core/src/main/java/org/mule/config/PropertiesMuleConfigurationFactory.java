/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config;

import org.mule.MuleServer;
import org.mule.api.config.MuleConfiguration;
import org.mule.util.BeanUtils;
import org.mule.util.ClassUtils;
import org.mule.util.FilenameUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class PropertiesMuleConfigurationFactory
{

    private static Log logger = LogFactory.getLog(PropertiesMuleConfigurationFactory.class);
    
    private Properties properties;

    public static String getMuleAppConfiguration(String muleConfig)
    {
        String directory = FilenameUtils.getFullPath(muleConfig);
        String muleAppConfiguration = directory + MuleServer.DEFAULT_APP_CONFIGURATION;
        return muleAppConfiguration;
    }
    
    public PropertiesMuleConfigurationFactory(String muleAppConfiguration)
    {
        URL muleAppURL = ClassUtils.getResource(muleAppConfiguration, getClass());
        if (muleAppURL != null)
        {
            this.properties = new Properties();
            InputStream inputStream = null;
            try
            {
                inputStream = muleAppURL.openStream();
                this.properties.load(inputStream);
            }
            catch (FileNotFoundException e)
            {
                logger.debug(e);
            }
            catch (IOException e)
            {
                logger.debug(e);
            }
            finally
            {
                IOUtils.closeQuietly(inputStream);
            }
        }
    }

    public DefaultMuleConfiguration createConfiguration()
    {
        DefaultMuleConfiguration configuration = new DefaultMuleConfiguration();
        if (this.properties != null)
        {
            this.initializeFromProperties(configuration); 
        }
        return configuration;
    }

    private void initializeFromProperties(MuleConfiguration configuration)
    {
        initializeFromProperties(configuration, this.properties);
    }
    
    public static void initializeFromProperties(MuleConfiguration configuration, Map properties)
    {
        for (Object entryObject : properties.entrySet())
        {
            Entry entry = (Entry) entryObject;
            String key = (String) entry.getKey();
            String value = (String) entry.getValue();

            if (key.startsWith("sys."))
            {
                String systemProperty = key.substring(4);
                System.setProperty(systemProperty, value);
            }
            else if (key.startsWith("mule.config."))
            {
                String configProperty = key.substring(12);
                try
                {
                    BeanUtils.setProperty(configuration, configProperty, value);
                }
                catch (IllegalAccessException e)
                {
                    logger.error(e);
                }
                catch (InvocationTargetException e)
                {
                    logger.error(e);
                }
            }
        }
    }
}
