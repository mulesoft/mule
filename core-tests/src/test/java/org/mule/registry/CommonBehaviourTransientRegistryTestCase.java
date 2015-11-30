/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.registry;

import org.mule.api.registry.Registry;

public class CommonBehaviourTransientRegistryTestCase extends AbstractRegistryTestCase
{
    @Override
    public Registry getRegistry()
    {
        return new TransientRegistry(null);
    }
}
