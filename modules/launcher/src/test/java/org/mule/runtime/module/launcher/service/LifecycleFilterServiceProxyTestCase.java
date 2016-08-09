/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.launcher.service;

import static org.mockito.Mockito.mock;
import org.mule.runtime.api.service.Service;
import org.mule.runtime.core.api.lifecycle.Startable;
import org.mule.runtime.core.api.lifecycle.Stoppable;
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

    final Startable serviceProxy = (Startable) LifecycleFilterServiceProxy.createServiceProxy(service);

    expected.expect(UnsupportedOperationException.class);
    serviceProxy.start();
  }

  @Test
  public void avoidsStopExecution() throws Exception {
    StoppableService service = mock(StoppableService.class);

    final Stoppable serviceProxy = (Stoppable) LifecycleFilterServiceProxy.createServiceProxy(service);

    expected.expect(UnsupportedOperationException.class);
    serviceProxy.stop();
  }

  public interface StartableService extends Service, Startable {

  }

  public interface StoppableService extends Service, Stoppable {

  }
}
