/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.bootstrap;

import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

import org.hamcrest.core.IsNull;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import org.mule.api.MuleContext;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.transaction.TransactionFactory;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class SimpleRegistryBootstrapTestCase extends AbstractMuleContextTestCase
{
    
    public static final String TEST_TRANSACTION_FACTORY_CLASS = "javax.jms.Connection";

    @Test(expected=ClassNotFoundException.class)
    public void registeringOptionalTransaction() throws Exception
    {
        createTestRegistryBootstrap(SimpleRegistryBootstrap.ArtifactType.APP);
        muleContext.getTransactionFactoryManager().getTransactionFactoryFor(Class.forName(TEST_TRANSACTION_FACTORY_CLASS));
    }

    @Test
    public void existingNotOptionalTransaction() throws Exception
    {
        createTestRegistryBootstrap(SimpleRegistryBootstrap.ArtifactType.APP);
        TransactionFactory transactionFactoryFor = muleContext.getTransactionFactoryManager().getTransactionFactoryFor(FakeTransactionResource.class);
        Assert.assertNotNull(transactionFactoryFor);
    }

    @Test
    public void registerOnlyAppPropertiesType() throws Exception
    {
        createTestRegistryBootstrap(SimpleRegistryBootstrap.ArtifactType.APP);
        assertThat(muleContext.getRegistry().lookupObject(String.class), notNullValue());
        assertThat(muleContext.getRegistry().lookupObject(Properties.class), nullValue());
        assertThat(muleContext.getRegistry().lookupObject(ArrayList.class), notNullValue());
    }

    @Test
    public void registerOnlyDomainPropertiesType() throws Exception
    {
        createTestRegistryBootstrap(SimpleRegistryBootstrap.ArtifactType.DOMAIN);
        assertThat(muleContext.getRegistry().lookupObject(String.class), nullValue());
        assertThat(muleContext.getRegistry().lookupObject(Properties.class), notNullValue());
        assertThat(muleContext.getRegistry().lookupObject(ArrayList.class), notNullValue());
    }

    private SimpleRegistryBootstrap createTestRegistryBootstrap(SimpleRegistryBootstrap.ArtifactType artifactType) throws InitialisationException
    {
        final Properties properties = new Properties();
        properties.put("1", String.format("java.lang.String,%s=%s", SimpleRegistryBootstrap.ArtifactType.APPLY_TO_ARTIFACT_TYPE_PARAMETER_KEY, SimpleRegistryBootstrap.ArtifactType.APP.getAsString()));
        properties.put("2", String.format("java.util.Properties,%s=%s", SimpleRegistryBootstrap.ArtifactType.APPLY_TO_ARTIFACT_TYPE_PARAMETER_KEY, SimpleRegistryBootstrap.ArtifactType.DOMAIN.getAsString()));
        properties.put("3", String.format("java.util.ArrayList,%s=%s", SimpleRegistryBootstrap.ArtifactType.APPLY_TO_ARTIFACT_TYPE_PARAMETER_KEY, SimpleRegistryBootstrap.ArtifactType.ALL.getAsString()));
        properties.put("jms.singletx.transaction.resource1", String.format("%s,optional)", TEST_TRANSACTION_FACTORY_CLASS));
        properties.put("test.singletx.transaction.factory1", FakeTransactionFactory.class.getName());
        properties.put("test.singletx.transaction.resource1", FakeTransactionResource.class.getName());
        SimpleRegistryBootstrap simpleRegistryBootstrap = new SimpleRegistryBootstrap()
        {
            @Override
            protected List<Properties> loadBootstrapProperties() throws InitialisationException
            {
                return Arrays.asList(properties);
            }
        };
        simpleRegistryBootstrap.setSupportedArtifactType(artifactType);
        simpleRegistryBootstrap.setMuleContext(muleContext);
        simpleRegistryBootstrap.initialise();
        return simpleRegistryBootstrap;
    }


}
