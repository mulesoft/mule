/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import junit.framework.TestCase;

import org.apache.commons.lang.SystemUtils;
import org.mule.util.CollectionUtils;

public class CollectionUtilsTestCase extends TestCase
{

    public void testCollectionNull() throws Exception
    {
        Collection c = null;
        assertEquals("[]", CollectionUtils.toString(c, false));
        assertEquals("[]", CollectionUtils.toString(c, true));
    }

    public void testCollectionEmpty() throws Exception
    {
        Collection c = new ArrayList();
        assertEquals("[]", CollectionUtils.toString(c, false));
        assertEquals("[]", CollectionUtils.toString(c, true));
    }

    public void testCollectionSingleElement() throws Exception
    {
        Collection c = Arrays.asList(new Object[]{"foo"});

        assertEquals("[foo]", CollectionUtils.toString(c, false));
        assertEquals("[" + SystemUtils.LINE_SEPARATOR + "foo" + SystemUtils.LINE_SEPARATOR + "]",
            CollectionUtils.toString(c, true));
    }

    public void ctestCollectionTwoElements() throws Exception
    {
        Collection c = Arrays.asList(new Object[]{"foo", this.getClass()});

        assertEquals("[foo, " + this.getClass().getName(), CollectionUtils.toString(c, false));

        assertEquals("[" + SystemUtils.LINE_SEPARATOR + "foo" + SystemUtils.LINE_SEPARATOR
                     + this.getClass().getName() + SystemUtils.LINE_SEPARATOR + "]",
            CollectionUtils.toString(c, true));
    }

}
