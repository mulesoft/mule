/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
