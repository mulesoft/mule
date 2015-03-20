/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.manager;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import org.mule.extension.introspection.Operation;
import org.mule.extension.runtime.OperationExecutor;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Before;
import org.junit.Test;

@SmallTest
public class ConfigurationInstanceWrapperTestCase extends AbstractMuleTestCase
{

    private static final String NAME = "name";

    private ConfigurationInstanceWrapper<Object> instanceWrapper;
    private Object configurationInstance;

    @Before
    public void before()
    {
        configurationInstance = new Object();
        instanceWrapper = new ConfigurationInstanceWrapper<>(NAME, configurationInstance);
    }

    @Test
    public void testEquals()
    {
        ConfigurationInstanceWrapper<Object> other = new ConfigurationInstanceWrapper<>("hello", configurationInstance);
        assertThat(instanceWrapper, equalTo(other));
    }

    @Test
    public void testNotEquals()
    {
        ConfigurationInstanceWrapper<Object> other = new ConfigurationInstanceWrapper<>(NAME, new Object());
        assertThat(instanceWrapper, not(equalTo(other)));
    }

    @Test
    public void testHashCode()
    {
        assertThat(instanceWrapper.hashCode(), is(configurationInstance.hashCode()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void noName()
    {
        new ConfigurationInstanceWrapper<>(null, instanceWrapper);
    }

    @Test(expected = IllegalArgumentException.class)
    public void noInstance()
    {
        new ConfigurationInstanceWrapper<>(NAME, null);
    }

    @Test
    public void getConfigurationInstance()
    {
        assertThat(instanceWrapper.getConfigurationInstance(), is(sameInstance(configurationInstance)));
    }

    @Test
    public void registerOperationExecutor()
    {
        Operation operation = mock(Operation.class);
        OperationExecutor executor = mock(OperationExecutor.class);

        instanceWrapper.registerOperationExecutor(operation, executor);
        assertThat(instanceWrapper.getOperationExecutor(operation), is(sameInstance(executor)));
    }

    @Test
    public void noRegisteredExecutor()
    {
        assertThat(instanceWrapper.getOperationExecutor(mock(Operation.class)), is(nullValue()));
    }
}
