/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.launcher;

import static org.junit.Assert.assertFalse;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.MediumTest;

import java.net.URL;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

@MediumTest
public class SynchronizedClassLoaderTestCase extends AbstractMuleTestCase
{

    private final CountDownLatch onExclusionZone = new CountDownLatch(2);
    private final CountDownLatch onTestComplete = new CountDownLatch(1);

    @Test
    public void synchronizesLoadClassInFineGrainedControlClassLoader() throws Exception
    {
        doLoadClassSynchronizationTest(new FineGrainedControlClassLoader(new URL[] {}, new TestClassLoader()));
    }

    @Test
    public void synchronizesLoadClassInMuleApplicationClassLoader() throws Exception
    {
        doLoadClassSynchronizationTest(new MuleApplicationClassLoader("test", new TestClassLoader(), null));
    }

    private void doLoadClassSynchronizationTest(ClassLoader classLoader) throws InterruptedException
    {

        LoadClass loadClass1 = new LoadClass(classLoader);
        Thread thread1 = new Thread(loadClass1);

        LoadClass loadClass2 = new LoadClass(classLoader);
        Thread thread2 = new Thread(loadClass2);

        try
        {
            thread1.start();
            thread2.start();

            assertFalse("There are multiple threads loading classes", onExclusionZone.await(250, TimeUnit.MILLISECONDS));
            assertFalse(loadClass1.error);
            assertFalse(loadClass2.error);
        }
        finally
        {
            onTestComplete.countDown();
        }
    }

    private static class LoadClass implements Runnable
    {

        private final ClassLoader classLoader;
        private boolean error;

        public LoadClass(ClassLoader classLoader)
        {
            this.classLoader = classLoader;
        }

        public void run()
        {
            try
            {
                classLoader.loadClass("org.mule.Foo");
            }
            catch (Exception e)
            {
                error = true;
            }
        }
    }

    private class TestClassLoader extends ClassLoader
    {

        @Override
        public Class<?> loadClass(String s) throws ClassNotFoundException
        {
            onExclusionZone.countDown();

            try
            {
                onTestComplete.await();
            }
            catch (InterruptedException e)
            {
                throw new IllegalStateException(e);
            }

            return null;
        }
    }
}
