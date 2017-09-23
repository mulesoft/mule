/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.interceptor;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.reflections.ReflectionUtils.getFields;
import static org.reflections.ReflectionUtils.withType;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.api.interception.ProcessorInterceptorManager;
import org.mule.runtime.core.internal.processor.interceptor.a.ProcessorInterceptorFactoryA;
import org.mule.runtime.core.internal.processor.interceptor.b.ProcessorInterceptorFactoryB;
import org.mule.runtime.core.internal.processor.interceptor.c.ProcessorInterceptorFactoryC;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.lang.reflect.Field;

import org.junit.Before;
import org.junit.Test;


public class DefaultProcessorInterceptorManagerTestCase extends AbstractMuleTestCase {

  private ProcessorInterceptorManager manager;

  @Before
  public void before() throws IllegalArgumentException, IllegalAccessException {
    manager = new DefaultProcessorInterceptorManager();
    injectMockMuleContext(manager);
  }

  private void injectMockMuleContext(Object injectionTarget) throws IllegalAccessException {
    final Field contextField = getFields(DefaultProcessorInterceptorManager.class, withType(MuleContext.class)).iterator().next();
    contextField.setAccessible(true);
    contextField.set(injectionTarget, mock(MuleContext.class, RETURNS_DEEP_STUBS));
  }

  @Test
  public void noInterceptors() {
    assertThat(manager.getInterceptorFactories(), empty());
  }

  @Test
  public void interceptorsOrderedAsRegisterd() {
    final ProcessorInterceptorFactoryA intFactoryA = new ProcessorInterceptorFactoryA();
    final ProcessorInterceptorFactoryB intFactoryB = new ProcessorInterceptorFactoryB();
    final ProcessorInterceptorFactoryC intFactoryC = new ProcessorInterceptorFactoryC();
    manager.addInterceptorFactory(intFactoryA);
    manager.addInterceptorFactory(intFactoryB);
    manager.addInterceptorFactory(intFactoryC);

    assertThat(manager.getInterceptorFactories(), contains(intFactoryA, intFactoryB, intFactoryC));
  }

  @Test
  public void interceptorsOrdered() {
    final ProcessorInterceptorFactoryA intFactoryA = new ProcessorInterceptorFactoryA();
    final ProcessorInterceptorFactoryB intFactoryB = new ProcessorInterceptorFactoryB();
    final ProcessorInterceptorFactoryC intFactoryC = new ProcessorInterceptorFactoryC();
    manager.addInterceptorFactory(intFactoryA);
    manager.addInterceptorFactory(intFactoryB);
    manager.addInterceptorFactory(intFactoryC);

    manager.setInterceptorsOrder("org.mule.runtime.core.internal.processor.interceptor.c",
                                 "org.mule.runtime.core.internal.processor.interceptor.b");

    assertThat(manager.getInterceptorFactories(), contains(intFactoryC, intFactoryB, intFactoryA));
  }

  @Test
  public void interceptorsOrderedManyPerOrderItem() {
    final ProcessorInterceptorFactoryA intFactoryA1 = new ProcessorInterceptorFactoryA();
    final ProcessorInterceptorFactoryA intFactoryA2 = new ProcessorInterceptorFactoryA();
    final ProcessorInterceptorFactoryA intFactoryA3 = new ProcessorInterceptorFactoryA();
    final ProcessorInterceptorFactoryA intFactoryA4 = new ProcessorInterceptorFactoryA();
    final ProcessorInterceptorFactoryA intFactoryA5 = new ProcessorInterceptorFactoryA();

    final ProcessorInterceptorFactoryB intFactoryB1 = new ProcessorInterceptorFactoryB();
    final ProcessorInterceptorFactoryB intFactoryB2 = new ProcessorInterceptorFactoryB();
    final ProcessorInterceptorFactoryB intFactoryB3 = new ProcessorInterceptorFactoryB();
    final ProcessorInterceptorFactoryB intFactoryB4 = new ProcessorInterceptorFactoryB();
    final ProcessorInterceptorFactoryB intFactoryB5 = new ProcessorInterceptorFactoryB();

    final ProcessorInterceptorFactoryC intFactoryC1 = new ProcessorInterceptorFactoryC();
    final ProcessorInterceptorFactoryC intFactoryC2 = new ProcessorInterceptorFactoryC();
    final ProcessorInterceptorFactoryC intFactoryC3 = new ProcessorInterceptorFactoryC();
    final ProcessorInterceptorFactoryC intFactoryC4 = new ProcessorInterceptorFactoryC();
    final ProcessorInterceptorFactoryC intFactoryC5 = new ProcessorInterceptorFactoryC();

    manager.addInterceptorFactory(intFactoryA1);
    manager.addInterceptorFactory(intFactoryA2);
    manager.addInterceptorFactory(intFactoryA3);
    manager.addInterceptorFactory(intFactoryA4);
    manager.addInterceptorFactory(intFactoryA5);

    manager.addInterceptorFactory(intFactoryB1);
    manager.addInterceptorFactory(intFactoryB2);
    manager.addInterceptorFactory(intFactoryB3);
    manager.addInterceptorFactory(intFactoryB4);
    manager.addInterceptorFactory(intFactoryB5);

    manager.addInterceptorFactory(intFactoryC1);
    manager.addInterceptorFactory(intFactoryC2);
    manager.addInterceptorFactory(intFactoryC3);
    manager.addInterceptorFactory(intFactoryC4);
    manager.addInterceptorFactory(intFactoryC5);

    manager.setInterceptorsOrder("org.mule.runtime.core.internal.processor.interceptor.c",
                                 "org.mule.runtime.core.internal.processor.interceptor.b");

    assertThat(manager.getInterceptorFactories(), contains(intFactoryC1, intFactoryC2, intFactoryC3, intFactoryC4, intFactoryC5,
                                                           intFactoryB1, intFactoryB2, intFactoryB3, intFactoryB4, intFactoryB5,
                                                           intFactoryA1, intFactoryA2, intFactoryA3, intFactoryA4, intFactoryA5));
  }
}
