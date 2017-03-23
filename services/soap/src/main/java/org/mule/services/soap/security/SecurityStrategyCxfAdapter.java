/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.services.soap.security;


import org.mule.services.soap.security.callback.WSPasswordCallbackHandler;

import java.util.Map;
import java.util.Optional;

/**
 * Base contract for objects that adds a level of security to the SOAP Protocol by preparing a set of CXF properties.
 * <p>
 * All securities have an Action Name and a Type (Whether should be applied to the SOAP request or SOAP response), and returns
 * a set of properties that needs to be set in the client to make it work.
 *
 * @since 4.0
 */
public interface SecurityStrategyCxfAdapter {

  /**
   * Returns the security action name that is going to be executed in the request phase (OUT interceptors).
   *
   * @return the request action name of {@code this} security strategy.
   */
  String securityAction();

  /**
   * Returns the type of the security strategy, if applies for the request or the response.
   *
   * @return whether is the security strategy applies for the request or the response.
   */
  SecurityStrategyType securityType();

  /**
   * Returns a set of properties to be set on the SOAP request interceptors (OUT interceptors) in order to
   * applies {@code this} security strategy.
   *
   * @return a {@link Map} with the properties required to apply the security strategy.
   */
  Map<String, Object> buildSecurityProperties();

  /**
   * Gives the option to return a custom {@link WSPasswordCallbackHandler} instance allowing to compose many password handlers
   * from different security strategies.
   *
   * @return an optional {@link WSPasswordCallbackHandler} to be added to the composite callback handler.
   */
  Optional<WSPasswordCallbackHandler> buildPasswordCallbackHandler();
}
