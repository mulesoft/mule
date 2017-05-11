/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.registry;

import org.mule.runtime.core.api.registry.Registry;
import org.mule.tck.core.registry.AbstractRegistryTestCase;

public class CommonBehaviourTransientRegistryTestCase extends AbstractRegistryTestCase {

  @Override
  public Registry getRegistry() {
    return new TransientRegistry(null);
  }
}
