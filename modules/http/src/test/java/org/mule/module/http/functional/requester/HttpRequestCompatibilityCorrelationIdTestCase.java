/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.requester;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.api.config.MuleProperties.MULE_CORRELATION_ID_PROPERTY;
import static org.mule.module.http.api.HttpConstants.HttpProperties.COMPATIBILITY_IGNORE_CORRELATION_ID;
import static org.mule.module.http.internal.listener.HttpRequestToMuleEvent.resetIgnoreCorrelationId;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Rule;
import org.mule.tck.junit4.rule.SystemProperty;

public class HttpRequestCompatibilityCorrelationIdTestCase extends HttpRequestCorrelationIdTestCase
{

  @Rule
  public SystemProperty ignoreCorrelationId = new SystemProperty(COMPATIBILITY_IGNORE_CORRELATION_ID, "true");

  @Before
  public void before() {
      resetIgnoreCorrelationId();
  }
  
  @AfterClass
  public static void afterClass() {
      resetIgnoreCorrelationId();
  }
  
  /**
   * To guarantee compatibility, the correlation ID should not be set from the received header and therefore not sent by the HTTP requester.
   */
  @Override
  protected void validateCorrelationId(String receivedCorrelationId)
  {
    assertThat(headers.get(MULE_CORRELATION_ID_PROPERTY), is(empty()));
  }

}
