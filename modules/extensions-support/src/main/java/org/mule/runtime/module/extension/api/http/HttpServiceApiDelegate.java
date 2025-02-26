/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.api.http;

import org.mule.sdk.api.http.HttpServiceApi;

public class HttpServiceApiDelegate implements HttpServiceApi {

  @Override
  public void printThis(String message) {
    System.out.println("I'm the implementation, and your message is: " + message);
  }
}
