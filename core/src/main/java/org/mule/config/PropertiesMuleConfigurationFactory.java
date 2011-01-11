/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.MuleServer;
import org.mule.api.config.MuleProperties;
import org.mule.util.ClassUtils;
import org.mule.util.FilenameUtils;
import org.mule.util.NumberUtils;

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

    private void initializeFromProperties(DefaultMuleConfiguration configuration)
    {
        initializeFromProperties(configuration, this.properties);
    }
    
    public static void initializeFromProperties(DefaultMuleConfiguration configuration, Map properties)
    {
        String p;

        p = (String) properties.get(MuleProperties.SYSTEM_PROPERTY_PREFIX + "encoding");
        if (p != null)
        {
            configuration.setDefaultEncoding(p);
        }
        p = (String) properties.get(MuleProperties.SYSTEM_PROPERTY_PREFIX + "endpoints.synchronous");
        if (p != null)
        {
            configuration.setDefaultSynchronousEndpoints(BooleanUtils.toBoolean(p));
        }
        p = (String) properties.get(MuleProperties.SYSTEM_PROPERTY_PREFIX + "systemModelType");
        if (p != null)
        {
            configuration.setSystemModelType(p);
        }
        p = (String) properties.get(MuleProperties.SYSTEM_PROPERTY_PREFIX + "timeout.synchronous");
        if (p != null)
        {
            configuration.setDefaultResponseTimeout(NumberUtils.toInt(p));
        }
        p = (String) properties.get(MuleProperties.SYSTEM_PROPERTY_PREFIX + "timeout.transaction");
        if (p != null)
        {
            configuration.setDefaultTransactionTimeout(NumberUtils.toInt(p));
        }
        p = (String) properties.get(MuleProperties.SYSTEM_PROPERTY_PREFIX + "workingDirectory");
        if (p != null)
        {
            configuration.setWorkingDirectory(p);
        }
        p = (String) properties.get(MuleProperties.SYSTEM_PROPERTY_PREFIX + "clientMode");
        if (p != null)
        {
            configuration.setClientMode(BooleanUtils.toBoolean(p));
        }
        p = (String) properties.get(MuleProperties.SYSTEM_PROPERTY_PREFIX + "serverId");
        if (p != null)
        {
            configuration.setId(p);
        }
        p = (String) properties.get(MuleProperties.SYSTEM_PROPERTY_PREFIX + "clusterId");
        if (p != null)
        {
            configuration.setClusterId(p);
        }
        p = (String) properties.get(MuleProperties.SYSTEM_PROPERTY_PREFIX + "domainId");
        if (p != null)
        {
            configuration.setDomainId(p);
        }
        p = (String) properties.get(MuleProperties.SYSTEM_PROPERTY_PREFIX + "message.cacheBytes");
        if (p != null)
        {
            configuration.setCacheMessageAsBytes(BooleanUtils.toBoolean(p));
        }
        p = (String) properties.get(MuleProperties.SYSTEM_PROPERTY_PREFIX + "message.cacheOriginal");
        if (p != null)
        {
            configuration.setCacheMessageOriginalPayload(BooleanUtils.toBoolean(p));
        }
        p = (String) properties.get(MuleProperties.SYSTEM_PROPERTY_PREFIX + "streaming.enable");
        if (p != null)
        {
            configuration.setEnableStreaming(BooleanUtils.toBoolean(p));
        }
        p = (String) properties.get(MuleProperties.SYSTEM_PROPERTY_PREFIX + "transform.autoWrap");
        if (p != null)
        {
            configuration.setAutoWrapMessageAwareTransform(BooleanUtils.toBoolean(p));
        }
    }
}
