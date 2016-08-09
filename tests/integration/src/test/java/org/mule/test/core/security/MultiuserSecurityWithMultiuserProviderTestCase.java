/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.core.security;

/**
 * Tests multi-user security against a security provider which holds authentications for multiple users concurrently.
 * 
 * see EE-979
 */
public class MultiuserSecurityWithMultiuserProviderTestCase extends MultiuserSecurityTestCase {

  @Override
  protected String[] getConfigFiles() {
    return new String[] {"multiuser-security-test-flow.xml", "multiuser-security-provider.xml"};
  }
}
