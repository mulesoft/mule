/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.requester;

import static org.mule.module.http.internal.request.DefaultHttpRequester.RETRY_ON_ALL_METHODS_PROPERTY;
import org.mule.tck.junit4.rule.SystemProperty;

import org.junit.Rule;

public class HttpRetryRequestOnAllMethodsTestCase extends HttpRetryRequestTestCase
{

  @Rule
  public SystemProperty retryOnAllMethods = new SystemProperty(RETRY_ON_ALL_METHODS_PROPERTY, "true");

  public HttpRetryRequestOnAllMethodsTestCase(Integer retryAttempts)
  {
    super(retryAttempts);
  }

  @Override
  protected int getIdempotentMethodExpectedRetries()
  {
    return retryAttempts;
  }
}
