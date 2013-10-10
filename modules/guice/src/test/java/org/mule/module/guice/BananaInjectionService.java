/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.guice;

import org.mule.api.MuleContext;
import org.mule.tck.testmodels.fruit.Banana;
import org.mule.tck.testmodels.fruit.BananaFactory;

import com.google.inject.Inject;

/**
 * Oooh er missus!
 */
public class BananaInjectionService implements BananaServiceInterface
{
    @Inject
    private BananaFactory factory;

    @Inject
    private MuleContext muleContext;

    public Banana doSomething(Object data) throws Exception
    {
        return (Banana)factory.getInstance(muleContext);
    }
}
