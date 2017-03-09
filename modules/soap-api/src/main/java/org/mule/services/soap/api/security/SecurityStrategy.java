/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.services.soap.api.security;

/**
 * Contract for objects that adds a level of security to the SOAP Protocol.
 *
 * @since 4.0
 */
public interface SecurityStrategy {

  /**
   * Dispatches in a reflective way to the method with the specific {@link SecurityStrategy} type as argument.
   *
   * @param visitor the accepted visitor.
   */
  void accept(SecurityStrategyVisitor visitor);

}
