/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal;

import java.util.Collection;

/**
 * Keeps track of keys that have been marked as optional on a registry-bootstrap file. It also tracks the optional keys that
 * couldn't in fact be instantiated and are discarded
 *
 * @since 3.7.0
 */
public interface OptionalObjectsController {

  /**
   * Registers the given {@code key} as optional
   *
   * @param key an object key
   */
  void registerOptionalKey(String key);

  /**
   * Registers the given {@code key} as a discarded object
   *
   * @param key an object key
   */
  void discardOptionalObject(String key);

  /**
   * @param key an object key
   * @return {@code true} if the given key is optional. {@code false} otherwise
   */
  boolean isOptional(String key);

  /**
   * @param key an object key
   * @return {@code true} if the given key is discarded. {@code false} otherwise
   */
  boolean isDiscarded(String key);

  /**
   * A placeholder for Spring to temporarily work with. This is because Spring can't handle {@code null} entries. This object will
   * be removed from the registry when {@link MuleArtifactContext} is fully started
   *
   * @return a generic object
   */
  Object getDiscardedObjectPlaceholder();

  /**
   * @return an immutable view of all the current optional keys
   */
  Collection<String> getAllOptionalKeys();
}
