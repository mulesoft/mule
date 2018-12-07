/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.el;

import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mule.runtime.api.el.BindingContext;
import org.mule.runtime.api.util.concurrent.Latch;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.util.func.CheckedSupplier;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class LazyExpressionLanguageAdaptorTestCase extends AbstractMuleTestCase {

  @Mock(answer = RETURNS_DEEP_STUBS)
  private CheckedSupplier<ExtendedExpressionLanguageAdaptor> supplier;

  @Mock
  private BindingContext b1;

  @Mock
  private BindingContext b2;

  @Mock
  private ExtendedExpressionLanguageAdaptor delegate;

  private LazyExpressionLanguageAdaptor lazyAdaptor;


  private Latch latch = new Latch();


  @Before
  public void before() {
    lazyAdaptor = new LazyExpressionLanguageAdaptor(supplier);
    when(supplier.get()).thenReturn(delegate);
  }

  @Test
  public void supplierInvokedOnlyOnce() throws Exception {
    Thread t1 = new Thread(() -> {
      try {
        latch.await();
        evaluate();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });

    Thread t2 = new Thread(() -> {
      latch.countDown();
      evaluate();
    });

    t1.start();
    t2.start();

    t1.join();
    t2.join();
  }

  @Test
  public void globalBindingsDeferred() throws Exception {
    lazyAdaptor.addGlobalBindings(b1);
    lazyAdaptor.addGlobalBindings(b2);

    verify(supplier, never()).get();

    evaluate();
    verify(delegate).addGlobalBindings(b1);
    verify(delegate).addGlobalBindings(b2);
  }

  @Test
  public void globalBIndingsNotDeferred() {
    evaluate();

    lazyAdaptor.addGlobalBindings(b1);
    lazyAdaptor.addGlobalBindings(b2);

    verify(delegate).addGlobalBindings(b1);
    verify(delegate).addGlobalBindings(b2);
  }

  private void evaluate() {
    String expression = "#[as]";
    CoreEvent event = mock(CoreEvent.class);
    lazyAdaptor.evaluate(expression, event, b1);

    verify(supplier).get();
    verify(delegate).evaluate(expression, event, b1);
  }
}
