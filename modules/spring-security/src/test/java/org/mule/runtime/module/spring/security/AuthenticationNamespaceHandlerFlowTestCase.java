/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.spring.security;

import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;

public class AuthenticationNamespaceHandlerFlowTestCase extends AuthenticationNamespaceHandlerTestCase {

  @Rule
  public DynamicPort port1 = new DynamicPort("port1");

  @Override
  protected String getConfigFile() {
    return "authentication-config-flow.xml";
  }

}
