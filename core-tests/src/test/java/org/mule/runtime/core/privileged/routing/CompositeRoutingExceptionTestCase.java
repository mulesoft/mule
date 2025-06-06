/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.routing;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.message.Error;

import java.util.Map;

import org.junit.Test;

public class CompositeRoutingExceptionTestCase {

  private Error mockErrorWithCause(Throwable cause) {
    Error error = mock(Error.class);
    when(error.getCause()).thenReturn(cause);
    return error;
  }

  @Test
  public void testGetLegacyDetailedMessage() {
    // Force the use of the legacy message construction by returning an empty map on RoutingResult.getFailuresWithExceptionInfo()
    RoutingResult result = mock(RoutingResult.class);
    when(result.getFailuresWithExceptionInfo()).thenReturn(Map.of());

    // Mock the getFailures method to return a map with one entry
    Map<String, Error> failures = Map.of("route1", mockErrorWithCause(new RuntimeException("Route 1 failed")));
    when(result.getFailures()).thenReturn(failures);
    CompositeRoutingException exception = new CompositeRoutingException(result);

    // Call getDetailedMessage to trigger the legacy message construction
    String message = exception.getDetailedMessage();

    // Assert that the message is constructed correctly
    assertThat(message, is("""
        Exception(s) were found for route(s): \n
        Route route1: Caught exception in Exception Strategy: Route 1 failed"""));
  }
}
