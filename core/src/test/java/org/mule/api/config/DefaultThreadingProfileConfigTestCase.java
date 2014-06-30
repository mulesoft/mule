/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.api.config;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mule.tck.MuleTestUtils.testWithSystemProperty;
import org.mule.tck.MuleTestUtils;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.util.ClassUtils;

import java.lang.reflect.Field;
import java.net.URLClassLoader;

import org.junit.Test;

@SmallTest
public class DefaultThreadingProfileConfigTestCase extends AbstractMuleTestCase
{

    public static final int MAX_THREADS_ACTIVE = 16;
    public static final int MAX_THREADS_IDLE = 1;
    public static final int MAX_BUFFER_SIZE = 0;
    public static final long MAX_THREAD_TTL = 60000;
    public static final long THREAD_WAIT_TIMEOUT = 30000;
    public static final String INVALID_PROPERTY_VALUE = "test";

    @Test
    public void usesDefaultMaxThreadsActive() throws Exception
    {
        checkMaxThreadsActive(MAX_THREADS_ACTIVE);
    }

    @Test
    public void usesCustomMaxThreadsActive() throws Exception
    {
        final int customValue = MAX_THREADS_ACTIVE + 1;

        testWithSystemProperty(DefaultThreadingProfileConfig.MAX_THREADS_ACTIVE_PROPERTY, Integer.toString(customValue),
                               new MuleTestUtils.TestCallback()
                               {
                                   @Override
                                   public void run() throws Exception
                                   {
                                       checkMaxThreadsActive(customValue);
                                   }
                               });
    }

    @Test(expected = ExceptionInInitializerError.class)
    public void failsOnInvalidCustomMaxThreadsActive() throws Exception
    {
        testWithSystemProperty(DefaultThreadingProfileConfig.MAX_THREADS_ACTIVE_PROPERTY, INVALID_PROPERTY_VALUE, new MuleTestUtils.TestCallback()
        {
            public void run() throws Exception
            {
                getMaxThreadsActive();
            }
        });
    }

    @Test
    public void usesDefaultMaxThreadsIdle() throws Exception
    {
        checkMaxThreadsIdle(MAX_THREADS_IDLE);
    }

    @Test
    public void usesCustomMaxThreadsIdle() throws Exception
    {
        final int customValue = MAX_THREADS_IDLE + 1;

        testWithSystemProperty(DefaultThreadingProfileConfig.MAX_THREADS_IDLE_PROPERTY, Integer.toString(customValue),
                               new MuleTestUtils.TestCallback()
                               {
                                   @Override
                                   public void run() throws Exception
                                   {
                                       checkMaxThreadsIdle(customValue);
                                   }
                               });
    }

    @Test(expected = ExceptionInInitializerError.class)
    public void failsOnInvalidCustomMaxThreadsIdle() throws Exception
    {
        testWithSystemProperty(DefaultThreadingProfileConfig.MAX_THREADS_IDLE_PROPERTY, INVALID_PROPERTY_VALUE, new MuleTestUtils.TestCallback()
        {
            public void run() throws Exception
            {
                getMaxThreadsIdle();
            }
        });
    }

    @Test
    public void usesDefaultMaxBufferSize() throws Exception
    {
        checkMaxBufferSize(MAX_BUFFER_SIZE);
    }

    @Test
    public void usesCustomMaxBufferSize() throws Exception
    {
        final int customValue = MAX_BUFFER_SIZE + 1;

        testWithSystemProperty(DefaultThreadingProfileConfig.MAX_BUFFER_SIZE_PROPERTY, Integer.toString(customValue),
                               new MuleTestUtils.TestCallback()
                               {
                                   @Override
                                   public void run() throws Exception
                                   {
                                       checkMaxBufferSize(customValue);
                                   }
                               });
    }

    @Test(expected = ExceptionInInitializerError.class)
    public void failsOnInvalidCustomMaxBufferSize() throws Exception
    {
        testWithSystemProperty(DefaultThreadingProfileConfig.MAX_BUFFER_SIZE_PROPERTY, INVALID_PROPERTY_VALUE, new MuleTestUtils.TestCallback()
        {
            public void run() throws Exception
            {
                getMaxBufferSize();
            }
        });
    }

    @Test
    public void usesDefaultMaxThreadTTL() throws Exception
    {
        checkMaxThreadTTL(MAX_THREAD_TTL);
    }

    @Test
    public void usesCustomMaxThreadTTL() throws Exception
    {
        final long customValue = MAX_THREAD_TTL + 1;

        testWithSystemProperty(DefaultThreadingProfileConfig.MAX_THREAD_TTL_PROPERTY, Long.toString(customValue),
                               new MuleTestUtils.TestCallback()
                               {
                                   @Override
                                   public void run() throws Exception
                                   {
                                       checkMaxThreadTTL(customValue);
                                   }
                               });
    }

    @Test(expected = ExceptionInInitializerError.class)
    public void failsOnInvalidCustomMaxThreadsTTL() throws Exception
    {
        testWithSystemProperty(DefaultThreadingProfileConfig.MAX_THREAD_TTL_PROPERTY, INVALID_PROPERTY_VALUE, new MuleTestUtils.TestCallback()
        {
            public void run() throws Exception
            {
                getMaxThreadTTL();
            }
        });
    }

    @Test
    public void usesDefaultThreadWaitTimeout() throws Exception
    {
        checkThreadWaitTimeout(THREAD_WAIT_TIMEOUT);
    }

    @Test
    public void usesCustomThreadWaitTimeout() throws Exception
    {
        final long customValue = THREAD_WAIT_TIMEOUT + 1;

        testWithSystemProperty(DefaultThreadingProfileConfig.MAX_WAIT_TIMEOUT_PROPERTY, Long.toString(customValue),
                               new MuleTestUtils.TestCallback()
                               {
                                   @Override
                                   public void run() throws Exception
                                   {
                                       checkThreadWaitTimeout(customValue);
                                   }
                               });
    }

    @Test(expected = ExceptionInInitializerError.class)
    public void failsOnInvalidCustomThreadWaitTimeout() throws Exception
    {
        testWithSystemProperty(DefaultThreadingProfileConfig.MAX_WAIT_TIMEOUT_PROPERTY, INVALID_PROPERTY_VALUE, new MuleTestUtils.TestCallback()
        {
            public void run() throws Exception
            {
                getThreadWaitTimeout();
            }
        });
    }

    private void checkMaxThreadsActive(int expected) throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException
    {
        assertThat(getMaxThreadsActive(), equalTo(expected));
    }

    private void checkMaxThreadsIdle(int expected) throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException
    {
        assertThat(getMaxThreadsIdle(), equalTo(expected));
    }

    private void checkMaxBufferSize(int expected) throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException
    {
        assertThat(getMaxBufferSize(), equalTo(expected));
    }

    private void checkMaxThreadTTL(long expected) throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException
    {
        assertThat(getMaxThreadTTL(), equalTo(expected));
    }

    private void checkThreadWaitTimeout(long expected) throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException
    {
        assertThat(getThreadWaitTimeout(), equalTo(expected));
    }

    private int getMaxThreadsActive() throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException
    {
        return getIntegerConstant("DEFAULT_MAX_THREADS_ACTIVE");
    }

    private int getMaxThreadsIdle() throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException
    {
        return getIntegerConstant("DEFAULT_MAX_THREADS_IDLE");
    }

    private int getMaxBufferSize() throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException
    {
        return getIntegerConstant("DEFAULT_MAX_BUFFER_SIZE");
    }

    private long getMaxThreadTTL() throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException
    {
        return getLongConstant("DEFAULT_MAX_THREAD_TTL");
    }

    private long getThreadWaitTimeout() throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException
    {
        return getLongConstant("DEFAULT_THREAD_WAIT_TIMEOUT");
    }

    private int getIntegerConstant(String fieldName) throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException
    {
        Class clazz = loadDefaultThreadingProfileClass();
        Field field = clazz.getDeclaredField(fieldName);

        return field.getInt(clazz);
    }

    private long getLongConstant(String fieldName) throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException
    {
        Class clazz = loadDefaultThreadingProfileClass();
        Field field = clazz.getDeclaredField(fieldName);

        return field.getLong(clazz);
    }

    private Class loadDefaultThreadingProfileClass() throws ClassNotFoundException
    {
        URLClassLoader classLoader = new URLClassLoader(((URLClassLoader) Thread.currentThread().getContextClassLoader()).getURLs(), null);

        return ClassUtils.loadClass(DefaultThreadingProfileConfig.class.getCanonicalName(), classLoader);
    }
}