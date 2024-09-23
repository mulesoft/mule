/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package com.example.external;

public class ExternalClass {

  // simulates an external library by being outside of application's package structure
  public ExternalClass() {}

  public static String getMessage() {
    return "Hello from External Library";
  }

}
