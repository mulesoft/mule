/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.factories;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Lifecycle;
import org.mule.api.lifecycle.Stoppable;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
@SmallTest
public class ConstantFactoryBeanTestCase extends AbstractMuleTestCase
{

    @Mock(extraInterfaces = {Lifecycle.class, MuleContextAware.class})
    private Object value;
    private ConstantFactoryBean<Object> factoryBean;

    @Before
    public void before() throws Exception
    {
        factoryBean = new ConstantFactoryBean<>(value);
    }

    @Test
    public void returnsValue() throws Exception
    {
        assertThat(factoryBean.getObject(), is(sameInstance(value)));
    }

    @Test
    public void singleton()
    {
        assertThat(factoryBean.isSingleton(), is(true));
    }

    @Test
    public void assertClass()
    {
        assertThat(factoryBean.getObjectType() == value.getClass(), is(true));
    }

    @Test
    public void setMuleContext()
    {
        MuleContext muleContext = mock(MuleContext.class);
        factoryBean.setMuleContext(muleContext);
        verify((MuleContextAware) value).setMuleContext(muleContext);
    }

    @Test
    public void initialise() throws InitialisationException
    {
        factoryBean.initialise();
        verify((Initialisable) value).initialise();
    }

    @Test
    public void stop() throws MuleException
    {
        factoryBean.stop();
        verify((Stoppable) value).stop();
    }

    @Test
    public void dispose() throws Exception
    {
        factoryBean.dispose();
        verify((Disposable) value).dispose();
    }

}
