/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.oauth;

import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.connectivity.ConnectionProviders;
import org.mule.runtime.extension.api.annotation.dsl.xml.Xml;
import org.mule.runtime.extension.api.connectivity.oauth.AuthCodeRequest;
import org.mule.runtime.extension.api.connectivity.oauth.AuthorizationCodeState;

import java.util.LinkedList;
import java.util.List;

@Extension(name = "Test OAuth Extension")
@ConnectionProviders({TestOAuthConnectionProvider.class, ScopelessOAuthConnectionProvider.class})
@Operations(TestOAuthOperations.class)
@Xml(prefix = "test-oauth")
public class TestOAuthExtension {

  private List<AuthCodeRequest> capturedAuthCodeRequests = new LinkedList<>();
  private List<AuthorizationCodeState> capturedAuthCodeStates = new LinkedList<>();

  public List<AuthCodeRequest> getCapturedAuthCodeRequests() {
    return capturedAuthCodeRequests;
  }

  public List<AuthorizationCodeState> getCapturedAuthCodeStates() {
    return capturedAuthCodeStates;
  }
}
