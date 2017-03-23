/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.services.soap.runtime.wss;

import static java.util.Collections.singletonList;
import org.mule.services.soap.api.security.SecurityStrategy;
import org.mule.services.soap.api.security.DecryptSecurityStrategy;
import org.mule.services.soap.api.security.config.WssKeyStoreConfiguration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.ws.security.wss4j.WSS4JOutInterceptor;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features("Web Service Consumer")
@Stories("WSS")
public class WssDecryptTestCase extends AbstractWebServiceSecurityTestCase {

  @Override
  protected Interceptor buildOutInterceptor() {
    final Map<String, Object> props = new HashMap<>();
    props.put("action", "Encrypt");
    props.put("encryptionUser", "s1as");

    final String encryptionPropRefId = "securityProperties";
    props.put("encryptionPropRefId", encryptionPropRefId);
    final Properties securityProperties = new Properties();
    securityProperties.put("org.apache.ws.security.crypto.merlin.truststore.type", "jks");
    securityProperties.put("org.apache.ws.security.crypto.merlin.truststore.password", "changeit");
    securityProperties.put("org.apache.ws.security.crypto.merlin.truststore.file", "security/ssltest-cacerts.jks");
    props.put(encryptionPropRefId, securityProperties);

    return new WSS4JOutInterceptor(props);
  }

  @Override
  protected List<SecurityStrategy> getSecurityStrategies() {
    return singletonList(new DecryptSecurityStrategy(
                                                     new WssKeyStoreConfiguration("s1as", "changeit", "changeit",
                                                                                  "security/ssltest-keystore.jks", "jks")));
  }
}
