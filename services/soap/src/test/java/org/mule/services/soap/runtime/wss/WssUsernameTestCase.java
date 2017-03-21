/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.services.soap.runtime.wss;

import static java.util.Collections.singletonList;
import static org.mule.services.soap.api.security.PasswordType.TEXT;
import org.mule.services.soap.api.security.SecurityStrategy;
import org.mule.services.soap.api.security.UsernameTokenSecurityStrategy;
import org.mule.services.soap.service.ServerPasswordCallback;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.ws.security.wss4j.WSS4JInInterceptor;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features("Web Service Consumer")
@Stories("WSS")
public class WssUsernameTestCase extends AbstractWebServiceSecurityTestCase {

  @Override
  protected Interceptor buildInInterceptor() {
    final Map<String, Object> props = new HashMap<>();
    props.put("action", "UsernameToken");
    props.put("passwordCallbackClass", ServerPasswordCallback.class.getName());
    return new WSS4JInInterceptor(props);
  }

  @Override
  protected List<SecurityStrategy> getSecurityStrategies() {
    return singletonList(new UsernameTokenSecurityStrategy("admin", "textPassword", TEXT, true, true));
  }
}
