/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.api.security;

import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.services.soap.api.security.SecurityStrategy;

/**
 * Base contract for classes that adds a level of security to the SOAP Protocol.
 * <p>
 * All securities have an Action Name and a Type (Whether should be applied to the SOAP request or SOAP response), and returns
 * a set of properties that needs to be set in the client to make it work.
 *
 * @since 4.0
 */
@Alias("security-strategy")
public interface SecurityStrategyAdapter {

  /**
   * Returns the security action name that is going to be executed in the request phase (OUT interceptors).
   *
   * @return the request action name of {@code this} security strategy.
   */
  SecurityStrategy getSecurityStrategy();
}
