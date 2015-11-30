/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

@SmallTest
public class CaseInsensitiveMapWrapperTestCase extends AbstractMuleTestCase
{
    private Map<String, Integer> map;

    @Before
    public void initializeMap()
    {
        map = new CaseInsensitiveMapWrapper(HashMap.class);
    }

    @Test
    public void getIsCaseInsensitive()
    {
        map.put("A", 1);

        assertThat(map.keySet().size(), is(1));
        assertThat(map.get("a"), is(1));
        assertThat(map.get("A"), is(1));
        assertThat(map.get("b"), is(nullValue()));
    }

    @Test
    public void retainKeyCase()
    {
        map.put("A", 1);

        assertThat(map.keySet().iterator().next(), is("A"));
    }

    @Test
    public void clearFromKeySet()
    {
        map.put("A", 1);
        map.keySet().clear();

        assertThat(map.size(), is(0));
    }

    @Test
    public void clearFromEntrySet()
    {
        map.put("A", 1);
        map.entrySet().clear();

        assertThat(map.size(), is(0));
    }

    @Test
    public void removeFromKeySetIterator()
    {
        map.put("A", 1);
        map.put("B", 2);
        map.put("C", 3);
        assertThat(map.size(), is(3));

        for(Iterator<String> it = map.keySet().iterator(); it.hasNext();)
        {
            String key = it.next();
            if(key.equals("B"))
            {
                it.remove();
            }
        }

        assertThat(map.size(), is(2));
        assertThat(map.get("a"), is(1));
        assertThat(map.get("b"), is(nullValue()));
        assertThat(map.get("c"), is(3));
    }

    @Test
    public void removeFromEntrySetIterator()
    {
        map.put("A", 1);
        map.put("B", 2);
        map.put("C", 3);
        assertThat(map.size(), is(3));

        for(Iterator<Map.Entry<String,Integer>> it = map.entrySet().iterator(); it.hasNext();)
        {
            String key = it.next().getKey();
            if(key.equals("B"))
            {
                it.remove();
            }
        }

        assertThat(map.size(), is(2));
        assertThat(map.get("a"), is(1));
        assertThat(map.get("b"), is(nullValue()));
        assertThat(map.get("c"), is(3));
    }

    @Test
    public void containsKey()
    {
        map.put("A", 1);

        assertThat(map.containsKey("A"), is(true));
        assertThat(map.containsKey("a"), is(true));
        assertThat(map.containsKey("B"), is(false));
    }

    @Test
    public void containsValue()
    {
        map.put("A", 1);

        assertThat(map.containsValue(1), is(true));
        assertThat(map.containsValue("a"), is(false));
        assertThat(map.containsValue(2), is(false));
    }

}