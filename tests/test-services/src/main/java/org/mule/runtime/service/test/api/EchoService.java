/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.service.test.api;

import org.mule.runtime.api.service.Service;

/**
 * Service API used for testing purposes.
 */
public interface EchoService extends Service {

  /**
   * Echoes the input message.
   *
   * @param message text to be echoed.
   * @return the echoed message.
   */
  String echo(String message);
}
