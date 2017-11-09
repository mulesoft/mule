/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.registry;

import static com.google.common.collect.ImmutableList.copyOf;
import static java.util.Collections.emptyList;

import java.util.Collection;
import java.util.Iterator;
import java.util.ServiceLoader;

/**
 * Implementation of {@link ServiceRegistry} that uses standard {@link java.util.ServiceLoader} to get the providers
 *
 * @since 3.7.0
 */
public class SpiServiceRegistry extends AbstractServiceRegistry {

  @Override
  protected synchronized <T> Collection<T> doLookupProviders(Class<T> providerClass, ClassLoader classLoader) {
    Iterator<T> iterator = ServiceLoader.load(providerClass, classLoader).iterator();
    if (iterator.hasNext()) {
      return copyOf(iterator);
    } else {
      return emptyList();
    }
  }

}
