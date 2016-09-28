/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.module.spring.security;

import org.mule.runtime.module.spring.security.SecureHttpPollingFunctionalTestCase;

public class SSSecureHttpPollingFunctionalTestCase extends SecureHttpPollingFunctionalTestCase {

  @Override
  protected String[] getConfigFiles() {
    return new String[] {"spring-security/secure-http-polling-server-flow.xml", "secure-http-polling-client-flow.xml"};
  }

}
