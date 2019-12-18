/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.execution;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.extension.api.runtime.operation.CompletableComponentExecutor.ExecutorCallback;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.extension.api.runtime.operation.Interceptor;
import org.mule.runtime.module.extension.internal.runtime.execution.interceptor.InterceptorChain;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class InterceptorChainTestCase extends AbstractMuleTestCase {

  @Mock
  private Interceptor<OperationModel> interceptor1;

  @Mock
  private Interceptor<OperationModel> interceptor2;

  @Mock
  private Interceptor<OperationModel> interceptor3;

  @Mock
  private ExecutionContext<OperationModel> ctx;

  @Mock
  private ExecutorCallback callback;

  private InterceptorChain chain;
  private InOrder inOrder;

  @Before
  public void before() {
    inOrder = inOrder(interceptor1, interceptor2, interceptor3);

    chain = InterceptorChain.builder()
        .addInterceptor(interceptor1)
        .addInterceptor(interceptor2)
        .addInterceptor(interceptor3)
        .build();

    mockErrorIdentity(interceptor1, interceptor2, interceptor3);
  }

  @Test
  public void beforePhase() throws Throwable {
    assertThat(chain.before(ctx, callback), is(nullValue()));
    inOrder.verify(interceptor1).before(ctx);
    inOrder.verify(interceptor2).before(ctx);
    inOrder.verify(interceptor3).before(ctx);

    inOrder.verify(interceptor1, never()).after(any(), any());
    inOrder.verify(interceptor2, never()).after(any(), any());
    inOrder.verify(interceptor3, never()).after(any(), any());
  }

  @Test
  public void beforeWithLastInterceptorFail() throws Throwable {
    Exception e = new Exception();
    Exception wrapped = new RuntimeException(e);

    doThrow(e).when(interceptor3).before(any());
    when(interceptor2.onError(ctx, e)).thenReturn(wrapped);

    assertThat(chain.before(ctx, callback), is(sameInstance(wrapped)));

    inOrder.verify(interceptor1).before(ctx);
    inOrder.verify(interceptor2).before(ctx);
    inOrder.verify(interceptor3).before(ctx);
    inOrder.verify(interceptor2).onError(ctx, e);
    inOrder.verify(interceptor2).after(ctx, null);
    inOrder.verify(interceptor1).onError(ctx, wrapped);
    inOrder.verify(interceptor1).after(ctx, null);
    verify(callback).error(wrapped);
  }

  @Test
  public void beforeWithLastInterceptorFailAndAfterFailsAsWell() throws Throwable {
    Exception e = new Exception();
    Exception wrapped = new RuntimeException(e);

    doThrow(e).when(interceptor3).before(any());
    when(interceptor2.onError(ctx, e)).thenReturn(wrapped);
    mockAfterFails(new RuntimeException(), interceptor1, interceptor2, interceptor3);

    assertThat(chain.before(ctx, callback), is(sameInstance(wrapped)));

    inOrder.verify(interceptor1).before(ctx);
    inOrder.verify(interceptor2).before(ctx);
    inOrder.verify(interceptor3).before(ctx);
    inOrder.verify(interceptor2).onError(ctx, e);
    inOrder.verify(interceptor2).after(ctx, null);
    inOrder.verify(interceptor1).onError(ctx, wrapped);
    inOrder.verify(interceptor1).after(ctx, null);
    verify(callback).error(wrapped);
  }

  @Test
  public void onSuccess() throws Throwable {
    Object result = new Object();
    chain.onSuccess(ctx, result);

    inOrder.verify(interceptor1).onSuccess(ctx, result);
    inOrder.verify(interceptor1).after(ctx, result);
    inOrder.verify(interceptor2).onSuccess(ctx, result);
    inOrder.verify(interceptor2).after(ctx, result);
    inOrder.verify(interceptor3).onSuccess(ctx, result);
    inOrder.verify(interceptor3).after(ctx, result);
  }

  @Test
  public void onSuccessWithFailure() throws Throwable {
    Object result = new Object();
    doThrow(new RuntimeException()).when(interceptor2).onSuccess(ctx, result);
    chain.onSuccess(ctx, result);

    inOrder.verify(interceptor1).onSuccess(ctx, result);
    inOrder.verify(interceptor1).after(ctx, result);
    inOrder.verify(interceptor2).onSuccess(ctx, result);
    inOrder.verify(interceptor2).after(ctx, result);
    inOrder.verify(interceptor3).onSuccess(ctx, result);
    inOrder.verify(interceptor3).after(ctx, result);
  }


  @Test
  public void onSuccessWithFailureAndAfterFailsAsWell() throws Throwable {
    Object result = new Object();
    doThrow(new RuntimeException()).when(interceptor2).onSuccess(ctx, result);
    mockAfterFails(new RuntimeException(), interceptor1, interceptor2, interceptor3);

    chain.onSuccess(ctx, result);

    inOrder.verify(interceptor1).onSuccess(ctx, result);
    inOrder.verify(interceptor1).after(ctx, result);
    inOrder.verify(interceptor2).onSuccess(ctx, result);
    inOrder.verify(interceptor2).after(ctx, result);
    inOrder.verify(interceptor3).onSuccess(ctx, result);
    inOrder.verify(interceptor3).after(ctx, result);
  }

  @Test
  public void onError() throws Throwable {
    Exception e = new Exception();
    chain.onError(ctx, e);

    inOrder.verify(interceptor1).onError(ctx, e);
    inOrder.verify(interceptor1).after(ctx, null);
    inOrder.verify(interceptor2).onError(ctx, e);
    inOrder.verify(interceptor2).after(ctx, null);
    inOrder.verify(interceptor3).onError(ctx, e);
    inOrder.verify(interceptor3).after(ctx, null);
  }

  @Test
  public void onErrorWithFailure() throws Throwable {
    Exception e = new Exception();
    doThrow(new RuntimeException()).when(interceptor2).onError(ctx, new RuntimeException());

    chain.onError(ctx, e);

    inOrder.verify(interceptor1).onError(ctx, e);
    inOrder.verify(interceptor1).after(ctx, null);
    inOrder.verify(interceptor2).onError(ctx, e);
    inOrder.verify(interceptor2).after(ctx, null);
    inOrder.verify(interceptor3).onError(ctx, e);
    inOrder.verify(interceptor3).after(ctx, null);
  }

  @Test
  public void onErrorWithFailureFailsAsWell() throws Throwable {
    Exception e = new Exception();
    doThrow(new RuntimeException()).when(interceptor2).onError(ctx, new RuntimeException());
    mockAfterFails(new RuntimeException(), interceptor1, interceptor2, interceptor3);

    chain.onError(ctx, e);

    inOrder.verify(interceptor1).onError(ctx, e);
    inOrder.verify(interceptor1).after(ctx, null);
    inOrder.verify(interceptor2).onError(ctx, e);
    inOrder.verify(interceptor2).after(ctx, null);
    inOrder.verify(interceptor3).onError(ctx, e);
    inOrder.verify(interceptor3).after(ctx, null);
  }

  @Test
  public void noInterceptors() {
    chain = InterceptorChain.builder().build();
    Object result = mock(Object.class);
    Exception e = spy(new Exception());

    assertThat(chain.before(ctx, callback), is(nullValue()));
    chain.onSuccess(ctx, result);
    assertThat(chain.onError(ctx, e), is(sameInstance(e)));

    verifyZeroInteractions(ctx, callback);
  }

  private void mockErrorIdentity(Interceptor<OperationModel>... interceptors) {
    for (Interceptor<OperationModel> interceptor : interceptors) {
      when(interceptor.onError(any(), any())).thenAnswer(inv -> inv.getArgument(1));
    }
  }

  private void mockAfterFails(Exception exception, Interceptor<OperationModel>... interceptors) {
    for (Interceptor<OperationModel> interceptor : interceptors) {
      doThrow(exception).when(interceptor).after(any(), any());
    }
  }
}
