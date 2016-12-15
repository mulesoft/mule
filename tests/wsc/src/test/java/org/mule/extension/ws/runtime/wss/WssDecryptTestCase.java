/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.runtime.wss;

import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features("Web Service Consumer")
@Stories("WSS")
public class WssDecryptTestCase extends AbstractWebServiceSecurityTestCase {

  private final static String TIMESTAMP = "timestamp";
  private final static String USERNAME = "username";
  private final static String VERIFY_SIGNATURE = "verify";
  private final static String SIGN = "sign";
  private final static String ENCRYPT = "encrypt";
  private final static String DECRYPT = "decrypt";

  public WssDecryptTestCase() {
    super(DECRYPT);
  }
}
