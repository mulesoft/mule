/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.util;

import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Banana;
import org.mule.tck.testmodels.fruit.Fruit;
import org.mule.tck.testmodels.fruit.WaterMelon;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

@SmallTest
public class MulticasterTestCase extends AbstractMuleTestCase
{

    @Test
    public void testMulticating() throws Exception
    {
        List fruit = new ArrayList();
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
