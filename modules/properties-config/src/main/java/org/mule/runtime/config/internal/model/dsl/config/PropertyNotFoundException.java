/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.config.internal.model.dsl.config;

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
