/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.oauth2.internal;

/**
 * OAuth app client credentials
 */
public interface ApplicationCredentials {

  /**
   * @return oauth client secret of the hosted application
   */
  String getClientSecret();

  /**
   * @return oauth client id of the hosted application
   */
  String getClientId();

}
