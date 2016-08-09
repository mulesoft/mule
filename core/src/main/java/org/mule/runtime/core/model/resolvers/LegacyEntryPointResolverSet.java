/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.model.resolvers;

/**
 * An {@link org.mule.runtime.core.api.model.EntryPointResolverSet} that mimics the behaviour of the Mule 1.x
 * DynamicEntryPointResolver. <b>NOTE:</b> Since 3.0 this legacy entry point resolver will always invoked after message
 * transformation and not before.
 */
public class LegacyEntryPointResolverSet extends DefaultEntryPointResolverSet {

  public LegacyEntryPointResolverSet() {
    addEntryPointResolver(new MethodHeaderPropertyEntryPointResolver());
    addEntryPointResolver(new CallableEntryPointResolver());

    ReflectionEntryPointResolver reflectionResolver = new ReflectionEntryPointResolver();
    // In Mule 1.x you could call setXX methods as service methods by default
    reflectionResolver.removeIgnoredMethod("set*");
    addEntryPointResolver(reflectionResolver);
  }

}
