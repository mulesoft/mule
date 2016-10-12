/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.http.functional.tls;

import org.mule.runtime.core.util.FileUtils;
import org.mule.test.module.http.functional.AbstractHttpTestCase;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;

import javax.net.ssl.SSLContext;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;


public abstract class AbstractHttpTlsContextTestCase extends AbstractHttpTestCase {

  private static final String keyStorePath = "tls/ssltest-keystore.jks";
  private static final String trustStorePath = "tls/ssltest-cacerts.jks";
  private static final String storePassword = "changeit";
  private static final String keyPassword = "changeit";
  private static final String protocol = "TLSv1.2";

  protected static HttpResponse executeGetRequest(String url) throws IOException, GeneralSecurityException {
    HttpClient client = getSecureClient();
    HttpGet getMethod = new HttpGet(url);
    return client.execute(getMethod);
  }

  private static HttpClient getSecureClient() throws IOException, GeneralSecurityException {
    HttpClient secureClient;
    secureClient = HttpClients.custom()
        .setSslcontext(getSslContext())
        .build();
    return secureClient;
  }

  private static SSLContext getSslContext() throws IOException, GeneralSecurityException {
    SSLContext customSslContext;
    File keyStore = FileUtils.getFile(FileUtils.getResourcePath(keyStorePath, AbstractHttpTlsContextTestCase.class));
    File trustStore = FileUtils.getFile(FileUtils.getResourcePath(trustStorePath, AbstractHttpTlsContextTestCase.class));
    char[] storePass = storePassword.toCharArray();
    char[] keyPass = keyPassword.toCharArray();
    customSslContext =
        SSLContexts.custom()
            .useProtocol(protocol)
            .loadKeyMaterial(keyStore, storePass, keyPass)
            .loadTrustMaterial(trustStore, storePass)
            .build();
    return customSslContext;
  }
}
