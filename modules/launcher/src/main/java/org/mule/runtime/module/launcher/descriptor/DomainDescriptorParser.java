/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.launcher.descriptor;

import static org.mule.runtime.module.launcher.descriptor.PropertiesDescriptorParser.PROPERTY_REDEPLOYMENT_ENABLED;
import static org.mule.runtime.core.util.PropertiesUtils.loadProperties;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.lang.BooleanUtils;

/**
 * Descriptor parser exclusive for domains.
 */
public class DomainDescriptorParser implements DescriptorParser<DomainDescriptor> {

  @Override
  public DomainDescriptor parse(File descriptor, String artifactName) throws IOException {
    final Properties properties = loadProperties(new FileInputStream(descriptor));
    DomainDescriptor domainDescriptor = new DomainDescriptor();
    domainDescriptor.setName(artifactName);
    domainDescriptor.setRedeploymentEnabled(BooleanUtils
        .toBoolean(properties.getProperty(PROPERTY_REDEPLOYMENT_ENABLED, Boolean.TRUE.toString())));
    return domainDescriptor;
  }
}
