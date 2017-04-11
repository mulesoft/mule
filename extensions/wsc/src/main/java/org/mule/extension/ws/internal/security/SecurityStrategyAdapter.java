/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.internal.security;

import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.soap.security.SecurityStrategy;

/**
 * Adapter interface for classes that wraps a security class for the SOAP Protocol.
 * 
 * @since 4.0
 */
@Alias("security-strategy")
public interface SecurityStrategyAdapter {

  /**
   * @return the {@link SecurityStrategy} needed by the soap-service.
   */
  SecurityStrategy getSecurityStrategy();
}
