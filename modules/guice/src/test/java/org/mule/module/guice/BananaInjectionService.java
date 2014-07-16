/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
