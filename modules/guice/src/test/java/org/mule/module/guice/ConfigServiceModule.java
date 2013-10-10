/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
