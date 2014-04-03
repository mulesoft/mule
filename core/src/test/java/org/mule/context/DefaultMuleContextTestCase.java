/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.context;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.mule.DefaultMuleContext;
import org.mule.api.MessagingException;
import org.mule.api.MuleContext;
import org.mule.api.config.MuleProperties;
import org.mule.api.exception.SystemExceptionHandler;
import org.mule.api.registry.ServiceType;
import org.mule.api.util.StreamCloserService;
import org.mule.config.ClusterConfiguration;
import org.mule.config.ExceptionHelper;
import org.mule.registry.MuleRegistryHelper;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.transport.PollingController;
import org.mule.util.IOUtils;
import org.mule.util.SpiUtils;
import org.mule.util.store.MuleObjectStoreManager;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;

import junit.framework.Assert;
import org.hamcrest.core.Is;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.internal.verification.VerificationModeFactory;

public class DefaultMuleContextTestCase extends AbstractMuleTestCase
{

    public static final String INITIAL_VALUE = "500";
    public static final String VALUE_AFTER_REDEPLOY = "222";
    public static final String TEST_PROTOCOL = "test2";

    private SystemExceptionHandler mockSystemExceptionHandler = Mockito.mock(SystemExceptionHandler.class);
    private MessagingException mockMessagingException = Mockito.mock(MessagingException.class);

    @Test
    public void testClearExceptionHelperCacheForAppWhenDispose() throws Exception
    {
        String resourcePath = SpiUtils.SERVICE_ROOT + ServiceType.EXCEPTION.getPath()+ "/" + TEST_PROTOCOL + "-exception-mappings.properties";
        URL resource = IOUtils.getResourceAsUrl(resourcePath, this.getClass());
        File file = new File(resource.getPath());

        createExceptionMappingFile(file, INITIAL_VALUE);

        MuleContext ctx = new DefaultMuleContextFactory().createMuleContext();
        String value = ExceptionHelper.getErrorMapping(TEST_PROTOCOL, IllegalArgumentException.class, ctx);
        assertThat(value, is(INITIAL_VALUE));
        ctx.dispose();

        createExceptionMappingFile(file, VALUE_AFTER_REDEPLOY);

        ctx = new DefaultMuleContextFactory().createMuleContext();
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
        MuleContext context = new DefaultMuleContextFactory().createMuleContext();
        context.setExceptionListener(mockSystemExceptionHandler);
        context.handleException(mockMessagingException);
        verify(mockSystemExceptionHandler, VerificationModeFactory.times(1)).handleException(mockMessagingException,null);
    }
    
    @Test
    public void getObjectStoreManager() throws Exception
    {
        MuleContext context = new DefaultMuleContextFactory().createMuleContext();
        Object osManager = context.getObjectStoreManager();
        Assert.assertTrue(osManager instanceof MuleObjectStoreManager);
    }

    @Test
    public void defaultMuleClusterConfiguration() throws Exception
    {
        MuleContext context = new DefaultMuleContextFactory().createMuleContext();
        context.start();
        org.junit.Assert.assertThat(context.getClusterId(), Is.is(""));
        org.junit.Assert.assertThat(context.getClusterNodeId(), Is.is(0));
    }

    @Test
    public void overriddenClusterConfiguration() throws Exception
    {
        final int clusterNodeId = 22;
        final String clusterId = "some-id";
        MuleContext context = new DefaultMuleContextFactory().createMuleContext();
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
        MuleContext context = new DefaultMuleContextFactory().createMuleContext();
        context.start();
        assertThat(context.isPrimaryPollingInstance(), is(true));
    }

    @Test
    public void overriddenMulePollingController() throws Exception
    {
        MuleContext context = new DefaultMuleContextFactory().createMuleContext();
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
        DefaultMuleContext context = (DefaultMuleContext) new DefaultMuleContextFactory().createMuleContext();
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

}
