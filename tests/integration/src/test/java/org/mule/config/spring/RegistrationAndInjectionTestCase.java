/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.config.MuleProperties;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Lifecycle;
import org.mule.api.lifecycle.LifecycleState;
import org.mule.api.lifecycle.LifecycleStateAware;
import org.mule.api.registry.RegistrationException;
import org.mule.api.store.ObjectStoreManager;
import org.mule.tck.junit4.FunctionalTestCase;

import javax.inject.Inject;

import org.junit.Test;

public class RegistrationAndInjectionTestCase extends FunctionalTestCase
{

    private static final String KEY = "key";

    @Override
    protected String[] getConfigFiles()
    {
        return new String[] {};
    }

    @Test
    public void applyLifecycleUponRegistration() throws Exception
    {
        TestLifecycleObject object = registerObject();
        assertRegistered(object);
        assertInitialisation(object);
    }

    @Test
    public void applyLifecycleUponUnregistration() throws Exception
    {
        applyLifecycleUponRegistration();
        TestLifecycleObject object = (TestLifecycleObject) muleContext.getRegistry().unregisterObject(KEY);
        assertThat(object, is(notNullValue()));
        assertShutdown(object);
    }

    @Test
    public void registerExistingKey() throws Exception
    {
        TestLifecycleObject object = registerObject();
        TestLifecycleObject replacement = registerObject();

        assertRegistered(replacement);
        assertShutdown(object);
        assertInitialisation(replacement);
    }

    @Test
    public void injectOnRegisteredObject() throws Exception
    {
        TestLifecycleObject object = registerObject();
        assertInjection(object);
    }

    @Test
    public void injectWithInheritance() throws Exception {
        TestLifecycleObject object = new ExtendedTestLifecycleObject();
        muleContext.getRegistry().registerObject(KEY, object);

        assertInjection(object);
    }

    @Test
    public void muleContextAware() throws Exception
    {
        MuleContextAware muleContextAware = mock(MuleContextAware.class);
        muleContext.getRegistry().registerObject(KEY, muleContextAware);
        assertRegistered(muleContextAware);
        verify(muleContextAware).setMuleContext(muleContext);
    }

    @Test
    public void lifecycleSateAware() throws Exception
    {
        LifecycleStateAware lifecycleStateAware = mock(LifecycleStateAware.class);
        muleContext.getRegistry().registerObject(KEY, lifecycleStateAware);
        assertRegistered(lifecycleStateAware);
        verify(lifecycleStateAware).setLifecycleState(any(LifecycleState.class));
    }

    private void assertInjection(TestLifecycleObject object)
    {
        assertThat(object.getObjectStoreManager(), is(muleContext.getRegistry().get(MuleProperties.OBJECT_STORE_MANAGER)));
        assertThat(object.getMuleContext(), is(muleContext));

        // just to make sure that injection is to thank for this
        assertThat(object, is(not(instanceOf(MuleContextAware.class))));
    }

    private void assertRegistered(Object object)
    {
        Object registered = muleContext.getRegistry().get(KEY);
        assertThat(registered, is(sameInstance(object)));
    }

    private TestLifecycleObject registerObject() throws RegistrationException
    {
        TestLifecycleObject object = new TestLifecycleObject();
        muleContext.getRegistry().registerObject(KEY, object);

        return object;
    }

    private void assertInitialisation(TestLifecycleObject object)
    {
        assertThat(object.getInitialise(), is(1));
        assertThat(object.getStart(), is(1));
        assertThat(object.getStop(), is(0));
        assertThat(object.getDispose(), is(0));
    }

    private void assertShutdown(TestLifecycleObject object)
    {
        assertThat(object.getStop(), is(1));
        assertThat(object.getDispose(), is(1));
    }

    public static class TestLifecycleObject implements Lifecycle
    {

        private int initialise = 0;
        private int start = 0;
        private int stop = 0;
        private int dispose = 0;

        @Inject
        private ObjectStoreManager objectStoreManager;

        @Inject
        private MuleContext muleContext;

        @Override
        public void initialise() throws InitialisationException
        {
            initialise++;
        }

        @Override
        public void start() throws MuleException
        {
            start++;
        }

        @Override
        public void stop() throws MuleException
        {
            stop++;
        }

        @Override
        public void dispose()
        {
            dispose++;
        }

        public int getInitialise()
        {
            return initialise;
        }

        public int getStart()
        {
            return start;
        }

        public int getStop()
        {
            return stop;
        }

        public int getDispose()
        {
            return dispose;
        }

        public ObjectStoreManager getObjectStoreManager()
        {
            return objectStoreManager;
        }

        public MuleContext getMuleContext()
        {
            return muleContext;
        }
    }

    public static class ExtendedTestLifecycleObject extends TestLifecycleObject
    {

    }
}
