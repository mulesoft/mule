/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.service.test.api;

import org.mule.runtime.api.service.Service;

/**
 * Service API used for testing purposes.
 */
public interface FooService extends Service {

  /**
   * Does some foo with an input message.
   *
   * @param message text to be processed.
   * @return the processed message.
   */
  String doFoo(String message);
}
