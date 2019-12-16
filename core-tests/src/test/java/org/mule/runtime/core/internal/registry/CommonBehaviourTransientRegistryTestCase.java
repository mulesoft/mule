/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.registry;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.internal.context.MuleContextWithRegistry;
import org.mule.runtime.core.internal.lifecycle.MuleLifecycleInterceptor;
import org.mule.runtime.core.privileged.PrivilegedMuleContext;
import org.mule.tck.core.registry.AbstractRegistryTestCase;

import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CommonBehaviourTransientRegistryTestCase extends AbstractRegistryTestCase {

  @Mock(answer = Answers.RETURNS_DEEP_STUBS, extraInterfaces = {MuleContextWithRegistry.class, PrivilegedMuleContext.class})
  private MuleContext muleContext;

  @Override
  public Registry getRegistry() {
    return new SimpleRegistry(muleContext, new MuleLifecycleInterceptor());
  }
}
