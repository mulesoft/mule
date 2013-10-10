/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config;

import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;

@SmallTest
public class PreferredObjectSelectorTestCase extends AbstractMuleTestCase
{

    @Test
    public void testSelectNoRegularClassIfThereIsNoPreferred()
    {
        List<Object> classes = new ArrayList<Object>();
        classes.add(new NonPreferred());

        PreferredObjectSelector selector = new PreferredObjectSelector();
        Object object = selector.select(classes.iterator());
        assertNotNull("Selector selected a wrong object", object instanceof NonPreferred);
    }

    @Test
    public void testSelectDefaultPreferredClassOverNoPreferredOne()
    {
        List<Object> classes = new ArrayList<Object>();
        classes.add(new NonPreferred());
        classes.add(new PreferredWithDefaultWeight());

        PreferredObjectSelector selector = new PreferredObjectSelector();
        Object object = selector.select(classes.iterator());
        assertNotNull("Selector selected a wrong object", object instanceof PreferredWithDefaultWeight);
    }

    @Test
    public void testSelectPreferredClassWithHighestWeight()
    {
        List<Object> classes = new ArrayList<Object>();
        classes.add(new NonPreferred());
        classes.add(new PreferredWithDefaultWeight());
        classes.add(new PreferredWithHighestWeight());

        PreferredObjectSelector selector = new PreferredObjectSelector();
        Object object = selector.select(classes.iterator());
        assertNotNull("Selector selected a wrong object", object instanceof PreferredWithHighestWeight);
    }

    public class NonPreferred
    {

    }

    @Preferred
    public class PreferredWithDefaultWeight
    {

    }

    @Preferred(weight = 10)
    public class PreferredWithHighestWeight
    {

    }
}
