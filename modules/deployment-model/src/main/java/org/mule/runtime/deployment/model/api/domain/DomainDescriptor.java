/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.deployment.model.api.domain;

import static org.mule.runtime.container.api.MuleFoldersUtil.getAppConfigFolderPath;
import org.mule.runtime.deployment.model.api.DeployableArtifactDescriptor;

import com.google.common.collect.ImmutableList;

import java.nio.file.Paths;
import java.util.List;

/**
 * Represents the description of a domain.
 */
public class DomainDescriptor extends DeployableArtifactDescriptor {

  public static final String DEFAULT_DOMAIN_NAME = "default";
  public static final String DEFAULT_CONFIGURATION_RESOURCE = "mule-domain-config.xml";
  public static final String DEFAULT_CONFIGURATION_RESOURCE_LOCATION =
      Paths.get("mule", DEFAULT_CONFIGURATION_RESOURCE).toString();

  /**
   * Creates a new domain descriptor
   *
   * @param name domain name. Non empty.
   */
  public DomainDescriptor(String name) {
    super(name);
  }

  @Override
  protected List<String> getDefaultConfigResources() {
    return ImmutableList.<String>builder().add(getAppConfigFolderPath() + DEFAULT_CONFIGURATION_RESOURCE).build();
  }
}
