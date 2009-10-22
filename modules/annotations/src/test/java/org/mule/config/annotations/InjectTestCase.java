/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.annotations;

import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Banana;
import org.mule.tck.testmodels.fruit.Orange;

import javax.inject.Inject;
import javax.inject.Named;

public class InjectTestCase extends AbstractMuleTestCase
{
    public void testInjectByType() throws Exception
    {
        muleContext.getRegistry().registerObject("apple1", new Apple());
        muleContext.getRegistry().registerObject("orange1", new Orange());
        Banana banana = new Banana();
        banana.bite();
        muleContext.getRegistry().registerObject("banana1", banana);

        BeansByType bean = new BeansByType();
        muleContext.getRegistry().registerObject("BeansByType", bean);

        assertNotNull(bean.getApple());
        assertNotNull(bean.getOrange());
        assertTrue(bean.getBanana().isBitten());
    }


    public void testInjectByKey() throws Exception
    {
        muleContext.getRegistry().registerObject("apple1", new Apple());
        muleContext.getRegistry().registerObject("apple2", new Apple());
        muleContext.getRegistry().registerObject("orange1", new Orange());
        muleContext.getRegistry().registerObject("orange2", new Orange(12, 3.4, "awesome"));
        muleContext.getRegistry().registerObject("banana1", new Banana());
        Banana banana = new Banana();
        banana.bite();
        muleContext.getRegistry().registerObject("bittenBanana", banana);

        BeansByKey bean = new BeansByKey();
        muleContext.getRegistry().registerObject("BeansByKey", bean);

        assertNotNull(bean.getApple());
        assertNotNull(bean.getOrange());
        assertEquals(12, bean.getOrange().getSegments().intValue());
        assertEquals(3.4, bean.getOrange().getRadius());
        assertEquals("awesome", bean.getOrange().getBrand());
        assertTrue(bean.getBanana().isBitten());
    }

    public void testInjectByTypeWithConflict() throws Exception
    {
        muleContext.getRegistry().registerObject("apple1", new Apple());
        muleContext.getRegistry().registerObject("apple2", new Apple());
        BeansByType bean = new BeansByType();
        try
        {
            muleContext.getRegistry().registerObject("BeansByType", bean);
            fail("There is more than one object of type apple registered");
        }
        catch (Exception e)
        {
            //Expected
        }
    }

    public void testInjectByKeyNotFound() throws Exception
    {
        muleContext.getRegistry().registerObject("appleNotFound", new Apple());
        BeansByKey bean = new BeansByKey();
        try
        {
            muleContext.getRegistry().registerObject("BeansByKey", bean);
            fail("There is no object with key 'apple1'");
        }
        catch (Exception e)
        {
            //extends
        }
    }

    public void testInjectByTypeAndKey() throws Exception
    {
        BeansByKeyAndType bean = new BeansByKeyAndType();
        try
        {
            muleContext.getRegistry().registerObject("Bean", bean);
            fail("should have failed since apple and banana registered");
        }
        catch (Exception e)
        {
            //exprected
        }

        muleContext.getRegistry().registerObject("apple1", new Apple());
        muleContext.getRegistry().registerObject("banana1", new Banana());
        muleContext.getRegistry().registerObject("Bean", bean);

        assertNotNull(bean.getApple());
        assertNotNull(bean.getBanana());
    }


    public class BeansByType
    {
        @Inject
        private Apple apple;

        @Inject
        private Orange orange;

        @Inject
        private Banana banana;

        public Apple getApple()
        {
            return apple;
        }

        public Orange getOrange()
        {
            return orange;
        }

        public Banana getBanana()
        {
            return banana;
        }
    }

    public class BeansByKey
    {
        @Named("apple1")
        private Apple apple;

        @Named("orange2")
        private Orange orange;

        @Named("bittenBanana")
        private Banana banana;

        public Apple getApple()
        {
            return apple;
        }

        public Orange getOrange()
        {
            return orange;
        }

        public Banana getBanana()
        {
            return banana;
        }
    }

    public class BeansByKeyAndType
    {
        @Named(value = "apple1")
        private Apple apple;

        @Named
        private Banana banana;

        public Apple getApple()
        {
            return apple;
        }

        public Banana getBanana()
        {
            return banana;
        }
    }
}
