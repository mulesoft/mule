/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
