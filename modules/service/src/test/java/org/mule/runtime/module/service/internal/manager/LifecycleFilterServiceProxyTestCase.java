/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.service.internal.manager;

import static org.hamcrest.CoreMatchers.is;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mule.runtime.module.service.internal.manager.LifecycleFilterServiceProxy.createLifecycleFilterServiceProxy;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.api.service.Service;
import org.mule.runtime.api.exception.DefaultMuleException;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class LifecycleFilterServiceProxyTestCase extends AbstractMuleTestCase {

  @Rule
  public ExpectedException expected = ExpectedException.none();

  @Test
  public void avoidsStartExecution() throws Exception {

    StartableService service = mock(StartableService.class);

    final Startable serviceProxy = (Startable) createLifecycleFilterServiceProxy(service);

    expected.expect(UnsupportedOperationException.class);
    serviceProxy.start();
  }

  @Test
  public void avoidsStopExecution() throws Exception {
    StoppableService service = mock(StoppableService.class);

    final Stoppable serviceProxy = (Stoppable) createLifecycleFilterServiceProxy(service);

    expected.expect(UnsupportedOperationException.class);
    serviceProxy.stop();
  }

  @Test
  public void checkedExceptionThrownUnwrapped() throws Exception {
    CheckedExceptionService checkExceptionService = mock(CheckedExceptionService.class);
    DefaultMuleException checkedException = new DefaultMuleException("ERROR");
    doThrow(checkedException).when(checkExceptionService).execute();

    final CheckedExceptionService checkedExceptionServiceProxy =
        (CheckedExceptionService) createLifecycleFilterServiceProxy(checkExceptionService);

    expected.expect(is(checkedException));
    checkedExceptionServiceProxy.execute();
  }

  @Test
  public void uncheckedExceptionThrownUnwrapped() throws Exception {
    UncheckedExceptionService uncheckedExceptionService = mock(UncheckedExceptionService.class);
    RuntimeException uncheckedException = new RuntimeException();
    doThrow(uncheckedException).when(uncheckedExceptionService).execute();

    final UncheckedExceptionService uncheckedExceptionServiceProxy =
        (UncheckedExceptionService) createLifecycleFilterServiceProxy(uncheckedExceptionService);

    expected.expect(is(uncheckedException));
    uncheckedExceptionServiceProxy.execute();
  }


  public interface StartableService extends Service, Startable {

  }

  public interface StoppableService extends Service, Stoppable {

  }

  public interface CheckedExceptionService extends Service {

    void execute() throws MuleException;
  }

  public interface UncheckedExceptionService extends Service {

    void execute();
  }


}
