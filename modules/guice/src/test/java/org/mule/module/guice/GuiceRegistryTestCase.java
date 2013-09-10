/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.guice;

import org.mule.api.registry.Registry;
import org.mule.registry.AbstractRegistryTestCase;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;

public class GuiceRegistryTestCase extends AbstractRegistryTestCase
{

    @Override
    public Registry getRegistry()
    {
        return new GuiceRegistry(Guice.createInjector(new EmptyModule()), null);
    }

    class EmptyModule extends AbstractModule
    {

        @Override
        protected void configure()
        {

        }
    }
}
