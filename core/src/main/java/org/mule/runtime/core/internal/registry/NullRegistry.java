/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.registry;

import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import org.mule.runtime.api.artifact.Registry;

import java.util.Collection;
import java.util.Optional;

public class NullRegistry implements Registry {

  private static final NullRegistry INSTANCE = new NullRegistry();

  public static NullRegistry getInstance() {
    return INSTANCE;
  }

  @Override
  public <T> Optional<T> lookupByType(Class<T> objectType) {
    return empty();
  }

  @Override
  public <T> Optional<T> lookupByName(String name) {
    return empty();
  }

  @Override
  public <T> Collection<T> lookupAllByType(Class<T> serviceType) {
    return emptyList();
  }
}
