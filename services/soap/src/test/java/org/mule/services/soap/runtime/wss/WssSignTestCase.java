/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.services.soap.runtime.wss;

import static org.mule.test.allure.AllureConstants.WscFeature.WSC_EXTENSION;
import static java.util.Collections.singletonList;
import org.mule.runtime.extension.api.soap.security.SecurityStrategy;
import org.mule.runtime.extension.api.soap.security.SignSecurityStrategy;
import org.mule.runtime.extension.api.soap.security.config.WssKeyStoreConfiguration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.ws.security.wss4j.WSS4JInInterceptor;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features(WSC_EXTENSION)
@Stories("WSS")
public class WssSignTestCase extends AbstractWebServiceSecurityTestCase {

  @Override
  protected Interceptor buildInInterceptor() {
    final Map<String, Object> props = new HashMap<>();
    props.put("action", "Signature");

    final String signaturePropRefId = "serverInSecurityProperties";
    props.put("signaturePropRefId", signaturePropRefId);
    final Properties securityProperties = new Properties();
    securityProperties.put("org.apache.ws.security.crypto.merlin.truststore.type", "jks");
    securityProperties.put("org.apache.ws.security.crypto.merlin.truststore.password", "mulepassword");
    securityProperties.put("org.apache.ws.security.crypto.merlin.truststore.file", "security/trustStore");
    props.put(signaturePropRefId, securityProperties);

    return new WSS4JInInterceptor(props);
  }

  @Override
  protected List<SecurityStrategy> getSecurityStrategies() {
    return singletonList(new SignSecurityStrategy(
                                                  new WssKeyStoreConfiguration("muleclient", "mulepassword", "mulepassword",
                                                                               "security/clientKeystore", "jks")));
  }
}
