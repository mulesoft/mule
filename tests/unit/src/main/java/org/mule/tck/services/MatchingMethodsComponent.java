/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.services;

import static org.apache.commons.lang3.StringUtils.reverse;

import java.rmi.Remote;

/**
 * A test service that has two service methods with matching signature
 */
public class MatchingMethodsComponent implements Remote {

  public String reverseString(String string) {
    return reverse(string);
  }

  public String upperCaseString(String string) {
    return string.toUpperCase();
  }
}
