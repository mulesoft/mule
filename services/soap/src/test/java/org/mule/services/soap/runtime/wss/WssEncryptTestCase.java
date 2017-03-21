/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.services.soap.runtime.wss;


import static java.util.Collections.singletonList;
import org.mule.services.soap.api.security.SecurityStrategy;
import org.mule.services.soap.api.security.EncryptSecurityStrategy;
import org.mule.services.soap.api.security.config.WssKeyStoreConfiguration;
import org.mule.services.soap.service.EncryptPasswordCallback;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.ws.security.wss4j.WSS4JInInterceptor;
import org.apache.ws.security.components.crypto.Merlin;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features("Web Service Consumer")
@Stories("WSS")
public class WssEncryptTestCase extends AbstractWebServiceSecurityTestCase {

  @Override
  protected Interceptor buildInInterceptor() {
    final Map<String, Object> props = new HashMap<>();
    props.put("action", "Encrypt");
    props.put("passwordCallbackClass", EncryptPasswordCallback.class.getName());
    final String decryptionPropRefId = "securityProperties";
    props.put("decryptionPropRefId", decryptionPropRefId);
    final Properties securityProperties = new Properties();
    securityProperties.put("org.apache.ws.security.crypto.provider", Merlin.class.getName());
    securityProperties.put("org.apache.ws.security.crypto.merlin.keystore.type", "jks");
    securityProperties.put("org.apache.ws.security.crypto.merlin.keystore.password", "changeit");
    securityProperties.put("org.apache.ws.security.crypto.merlin.keystore.private.password", "changeit");
    securityProperties.put("org.apache.ws.security.crypto.merlin.keystore.alias", "s1as");
    securityProperties.put("org.apache.ws.security.crypto.merlin.keystore.file", "security/ssltest-keystore.jks");
    props.put(decryptionPropRefId, securityProperties);
    return new WSS4JInInterceptor(props);
  }

  @Override
  protected List<SecurityStrategy> getSecurityStrategies() {
    return singletonList(new EncryptSecurityStrategy(
                                                     new WssKeyStoreConfiguration("s1as", "changeit",
                                                                                  "security/ssltest-cacerts.jks")));
  }
}
