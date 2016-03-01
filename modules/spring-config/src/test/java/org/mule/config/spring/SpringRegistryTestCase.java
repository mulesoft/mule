/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;

import org.mule.api.registry.Registry;
import org.mule.registry.AbstractRegistryTestCase;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.hamcrest.core.Is;
import org.junit.Test;
import org.springframework.context.support.StaticApplicationContext;

public class SpringRegistryTestCase extends AbstractRegistryTestCase
{

    public static final String BEAN_KEY = "someBean";
    public static final String ANOTHER_BEAN_KEY = "someOtherBean";
    public static final String REGISTERY_ID = "someId";

    private StaticApplicationContext applicationContext;
    private StaticApplicationContext parentApplicationContext;
    private SpringRegistry springRegistry;

    @Override
    public Registry getRegistry()
    {
        return new SpringRegistry(new StaticApplicationContext(), null);
    }

    @Test
    public void lookupByTypeSearchInParentAlso()
    {
        createSpringRegistryWithParentContext();
        applicationContext.registerSingleton(BEAN_KEY, String.class);
        parentApplicationContext.registerSingleton(ANOTHER_BEAN_KEY, String.class);
        Collection<String> values = springRegistry.lookupObjects(String.class);
        assertThat(values.size(), is(2));
    }

    @Test
    public void lookupByIdReturnsApplicationContextBean()
    {
        createSpringRegistryWithParentContext();
        applicationContext.registerSingleton(BEAN_KEY, String.class);
        parentApplicationContext.registerSingleton(BEAN_KEY, Integer.class);
        assertThat(springRegistry.get(BEAN_KEY), is(instanceOf(String.class)));
    }

    @Test
    public void lookupByIdReturnsParentApplicationContextBean()
    {
        createSpringRegistryWithParentContext();
        parentApplicationContext.registerSingleton(BEAN_KEY, Object.class);
        assertThat(springRegistry.get(BEAN_KEY), Is.is(Object.class));
    }

    @Test
    public void lookupByLifecycleReturnsApplicationContextBeanOnly()
    {
        createSpringRegistryWithParentContext();
        applicationContext.registerSingleton(BEAN_KEY, String.class);
        parentApplicationContext.registerSingleton(ANOTHER_BEAN_KEY, String.class);
        assertThat(springRegistry.lookupObjectsForLifecycle(String.class).size(), is(1));
    }

    @Test
    public void lookupByTypeWhileUnregistering() throws Throwable
    {
        createSpringRegistryWithParentContext();
        applicationContext.registerSingleton(BEAN_KEY, String.class);
        applicationContext.registerSingleton(ANOTHER_BEAN_KEY, String.class);

        int modifiers = 50;
        for (int i = 0; i < modifiers; i++)
        {
            applicationContext.registerSingleton(BEAN_KEY + "_" + i, String.class);
        }

        final CountDownLatch modifyRegistryLatch = new CountDownLatch(modifiers);
        Runnable readRegistryByTypeRunnable = new Thread()
        {
            @Override
            public void run()
            {
                while (modifyRegistryLatch.getCount() > 0)
                {
                    assertThat("Check SpringRegistry#internalLookupByType() exception handler.",
                            springRegistry.lookupObjects(String.class), not(nullValue()));
                }
            }
        };
        
        ExecutorService executor = Executors.newFixedThreadPool(1 + modifiers);
        try
        {
            List<Future> futures = new ArrayList<>();
            futures.add(executor.submit(readRegistryByTypeRunnable));
            for (int i = 0; i < modifiers; ++i)
            {
                futures.add(executor.submit(new ModifyRegistryCallable(modifyRegistryLatch, BEAN_KEY + "_" + i)));
            }

            modifyRegistryLatch.await();
            for (Future future : futures)
            {
                // Check for exceptions in the executed tasks
                future.get();
            }
        }
        finally
        {
            executor.shutdown();
        }
    }

    private final class ModifyRegistryCallable implements Callable
    {
        private final CountDownLatch modifyRegistryLatch;
        private String beanKey;

        private ModifyRegistryCallable(CountDownLatch modifyRegistryLatch, String beanKey)
        {
            this.modifyRegistryLatch = modifyRegistryLatch;
            this.beanKey = beanKey;
        }

        @Override
        public Object call() throws Exception
        {
            try
            {
                assertThat(springRegistry.unregisterObject(beanKey), not(nullValue()));
                return null;
            }
            finally
            {
                modifyRegistryLatch.countDown();
            }
        }
    }

    private void createSpringRegistryWithParentContext()
    {
        applicationContext = new StaticApplicationContext();
        parentApplicationContext = new StaticApplicationContext();
        springRegistry = new SpringRegistry(REGISTERY_ID, applicationContext, parentApplicationContext, null);
    }
}
