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

import org.mule.RegistryContext;
import org.mule.config.ConfigurationBuilder;
import org.mule.config.ConfigurationException;
import org.mule.config.ReaderResource;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.config.spring.MuleApplicationContext;
import org.mule.umo.UMOManagementContext;
import org.mule.util.IOUtils;
import org.mule.util.PropertiesUtils;
import org.mule.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * <code>MuleXmlConfigurationBuilder</code> Enables Mule to be loaded from as Spring
 * context. Multiple configuration files can be loaded from this builder (specified
 * as a comma-separated list) the files can be String Beans documents or Mule Xml
 * Documents or a combination of both. Any Mule Xml documents will be transformed at
 * run-time in to Spring Bean documents before the bean definitions are loaded. Make
 * sure that the DTD definitions for each of the document types are declared in the
 * documents.
 */
public class MuleXmlConfigurationBuilder implements ConfigurationBuilder
{
    private String defaultConfigResource = "default-mule-config.xml";

    private boolean used = false;
    /**
     * Will configure a UMOManager based on the configurations made available through
     * Readers.
     *
     * @param configResources an array of Readers
     * @return A configured UMOManager
     * @throws org.mule.config.ConfigurationException
     */
    public UMOManagementContext configure(ReaderResource[] configResources) throws ConfigurationException
    {
        // just in case it's ever implemented
        return configure(configResources, null);
    }

    /**
     * Will configure a UMOManager based on the configurations made available through
     * Readers.
     *
     * @param configResources an array of Readers
     * @return A configured UMOManager
     * @throws org.mule.config.ConfigurationException
     */
    public UMOManagementContext configure(ReaderResource[] configResources, Properties startupProperties)
        throws ConfigurationException
    {
        throw new UnsupportedOperationException("Not implemented");
    }

    public UMOManagementContext configure(String configResources) throws ConfigurationException
    {
        return configure(configResources, null);
    }

    public UMOManagementContext configure(String configResource, String startupPropertiesFile)
        throws ConfigurationException
    {
        // Load startup properties if any.
        if (StringUtils.isNotBlank(startupPropertiesFile))
        {
            try
            {
                startupPropertiesFile = StringUtils.trimToEmpty(startupPropertiesFile);
                Properties startupProperties = PropertiesUtils.loadProperties(startupPropertiesFile,
                    getClass());
                //TODO RM* URGENT How do we handle this: ((MuleManager)managementContext).addProperties(startupProperties);
            }
            catch (IOException e)
            {
                throw new ConfigurationException(new Message(Messages.FAILED_TO_START_X,
                    "Mule server from builder"), e);
            }
        }

        if (configResource == null)
        {
            throw new ConfigurationException(new Message(Messages.X_IS_NULL, "Configuration Resource"));
        }
        String[] resources = org.springframework.util.StringUtils.tokenizeToStringArray(configResource, ",;",
            true, true);

        MuleApplicationContext root = null;
        if(defaultConfigResource!=null)
        {
           // new MuleApplicationContext(defaultConfigResource);
        }

        String[] all = new String[resources.length + 1];
        all[0] = defaultConfigResource;
        System.arraycopy(resources, 0, all, 1, resources.length);

        used = true;
        MuleApplicationContext context;

//        if(root!=null)
//        {
//            context = new MuleApplicationContext(root, resources);
//        }
//        else
//        {
            context = new MuleApplicationContext(all);
        //}
//        try
//        {
//            if (System.getProperty(MuleProperties.MULE_START_AFTER_CONFIG_SYSTEM_PROPERTY, "true")
//                .equalsIgnoreCase("true"))
//            {
//                managementContext.start();
//            }
//        }
//        catch (UMOException e)
//        {
//            throw new ConfigurationException(new Message(Messages.FAILED_TO_START_X,
//                "Mule server from builder"), e);
//        }

        try
        {
            UMOManagementContext mc = context.getManagementContext();
            mc.start();

            RegistryContext.getConfiguration().setConfigResources(resources);
            
            return mc;
        }
        catch (Exception e)
        {
            throw new ConfigurationException(new Message(Messages.FAILED_TO_START_X,
                "Mule server from builder"), e);
        }
    }

    /**
     * Indicate whether this ConfigurationBulder has been configured yet
     *
     * @return <code>true</code> if this ConfigurationBulder has been configured
     */
    public boolean isConfigured()
    {
        return used;
    }

    /**
     * Attempt to load a configuration resource from the file system, classpath, or
     * as a URL, in that order.
     *
     * @param configResource Mule configuration resources
     * @return an InputStream to the resource
     * @throws ConfigurationException if the resource could not be loaded by any
     *             means
     */
    protected InputStream loadConfig(String configResource) throws ConfigurationException
    {
        InputStream is;
        try
        {
            is = IOUtils.getResourceAsStream(configResource, getClass());
        }
        catch (IOException e)
        {
            throw new ConfigurationException(new Message(Messages.CANT_LOAD_X_FROM_CLASSPATH_FILE,
                configResource), e);
        }

        if (is != null)
        {
            return is;
        }
        else
        {
            throw new ConfigurationException(new Message(Messages.CANT_LOAD_X_FROM_CLASSPATH_FILE,
                configResource));
        }
    }


    public String getDefaultConfigResource()
    {
        return defaultConfigResource;
    }

    public void setDefaultConfigResource(String defaultConfigResource)
    {
        this.defaultConfigResource = defaultConfigResource;
    }
}
