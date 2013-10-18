/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util;

import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;

import org.junit.Test;

import static junit.framework.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@SmallTest
public class CollectionUtilsTestCase extends AbstractMuleTestCase
{

    @Test
    public void testToArrayOfComponentTypeNullCollection()
    {
        assertNull(CollectionUtils.toArrayOfComponentType(null, String.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToArrayOfComponentTypeNullType()
    {
        CollectionUtils.toArrayOfComponentType(Collections.EMPTY_LIST, null);
    }

    @Test
    public void testToArrayOfComponentTypeEmptyCollection()
    {
        assertTrue(Arrays.equals(new String[0], CollectionUtils.toArrayOfComponentType(
                Collections.EMPTY_LIST, String.class)));
    }

    @Test(expected = ArrayStoreException.class)
    public void testToArrayOfComponentTypeWrongElement()
    {
        CollectionUtils.toArrayOfComponentType(Collections.singleton("foo"), Integer.class);
    }

    @Test
    public void testToArrayOfComponentTypeOK()
    {
        String[] objects = new String[] {"foo", "bar", "baz"};
        assertTrue(Arrays.equals(objects, CollectionUtils.toArrayOfComponentType(Arrays.asList(objects),
                                                                                 String.class)));
    }

    @Test
    public void testToStringNull() throws Exception
    {
        Collection<?> c = null;
        assertEquals("[]", CollectionUtils.toString(c, false));
        assertEquals("[]", CollectionUtils.toString(c, true));
    }

    @Test
    public void testToStringEmpty() throws Exception
    {
        Collection<?> c = new ArrayList<Object>();
        assertEquals("[]", CollectionUtils.toString(c, false));
        assertEquals("[]", CollectionUtils.toString(c, true));
    }

    @Test
    public void testToStringSingleElement() throws Exception
    {
        Collection<String> c = Arrays.asList("foo");

        assertEquals("[foo]", CollectionUtils.toString(c, false));
        assertEquals("[" + SystemUtils.LINE_SEPARATOR + "foo" + SystemUtils.LINE_SEPARATOR + "]",
                     CollectionUtils.toString(c, true));
    }

    @Test
    public void testToStringMultipleElements() throws Exception
    {
        Collection<Serializable> c = Arrays.asList("foo", this.getClass());

        assertEquals("[foo, " + this.getClass().getName() + "]", CollectionUtils.toString(c, false));

        assertEquals("[" + SystemUtils.LINE_SEPARATOR + "foo" + SystemUtils.LINE_SEPARATOR
                     + this.getClass().getName() + SystemUtils.LINE_SEPARATOR + "]", CollectionUtils
                .toString(c, true));
    }

    @Test
    public void testToStringTooManyElements()
    {
        Collection<Number> test = new ArrayList<Number>(100);
        for (int i = 0; i < 100; i++)
        {
            test.add(new Integer(i));
        }

        // the String will contain not more than exactly MAX_ARRAY_LENGTH elements
        String result = CollectionUtils.toString(test, 10);
        assertTrue(result.endsWith("[..]]"));
        assertEquals(9, StringUtils.countMatches(result, ","));
    }

    @Test
    public void testContainsTypeTrue()
    {
        Collection<Object> c = new ArrayList<Object>();
        c.add(new String());
        c.add(new Date());
        assertTrue(CollectionUtils.containsType(c, Date.class));
    }

    @Test
    public void testContainsTypeFalse()
    {
        Collection<Object> c = new ArrayList<Object>();
        c.add(new String());
        c.add(new Integer(1));
        assertFalse(CollectionUtils.containsType(c, Date.class));
    }

    @Test
    public void testContainsTypeNullChecks()
    {
        Collection<Object> c = new ArrayList<Object>();
        c.add(new String());
        c.add(new Integer(1));
        assertFalse(CollectionUtils.containsType(c, null));
        assertFalse(CollectionUtils.containsType(null, Date.class));
    }

    @Test
    public void testRemoveType()
    {
        Collection<Object> c = new ArrayList<Object>();
        c.add(new String());
        c.add(new Integer(1));
        CollectionUtils.removeType(c, String.class);
        assertEquals(1, c.size());
        assertFalse(CollectionUtils.containsType(c, null));
        assertFalse(CollectionUtils.containsType(null, Date.class));
    }

}
