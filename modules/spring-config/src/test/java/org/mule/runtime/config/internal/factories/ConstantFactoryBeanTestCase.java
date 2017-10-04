/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.factories;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.config.internal.factories.ConstantFactoryBean;
import org.mule.runtime.core.api.Injector;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
@SmallTest
public class ConstantFactoryBeanTestCase extends AbstractMuleTestCase {

  @Mock(extraInterfaces = {Lifecycle.class, MuleContextAware.class})
  private Object value;
  private ConstantFactoryBean<Object> factoryBean;

  @Before
  public void before() throws Exception {
    factoryBean = new ConstantFactoryBean<>(value);
  }

  @Test
  public void returnsValue() throws Exception {
    assertThat(factoryBean.getObject(), is(sameInstance(value)));
  }

  @Test
  public void singleton() {
    assertThat(factoryBean.isSingleton(), is(true));
  }

  @Test
  public void assertClass() {
    assertThat(factoryBean.getObjectType() == value.getClass(), is(true));
  }

  @Test
  public void initialise() throws Exception {
    MuleContext muleContext = mock(MuleContext.class);
    Injector injector = mock(Injector.class);
    when(muleContext.getInjector()).thenReturn(injector);

    factoryBean.setMuleContext(muleContext);
    factoryBean.initialise();

    verify((Initialisable) value).initialise();
    verify((MuleContextAware) value).setMuleContext(muleContext);
    verify(injector).inject(value);
  }

  @Test
  public void stop() throws MuleException {
    factoryBean.stop();
    verify((Stoppable) value).stop();
  }

  @Test
  public void dispose() throws Exception {
    factoryBean.dispose();
    verify((Disposable) value).dispose();
  }

}
