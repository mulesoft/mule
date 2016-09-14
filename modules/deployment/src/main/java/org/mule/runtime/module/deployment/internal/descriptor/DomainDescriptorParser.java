/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal.descriptor;

import static java.lang.Boolean.TRUE;
import static org.apache.commons.lang.BooleanUtils.toBoolean;
import static org.mule.runtime.core.util.PropertiesUtils.loadProperties;
import static org.mule.runtime.module.deployment.internal.descriptor.PropertiesDescriptorParser.PROPERTY_REDEPLOYMENT_ENABLED;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Descriptor parser exclusive for domains.
 */
public class DomainDescriptorParser implements DescriptorParser<DomainDescriptor> {

  /**
   * {@inheritDoc}
   */
  @Override
  public DomainDescriptor parse(File location, File descriptor, String artifactName) throws IOException {
    final Properties properties = loadProperties(new FileInputStream(descriptor));
    final DomainDescriptor domainDescriptor = new DomainDescriptor(artifactName);
    domainDescriptor.setRedeploymentEnabled(toBoolean(properties.getProperty(PROPERTY_REDEPLOYMENT_ENABLED, TRUE.toString())));
    domainDescriptor.setArtifactLocation(location);
    domainDescriptor.setRootFolder(location.getParentFile());
    return domainDescriptor;
  }
}
