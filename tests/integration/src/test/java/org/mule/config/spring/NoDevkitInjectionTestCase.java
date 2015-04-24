/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import org.mule.api.devkit.ProcessAdapter;
import org.mule.api.devkit.ProcessTemplate;
import org.mule.api.store.ObjectStoreManager;
import org.mule.tck.junit4.FunctionalTestCase;

import javax.inject.Inject;

import org.junit.Test;

public class NoDevkitInjectionTestCase extends FunctionalTestCase
{
    @Override
    protected String[] getConfigFiles()
    {
        return new String[] {};
    }

    @Test
    public void doNotInjectProcessAdapter() throws Exception
    {
        TestInjectableObject adapter = muleContext.getInjector().inject(new TestProcessAdapter());
        assertThat(adapter.getObjectStoreManager(), is(nullValue()));
    }

    @Test
    public void doInjectAnythingElse() throws Exception
    {
        TestInjectableObject adapter = muleContext.getInjector().inject(new TestInjectableObject());
        assertThat(adapter.getObjectStoreManager(), is(notNullValue()));
    }

    public static class TestInjectableObject
    {

        @Inject
        private ObjectStoreManager objectStoreManager;

        public ObjectStoreManager getObjectStoreManager()
        {
            return objectStoreManager;
        }
    }

    public static class TestProcessAdapter extends TestInjectableObject implements ProcessAdapter<Object>
    {

        @Override
        public <T> ProcessTemplate<T, Object> getProcessTemplate()
        {
            return null;
        }
    }
}
