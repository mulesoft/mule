/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.config.internal.registry;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.ImmutableList;

/**
 * Default implementation of {@link OptionalObjectsController}
 *
 * @since 3.7.0
 */
public class DefaultOptionalObjectsController implements OptionalObjectsController {

  private final Set<String> optionalKeys = new HashSet<>();
  private final Set<String> discardedKeys = new HashSet<>();
  private final Object discardedObjectPlaceholder = new Object();

  @Override
  public void registerOptionalKey(String key) {
    optionalKeys.add(key);
  }

  @Override
  public void discardOptionalObject(String key) {
    discardedKeys.add(key);
  }

  @Override
  public boolean isOptional(String key) {
    return optionalKeys.contains(key);
  }

  @Override
  public boolean isDiscarded(String key) {
    return discardedKeys.contains(key);
  }

  @Override
  public Object getDiscardedObjectPlaceholder() {
    return discardedObjectPlaceholder;
  }

  @Override
  public Collection<String> getAllOptionalKeys() {
    return ImmutableList.copyOf(optionalKeys);
  }
}
