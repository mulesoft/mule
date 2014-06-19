/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.result.statement;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import org.mule.api.Closeable;
import org.mule.api.DefaultMuleException;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

@SmallTest
public class CloseableMapTest extends AbstractMuleTestCase
{

    public static final String FOO_KEY = "foo";
    public static final Object FOO_VALUE = new Object();
    public static final String BAR_KEY = "bar";
    public static final Object BAR_VALUE = new Object();
    public static final String INVALID_KEY = "oops";
    public static final String NEW_KEY_1 = "key1";
    public static final String NEW_KEY_2 = "key2";

    private final CloseableMap<String, Object> closeableMap = new CloseableMap<String, Object>();

    @Before
    public void fillTestData() throws Exception
    {
        closeableMap.put(FOO_KEY, FOO_VALUE);
        closeableMap.put(BAR_KEY, BAR_VALUE);
    }

    @Test
    public void closesCloseableElements() throws Exception
    {
        Closeable closeable = mock(Closeable.class);
        closeableMap.put(NEW_KEY_1, closeable);

        closeableMap.close();

        verify(closeable).close();
    }

    @Test
    public void closesCloseableElementsBesidesOfExceptions() throws Exception
    {
        Closeable closeable1 = mock(Closeable.class);
        doThrow(new DefaultMuleException("Error")).when(closeable1).close();
        closeableMap.put(NEW_KEY_1, closeable1);

        Closeable closeable2 = mock(Closeable.class);
        closeableMap.put(NEW_KEY_2, closeable2);

        closeableMap.close();

        verify(closeable1).close();
        verify(closeable2).close();
    }

    @Test
    public void delegatesSize() throws Exception
    {
        assertThat(closeableMap.size(), equalTo(2));
    }

    @Test
    public void delegatesIsEmpty() throws Exception
    {
        assertThat(closeableMap.isEmpty(), equalTo(false));

        closeableMap.clear();

        assertThat(closeableMap.isEmpty(), equalTo(true));
    }

    @Test
    public void delegatesContainsKey() throws Exception
    {
        assertThat(closeableMap.containsKey(FOO_KEY), equalTo(true));
        assertThat(closeableMap.containsKey(INVALID_KEY), equalTo(false));
    }

    @Test
    public void delegatesContainsValue() throws Exception
    {
        assertThat(closeableMap.containsValue(FOO_VALUE), equalTo(true));
        assertThat(closeableMap.containsValue(new Object()), equalTo(false));
    }

    @Test
    public void delegatesGet() throws Exception
    {
        assertThat(closeableMap.get(FOO_KEY), equalTo(FOO_VALUE));
        assertThat(closeableMap.get(INVALID_KEY), equalTo(null));
    }

    @Test
    public void delegatesPut() throws Exception
    {
        Object newFooValue = new Object();
        closeableMap.put(FOO_KEY, newFooValue);

        assertThat(closeableMap.get(FOO_KEY), equalTo(newFooValue));
    }

    @Test
    public void delegatesRemove() throws Exception
    {
        assertThat(closeableMap.remove(FOO_KEY), equalTo(FOO_VALUE));
        assertThat(closeableMap.remove(INVALID_KEY), equalTo(null));
    }

    @Test
    public void delegatesPutAll() throws Exception
    {
        Map<String, Object> newValues = new HashMap<String, Object>();
        newValues.put(NEW_KEY_1, new Object());
        newValues.put(NEW_KEY_2, new Object());

        closeableMap.putAll(newValues);

        assertThat(closeableMap.containsKey(NEW_KEY_1), equalTo(true));
        assertThat(closeableMap.containsKey(NEW_KEY_2), equalTo(true));
    }
}
