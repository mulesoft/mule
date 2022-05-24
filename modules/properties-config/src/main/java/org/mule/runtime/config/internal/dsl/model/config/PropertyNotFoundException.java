/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.model.config;

import org.mule.runtime.api.util.Pair;

/**
 * Exception thrown when a key could not be resolved.
 *
 * @since 4.0
 */
public class PropertyNotFoundException extends org.mule.runtime.ast.api.exception.PropertyNotFoundException {

  /**
   * Creates a new instance. This constructor must be used when the resolver has no parent and was not able to resolve a key
   *
   * @param resolverKeyPair the resolver descriptor and the key that was not able to resolve.
   */
  public PropertyNotFoundException(Pair<String, String> resolverKeyPair) {
    super(resolverKeyPair);
  }

  /**
   * Creates a new instance. This constructor must be used when the resolver invoke the parent and it failed because it wasn't
   * able to resolve a key
   *
   * @param propertyNotFoundException exception thrown by the parent resolver.
   * @param resolverKeyPair           the resolver descriptor and the key that was not able to resolve.
   */
  public PropertyNotFoundException(PropertyNotFoundException propertyNotFoundException,
                                   Pair<String, String> resolverKeyPair) {
    super(propertyNotFoundException, resolverKeyPair);
  }
}
