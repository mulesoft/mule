/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.registry;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.mule.api.MuleContext;
import org.mule.api.registry.ObjectLimbo;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class DefaultObjectLimboTestCase extends AbstractMuleTestCase
{

    private final static String KEY1 = "key1";
    private final static String KEY2 = "key2";
    private final static String VALUE1 = "value1";
    private final static String VALUE2 = "value1";

    @Mock
    private MuleContext muleContext;

    private ObjectLimbo limbo;

    @Before
    public void before() throws Exception
    {
        limbo = new DefaultObjectLimbo(muleContext);
        limbo.registerObject(KEY1, VALUE1);
        limbo.registerObject(KEY2, VALUE2);
    }

    @Test
    public void registerAndRetrieve() throws Exception
    {
        assertRegistration(KEY1, VALUE1);
        assertRegistration(KEY2, VALUE2);
    }

    @Test
    public void registerAndOverride() throws Exception
    {
        limbo.registerObject(KEY1, VALUE2);
        assertRegistration(KEY1, VALUE2);
    }

    @Test
    public void removeRegistration() throws Exception
    {
        limbo.unregisterObject(KEY1);
        assertRegistration(KEY2, VALUE2);

        Map<String, Object> objects = limbo.lookupByType(Object.class);
        assertThat(objects.size(), is(1));
        assertThat(objects.containsKey(KEY1), is(false));
    }

    @Test
    public void getObjects()
    {
        Map<String, Object> objects = limbo.lookupByType(Object.class);
        assertThat(objects.size(), is(2));
        assertThat(objects.get(KEY1), is((Object) VALUE1));
        assertThat(objects.get(KEY2), is((Object) VALUE2));
    }

    @Test
    public void clear()
    {
        limbo.clear();
        assertThat(limbo.lookupByType(Object.class).isEmpty(), is(true));
    }

    private void assertRegistration(String key, Object value)
    {
        assertThat(limbo.lookupByType(Object.class).get(key), is(value));
    }
}
