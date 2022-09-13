/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.lazy;

import static org.mule.test.allure.AllureConstants.ExpressionLanguageFeature.EXPRESSION_LANGUAGE;
import static org.mule.test.allure.AllureConstants.LazyInitializationFeature.LAZY_INITIALIZATION;

import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.el.BindingContext;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.util.concurrent.Latch;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.util.func.CheckedSupplier;
import org.mule.runtime.core.internal.el.ExtendedExpressionLanguageAdaptor;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import io.qameta.allure.Issue;
import io.qameta.allure.Feature;
import io.qameta.allure.Features;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;


@SmallTest
@Features({@Feature(LAZY_INITIALIZATION), @Feature(EXPRESSION_LANGUAGE)})
public class LazyExpressionLanguageAdaptorTestCase extends AbstractMuleTestCase {

  @Rule
  public MockitoRule rule = MockitoJUnit.rule();

  @Mock(answer = RETURNS_DEEP_STUBS)
  private CheckedSupplier<ExtendedExpressionLanguageAdaptor> supplier;

  @Mock
  private BindingContext b1;

  @Mock
  private BindingContext b2;

  @Mock(extraInterfaces = Disposable.class)
  private ExtendedExpressionLanguageAdaptor delegate;

  private LazyExpressionLanguageAdaptor lazyAdaptor;


  private final Latch latch = new Latch();


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
  @Issue("W-11745207")
  public void delegateIsDisposedWhenAdaptorDisposed() {
    evaluate();
    lazyAdaptor.dispose();
    verify(((Disposable) delegate)).dispose();
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
