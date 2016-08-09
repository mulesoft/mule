/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
