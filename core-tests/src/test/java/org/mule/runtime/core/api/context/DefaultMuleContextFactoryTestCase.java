/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.context;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;
import static junit.framework.Assert.assertNotNull;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.store.ObjectStoreManager.BASE_IN_MEMORY_OBJECT_STORE_KEY;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_MULE_SIMPLE_REGISTRY_BOOTSTRAP;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_QUEUE_MANAGER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_SECURITY_MANAGER;
import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.APP;
import static org.mule.tck.util.MuleContextUtils.mockMuleContext;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.api.config.DefaultMuleConfiguration;
import org.mule.runtime.core.api.config.builders.SimpleConfigurationBuilder;
import org.mule.runtime.core.api.context.notification.MuleContextListener;
import org.mule.runtime.core.internal.config.builders.DefaultsConfigurationBuilder;
import org.mule.runtime.core.internal.context.DefaultMuleContext;
import org.mule.runtime.core.internal.context.DefaultMuleContextBuilder;
import org.mule.runtime.core.internal.context.MuleContextWithRegistries;
import org.mule.tck.config.TestServicesConfigurationBuilder;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.testmodels.fruit.Banana;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InOrder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

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

  private SimpleConfigurationBuilder testConfigBuilder;
  private SimpleConfigurationBuilder testConfigBuilder2;

  @Before
  public void before() {
    Map<String, Object> objects = new HashMap<>();
    objects.put(TEST_STRING_KEY, TEST_STRING_VALUE);
    objects.put(TEST_OBJECT_NAME, new Banana());
    testConfigBuilder = new SimpleConfigurationBuilder(objects);

    testConfigBuilder2 = new SimpleConfigurationBuilder(singletonMap(TEST_STRING_KEY2, TEST_STRING_VALUE2));
  }

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
    context = muleContextFactory.createMuleContext(testServicesConfigurationBuilder, testConfigBuilder);

    assertMuleContextConfiguration(context);
    assertConfigurationBuilder1Objects(context);
    assertNoDefaults(context);
  }

  @Test
  public void testCreateMuleContextListMuleContextBuilder() throws InitialisationException, ConfigurationException {
    List<ConfigurationBuilder> configBuilders = new ArrayList<>();
    configBuilders.add(testServicesConfigurationBuilder);
    configBuilders.add(testConfigBuilder);
    configBuilders.add(testConfigBuilder2);

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
        .createMuleContext(asList(testServicesConfigurationBuilder, testConfigBuilder2), muleContextBuilder);

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
      assertThat(e.getMessage(),
                 equalTo("No suitable configuration builder for resource \"[ConfigResource{resourceName='log4j2-test.xml'}]\" found.  "
                     + "Check you have configuration module on your classpath and are using correct file extension."));
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
      context = muleContextFactory.createMuleContext("log4j2-test.xml", (Map) properties);
    } catch (ConfigurationException e) {
      assertThat(e.getMessage(),
                 equalTo("No suitable configuration builder for resource \"[ConfigResource{resourceName='log4j2-test.xml'}]\" found.  "
                     + "Check you have configuration module on your classpath and are using correct file extension."));
    }

    assertNull(context);
  }

  @Test
  public void testCreateMuleContextConfigurationBuilderProperties() throws InitialisationException, ConfigurationException {
    Properties properties = new Properties();
    properties.put("testKey3", "testValue3");
    properties.put("testKey4", "testValue4");

    context = muleContextFactory.createMuleContext(asList(testServicesConfigurationBuilder, testConfigBuilder),
                                                   (Map) properties);

    assertMuleContextConfiguration(context);
    assertConfigurationBuilder1Objects(context);

    assertEquals("testValue3", ((MuleContextWithRegistries) context).getRegistry().lookupObject("testKey3"));
    assertEquals("testValue4", ((MuleContextWithRegistries) context).getRegistry().lookupObject("testKey4"));

    assertNoDefaults(context);
  }

  @Test
  public void notifiesMuleContextEvents() throws InitialisationException, ConfigurationException {
    MuleContextListener listener = mock(MuleContextListener.class);
    ConfigurationBuilder configurationBuilder = mock(ConfigurationBuilder.class);
    context = mockMuleContext();
    MuleContextBuilder contextBuilder = mock(MuleContextBuilder.class);
    when(contextBuilder.buildMuleContext()).thenReturn(context);

    muleContextFactory.addListener(listener);

    muleContextFactory.createMuleContext(configurationBuilder, contextBuilder);

    InOrder inOrder = inOrder(listener);
    inOrder.verify(listener, times(1)).onCreation(context);
    inOrder.verify(listener, times(1)).onConfiguration(context);
    // TODO MULE-10061 - Review once the MuleContext lifecycle is clearly defined
    inOrder.verify(listener, times(1)).onInitialization(eq(context), any(Registry.class));
  }

  private void assertDefaults(MuleContext context) {
    // Assert existance of defaults in registry
    assertNotNull(((MuleContextWithRegistries) context).getRegistry().lookupObject(OBJECT_QUEUE_MANAGER));
    assertNotNull(((MuleContextWithRegistries) context).getRegistry().lookupObject(OBJECT_SECURITY_MANAGER));
    assertNotNull(((MuleContextWithRegistries) context).getRegistry().lookupObject(BASE_IN_MEMORY_OBJECT_STORE_KEY));
  }

  private void assertNoDefaults(MuleContext context) {
    // Assert non-existance of defaults in registry
    assertNull(((MuleContextWithRegistries) context).getRegistry().lookupObject(OBJECT_QUEUE_MANAGER));
    assertNull(((MuleContextWithRegistries) context).getRegistry().lookupObject(OBJECT_SECURITY_MANAGER));
    assertNull(((MuleContextWithRegistries) context).getRegistry().lookupObject(BASE_IN_MEMORY_OBJECT_STORE_KEY));
    assertNull(((MuleContextWithRegistries) context).getRegistry().lookupObject(OBJECT_MULE_SIMPLE_REGISTRY_BOOTSTRAP));
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
    assertEquals(TEST_STRING_VALUE, ((MuleContextWithRegistries) context).getRegistry().lookupObject(TEST_STRING_KEY));

    Object obj = ((MuleContextWithRegistries) context).getRegistry().lookupObject(TEST_OBJECT_NAME);
    assertNotNull(obj);
    assertEquals(Banana.class, obj.getClass());
  }

  private void assertConfigurationBuilder2Objects(MuleContext context) {
    // Test Registry contents for existance of object configured by
    // TestConfigurationBuilder2
    assertEquals(TEST_STRING_VALUE2, ((MuleContextWithRegistries) context).getRegistry().lookupObject(TEST_STRING_KEY2));
  }

  static class TestMuleContextBuilder extends DefaultMuleContextBuilder {

    public TestMuleContextBuilder() {
      super(APP);
    }

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
