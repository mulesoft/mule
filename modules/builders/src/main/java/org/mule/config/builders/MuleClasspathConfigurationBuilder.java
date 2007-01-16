/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.builders;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.config.ConfigurationException;
import org.mule.config.ReaderResource;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.umo.manager.UMOManager;
import org.mule.util.StringUtils;

/**
 * <code>MuleClasspathConfigurationBuilder</code> can be used to configure a
 * MuleManager based on the configuration files on the classpath. the default config
 * resource name is <b>mule-config.xml</b> but this can be overrided by passing a
 * config resourse name or a list of resource names (comma separated) to the
 * configure method.
 * 
 * @deprecated The functionality of this configuration builder (loading resources
 *             from the classpath) is now available in the standard
 *             MuleXmlConfigurationBuilder. If you are using this builder, please
 *             verify whether your configuration will work with
 *             org.mule.config.builders.MuleXmlConfigurationBuilder as this class is
 *             deprecated and is soon to be removed.
 */
public class MuleClasspathConfigurationBuilder extends MuleXmlConfigurationBuilder
{
    /**
     * logger used by this class
     */
    protected static final Log logger = LogFactory.getLog(MuleClasspathConfigurationBuilder.class);

    public static final String MULE_CONFIGURATION_RESOURCE = "mule-config.xml";

    public MuleClasspathConfigurationBuilder() throws ConfigurationException
    {
        super();
    }

    /**
     * Will configure a UMOManager based on the configuration file(s) provided.
     * 
     * @param configResources can be null or a comma separated resources name string
     *            that will be used to search the classpath. The default is
     *            mule-config.xml.
     * @return A configured UMOManager
     * @throws org.mule.config.ConfigurationException if the configResources param is
     *             invalid or the configurations fail to load
     */
    public UMOManager configure(String configResources, String startupPropertiesFile)
        throws ConfigurationException
    {
        if (StringUtils.isBlank(configResources))
        {
            configResources = MULE_CONFIGURATION_RESOURCE;
        }

        URL url = null;
        List list = new ArrayList();
        String[] resString;
        int i = 0;

        try
        {
            resString = StringUtils.splitAndTrim(configResources, ",");
            for (i = 0; i < resString.length; i++)
            {
                url = Thread.currentThread().getContextClassLoader().getResource(resString[i]);
                if (url == null) break;
                list.add(new ReaderResource(url.toExternalForm(), new InputStreamReader(url.openStream())));
            }
        }
        catch (IOException ioe)
        {
            throw new ConfigurationException(new Message(Messages.FAILED_LOAD_X, "Config: "
                                                                                 + ObjectUtils.toString(url,
                                                                                     "null")), ioe);
        }

        if (list.size() != resString.length)
        {
            throw new ConfigurationException(new Message(Messages.FAILED_LOAD_X,
                "Not all resources specified loaded: " + resString[i]));
        }

        ReaderResource[] resources = new ReaderResource[list.size()];
        resources = (ReaderResource[])list.toArray(resources);
        configure(resources, null);
        return manager;
    }
}
