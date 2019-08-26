/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.api.component;

import static java.util.Arrays.asList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mule.runtime.api.component.AbstractComponent.ROOT_CONTAINER_NAME_KEY;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static reactor.core.publisher.Flux.just;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import com.google.common.collect.ImmutableMap;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import reactor.core.publisher.Flux;

public class OnErrorCheckLogHandlerTestCase extends AbstractMuleContextTestCase {

  private static final LogChecker successfulChecker = mock(LogChecker.class);
  private static final LogChecker failingChecker = mock(LogChecker.class);

  private OnErrorCheckLogHandler checkLogHandler;

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @BeforeClass
  public static void serMocks() {
    doNothing().when(successfulChecker).check(any());
    doThrow(new AssertionError()).when(failingChecker).check(any());
  }

  @Before
  public void resetLogHandler() throws Exception {
    checkLogHandler = new OnErrorCheckLogHandler();
    checkLogHandler.setAnnotations(ImmutableMap.of(ROOT_CONTAINER_NAME_KEY, "someContainerName"));
    initialiseIfNeeded(checkLogHandler, muleContext);
    checkLogHandler.start();
  }

  @Test
  public void handlerFailsIfThereIsNoExceptionToHandle() throws Exception {
    expectedException.expect(AssertionError.class);
    expectedException.expectMessage("Handler could not check any exception log because no exception was raised");
    checkLogHandler.verify();
  }

  @Test
  public void handlerSucceedsByDefault() throws Exception {
    assertHandler();
  }

  @Test
  public void handlerSucceedsIfSuccessfulChecker() throws Exception {
    checkLogHandler.setCheckers(asList(successfulChecker));
    assertHandler();
  }

  @Test
  public void handlerFailsIfFailingChecker() throws Exception {
    checkLogHandler.setCheckers(asList(failingChecker));
    expectedException.expect(AssertionError.class);
    assertHandler();
  }

  @Test
  public void handlerFailsIfAtLeastOneFailingChecker() throws Exception {
    checkLogHandler.setCheckers(asList(successfulChecker, successfulChecker, failingChecker, successfulChecker));
    expectedException.expect(AssertionError.class);
    assertHandler();
  }

  @Test
  public void failIfNoLogAndFlagNotSet() throws Exception {
    expectedException.expect(AssertionError.class);
    handleException();
    checkLogHandler.verify();
  }

  @Test
  public void succeedsIfNotLogAndFlagSet() throws Exception {
    checkLogHandler.setSucceedIfNoLog(true);
    handleException();
    checkLogHandler.verify();
  }

  private void assertHandler() throws Exception {
    handleException();
    checkLogHandler.doLogException(null, null);
    checkLogHandler.verify();
  }

  private void handleException() {
    Flux.from(checkLogHandler.route(just(mock(CoreEvent.class)))).subscribe(e -> {
    }, e -> {
    });
  }

}
