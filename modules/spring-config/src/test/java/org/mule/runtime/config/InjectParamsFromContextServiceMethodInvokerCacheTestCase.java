/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config;

import org.junit.Test;
import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.config.internal.InjectParamsFromContextServiceMethodInvoker;
import org.mule.runtime.config.utils.Utils;
import org.mule.runtime.config.utils.Utils.AugmentedMethodService;
import org.mule.runtime.config.utils.Utils.BaseService;

import java.lang.reflect.Method;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class InjectParamsFromContextServiceMethodInvokerCacheTestCase {

  private Registry registry = mock(Registry.class);
  private InjectParamsFromContextServiceMethodInvoker injectParamsFromContextServiceMethodInvoker =
      new InjectParamsFromContextServiceMethodInvoker(registry);

  @Test
  public void lookupByNameCaching() throws Throwable {
    when(registry.lookupByName(any())).thenReturn(java.util.Optional.of("malaFama"));
    BaseService service = new Utils.InvalidNamedAugmentedMethodService();
    Method method = BaseService.class.getMethod("augmented");
    injectParamsFromContextServiceMethodInvoker.invoke(service, method, null);
    injectParamsFromContextServiceMethodInvoker.invoke(service, method, null);

    verify(registry, times(1)).lookupByName(any());
  }

  @Test
  public void lookupAllByTypeCaching() throws Throwable {
    BaseService service = new AugmentedMethodService();
    Method method = BaseService.class.getMethod("augmented");
    injectParamsFromContextServiceMethodInvoker.invoke(service, method, null);
    injectParamsFromContextServiceMethodInvoker.invoke(service, method, null);

    verify(registry, times(1)).lookupAllByType(any());
  }

}
