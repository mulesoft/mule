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

import org.mule.util.PropertiesUtils;
import org.mule.util.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang.BooleanUtils;

/**
 *
 */
public class PropertiesDescriptorParser implements DescriptorParser
{

    public ApplicationDescriptor parse(File descriptor) throws IOException
    {
        final Properties p = new Properties();
        p.load(new FileInputStream(descriptor));

        ApplicationDescriptor d = new ApplicationDescriptor();
        d.setEncoding(p.getProperty("encoding"));
        d.setConfigurationBuilder(p.getProperty("config.builder"));
        d.setDomain(p.getProperty("domain"));

        // get a ref to an optional app props file (right next to the descriptor)
        final File appPropsFile = new File(descriptor.getParent(), "mule-app.properties");
        if (appPropsFile.exists() && appPropsFile.canRead())
        {
            final Properties props = PropertiesUtils.loadProperties(appPropsFile.toURI().toURL());
            // ugh, no straightforward way to convert to a map
            Map<String, String> m = new HashMap<String, String>(props.size());
            for (String key : m.keySet())
            {
                m.put(key, props.getProperty(key));
            }
            d.setAppProperties(m);
        }
        
        // supports true (case insensitive), yes, on as positive values
        d.setParentFirstClassLoader(BooleanUtils.toBoolean(p.getProperty("classloader.parentFirst", Boolean.TRUE.toString())));

        final String resProps = p.getProperty("config.resources");
        String[] urls;
        if (StringUtils.isBlank(resProps))
        {
            urls = new String[] {ApplicationDescriptor.DEFAULT_CONFIGURATION_RESOURCE};
        }
        else
        {
            urls = resProps.split(",");
        }
        d.setConfigResources(urls);

        // supports true (case insensitive), yes, on as positive values
        d.setRedeploymentEnabled(BooleanUtils.toBoolean(p.getProperty("redeployment.enabled", Boolean.TRUE.toString())));

        return d;
    }

    public String getSupportedFormat()
    {
        return "properties";
    }
}
