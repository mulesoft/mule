/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util.collection;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

@SmallTest
public class DuplicatesFreeListWrapperTestCase extends AbstractMuleTestCase
{

    private static final String VALUE = "Who are you?";
    private static final String VALUE2 = "I'm Batman";

    private List<String> list;

    @Before
    public void before()
    {
        list = new DuplicatesFreeListWrapper<>(new ArrayList<String>());
    }

    @Test
    public void add()
    {
        assertThat(list.add(VALUE), is(true));
        assertThat(list.add(VALUE), is(false));

        assertThat(list.add(VALUE2), is(true));
        assertThat(list.add(VALUE2), is(false));

        assertThat(list, hasSize(2));
    }

    @Test
    public void addAll()
    {
        list.addAll(Arrays.asList(VALUE, VALUE2, VALUE, VALUE2));

        assertThat(list, hasSize(2));
        assertThat(list.get(0), is(VALUE));
        assertThat(list.get(1), is(VALUE2));
    }

    @Test
    public void addAndRemove()
    {
        assertThat(list.add(VALUE), is(true));
        assertThat(list.remove(VALUE), is(true));
        assertThat(list.add(VALUE), is(true));
        assertThat(list, hasSize(1));
    }

    @Test
    public void iterator()
    {
        List<String> original = Arrays.asList(VALUE, VALUE2);
        list.addAll(original);

        int i = 0;
        for (Iterator<String> it = list.iterator(); it.hasNext(); i++)
        {
            assertThat(it.next(), is(original.get(i)));
            it.remove();
        }

        assertThat(list, hasSize(0));
    }

    @Test
    public void set()
    {
        list.add(VALUE);
        assertThat(list.set(0, VALUE2), is(VALUE));
        assertThat(list.set(0, VALUE2), is(VALUE2));

        assertThat(list, hasSize(1));
        assertThat(list.get(0), is(VALUE2));
    }
}
