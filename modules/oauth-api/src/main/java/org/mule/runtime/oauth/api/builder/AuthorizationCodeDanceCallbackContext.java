/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.oauth.api.builder;

import org.mule.runtime.oauth.api.AuthorizationCodeOAuthDancer;

import java.util.Optional;

/**
 * Implementations keep context information between the before and after callbacks set for an
 * {@link AuthorizationCodeOAuthDancer}.
 * 
 * @since 4.0
 */
public interface AuthorizationCodeDanceCallbackContext {

  /**
   * Allows access to an attribute of this context, as defined by the implementation.
   * 
   * @param paramKey the key of the parameter to get from the context.
   * @return an {@link Optional} containing the value for the requested {@code paramKey} if present, or {@code empty} if not.
   */
  Optional<Object> getParameter(String paramKey);
}
