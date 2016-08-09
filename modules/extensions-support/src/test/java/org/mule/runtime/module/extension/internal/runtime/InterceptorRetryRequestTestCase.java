/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import org.mule.runtime.extension.api.runtime.operation.Interceptor;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class InterceptorRetryRequestTestCase extends AbstractMuleTestCase {

  @Mock
  private Interceptor interceptor;

  private InterceptorsRetryRequest request;

  @Before
  public void before() {
    request = new InterceptorsRetryRequest(interceptor, null);
  }

  @Test
  public void noRequest() {
    assertThat(request.isRetryRequested(), is(false));
  }

  @Test
  public void request() {
    request.request();
    assertThat(request.isRetryRequested(), is(true));
  }

  @Test
  public void multipleRequestsOnSame() {
    request();
    requestAndFail();
  }

  @Test
  public void multipleRequestOnDifferent() {
    request();
    request = new InterceptorsRetryRequest(interceptor, request);
    assertThat(request.isRetryRequested(), is(false));
    requestAndFail();
  }

  @Test
  public void requestsByDifferentOwner() {
    request();
    request = new InterceptorsRetryRequest(mock(Interceptor.class), request);
    assertThat(request.isRetryRequested(), is(false));
    request();

    // go back to original interceptor
    request = new InterceptorsRetryRequest(interceptor, request);
    requestAndFail();
  }

  private void requestAndFail() {
    try {
      request();
      fail("was expecting exception");
    } catch (IllegalStateException e) {
      // we're cool bro...
    }
  }
}
