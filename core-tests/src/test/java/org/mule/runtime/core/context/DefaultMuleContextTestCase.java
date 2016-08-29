/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.context;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_CONVERTER_RESOLVER;

import org.mule.runtime.core.DataTypeConversionResolver;
import org.mule.runtime.core.DefaultMuleContext;
import org.mule.runtime.core.DynamicDataTypeConversionResolver;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.config.MuleProperties;
import org.mule.runtime.core.api.context.MuleContextFactory;
import org.mule.runtime.core.api.exception.SystemExceptionHandler;
import org.mule.runtime.core.api.registry.ServiceType;
import org.mule.runtime.core.api.util.StreamCloserService;
import org.mule.runtime.core.config.ClusterConfiguration;
import org.mule.runtime.core.config.ExceptionHelper;
import org.mule.runtime.core.connector.PollingController;
import org.mule.runtime.core.registry.MuleRegistryHelper;
import org.mule.runtime.core.util.store.MuleObjectStoreManager;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.internal.verification.VerificationModeFactory;

public class DefaultMuleContextTestCase extends AbstractMuleTestCase {

  public static final String INITIAL_VALUE = "500";
  public static final String VALUE_AFTER_REDEPLOY = "222";
  public static final String TEST_PROTOCOL = "test2";

  private SystemExceptionHandler mockSystemExceptionHandler = mock(SystemExceptionHandler.class);
  private MessagingException mockMessagingException = mock(MessagingException.class);
  private MuleContextFactory muleContextFactory;

  private MuleContext context;

  @Before
  public void before() {
    muleContextFactory = new DefaultMuleContextFactory();
  }

  @After
  public void after() throws MuleException {
    if (context != null) {
      if (context.isStarted()) {
        context.stop();
      }
      context.dispose();
    }
  }

  @Test
  public void testClearExceptionHelperCacheForAppWhenDispose() throws Exception {
    URL baseUrl = DefaultMuleContextTestCase.class.getClassLoader().getResource(".");
    File file = new File(baseUrl.getFile() + ExceptionHelper.SERVICE_ROOT + ServiceType.EXCEPTION.getPath() + "/" + TEST_PROTOCOL
        + "-exception-mappings.properties");
    createExceptionMappingFile(file, INITIAL_VALUE);

    context = muleContextFactory.createMuleContext();
    String value = ExceptionHelper.getErrorMapping(TEST_PROTOCOL, IllegalArgumentException.class, context);
    assertThat(value, is(INITIAL_VALUE));
    context.dispose();

    createExceptionMappingFile(file, VALUE_AFTER_REDEPLOY);

    context = muleContextFactory.createMuleContext();
    context.setExecutionClassLoader(getClass().getClassLoader());
    value = ExceptionHelper.getErrorMapping(TEST_PROTOCOL, IllegalArgumentException.class, context);
    assertThat(value, is(VALUE_AFTER_REDEPLOY));
  }

  private void createExceptionMappingFile(File exceptionMappingFile, String value) throws IOException {
    FileWriter fileWriter = null;
    try {
      fileWriter = new FileWriter(exceptionMappingFile);
      fileWriter.append("\njava.lang.IllegalArgumentException=" + value);
    } finally {
      if (fileWriter != null) {
        fileWriter.close();
      }
    }
  }

  @Test
  public void callSystemExceptionHandlerWhenExceptionIsMessagingException() throws Exception {
    context = muleContextFactory.createMuleContext();
    context.setExceptionListener(mockSystemExceptionHandler);
    context.handleException(mockMessagingException);
    verify(mockSystemExceptionHandler, VerificationModeFactory.times(1)).handleException(mockMessagingException, null);
  }

  @Test
  public void getObjectStoreManager() throws Exception {
    context = muleContextFactory.createMuleContext();
    Object osManager = context.getObjectStoreManager();
    assertThat(osManager, instanceOf(MuleObjectStoreManager.class));
  }

  @Test
  public void defaultMuleClusterConfiguration() throws Exception {
    context = muleContextFactory.createMuleContext();
    context.start();
    assertThat(context.getClusterId(), is(""));
    assertThat(context.getClusterNodeId(), is(0));
  }

  @Test
  public void overriddenClusterConfiguration() throws Exception {
    final int clusterNodeId = 22;
    final String clusterId = "some-id";
    context = muleContextFactory.createMuleContext();
    context.getRegistry().registerObject(MuleProperties.OBJECT_CLUSTER_CONFIGURATION, new ClusterConfiguration() {

      @Override
      public String getClusterId() {
        return clusterId;
      }

      @Override
      public int getClusterNodeId() {

        return clusterNodeId;
      }
    });
    context.start();
    assertThat(context.getClusterId(), is(clusterId));
    assertThat(context.getClusterNodeId(), is(clusterNodeId));
  }

  @Test
  public void defaultMulePollingController() throws Exception {
    context = muleContextFactory.createMuleContext();
    context.start();
    assertThat(context.isPrimaryPollingInstance(), is(true));
  }

  @Test
  public void overriddenMulePollingController() throws Exception {
    context = muleContextFactory.createMuleContext();
    context.getRegistry().registerObject(MuleProperties.OBJECT_POLLING_CONTROLLER, (PollingController) () -> false);
    context.start();
    assertThat(context.isPrimaryPollingInstance(), is(false));
  }

  @Test
  public void getStreamCloserService() throws Exception {
    context = muleContextFactory.createMuleContext();
    StreamCloserService serviceFromRegistry =
        context.getRegistry().lookupObject(MuleProperties.OBJECT_MULE_STREAM_CLOSER_SERVICE);
    MuleRegistryHelper registry = spy((MuleRegistryHelper) context.getRegistry());
    ((DefaultMuleContext) context).setMuleRegistry(registry);

    StreamCloserService streamCloserService = context.getStreamCloserService();
    assertNotNull(streamCloserService);

    assertSame(serviceFromRegistry, streamCloserService);

    // test that subsequent invocations consistently returns the same object
    assertSame(streamCloserService, context.getStreamCloserService());

    // verify we're not fetching from registry many times
    verify(registry, times(1)).lookupObject(MuleProperties.OBJECT_MULE_STREAM_CLOSER_SERVICE);
  }

  @Test
  public void cachesDataTypeConversionResolver() throws Exception {
    context = new DefaultMuleContextFactory().createMuleContext();
    final MuleRegistryHelper muleRegistry = mock(MuleRegistryHelper.class);
    ((DefaultMuleContext) context).setMuleRegistry(muleRegistry);

    DataTypeConversionResolver dataTypeConverterResolver1 = context.getDataTypeConverterResolver();
    DataTypeConversionResolver dataTypeConverterResolver2 = context.getDataTypeConverterResolver();

    assertThat(dataTypeConverterResolver1, instanceOf(DynamicDataTypeConversionResolver.class));
    assertThat(dataTypeConverterResolver2, sameInstance(dataTypeConverterResolver1));
    verify(muleRegistry).lookupObject(OBJECT_CONVERTER_RESOLVER);
  }
}
