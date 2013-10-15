/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
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
import java.util.Properties;

import org.apache.commons.lang.BooleanUtils;

public class PropertiesDescriptorParser implements DescriptorParser
{
    protected static final String PROPERTY_ENCODING = "encoding";
    protected static final String PROPERTY_CONFIG_BUILDER = "config.builder";
    protected static final String PROPERTY_DOMAIN = "domain";
    // support not yet implemented for CL reversal
    protected static final String PROPERTY_CLASSLOADER_PARENT_FIRST = "classloader.parentFirst";
    protected static final String PROPERTY_CONFIG_RESOURCES = "config.resources";
    protected static final String PROPERTY_REDEPLOYMENT_ENABLED = "redeployment.enabled";
    protected static final String PROPERTY_PRIVILEDGED = "priviledged";
    protected static final String PROPERTY_SCAN_PACKAGES = "scan.packages";

    public ApplicationDescriptor parse(File descriptor) throws IOException
    {
        final Properties p = PropertiesUtils.loadProperties(new FileInputStream(descriptor));

        ApplicationDescriptor d = new ApplicationDescriptor();
        d.setEncoding(p.getProperty(PROPERTY_ENCODING));
        d.setConfigurationBuilder(p.getProperty(PROPERTY_CONFIG_BUILDER));
        d.setDomain(p.getProperty(PROPERTY_DOMAIN));
        d.setPackagesToScan(p.getProperty(PROPERTY_SCAN_PACKAGES));

        // supports 'true' (case insensitive), 'yes', 'on' as positive values
        d.setParentFirstClassLoader(BooleanUtils.toBoolean(p.getProperty(PROPERTY_CLASSLOADER_PARENT_FIRST, Boolean.TRUE.toString())));

        final String resProps = p.getProperty(PROPERTY_CONFIG_RESOURCES);
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
        d.setRedeploymentEnabled(BooleanUtils.toBoolean(p.getProperty(PROPERTY_REDEPLOYMENT_ENABLED, Boolean.TRUE.toString())));

        d.setPriviledged(BooleanUtils.toBoolean(p.getProperty(PROPERTY_PRIVILEDGED, Boolean.FALSE.toString())));

        return d;
    }

    public String getSupportedFormat()
    {
        return "properties";
    }
}
