/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.introspection.describer;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.introspection.connection.ConnectionProviderFactory;
import org.mule.test.petstore.extension.SimplePetStoreConnectionProvider;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Test;

@SmallTest
public class DefaultConnectionProviderFactoryTestCase extends AbstractMuleTestCase
{

    private ConnectionProviderFactory factory = new DefaultConnectionProviderFactory<>(SimplePetStoreConnectionProvider.class);

    @Test
    public void getObjectType()
    {
        assertThat(factory.getObjectType(), equalTo(SimplePetStoreConnectionProvider.class));
    }

    @Test
    public void newInstance() throws Exception
    {
        assertThat(factory.newInstance(), is(instanceOf(SimplePetStoreConnectionProvider.class)));
    }

    @Test
    public void returnsDifferentInstances() throws Exception
    {
        assertThat(factory.newInstance(), is(not(sameInstance(factory.newInstance()))));
    }

    @Test(expected = IllegalModelDefinitionException.class)
    public void notProviderClass()
    {
        new DefaultConnectionProviderFactory<>(Object.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void notInstantiable()
    {
        new DefaultConnectionProviderFactory<>(ConnectionProvider.class);
    }
}
