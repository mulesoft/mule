/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.lifecycle.Startable;
import org.mule.runtime.core.config.builders.AbstractConfigurationBuilder;

import java.util.List;

import org.junit.Test;

public class ManuallyRegisteredObjectLifecycleTestCase extends FunctionalTestCase
{

    private static final String INITIALISABLE = "initialisable";
    private static final String STARTABLE = "startable";


    @Override
    protected String[] getConfigFiles()
    {
        return new String[] {};
    }

    @Override
    protected void addBuilders(List<ConfigurationBuilder> builders)
    {
        super.addBuilders(builders);
        builders.add(new AbstractConfigurationBuilder()
        {
            @Override
            protected void doConfigure(MuleContext muleContext) throws Exception
            {
                muleContext.getRegistry().registerObject("TestInitialisableObject", new TestInitialisableObject());
                muleContext.getRegistry().registerObject("TestStartableObject", new TestStartableObject());
            }
        });
    }

    @Test
    public void manuallyRegisteredStartableLifecycle() throws Exception
    {
        assertLifecycle(STARTABLE);
    }

    @Test
    public void manuallyRegisteredInitialisableLifecycle() throws Exception
    {
        assertLifecycle(INITIALISABLE);
    }


    private void assertLifecycle(String key)
    {
        TestLifecycleObject testLifecycleObject = muleContext.getRegistry().get(key);
        assertThat(testLifecycleObject, is(notNullValue()));

        assertThat(testLifecycleObject.getInitialise(), is(1));
        assertThat(testLifecycleObject.getStart(), is(1));
    }

    private abstract class RegisteringObject implements MuleContextAware
    {

        private MuleContext muleContext;

        @Override
        public void setMuleContext(MuleContext muleContext)
        {
            this.muleContext = muleContext;
        }

        protected void manuallyRegisterObject() throws MuleException
        {
            Object o = new TestLifecycleObject();
            muleContext.getRegistry().registerObject(getKey(), o);
        }

        protected abstract String getKey();
    }

    private class TestStartableObject extends RegisteringObject implements Startable
    {


        @Override
        public void start() throws MuleException
        {
            manuallyRegisterObject();
        }

        @Override
        protected String getKey()
        {
            return STARTABLE;
        }
    }

    private class TestInitialisableObject extends RegisteringObject implements Initialisable
    {

        @Override
        public void initialise() throws InitialisationException
        {
            try
            {
                manuallyRegisterObject();
            }
            catch (MuleException e)
            {
                throw new InitialisationException(e, this);
            }
        }

        @Override
        protected String getKey()
        {
            return INITIALISABLE;
        }
    }
}
