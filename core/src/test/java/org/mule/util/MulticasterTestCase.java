/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util;

import static org.junit.Assert.assertTrue;

import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Banana;
import org.mule.tck.testmodels.fruit.Fruit;
import org.mule.tck.testmodels.fruit.WaterMelon;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

@SmallTest
public class MulticasterTestCase extends AbstractMuleTestCase
{
    @Test
    public void testMulticating() throws Exception
    {
        List<Fruit> fruit = new ArrayList<Fruit>();
        Apple apple = new Apple();
        Banana banana = new Banana();
        WaterMelon melon = new WaterMelon();
        fruit.add(apple);
        fruit.add(banana);
        fruit.add(melon);

        Fruit caster = (Fruit)Multicaster.create(Fruit.class, fruit);
        caster.bite();

        assertTrue(apple.isBitten());
        assertTrue(banana.isBitten());
        assertTrue(melon.isBitten());
    }
}
