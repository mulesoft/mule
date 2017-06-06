/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.context;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_CLUSTER_CONFIGURATION;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_CONVERTER_RESOLVER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_MULE_STREAM_CLOSER_SERVICE;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_POLLING_CONTROLLER;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.registry.ServiceType.EXCEPTION;
import static org.mule.runtime.core.config.ExceptionHelper.RESOURCE_ROOT;
import static org.mule.runtime.core.config.ExceptionHelper.getErrorMapping;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.core.DefaultMuleContext;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.connector.SchedulerController;
import org.mule.runtime.core.api.context.MuleContextFactory;
import org.mule.runtime.core.api.exception.SystemExceptionHandler;
import org.mule.runtime.core.api.transformer.DataTypeConversionResolver;
import org.mule.runtime.core.api.util.StreamCloserService;
import org.mule.runtime.core.config.ClusterConfiguration;
import org.mule.runtime.core.config.builders.DefaultsConfigurationBuilder;
import org.mule.runtime.core.context.notification.ServerNotificationManager;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.internal.transformer.DynamicDataTypeConversionResolver;
import org.mule.runtime.core.lifecycle.MuleContextLifecycleManager;
import org.mule.runtime.core.registry.MuleRegistryHelper;
import org.mule.runtime.core.util.store.MuleObjectStoreManager;
import org.mule.tck.config.TestServicesConfigurationBuilder;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;

public class DefaultMuleContextTestCase extends AbstractMuleTestCase {

  private static final Logger LOGGER = getLogger(DefaultMuleContextTestCase.class);

  public static final String INITIAL_VALUE = "500";
  public static final String VALUE_AFTER_REDEPLOY = "222";
  public static final String TEST_PROTOCOL = "test2";

  private SystemExceptionHandler mockSystemExceptionHandler = mock(SystemExceptionHandler.class);
  private MessagingException mockMessagingException = mock(MessagingException.class);
  @Rule
  public TestServicesConfigurationBuilder testServicesConfigurationBuilder = new TestServicesConfigurationBuilder();
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
  public void callDisposeIfInitFails() throws Exception {
    ServerNotificationManager mockNotificationManager = mock(ServerNotificationManager.class);
    doThrow(MuleRuntimeException.class).when(mockNotificationManager).initialise();

    DefaultMuleContextBuilder muleContextBuilder = new DefaultMuleContextBuilder();
    muleContextBuilder.setLifecycleManager(new MuleContextLifecycleManager());
    muleContextBuilder.setNotificationManager(mockNotificationManager);
    DefaultMuleContext defaultMuleContext = (DefaultMuleContext) muleContextBuilder.buildMuleContext();

    try {
      defaultMuleContext.initialise();
      fail("exception expected");
    } catch (Exception e) {
      verify(mockNotificationManager).dispose();
    }
  }

  @Test
  public void testClearExceptionHelperCacheForAppWhenDispose() throws Exception {
    File file = new File(DefaultMuleContextTestCase.class.getClassLoader()
        .getResource(RESOURCE_ROOT + EXCEPTION.getPath() + "/" + TEST_PROTOCOL + "-exception-mappings.properties").toURI());
    createExceptionMappingFile(file, INITIAL_VALUE);

    createMuleContext();
    String value = getErrorMapping(TEST_PROTOCOL, IllegalArgumentException.class, context);
    assertThat(value, is(INITIAL_VALUE));
    context.dispose();

    createExceptionMappingFile(file, VALUE_AFTER_REDEPLOY);

    createMuleContext();
    context.setExecutionClassLoader(getClass().getClassLoader());
    value = getErrorMapping(TEST_PROTOCOL, IllegalArgumentException.class, context);
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
    createMuleContext();
    disposeIfNeeded(context.getExceptionListener(), LOGGER);
    context.setExceptionListener(mockSystemExceptionHandler);
    context.handleException(mockMessagingException);
    verify(mockSystemExceptionHandler, times(1)).handleException(mockMessagingException, null);
  }

  @Test
  public void getObjectStoreManager() throws Exception {
    createMuleContext();
    Object osManager = context.getObjectStoreManager();
    assertThat(osManager, instanceOf(MuleObjectStoreManager.class));
  }

  @Test
  public void defaultMuleClusterConfiguration() throws Exception {
    createMuleContext();
    context.start();
    assertThat(context.getClusterId(), is(""));
    assertThat(context.getClusterNodeId(), is(0));
  }

  @Test
  public void overriddenClusterConfiguration() throws Exception {
    final int clusterNodeId = 22;
    final String clusterId = "some-id";
    createMuleContext();
    context.getRegistry().registerObject(OBJECT_CLUSTER_CONFIGURATION, new ClusterConfiguration() {

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
    createMuleContext();
    context.start();
    assertThat(context.isPrimaryPollingInstance(), is(true));
  }

  @Test
  public void overriddenMulePollingController() throws Exception {
    createMuleContext();
    context.getRegistry().registerObject(OBJECT_POLLING_CONTROLLER, (SchedulerController) () -> false);
    context.start();
    assertThat(context.isPrimaryPollingInstance(), is(false));
  }

  @Test
  public void getStreamCloserService() throws Exception {
    createMuleContext();
    StreamCloserService serviceFromRegistry = context.getRegistry().lookupObject(OBJECT_MULE_STREAM_CLOSER_SERVICE);
    MuleRegistryHelper registry = spy((MuleRegistryHelper) context.getRegistry());
    ((DefaultMuleContext) context).setMuleRegistry(registry);

    StreamCloserService streamCloserService = context.getStreamCloserService();
    assertThat(streamCloserService, not(nullValue()));

    assertThat(streamCloserService, is(sameInstance(serviceFromRegistry)));

    // test that subsequent invocations consistently returns the same object
    assertThat(context.getStreamCloserService(), is(sameInstance(streamCloserService)));

    // verify we're not fetching from registry many times
    verify(registry, times(1)).lookupObject(OBJECT_MULE_STREAM_CLOSER_SERVICE);
  }

  @Test
  public void cachesDataTypeConversionResolver() throws Exception {
    createMuleContext();
    disposeIfNeeded(context.getRegistry(), LOGGER);
    final MuleRegistryHelper muleRegistry = mock(MuleRegistryHelper.class);
    ((DefaultMuleContext) context).setMuleRegistry(muleRegistry);

    DataTypeConversionResolver dataTypeConverterResolver1 = context.getDataTypeConverterResolver();
    DataTypeConversionResolver dataTypeConverterResolver2 = context.getDataTypeConverterResolver();

    assertThat(dataTypeConverterResolver1, instanceOf(DynamicDataTypeConversionResolver.class));
    assertThat(dataTypeConverterResolver2, sameInstance(dataTypeConverterResolver1));
    verify(muleRegistry).lookupObject(OBJECT_CONVERTER_RESOLVER);
  }

  protected void createMuleContext() throws MuleException {
    context = muleContextFactory.createMuleContext(testServicesConfigurationBuilder, new DefaultsConfigurationBuilder());
  }
}
