/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher.descriptor;

import static org.mule.module.launcher.descriptor.PropertiesDescriptorParser.PROPERTY_LOADER_OVERRIDE;
import static org.mule.module.launcher.descriptor.PropertiesDescriptorParser.PROPERTY_REDEPLOYMENT_ENABLED;
import static org.mule.util.PropertiesUtils.loadProperties;
import org.mule.util.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang.BooleanUtils;

/**
 * Descriptor parser exclusive for domains.
 */
public class DomainDescriptorParser
{

    public DomainDescriptor parse(File descriptor) throws IOException
    {
        final Properties properties = loadProperties(new FileInputStream(descriptor));
        DomainDescriptor domainDescriptor = new DomainDescriptor();

        domainDescriptor.setRedeploymentEnabled(BooleanUtils.toBoolean(properties.getProperty(PROPERTY_REDEPLOYMENT_ENABLED, Boolean.TRUE.toString())));

        final String overrideString = properties.getProperty(PROPERTY_LOADER_OVERRIDE);
        if (StringUtils.isNotBlank(overrideString))
        {
            Set<String> values = new HashSet<>();
            final String[] overrides = overrideString.split(",");
            Collections.addAll(values, overrides);
            domainDescriptor.setLoaderOverride(values);
        }
        return domainDescriptor;
    }

}
