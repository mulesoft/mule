/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.infrastructure.process;

public class MuleProcessControllerFactory {

  public MuleProcessController create(String muleHome) {
    return new MuleProcessController(muleHome);
  }

  public MuleProcessController create(String muleHome, int timeout) {
    return new MuleProcessController(muleHome, timeout);
  }

  public MuleProcessController create(String muleHome, String locationSuffix) {
    return new MuleProcessController(muleHome, locationSuffix);
  }
}
