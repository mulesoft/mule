/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.tck.config;

import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_POLICY_PROVIDER;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.builders.AbstractConfigurationBuilder;
import org.mule.runtime.core.internal.policy.NullPolicyProvider;

public class TestPolicyProviderConfigurationBuilder extends AbstractConfigurationBuilder {

  @Override
  protected void doConfigure(MuleContext muleContext) {
    muleContext.getCustomizationService().registerCustomServiceImpl(OBJECT_POLICY_PROVIDER, new NullPolicyProvider());
  }

}
