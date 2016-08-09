/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.spring.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mule.runtime.module.http.api.HttpConstants.HttpStatus.UNAUTHORIZED;

import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

public class CustomSecManagerHttpFilterFunctionalTestCase extends FunctionalTestCase {

  @Rule
  public DynamicPort dynamicPort = new DynamicPort("port1");

  @Override
  protected String getConfigFile() {
    return "custom-security-manager-test.xml";
  }

  protected String getUrl() {
    return "http://localhost:" + dynamicPort.getValue() + "/authenticate";
  }

  @Test
  public void testAuthenticationFailureNoContext() throws Exception {
    HttpClient client = new HttpClient();
    client.getParams().setAuthenticationPreemptive(true);
    GetMethod get = new GetMethod(getUrl());

    get.setDoAuthentication(false);

    try {
      int status = client.executeMethod(get);
      assertEquals(UNAUTHORIZED.getStatusCode(), status);
      assertTrue(get.getResponseBodyAsString().contains("no security context on the session. Authentication denied on endpoint"));
    } finally {
      get.releaseConnection();
    }
  }

  @Test
  public void testAuthenticationFailureBadCredentials() throws Exception {
    doRequest(null, "localhost", "anonX", "anonX", getUrl(), false, 401);
  }

  @Ignore // TODO Realm validataion seems to be completely ignored
  @Test
  public void testAuthenticationFailureBadRealm() throws Exception {
    doRequest("blah", "localhost", "anon", "anon", getUrl(), false, 401);
  }

  @Test
  public void testAuthenticationAuthorised() throws Exception {
    doRequest(null, "localhost", "anon", "anon", getUrl(), false, 200);
  }

  @Test
  public void testAuthenticationAuthorisedWithHandshake() throws Exception {
    doRequest(null, "localhost", "anon", "anon", getUrl(), true, 200);
  }

  @Ignore // TODO Realm validataion seems to be completely ignored
  @Test
  public void testAuthenticationAuthorisedWithHandshakeAndBadRealm() throws Exception {
    doRequest("blah", "localhost", "anon", "anon", getUrl(), true, 401);
  }

  @Test
  public void testAuthenticationAuthorisedWithHandshakeAndRealm() throws Exception {
    doRequest("mule-realm", "localhost", "ross", "ross", getUrl(), true, 200);
  }

  private void doRequest(String realm, String host, String user, String pass, String url, boolean handshake, int result)
      throws Exception {
    HttpClient client = new HttpClient();
    client.getParams().setAuthenticationPreemptive(true);
    client.getState().setCredentials(new AuthScope(host, -1, realm), new UsernamePasswordCredentials(user, pass));
    GetMethod get = new GetMethod(url);
    get.setDoAuthentication(handshake);

    try {
      int status = client.executeMethod(get);
      if (status == UNAUTHORIZED.getStatusCode() && handshake == true) {
        // doAuthentication = true means that if the request returns 401,
        // the HttpClient will resend the request with credentials
        status = client.executeMethod(get);
      }
      assertEquals(result, status);
    } finally {
      get.releaseConnection();
    }
  }
}

