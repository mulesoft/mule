/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.service.internal.test.manager;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.CoreMatchers.is;
import static org.mockito.Mockito.mock;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.module.service.api.discoverer.ServiceAssembly;
import org.mule.runtime.module.service.internal.manager.DefaultServiceRegistry;
import org.mule.runtime.module.service.internal.manager.LazyServiceProxyApplicationDecorator;
import org.junit.Test;

public class LazyServiceProxyApplicationDecoratorTestCase {

  @Test
  public void testDecorate() throws Throwable {
    LazyServiceProxyApplicationDecorator decorator =
        new LazyServiceProxyApplicationDecorator(mock(ServiceAssembly.class), mock(DefaultServiceRegistry.class),
                                                 new LazyValue<>(() -> null), null);
    Starting starting = new Starting();
    Stopping stopping = new Stopping();
    assertThat(decorator.invoke(starting, Startable.class.getMethod("start"), new Object[] {}), is(nullValue()));
    assertThat(starting.isStarted, is(false));
    assertThat(decorator.invoke(stopping, Stoppable.class.getMethod("stop"), new Object[] {}), is(nullValue()));
    assertThat(stopping.isStopped, is(false));
  }

  private static class Starting implements Startable {

    private boolean isStarted = false;

    @Override
    public void start() throws MuleException {
      isStarted = true;
    }
  }

  private static class Stopping implements Stoppable {

    private boolean isStopped = false;

    @Override
    public void stop() throws MuleException {
      isStopped = true;
    }
  }
}
