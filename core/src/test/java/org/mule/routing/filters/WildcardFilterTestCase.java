/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing.filters;

import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@SmallTest
public class WildcardFilterTestCase extends AbstractMuleTestCase
{

    @Test
    public void testWildcardFilterNoPattern()
    {
        // start with default
        WildcardFilter filter = new WildcardFilter();
        assertFalse(filter.accept("No tengo dinero"));

        // activate a pattern
        filter.setPattern("* brown fox");
        assertTrue(filter.accept("The quick brown fox"));

        // remove pattern again, i.e. block all
        filter.setPattern(null);
        assertFalse(filter.accept("oh-oh"));
    }

    @Test
    public void testWildcardFilterPostfix()
    {
        WildcardFilter filter = new WildcardFilter("The quick *");
        assertNotNull(filter.getPattern());
        assertTrue(filter.accept("The quick brown fox"));
        assertTrue(filter.accept("The quick *"));

        assertTrue(!filter.accept("The quickbrown fox"));
        assertTrue(!filter.accept("he quick brown fox"));
    }

    @Test
    public void testWildcardFilterPrefix()
    {
        WildcardFilter filter = new WildcardFilter();
        filter.setPattern("* brown fox");
        assertTrue(filter.accept("The quick brown fox"));
        assertTrue(filter.accept("* brown fox"));

        assertTrue(!filter.accept("The quickbrown fox"));
        assertTrue(!filter.accept("The quick brown fo"));

    }

    @Test
    public void testWildcardFilterExactMatch()
    {
        WildcardFilter filter = new WildcardFilter();
        filter.setPattern("fox");
        assertTrue(filter.accept("fox"));

        filter.setPattern("");
        assertTrue(filter.accept(""));
    }

    @Test
    public void testWildcardFilterPrePost()
    {
        WildcardFilter filter = new WildcardFilter();
        filter.setPattern("* brown *");
        assertTrue(filter.accept("The quick brown fox"));
        assertTrue(filter.accept("* brown fox"));

        assertTrue(!filter.accept("The quickbrown fox"));
        assertTrue(filter.accept("The quick brown fo"));

        filter.setPattern("**");
        assertTrue(filter.accept("The quick brown fox"));

        filter.setPattern("*w*");
        assertTrue(filter.accept("The quick brown fox"));

        filter.setPattern("*");
        assertTrue(filter.accept("The quick brown fox"));

        filter.setPattern("*.*");
        assertTrue(filter.accept("test.xml"));

        filter.setPattern("*.txt");
        assertTrue(filter.accept("test.txt"));
    }

    @Test
    public void testWildcardFilterMultiplePatterns()
    {
        WildcardFilter filter = new WildcardFilter();
        filter.setPattern("* brown*, The*");
        assertTrue(filter.accept("The quick brown fox"));
        assertTrue(filter.accept(" brown fox"));

        assertTrue(filter.accept("The quickbrown fox"));

        filter.setPattern("* if, The*");
        assertTrue(!filter.accept("What The!"));
        assertTrue(!filter.accept("simplify"));

    }

    @Test
    public void testWildcardFilterCasesensitive()
    {
        WildcardFilter filter = new WildcardFilter();
        filter.setPattern("* brown fox");
        assertFalse(filter.accept("The quick Brown fox"));
        assertTrue(filter.accept("* brown fox"));
        filter.setCaseSensitive(false);
        assertTrue(filter.accept("The quick Brown fox"));
    }

    @Test
    public void testClassAndSubclass()
    {
        WildcardFilter filter = new WildcardFilter();

        filter.setPattern("java.lang.Throwable+");
        assertTrue(filter.accept(new Exception()));
        assertTrue(filter.accept(new Throwable()));
        assertFalse(filter.accept(new Object()));

        filter.setPattern("java.lang.Throwable");
        assertFalse(filter.accept(new Exception()));
        assertTrue(filter.accept(new Throwable()));
        assertFalse(filter.accept(new Object()));
    }

    @Test
    public void testClassAndSubclassUsingString()
    {
        WildcardFilter filter = new WildcardFilter();

        filter.setPattern("java.lang.Throwable+");
        assertTrue(filter.accept(new Exception().getClass().getName()));
        assertTrue(filter.accept(new Throwable().getClass().getName()));
        assertFalse(filter.accept(new Object().getClass().getName()));

        filter.setPattern("java.lang.Throwable");
        assertFalse(filter.accept(new Exception().getClass().getName()));
        assertTrue(filter.accept(new Throwable().getClass().getName()));
        assertFalse(filter.accept(new Object().getClass().getName()));
    }

}
