/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.util;

import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Banana;
import org.mule.tck.testmodels.fruit.Fruit;
import org.mule.tck.testmodels.fruit.WaterMelon;

import java.util.ArrayList;
import java.util.List;

public class MulticasterTestCase extends AbstractMuleTestCase
{

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
