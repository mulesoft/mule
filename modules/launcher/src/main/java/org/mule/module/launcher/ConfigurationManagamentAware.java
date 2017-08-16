/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher;

import static java.io.File.separator;
import static org.mule.util.FileUtils.newFile;

import org.mule.api.MuleRuntimeException;
import org.mule.config.i18n.CoreMessages;
import org.mule.config.i18n.Message;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

public class ConfigurationManagamentAware
{
    private static final String CONFIGURATION_MANAGEMENT_FILE_NAME = "configuration-management.properties";

    protected Properties manageConfigurationManagementPersistence(String workingDirectory, Properties configurationManagementProperties) throws IOException
    {
        String configurationManagementPath = workingDirectory + separator + "configuration-management";
        initConfigurationManagementDirectory(configurationManagementPath);
        if (configurationManagementProperties != null)
        {
            persistConfigurationManagementFile(configurationManagementPath, configurationManagementProperties);
        }
        else
        {
            return getConfigurationManagementProperties(configurationManagementPath);
        }
        
        return configurationManagementProperties; 
    }
    
    private Properties getConfigurationManagementProperties(String configurationManagementPath) throws IOException
    {
        File configFile = new File(configurationManagementPath + separator + CONFIGURATION_MANAGEMENT_FILE_NAME);
        Properties props = new Properties();
        
        if (!configFile.exists())
        {
            return props;
        }
        
        FileReader reader = new FileReader(configFile);
         
        props.load(reader);
        
        return props;
    }

    private void initConfigurationManagementDirectory(String  configurationManagementPath)
    {
        
        File configurationManagementDirectory = newFile(configurationManagementPath);
        if (!configurationManagementDirectory.exists())
        {
            createConfigurationManagementDirectory(configurationManagementDirectory);
        }
    }

    private synchronized void createConfigurationManagementDirectory(File configurationManagementDirectory)
    {
        if (!configurationManagementDirectory.exists() && !configurationManagementDirectory.mkdirs())
        {
            Message message = CoreMessages.failedToCreate("configuration management directory "
                                                          + configurationManagementDirectory.getAbsolutePath());
            throw new MuleRuntimeException(message);
        }
    }

    private void persistConfigurationManagementFile(String configurationManagementPath, Properties configurationManagementProperties) throws IOException
    {
        File configurationManagementFile = new File(configurationManagementPath, CONFIGURATION_MANAGEMENT_FILE_NAME);
        FileWriter fileWriter = new FileWriter(configurationManagementFile.getAbsolutePath(), false);
        configurationManagementProperties.store(fileWriter, "configuration management properties");
        fileWriter.close();
    }


}
