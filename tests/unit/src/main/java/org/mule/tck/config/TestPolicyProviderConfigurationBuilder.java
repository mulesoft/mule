/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
