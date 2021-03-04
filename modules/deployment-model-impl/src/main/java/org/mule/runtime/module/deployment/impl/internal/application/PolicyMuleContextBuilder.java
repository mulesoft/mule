/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.application;

import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.POLICY;
import static org.mule.runtime.core.api.util.StringUtils.isBlank;

import org.mule.runtime.core.api.config.DefaultMuleConfiguration;

import java.util.Map;

/**
 * Takes Mule policy descriptor into account when building the context.
 * 
 * @since 4.1
 */
public class PolicyMuleContextBuilder extends SupportsPropertiesMuleContextBuilder {

  private final String policyName;
  private final String defaultEncoding;

  public PolicyMuleContextBuilder(String policyName, Map<String, String> policyProperties, String defaultEncoding) {
    super(POLICY, policyProperties);
    this.policyName = policyName;
    this.defaultEncoding = defaultEncoding;
  }

  @Override
  protected DefaultMuleConfiguration createMuleConfiguration() {
    final DefaultMuleConfiguration configuration = new DefaultMuleConfiguration(true);
    initializeFromProperties(configuration);
    configuration.setId(policyName);
    final String encoding = defaultEncoding;
    if (!isBlank(encoding)) {
      configuration.setDefaultEncoding(encoding);
    }
    return configuration;
  }
}
