/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.test.opt.javaspecific;


public class FromThirdPartyLib {

  public static String useLib(String className) {
    try {
      FromThirdPartyLib.class.getClassLoader().loadClass(className);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e.toString(), e);
    }

    return "OK";
  }

}
