/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.registry;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import org.mule.api.registry.ObjectLimbo;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;

@SmallTest
public class DefaultObjectLimboTestCase extends AbstractMuleTestCase
{

    private final static String KEY1 = "key1";
    private final static String KEY2 = "key2";
    private final static String VALUE1 = "value1";
    private final static String VALUE2 = "value1";

    private ObjectLimbo limbo;

    @Before
    public void before()
    {
        limbo = new DefaultObjectLimbo();
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
    public void removeRegistration()
    {
        limbo.unregisterObject(KEY1);
        assertRegistration(KEY2, VALUE2);

        Map<String, Object> objects = limbo.getObjects();
        assertThat(objects.size(), is(1));
        assertThat(objects.containsKey(KEY1), is(false));
    }

    @Test
    public void getObjects()
    {
        Map<String, Object> objects = limbo.getObjects();
        assertThat(objects.size(), is(2));
        assertThat(objects.get(KEY1), is((Object) VALUE1));
        assertThat(objects.get(KEY2), is((Object) VALUE2));
    }

    @Test
    public void getObjectsIsImmutable() throws Exception
    {
        final Map<String, Object> objects = limbo.getObjects();
        attemptAndFail(new Attempt()
        {
            @Override
            public String getOperation()
            {
                return "put";
            }

            @Override
            public void run()
            {
                objects.put("something", "forbidden");
            }
        });

        attemptAndFail(new Attempt()
        {
            @Override
            public String getOperation()
            {
                return "remove";
            }

            @Override
            public void run()
            {
                objects.remove(KEY1);
            }
        });

        attemptAndFail(new Attempt()
        {
            @Override
            public String getOperation()
            {
                return "clear";
            }

            @Override
            public void run()
            {
                objects.clear();
            }
        });
    }

    @Test
    public void clear()
    {
        limbo.clear();
        assertThat(limbo.getObjects().isEmpty(), is(true));
    }

    private void assertRegistration(String key, Object value)
    {
        assertThat(limbo.getObjects().get(key), is(value));
    }

    private void attemptAndFail(Attempt attempt) throws Exception
    {
        boolean fail = false;
        try
        {
            attempt.run();
        }
        catch (Exception e)
        {
            fail = true;
        }

        if (!fail)
        {
            fail(String.format("Was able to perform operation %s but shouldn't have", attempt.getOperation()));
        }
    }

    private interface Attempt extends Runnable
    {

        String getOperation();
    }
}
