/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.registry;

/**
 * A RegistryBroker delegates calls to a collection of Registries.
 *
 * @deprecated as of 3.7.0. This will be removed in Mule 4.0
 */
@Deprecated
public interface RegistryBroker extends Registry {

  void addRegistry(Registry registry);

  void removeRegistry(Registry registry);
}


