/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.launcher.descriptor;

import org.mule.util.PropertiesUtils;
import org.mule.util.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang.BooleanUtils;

public class PropertiesDescriptorParser implements DescriptorParser
{
    protected static final String PROPERTY_ENCODING = "encoding";
    protected static final String PROPERTY_CONFIG_BUILDER = "config.builder";
    protected static final String PROPERTY_DOMAIN = "domain";
    // support not yet implemented for CL reversal
    protected static final String PROPERTY_CONFIG_RESOURCES = "config.resources";
    protected static final String PROPERTY_REDEPLOYMENT_ENABLED = "redeployment.enabled";
    // there was a typo in the prop name, but we still support it
    protected static final String PROPERTY_LEGACY_PRIVILEGED = "priviledged";
    protected static final String PROPERTY_PRIVILEGED = "privileged";
    protected static final String PROPERTY_LOADER_OVERRIDE = "loader.override";
    protected static final String PROPERTY_SCAN_PACKAGES = "scan.packages";

    public ApplicationDescriptor parse(File descriptor) throws IOException
    {
        final Properties p = PropertiesUtils.loadProperties(new FileInputStream(descriptor));

        ApplicationDescriptor d = new ApplicationDescriptor();
        d.setEncoding(p.getProperty(PROPERTY_ENCODING));
        d.setConfigurationBuilder(p.getProperty(PROPERTY_CONFIG_BUILDER));
        d.setDomain(p.getProperty(PROPERTY_DOMAIN));
        d.setPackagesToScan(p.getProperty(PROPERTY_SCAN_PACKAGES));

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

        // fallback to a legacy property name
        d.setPrivileged(BooleanUtils.toBoolean(p.getProperty(PROPERTY_PRIVILEGED,
                                                             p.getProperty(PROPERTY_LEGACY_PRIVILEGED, Boolean.FALSE.toString()))));

        final String overrideString = p.getProperty(PROPERTY_LOADER_OVERRIDE);
        if (StringUtils.isNotBlank(overrideString))
        {
            Set<String> values = new HashSet<String>();
            final String[] overrides = overrideString.split(",");
            Collections.addAll(values, overrides);
            d.setLoaderOverride(values);
        }

        return d;
    }

    public String getSupportedFormat()
    {
        return "properties";
    }
}
