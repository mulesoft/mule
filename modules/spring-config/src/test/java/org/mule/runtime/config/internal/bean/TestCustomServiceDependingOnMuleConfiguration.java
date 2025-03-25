/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.bean;

import org.mule.runtime.core.api.config.MuleConfiguration;

import jakarta.inject.Inject;

/**
 * A test object that depends on the {@link MuleConfiguration}, to be used as a singleton bean.
 */
public class TestCustomServiceDependingOnMuleConfiguration {

  @javax.inject.Inject
  public MuleConfiguration muleConfigurationJavax;

  @Inject
  public MuleConfiguration muleConfiguration;

  public MuleConfiguration getMuleConfigurationJavax() {
    return muleConfigurationJavax;
  }

  public MuleConfiguration getMuleConfiguration() {
    return muleConfiguration;
  }
}
