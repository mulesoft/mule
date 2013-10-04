/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.guice;

import org.mule.api.transformer.Transformer;
import org.mule.tck.testmodels.fruit.BananaFactory;
import org.mule.tck.testmodels.mule.TestAgent;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.name.Named;

/**
 * Simple Guice module that binds a service interface
 */
public class ConfigServiceModule extends AbstractModule
{
    protected void configure()
    {
        //Our auto transform service component
        this.bind(AutoTransformServiceInterface.class).to(DefaultAutoTransformService.class);

        //Injection service component
        this.bind(BananaServiceInterface.class).to(BananaInjectionService.class);

        //Will make the transformer available in Mule
        //this.bind(Transformer.class).to(OrangetoAppleTransformer.class);

    }

    //This automatically binds the intance to the return type
    @Provides
    BananaFactory provideBananaFactory()
    {
        return new BananaFactory();
    }

    @Provides
    TestAgent provideTestAgent()
    {
        return new TestAgent();
    }

    @Provides @Named("orange-to-apple")
    Transformer providesOrangetoAppleTransformer()
    {
        return new OrangetoAppleTransformer();
    }
}
