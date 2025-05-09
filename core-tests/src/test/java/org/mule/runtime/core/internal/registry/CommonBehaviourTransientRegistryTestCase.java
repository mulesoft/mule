/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.registry;

import static org.mockito.Answers.RETURNS_DEEP_STUBS;

import org.mule.runtime.core.internal.context.DefaultMuleContext;
import org.mule.runtime.core.internal.context.MuleContextWithRegistry;
import org.mule.runtime.core.internal.lifecycle.MuleLifecycleInterceptor;
import org.mule.runtime.core.privileged.PrivilegedMuleContext;
import org.mule.tck.core.registry.AbstractRegistryTestCase;

import org.junit.Rule;

import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class CommonBehaviourTransientRegistryTestCase extends AbstractRegistryTestCase {

  @Rule
  public MockitoRule rule = MockitoJUnit.rule();

  @Mock(answer = RETURNS_DEEP_STUBS, extraInterfaces = {MuleContextWithRegistry.class, PrivilegedMuleContext.class})
  private DefaultMuleContext muleContext;

  @Override
  public Registry getRegistry() {
    return new SimpleRegistry(muleContext, new MuleLifecycleInterceptor(), muleContext.getConfiguration());
  }
}
