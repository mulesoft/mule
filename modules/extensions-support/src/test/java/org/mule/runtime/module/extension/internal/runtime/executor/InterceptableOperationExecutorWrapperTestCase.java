/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.executor;

import static org.mockito.Mockito.verify;
import org.mule.runtime.core.api.lifecycle.Disposable;
import org.mule.runtime.core.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.lifecycle.Lifecycle;
import org.mule.runtime.core.api.lifecycle.Startable;
import org.mule.runtime.core.api.lifecycle.Stoppable;
import org.mule.runtime.extension.api.runtime.operation.OperationContext;
import org.mule.runtime.extension.api.runtime.operation.OperationExecutor;
import org.mule.runtime.module.extension.internal.AbstractInterceptableContractTestCase;

import org.junit.Test;
import org.mockito.Mock;

public class InterceptableOperationExecutorWrapperTestCase
    extends AbstractInterceptableContractTestCase<InterceptableOperationExecutorWrapper> {

  @Mock(extraInterfaces = Lifecycle.class)
  private OperationExecutor executor;

  @Mock
  private OperationContext operationContext;

  @Override
  protected InterceptableOperationExecutorWrapper createInterceptable() {
    return new InterceptableOperationExecutorWrapper(executor, getInterceptors());
  }

  @Test
  public void execute() throws Exception {
    interceptable.execute(operationContext);
    verify(executor).execute(operationContext);
  }

  @Test
  public void executorInjected() throws Exception {
    interceptable.initialise();
    verify(injector).inject(executor);
  }

  @Test
  public void executorInitialised() throws Exception {
    interceptable.initialise();
    verify((Initialisable) executor).initialise();
  }

  @Test
  public void executorStarted() throws Exception {
    interceptable.start();
    verify((Startable) executor).start();
  }

  @Test
  public void executorStopped() throws Exception {
    interceptable.stop();
    verify((Stoppable) executor).stop();
  }

  @Test
  public void executorDisposed() throws Exception {
    interceptable.dispose();
    verify((Disposable) executor).dispose();
  }
}
