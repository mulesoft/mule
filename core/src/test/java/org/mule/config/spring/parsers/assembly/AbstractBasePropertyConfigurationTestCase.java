/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.parsers.assembly;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

public abstract class AbstractBasePropertyConfigurationTestCase extends TestCase
{

    public static final String ALIAS = "Alias";
    public static final String NAME = "Name";
    public static final String COLLECTION = "Collection";
    public static final String IGNORED = "Ignored";
    public static final String MAP_CAPS = "MapCaps";
    public static final String MAP_DIGITS = "MapNumbers";
    public static final String REFERENCE = "Reference";
    public static final String UNUSED = "Unused";

    public static final String TO_DIGITS = "one=1,two=2,three=3";
    public static final Map TO_CAPS;

    static {
        TO_CAPS = new HashMap();
        String alphabet = "abcdefghijklmnopqrstuvwxyz";
        for (int i = 0; i < alphabet.length(); i++)
        {
            String letter = alphabet.substring(i, i+1);
            TO_CAPS.put(letter.toLowerCase(), letter.toUpperCase());
        }
    }

    protected void setTestValues(String prefix, PropertyConfiguration config)
    {
        config.addAlias(prefix + ALIAS, prefix + NAME);
        config.addCollection(prefix + COLLECTION);
        config.addIgnored(prefix + IGNORED);
        config.addMapping(prefix + MAP_CAPS, TO_CAPS);
        config.addMapping(prefix + MAP_DIGITS, TO_DIGITS);
        config.addReference(prefix + REFERENCE);
    }

    protected void verifyTestValues(String prefix, PropertyConfiguration config)
    {
        assertEquals(prefix + NAME, config.getAttributeMapping(prefix + ALIAS));
        assertEquals(prefix + NAME, config.translateName(prefix + ALIAS));
        assertEquals(prefix + UNUSED, config.getAttributeMapping(prefix + UNUSED));
        assertEquals(true, config.isCollection(prefix + COLLECTION));
        assertEquals(false, config.isCollection(prefix + UNUSED));
        assertEquals(true, config.isIgnored(prefix + IGNORED));
        assertEquals(false, config.isIgnored(prefix + UNUSED));
        assertEquals("A", config.translateValue(prefix + MAP_CAPS, "a"));
        assertEquals("a", config.translateValue(prefix + UNUSED, "a"));
        assertEquals("Z", config.translateValue(prefix + MAP_CAPS, "z"));
        assertEquals("z", config.translateValue(prefix + UNUSED, "z"));
        assertEquals("1", config.translateValue(prefix + MAP_CAPS, "1"));
        assertEquals("1", config.translateValue(prefix + MAP_DIGITS, "one"));
        assertEquals("2", config.translateValue(prefix + MAP_DIGITS, "two"));
        assertEquals("3", config.translateValue(prefix + MAP_DIGITS, "three"));
        assertEquals("four", config.translateValue(prefix + MAP_DIGITS, "four"));
        assertEquals("one", config.translateValue(prefix + UNUSED, "one"));
    }

    protected void verifyMissing(String prefix, PropertyConfiguration config)
    {
        assertEquals(prefix + ALIAS, config.getAttributeMapping(prefix + ALIAS));
        assertEquals(prefix + ALIAS, config.translateName(prefix + ALIAS));
        assertEquals(prefix + UNUSED, config.getAttributeMapping(prefix + UNUSED));
        assertEquals(false, config.isCollection(prefix + COLLECTION));
        assertEquals(false, config.isCollection(prefix + UNUSED));
        assertEquals(false, config.isIgnored(prefix + IGNORED));
        assertEquals(false, config.isIgnored(prefix + UNUSED));
        assertEquals("a", config.translateValue(prefix + MAP_CAPS, "a"));
        assertEquals("a", config.translateValue(prefix + UNUSED, "a"));
        assertEquals("z", config.translateValue(prefix + MAP_CAPS, "z"));
        assertEquals("z", config.translateValue(prefix + UNUSED, "z"));
        assertEquals("1", config.translateValue(prefix + MAP_CAPS, "1"));
        assertEquals("one", config.translateValue(prefix + MAP_DIGITS, "one"));
        assertEquals("two", config.translateValue(prefix + MAP_DIGITS, "two"));
        assertEquals("three", config.translateValue(prefix + MAP_DIGITS, "three"));
        assertEquals("four", config.translateValue(prefix + MAP_DIGITS, "four"));
        assertEquals("one", config.translateValue(prefix + UNUSED, "one"));
    }

}