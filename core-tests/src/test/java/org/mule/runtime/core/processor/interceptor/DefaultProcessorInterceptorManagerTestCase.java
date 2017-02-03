package org.mule.runtime.core.processor.interceptor;

import static org.hamcrest.collection.IsEmptyCollection.empty;

import static org.hamcrest.Matchers.contains;

import static org.junit.Assert.assertThat;

import org.mule.runtime.core.api.interception.ProcessorInterceptorProvider;
import org.mule.runtime.core.processor.interceptor.a.ProcessorInterceptorFactoryA;
import org.mule.runtime.core.processor.interceptor.b.ProcessorInterceptorFactoryB;
import org.mule.runtime.core.processor.interceptor.c.ProcessorInterceptorFactoryC;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.Before;
import org.junit.Test;


public class DefaultProcessorInterceptorManagerTestCase extends AbstractMuleTestCase {

  private ProcessorInterceptorProvider manager;

  @Before
  public void before() {
    manager = new DefaultProcessorInterceptorManager();
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
    manager.addInterceptor(intFactoryA);
    manager.addInterceptor(intFactoryB);
    manager.addInterceptor(intFactoryC);

    assertThat(manager.getInterceptorFactories(), contains(intFactoryA, intFactoryB, intFactoryC));
  }

  @Test
  public void interceptorsOrdered() {
    final ProcessorInterceptorFactoryA intFactoryA = new ProcessorInterceptorFactoryA();
    final ProcessorInterceptorFactoryB intFactoryB = new ProcessorInterceptorFactoryB();
    final ProcessorInterceptorFactoryC intFactoryC = new ProcessorInterceptorFactoryC();
    manager.addInterceptor(intFactoryA);
    manager.addInterceptor(intFactoryB);
    manager.addInterceptor(intFactoryC);

    manager.setInterceptorsOrder("org.mule.runtime.core.processor.interceptor.c",
                                 "org.mule.runtime.core.processor.interceptor.b");

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

    manager.addInterceptor(intFactoryA1);
    manager.addInterceptor(intFactoryA2);
    manager.addInterceptor(intFactoryA3);
    manager.addInterceptor(intFactoryA4);
    manager.addInterceptor(intFactoryA5);

    manager.addInterceptor(intFactoryB1);
    manager.addInterceptor(intFactoryB2);
    manager.addInterceptor(intFactoryB3);
    manager.addInterceptor(intFactoryB4);
    manager.addInterceptor(intFactoryB5);

    manager.addInterceptor(intFactoryC1);
    manager.addInterceptor(intFactoryC2);
    manager.addInterceptor(intFactoryC3);
    manager.addInterceptor(intFactoryC4);
    manager.addInterceptor(intFactoryC5);

    manager.setInterceptorsOrder("org.mule.runtime.core.processor.interceptor.c",
                                 "org.mule.runtime.core.processor.interceptor.b");

    assertThat(manager.getInterceptorFactories(), contains(intFactoryC1, intFactoryC2, intFactoryC3, intFactoryC4, intFactoryC5,
                                                           intFactoryB1, intFactoryB2, intFactoryB3, intFactoryB4, intFactoryB5,
                                                           intFactoryA1, intFactoryA2, intFactoryA3, intFactoryA4, intFactoryA5));
  }
}
