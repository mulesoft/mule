/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import static org.mockito.Mockito.verify;
import static org.mule.tck.MuleTestUtils.spyInjector;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Before;
import org.junit.Test;

@SmallTest
public abstract class AbstractValueResolverWrapperTestCase<T> extends AbstractMuleContextTestCase {

  protected AbstractValueResolverWrapper<T> wrapper;
  protected ValueResolver<T> delegate;

  @Before
  public void before() throws Exception {
    spyInjector(muleContext);
    delegate = createDelegate();
    wrapper = createWrapper(delegate);
    muleContext.getInjector().inject(wrapper);
  }

  protected abstract AbstractValueResolverWrapper<T> createWrapper(ValueResolver<T> delegate);

  protected abstract ValueResolver<T> createDelegate();

  @Test
  public void initialise() throws Exception {
    wrapper.initialise();

    if (delegate instanceof Initialisable) {
      verify((Initialisable) delegate).initialise();
    }

    verify(muleContext.getInjector()).inject(delegate);
  }

  @Test
  public void start() throws Exception {
    wrapper.start();
    if (delegate instanceof Startable) {
      verify((Startable) delegate).start();
    }
  }

  @Test
  public void stop() throws Exception {
    wrapper.stop();
    if (delegate instanceof Stoppable) {
      verify((Stoppable) delegate).stop();
    }
  }

  @Test
  public void dispose() throws Exception {
    wrapper.dispose();
    if (delegate instanceof Disposable) {
      verify((Disposable) delegate).dispose();
    }
  }
}
