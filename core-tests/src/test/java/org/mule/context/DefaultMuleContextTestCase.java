/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.context;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mule.api.config.MuleProperties.OBJECT_CONVERTER_RESOLVER;
import org.mule.DataTypeConversionResolver;
import org.mule.DefaultMuleContext;
import org.mule.DynamicDataTypeConversionResolver;
import org.mule.api.MessagingException;
import org.mule.api.MuleContext;
import org.mule.api.config.MuleProperties;
import org.mule.api.context.MuleContextFactory;
import org.mule.api.exception.SystemExceptionHandler;
import org.mule.api.registry.ServiceType;
import org.mule.api.util.StreamCloserService;
import org.mule.config.ClusterConfiguration;
import org.mule.config.ExceptionHelper;
import org.mule.registry.MuleRegistryHelper;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.testmodels.mule.TestTransactionManagerFactory;
import org.mule.transport.PollingController;
import org.mule.util.SpiUtils;
import org.mule.util.store.MuleObjectStoreManager;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;

import javax.transaction.TransactionManager;

import junit.framework.Assert;
import org.hamcrest.CoreMatchers;
import org.hamcrest.core.Is;
import org.hamcrest.core.IsNull;
import org.junit.Before;
import org.junit.Test;
import org.mockito.internal.verification.VerificationModeFactory;

public class DefaultMuleContextTestCase extends AbstractMuleTestCase
{

    public static final String INITIAL_VALUE = "500";
    public static final String VALUE_AFTER_REDEPLOY = "222";
    public static final String TEST_PROTOCOL = "test2";

    private SystemExceptionHandler mockSystemExceptionHandler = mock(SystemExceptionHandler.class);
    private MessagingException mockMessagingException = mock(MessagingException.class);
    private MuleContextFactory muleContextFactory;

    @Before
    public void before()
    {
        muleContextFactory = new DefaultMuleContextFactory();
    }

    @Test
    public void testClearExceptionHelperCacheForAppWhenDispose() throws Exception
    {
        URL baseUrl = DefaultMuleContextTestCase.class.getClassLoader().getResource(".");
        File file = new File(baseUrl.getFile() + SpiUtils.SERVICE_ROOT + ServiceType.EXCEPTION.getPath()+ "/" + TEST_PROTOCOL + "-exception-mappings.properties");
        createExceptionMappingFile(file, INITIAL_VALUE);

        MuleContext ctx = muleContextFactory.createMuleContext();
        String value = ExceptionHelper.getErrorMapping(TEST_PROTOCOL, IllegalArgumentException.class, ctx);
        assertThat(value,is(INITIAL_VALUE));
        ctx.dispose();

        createExceptionMappingFile(file, VALUE_AFTER_REDEPLOY);

        ctx = muleContextFactory.createMuleContext();
        ctx.setExecutionClassLoader(getClass().getClassLoader());
        value = ExceptionHelper.getErrorMapping(TEST_PROTOCOL, IllegalArgumentException.class, ctx);
        assertThat(value, is(VALUE_AFTER_REDEPLOY));
    }

    private void createExceptionMappingFile(File exceptionMappingFile, String value) throws IOException
    {
        FileWriter fileWriter = null;
        try
        {
            fileWriter = new FileWriter(exceptionMappingFile);
            fileWriter.append("\njava.lang.IllegalArgumentException=" + value);
        }
        finally
        {
            if (fileWriter != null)
            {
                fileWriter.close();
            }
        }
    }

    @Test
    public void callSystemExceptionHandlerWhenExceptionIsMessagingException() throws Exception
    {
        MuleContext context = muleContextFactory.createMuleContext();
        context.setExceptionListener(mockSystemExceptionHandler);
        context.handleException(mockMessagingException);
        verify(mockSystemExceptionHandler, VerificationModeFactory.times(1)).handleException(mockMessagingException,null);
    }
    
    @Test
    public void getObjectStoreManager() throws Exception
    {
        MuleContext context = muleContextFactory.createMuleContext();
        Object osManager = context.getObjectStoreManager();
        Assert.assertTrue(osManager instanceof MuleObjectStoreManager);
    }

    @Test
    public void defaultMuleClusterConfiguration() throws Exception
    {
        MuleContext context = muleContextFactory.createMuleContext();
        context.start();
        org.junit.Assert.assertThat(context.getClusterId(), Is.is(""));
        org.junit.Assert.assertThat(context.getClusterNodeId(), Is.is(0));
    }

    @Test
    public void overriddenClusterConfiguration() throws Exception
    {
        final int clusterNodeId = 22;
        final String clusterId = "some-id";
        MuleContext context = muleContextFactory.createMuleContext();
        context.getRegistry().registerObject(MuleProperties.OBJECT_CLUSTER_CONFIGURATION, new ClusterConfiguration()
        {
            @Override
            public String getClusterId()
            {
                return clusterId;
            }

            @Override
            public int getClusterNodeId()
            {

                return clusterNodeId;
            }
        });
        context.start();
        assertThat(context.getClusterId(), is(clusterId));
        assertThat(context.getClusterNodeId(), is(clusterNodeId));
    }

    @Test
    public void defaultMulePollingController() throws Exception
    {
        MuleContext context = muleContextFactory.createMuleContext();
        context.start();
        assertThat(context.isPrimaryPollingInstance(), is(true));
    }

    @Test
    public void overriddenMulePollingController() throws Exception
    {
        MuleContext context = muleContextFactory.createMuleContext();
        context.getRegistry().registerObject(MuleProperties.OBJECT_POLLING_CONTROLLER, new PollingController()
        {
            @Override
            public boolean isPrimaryPollingInstance()
            {
                return false;
            }
        });
        context.start();
        assertThat(context.isPrimaryPollingInstance(), is(false));
    }

    @Test
    public void getStreamCloserService() throws Exception
    {
        DefaultMuleContext context = (DefaultMuleContext) muleContextFactory.createMuleContext();
        StreamCloserService serviceFromRegistry = context.getRegistry().lookupObject(MuleProperties.OBJECT_MULE_STREAM_CLOSER_SERVICE);
        MuleRegistryHelper registry = spy((MuleRegistryHelper) context.getRegistry());
        context.setMuleRegistry(registry);

        StreamCloserService streamCloserService = context.getStreamCloserService();
        assertNotNull(streamCloserService);

        assertSame(serviceFromRegistry, streamCloserService);

        // test that subsequent invocations consistently returns the same object
        assertSame(streamCloserService, context.getStreamCloserService());

        // verify we're not fetching from registry many times
        verify(registry, times(1)).lookupObject(MuleProperties.OBJECT_MULE_STREAM_CLOSER_SERVICE);
    }

    @Test
    public void registerTransactionManager() throws Exception
    {
        DefaultMuleContext context = (DefaultMuleContext) new DefaultMuleContextFactory().createMuleContext();
        context.getRegistry().registerObject("Test Transaction Manager Factory", new TestTransactionManagerFactory());
        assertThat(context.getRegistry().lookupObject(MuleProperties.OBJECT_TRANSACTION_MANAGER), is(IsNull.nullValue()));
        TransactionManager transactionManager = context.getTransactionManager();
        assertThat(transactionManager, not(is(IsNull.nullValue())));
        assertThat((TransactionManager)context.getRegistry().lookupObject(MuleProperties.OBJECT_TRANSACTION_MANAGER), is(CoreMatchers.sameInstance(transactionManager)));
    }

    @Test
    public void cachesDataTypeConversionResolver() throws Exception
    {
        DefaultMuleContext context = (DefaultMuleContext) new DefaultMuleContextFactory().createMuleContext();
        final MuleRegistryHelper muleRegistry = mock(MuleRegistryHelper.class);
        context.setMuleRegistry(muleRegistry);

        DataTypeConversionResolver dataTypeConverterResolver1 = context.getDataTypeConverterResolver();
        DataTypeConversionResolver dataTypeConverterResolver2 = context.getDataTypeConverterResolver();

        assertThat(dataTypeConverterResolver1, instanceOf(DynamicDataTypeConversionResolver.class));
        assertThat(dataTypeConverterResolver2, sameInstance(dataTypeConverterResolver1));
        verify(muleRegistry).lookupObject(OBJECT_CONVERTER_RESOLVER);
    }
}
