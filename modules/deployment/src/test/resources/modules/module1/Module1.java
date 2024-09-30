/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package modules.module1;

import modules.module2.Module2;

public class Module1 {
  private static Module2 module2 = new Module2();

  public static String getMessage() {
    return "Hi, I'm Module1 depending on " + module2.getName();
  }
}
