/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.guice;

import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.BananaFactory;
import org.mule.tck.testmodels.fruit.Banana;

import com.google.inject.Inject;

/**
 * Oooh er missus!
 */
public class BananaInjectionService implements BananaServiceInterface
{
    @Inject
    private BananaFactory factory;

    public Banana doSomething(Object data) throws Exception
    {
        return (Banana)factory.getInstance();
    }
}