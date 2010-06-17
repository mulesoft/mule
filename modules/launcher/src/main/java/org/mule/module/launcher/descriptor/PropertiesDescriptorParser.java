/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.launcher.descriptor;

import org.mule.util.StringUtils;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

/**
 *
 */
public class PropertiesDescriptorParser implements DescriptorParser
{

    public ApplicationDescriptor parse(File descriptor) throws IOException
    {
        final Properties p = new Properties();
        p.load(new FileReader(descriptor));

        ApplicationDescriptor d = new ApplicationDescriptor();
        d.setEncoding(p.getProperty("encoding"));
        d.setConfigurationBuilder(p.getProperty("config.builder"));
        d.setDomain(p.getProperty("domain"));
        // TODO use BooleanUtils.toBoolean() for friendlier flag definition support (true/false, on, yes/no)
        d.setParentFirstClassLoader(Boolean.parseBoolean(p.getProperty("classloader.parentFirst", Boolean.TRUE.toString())));

        final String urlsProp = p.getProperty("config.urls");
        String[] urls;
        if (StringUtils.isBlank(urlsProp))
        {
            urls = new String[] {ApplicationDescriptor.DEFAULT_CONFIGURATION_URL};
        }
        else
        {
            urls = urlsProp.split(",");
        }
        d.setConfigUrls(urls);

        // TODO use BooleanUtils.toBoolean() for friendlier flag definition support (true/false, on, yes/no)
        d.setRedeploymentEnabled(Boolean.parseBoolean(p.getProperty("redeployment.enabled", Boolean.TRUE.toString())));

        return d;
    }

    public String getSupportedFormat()
    {
        return "properties";
    }
}
