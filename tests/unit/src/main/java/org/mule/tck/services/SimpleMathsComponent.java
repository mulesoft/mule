/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.tck.services;

import java.rmi.Remote;

public class SimpleMathsComponent implements Remote, AdditionService {

  public Integer addTen(Integer number) {
    return new Integer(number.intValue() + 10);
  }

  public int add(int[] args) {
    int result = 0;
    for (int i = 0; i < args.length; i++) {
      result += args[i];
    }
    return result;
  }
}
