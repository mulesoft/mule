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
import org.mule.runtime.extension.api.soap.security.VerifySignatureSecurityStrategy;
import org.mule.runtime.extension.api.soap.security.config.WssTrustStoreConfiguration;
import org.mule.services.soap.service.VerifyPasswordCallback;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.ws.security.wss4j.WSS4JOutInterceptor;
import org.apache.ws.security.components.crypto.Merlin;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features(WSC_EXTENSION)
@Stories("WSS")
public class WssVerifySignatureTestCase extends AbstractWebServiceSecurityTestCase {

  @Override
  protected Interceptor buildOutInterceptor() {
    final Map<String, Object> props = new HashMap<>();
    props.put("action", "Signature");
    props.put("signatureUser", "muleserver");
    props.put("passwordCallbackClass", VerifyPasswordCallback.class.getName());

    final String signaturePropRefId = "serverOutSecurityProperties";
    props.put("signaturePropRefId", signaturePropRefId);
    final Properties securityProperties = new Properties();
    securityProperties.put("org.apache.ws.security.crypto.provider", Merlin.class.getName());
    securityProperties.put("org.apache.ws.security.crypto.merlin.keystore.type", "jks");
    securityProperties.put("org.apache.ws.security.crypto.merlin.keystore.password", "mulepassword");
    securityProperties.put("org.apache.ws.security.crypto.merlin.keystore.private.password", "mulepassword");
    securityProperties.put("org.apache.ws.security.crypto.merlin.keystore.alias", "muleserver");
    securityProperties.put("org.apache.ws.security.crypto.merlin.keystore.file", "security/serverKeystore");
    props.put(signaturePropRefId, securityProperties);

    return new WSS4JOutInterceptor(props);
  }

  @Override
  protected List<SecurityStrategy> getSecurityStrategies() {
    return singletonList(
                         new VerifySignatureSecurityStrategy(new WssTrustStoreConfiguration("security/trustStore", "mulepassword",
                                                                                            "jks")));
  }
}
