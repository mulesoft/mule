/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.test.mule.javaspecific;

import org.test.opt.javaspecific.FromThirdPartyLib;

public class FromMuleModule {

  public static String useThirdPartyLib() {
    try {
      FromMuleModule.class.getClassLoader().loadClass("org.test.user.FromUserLib");
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e.toString(), e);
    }

    return "OK";
  }

  public static String useThirdPartyUsingUserLib() {
    return FromThirdPartyLib.useLib("org.test.user.FromUserLib");
  }

  public static void thirdPartyUsingMuleLib() {
    // fails

  }

}
