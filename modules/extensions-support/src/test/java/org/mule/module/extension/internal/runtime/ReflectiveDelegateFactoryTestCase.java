/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.runtime;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.tck.testmodels.fruit.Fruit;
import org.mule.tck.testmodels.fruit.Kiwi;

import java.util.HashMap;

import org.junit.Before;
import org.junit.Test;

@SmallTest
public class ReflectiveDelegateFactoryTestCase extends AbstractMuleTestCase
{

    private ReflectiveDelegateFactory factory;
    private Kiwi kiwi;

    @Before
    public void before()
    {
        factory = new ReflectiveDelegateFactory();
        kiwi = new Kiwi();
    }

    @Test
    public void createDelegate()
    {
        KiwiDelegate delegate = factory.getDelegate(KiwiDelegate.class, kiwi);
        assertThat(kiwi, is(delegate.getFruit()));
    }

    @Test
    public void sameDelegateForSameConfigInstance()
    {
        KiwiDelegate delegate = factory.getDelegate(KiwiDelegate.class, kiwi);
        KiwiDelegate delegate2 = factory.getDelegate(KiwiDelegate.class, kiwi);
        assertThat(delegate, is(sameInstance(delegate2)));
    }

    @Test
    public void notSameDelegateForSameConfigButDifferentType()
    {
        FruitDelegate kiwiDelegate = factory.getDelegate(KiwiDelegate.class, kiwi);
        FruitDelegate genericDelegate = factory.getDelegate(GenericDelegate.class, kiwi);

        assertThat(kiwi, is(kiwiDelegate.getFruit()));
        assertThat(kiwi, is(genericDelegate.getFruit()));

        assertThat(kiwiDelegate, is(not(sameInstance(genericDelegate))));
    }

    @Test(expected = IllegalArgumentException.class)
    public void noSuitableConstructor()
    {
        factory.getDelegate(HashMap.class, kiwi);
    }

    static abstract class FruitDelegate
    {

        private final Fruit fruit;

        public FruitDelegate(Fruit fruit)
        {
            this.fruit = fruit;
        }

        public Fruit getFruit()
        {
            return fruit;
        }
    }

    public static class KiwiDelegate extends FruitDelegate
    {

        public KiwiDelegate(Kiwi kiwi)
        {
            super(kiwi);
        }
    }

    public static class GenericDelegate extends FruitDelegate
    {

        public GenericDelegate(Fruit fruit)
        {
            super(fruit);
        }
    }
}
