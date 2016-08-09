/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.module.spring.security;

import org.mule.runtime.module.spring.security.AuthenticationNamespaceHandlerFlowTestCase;

public class SSAuthenticationNamespaceHandlerFlowTestCase extends AuthenticationNamespaceHandlerFlowTestCase {

  @Override
  protected String getConfigFile() {
    return "spring-security/authentication-config-flow.xml";
  }

}
