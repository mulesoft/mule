/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.registry;

import static org.mule.runtime.core.api.util.UUID.getUUID;
import org.mule.runtime.core.internal.lifecycle.MuleLifecycleInterceptor;
import org.mule.tck.core.registry.AbstractRegistryTestCase;

public class CommonBehaviourTransientRegistryTestCase extends AbstractRegistryTestCase {

  @Override
  public Registry getRegistry() {
    return new TransientRegistry(getUUID(), null, new MuleLifecycleInterceptor());
  }
}
