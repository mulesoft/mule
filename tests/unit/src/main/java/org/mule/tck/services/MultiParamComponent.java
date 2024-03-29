/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.services;

/**
 * A simple test component used for testing multiple parameters
 */
public class MultiParamComponent {

  public String append(String param1, String param2) {
    return param1 + param2;
  }
}
