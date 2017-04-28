/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core;

import static java.util.Arrays.asList;
import static junit.framework.Assert.assertNotNull;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_MULE_SIMPLE_REGISTRY_BOOTSTRAP;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_QUEUE_MANAGER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_SECURITY_MANAGER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_STORE_DEFAULT_IN_MEMORY_NAME;
import static org.mule.runtime.core.api.config.MuleProperties.QUEUE_STORE_DEFAULT_IN_MEMORY_NAME;

import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.api.context.MuleContextBuilder;
import org.mule.runtime.core.api.context.notification.MuleContextListener;
import org.mule.runtime.core.config.DefaultMuleConfiguration;
import org.mule.runtime.core.config.builders.AbstractConfigurationBuilder;
import org.mule.runtime.core.config.builders.DefaultsConfigurationBuilder;
import org.mule.runtime.core.config.builders.SimpleConfigurationBuilder;
import org.mule.runtime.core.context.DefaultMuleContextBuilder;
import org.mule.runtime.core.context.DefaultMuleContextFactory;
import org.mule.tck.config.TestServicesConfigurationBuilder;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.testmodels.fruit.Banana;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InOrder;

public class DefaultMuleContextFactoryTestCase extends AbstractMuleTestCase {

  @Rule
  public TestServicesConfigurationBuilder testServicesConfigurationBuilder = new TestServicesConfigurationBuilder();

  private DefaultMuleContextFactory muleContextFactory = new DefaultMuleContextFactory();
  private static String TEST_STRING_KEY = "test";
  private static String TEST_STRING_VALUE = "test_value";
  private static String TEST_STRING_KEY2 = "test2";
  private static String TEST_STRING_VALUE2 = "test_value2";
  private static String TEST_OBJECT_NAME = "testObject";

  private MuleContext context;

  @After
  public void disposeContext() {
    if (context != null && !context.isDisposed()) {
      context.dispose();
    }
  }

  @Test
  public void testCreateMuleContext() throws InitialisationException, ConfigurationException {
    context = muleContextFactory.createMuleContext(testServicesConfigurationBuilder, new DefaultsConfigurationBuilder());

    assertMuleContextConfiguration(context);
    assertDefaults(context);
  }

  @Test
  public void testCreateMuleContextConfigurationBuilder() throws InitialisationException, ConfigurationException {
    context = muleContextFactory.createMuleContext(testServicesConfigurationBuilder, new TestConfigurationBuilder());

    assertMuleContextConfiguration(context);
    assertConfigurationBuilder1Objects(context);
    assertNoDefaults(context);
  }

  @Test
  public void testCreateMuleContextListMuleContextBuilder() throws InitialisationException, ConfigurationException {
    List<ConfigurationBuilder> configBuilders = new ArrayList<>();
    configBuilders.add(testServicesConfigurationBuilder);
    configBuilders.add(new TestConfigurationBuilder());
    configBuilders.add(new TestConfigurationBuilder2());

    TestMuleContextBuilder muleContextBuilder = new TestMuleContextBuilder();
    context = muleContextFactory.createMuleContext(configBuilders, muleContextBuilder);

    assertCustomMuleContext(context);
    assertConfigurationBuilder1Objects(context);
    assertConfigurationBuilder2Objects(context);
    assertNoDefaults(context);
  }

  @Test
  public void testCreateMuleContextMuleContextBuilder() throws InitialisationException, ConfigurationException {
    TestMuleContextBuilder muleContextBuilder = new TestMuleContextBuilder();
    context =
        muleContextFactory.createMuleContext(asList(testServicesConfigurationBuilder, new SimpleConfigurationBuilder(null)),
                                             muleContextBuilder);

    assertCustomMuleContext(context);
    assertNoDefaults(context);
  }

  @Test
  public void testCreateMuleContextConfigurationBuilderMuleContextBuilder()
      throws InitialisationException, ConfigurationException {
    TestMuleContextBuilder muleContextBuilder = new TestMuleContextBuilder();
    context = muleContextFactory
        .createMuleContext(asList(testServicesConfigurationBuilder, new TestConfigurationBuilder2()), muleContextBuilder);

    assertCustomMuleContext(context);
    assertConfigurationBuilder2Objects(context);
    assertNoDefaults(context);
  }

  @Test
  public void testCreateMuleContextString() throws InitialisationException, ConfigurationException {
    context = null;
    try {
      context = muleContextFactory.createMuleContext("log4j2-test.xml");
    } catch (ConfigurationException e) {
      assertEquals("No suitable configuration builder for resource \"[ConfigResource{resourceName='log4j2-test.xml'}]\" found.  "
          + "Check you have configuration module on your classpath and are using correct file extension. "
          + "(org.mule.runtime.core.api.config.ConfigurationException)", e.getMessage());
    }
    assertNull(context);
  }

  @Test
  public void testCreateMuleContextStringProperties() throws InitialisationException, ConfigurationException {
    Properties properties = new Properties();
    properties.put("testKey1", "testValue1");
    properties.put("testKey2", "testValue2");

    context = null;
    try {
      context = muleContextFactory.createMuleContext("log4j2-test.xml", properties);
    } catch (ConfigurationException e) {
      assertEquals("No suitable configuration builder for resource \"[ConfigResource{resourceName='log4j2-test.xml'}]\" found.  "
          + "Check you have configuration module on your classpath and are using correct file extension. "
          + "(org.mule.runtime.core.api.config.ConfigurationException)", e.getMessage());
    }

    assertNull(context);
  }

  @Test
  public void testCreateMuleContextConfigurationBuilderProperties() throws InitialisationException, ConfigurationException {
    Properties properties = new Properties();
    properties.put("testKey3", "testValue3");
    properties.put("testKey4", "testValue4");

    context = muleContextFactory.createMuleContext(asList(testServicesConfigurationBuilder, new TestConfigurationBuilder()),
                                                   properties);

    assertMuleContextConfiguration(context);
    assertConfigurationBuilder1Objects(context);

    assertEquals("testValue3", context.getRegistry().lookupObject("testKey3"));
    assertEquals("testValue4", context.getRegistry().lookupObject("testKey4"));

    assertNoDefaults(context);
  }

  @Test
  public void notifiesMuleContextEvents() throws InitialisationException, ConfigurationException {
    MuleContextListener listener = mock(MuleContextListener.class);
    ConfigurationBuilder configurationBuilder = mock(ConfigurationBuilder.class);
    context = mock(MuleContext.class);
    MuleContextBuilder contextBuilder = mock(MuleContextBuilder.class);
    when(contextBuilder.buildMuleContext()).thenReturn(context);

    muleContextFactory.addListener(listener);

    muleContextFactory.createMuleContext(configurationBuilder, contextBuilder);

    InOrder inOrder = inOrder(listener);
    inOrder.verify(listener, times(1)).onCreation(context);
    inOrder.verify(listener, times(1)).onConfiguration(context);
    // TODO MULE-10061 - Review once the MuleContext lifecycle is clearly defined
    inOrder.verify(listener, times(1)).onInitialization(context);
  }

  private void assertDefaults(MuleContext context) {
    // Asert existance of defauts in registry
    assertNotNull(context.getRegistry().lookupObject(OBJECT_QUEUE_MANAGER));
    assertNotNull(context.getRegistry().lookupObject(OBJECT_SECURITY_MANAGER));
    assertNotNull(context.getRegistry().lookupObject(OBJECT_STORE_DEFAULT_IN_MEMORY_NAME));
    assertNotNull(context.getRegistry().lookupObject(QUEUE_STORE_DEFAULT_IN_MEMORY_NAME));
  }

  private void assertNoDefaults(MuleContext context) {
    // Asert non-existance of defauts in registry
    assertNull(context.getRegistry().lookupObject(OBJECT_QUEUE_MANAGER));
    assertNull(context.getRegistry().lookupObject(OBJECT_SECURITY_MANAGER));
    assertNull(context.getRegistry().lookupObject(OBJECT_STORE_DEFAULT_IN_MEMORY_NAME));
    assertNull(context.getRegistry().lookupObject(QUEUE_STORE_DEFAULT_IN_MEMORY_NAME));
    assertNull(context.getRegistry().lookupObject(OBJECT_MULE_SIMPLE_REGISTRY_BOOTSTRAP));
  }

  private void assertMuleContextConfiguration(MuleContext context) {
    assertThat(context, notNullValue());
    assertThat(context, instanceOf(DefaultMuleContext.class));
    assertTrue(context.isInitialised());
    assertThat(context.getConfiguration(), notNullValue());
    assertThat(context.getConfiguration(), instanceOf(DefaultMuleConfiguration.class));
    assertThat(context.getLifecycleManager(), notNullValue());
    assertThat(context.getNotificationManager(), notNullValue());
  }

  private void assertCustomMuleContext(MuleContext context) {
    assertThat(context, notNullValue());
    assertThat(context, instanceOf(TestMuleContext.class));
    assertTrue(context.isInitialised());
    assertThat(context.getConfiguration(), notNullValue());
    assertThat(context.getLifecycleManager(), notNullValue());
    assertThat(context.getNotificationManager(), notNullValue());
  }

  private void assertConfigurationBuilder1Objects(MuleContext context) {
    // Test Registry contents for existance of object configured by
    // TestConfigurationBuilder
    assertEquals(TEST_STRING_VALUE, context.getRegistry().lookupObject(TEST_STRING_KEY));

    Object obj = context.getRegistry().lookupObject(TEST_OBJECT_NAME);
    assertNotNull(obj);
    assertEquals(Banana.class, obj.getClass());
  }

  private void assertConfigurationBuilder2Objects(MuleContext context) {
    // Test Registry contents for existance of object configured by
    // TestConfigurationBuilder2
    assertEquals(TEST_STRING_VALUE2, context.getRegistry().lookupObject(TEST_STRING_KEY2));
  }

  static class TestConfigurationBuilder extends AbstractConfigurationBuilder {

    @Override
    protected void doConfigure(MuleContext context) throws Exception {
      context.getRegistry().registerObject(TEST_STRING_KEY, TEST_STRING_VALUE);
      context.getRegistry().registerObject(TEST_OBJECT_NAME, new Banana());
    }
  }

  static class TestConfigurationBuilder2 extends AbstractConfigurationBuilder {

    @Override
    protected void doConfigure(MuleContext context) throws Exception {
      context.getRegistry().registerObject(TEST_STRING_KEY2, TEST_STRING_VALUE2);
    }
  }

  static class TestMuleContextBuilder extends DefaultMuleContextBuilder {

    @Override
    protected DefaultMuleContext createDefaultMuleContext() {
      return new TestMuleContext();
    }
  }

  static class TestMuleContext extends DefaultMuleContext {

    public TestMuleContext() {
      super();
    }
  }

  static class TestMuleConfiguration extends DefaultMuleConfiguration {
    // just a skeleton
  }
}
