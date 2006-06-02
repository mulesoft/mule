/*
 * $Header$ 
 * $Revision$ 
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 * 
 * Copyright (c) SymphonySoft Limited. All rights reserved. http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD style
 * license a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 *  
 */

package org.mule.test.util;

import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Banana;
import org.mule.tck.testmodels.fruit.Fruit;
import org.mule.tck.testmodels.fruit.WaterMelon;
import org.mule.util.Multicaster;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

/**
 * <code>MulticasterTestCase</code> TODO (document class)
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class MulticasterTestCase extends TestCase
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

        Fruit caster = (Fruit) Multicaster.create(Fruit.class, fruit);
        caster.bite();

        assertTrue(apple.isBitten());
        assertTrue(banana.isBitten());
        assertTrue(melon.isBitten());
    }
}
