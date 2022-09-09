/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.config.internal.el;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.withSettings;

import org.hamcrest.core.IsInstanceOf;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.core.internal.el.ExtendedExpressionLanguageAdaptor;

import io.qameta.allure.Issue;
import org.junit.Test;
import org.springframework.beans.factory.DisposableBean;

@Issue("W-10672687")
public class DataWeaveExtendedExpressionLanguageAdaptorFactoryBeanTestCase {

  @Test
  public void whenDataWeaveExtendedExpressionLanguageAdaptorFactoryBeanIsDistroyedTheAdapatorIsDisposed() throws Exception {
    TestDataWeaveExtendedExpressionLanguageAdaptorFactoryBean factoryBean =
        new TestDataWeaveExtendedExpressionLanguageAdaptorFactoryBean();

    // We get the singleton object from the factory bean.
    ExtendedExpressionLanguageAdaptor bean = factoryBean.getObject();
    assertThat(bean, equalTo(factoryBean.getSingleton()));

    // we verify that factory bean is instance of DisposableBean
    // That guarantees that the factory destroy method will be invoked by spring.
    assertThat(factoryBean, IsInstanceOf.instanceOf(DisposableBean.class));
    factoryBean.destroy();

    // When the method is invoked, the adaptor is disposed.
    verify(factoryBean.getSingleton()).dispose();
  }

  /**
   * A test {@link DataWeaveExtendedExpressionLanguageAdaptorFactoryBean}
   */
  private static class TestDataWeaveExtendedExpressionLanguageAdaptorFactoryBean
      extends DataWeaveExtendedExpressionLanguageAdaptorFactoryBean {

    private Disposable singleton;

    @Override
    protected ExtendedExpressionLanguageAdaptor getExtendedExpressionLanguageAdaptor() {
      singleton = mock(Disposable.class, withSettings().extraInterfaces(ExtendedExpressionLanguageAdaptor.class));
      return (ExtendedExpressionLanguageAdaptor) singleton;
    }

    public Disposable getSingleton() {
      return singleton;
    }
  }
}
