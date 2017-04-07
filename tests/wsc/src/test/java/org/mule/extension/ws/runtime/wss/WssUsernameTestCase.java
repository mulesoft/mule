/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.runtime.wss;

import static org.mule.test.allure.AllureConstants.WscFeature.WSC_EXTENSION;

import org.mule.extension.ws.service.ServerPasswordCallback;

import java.util.HashMap;
import java.util.Map;

import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.ws.security.wss4j.WSS4JInInterceptor;

import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features(WSC_EXTENSION)
@Stories("WSS")
public class WssUsernameTestCase extends AbstractWebServiceSecurityTestCase {

  private final static String USERNAME = "username";

  public WssUsernameTestCase() {
    super(USERNAME);
  }

  @Override
  protected Interceptor buildInInterceptor() {
    final Map<String, Object> props = new HashMap<>();
    props.put("action", "UsernameToken");
    props.put("passwordCallbackClass", ServerPasswordCallback.class.getName());
    return new WSS4JInInterceptor(props);
  }
}
