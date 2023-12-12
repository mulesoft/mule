/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.context;

import static org.mule.runtime.api.store.ObjectStoreManager.BASE_IN_MEMORY_OBJECT_STORE_KEY;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_CLUSTER_SERVICE;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_MULE_SIMPLE_REGISTRY_BOOTSTRAP;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_QUEUE_MANAGER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_SECURITY_MANAGER;
import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.APP;
import static org.mule.tck.util.MuleContextUtils.mockMuleContext;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;

import static junit.framework.Assert.assertNotNull;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.config.custom.CustomizationService;
import org.mule.runtime.api.config.custom.CustomizationService.ServiceInterceptor;
import org.mule.runtime.api.config.custom.ServiceConfigurator;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.api.config.DefaultMuleConfiguration;
import org.mule.runtime.core.api.config.builders.SimpleConfigurationBuilder;
import org.mule.runtime.core.api.context.notification.MuleContextListener;
import org.mule.runtime.core.internal.config.builders.MinimalConfigurationBuilder;
import org.mule.runtime.core.internal.context.DefaultMuleContext;
import org.mule.runtime.core.internal.context.DefaultMuleContextBuilder;
import org.mule.runtime.core.internal.context.MuleContextWithRegistry;
import org.mule.tck.config.TestServicesConfigurationBuilder;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.testmodels.fruit.Banana;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Consumer;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InOrder;

public class DefaultMuleContextFactoryTestCase extends AbstractMuleTestCase {

  @Rule
  public TestServicesConfigurationBuilder testServicesConfigurationBuilder = new TestServicesConfigurationBuilder();

  private final DefaultMuleContextFactory muleContextFactory = new DefaultMuleContextFactory();
  private static final String TEST_STRING_KEY = "test";
  private static final String TEST_STRING_VALUE = "test_value";
  private static final String TEST_STRING_KEY2 = "test2";
  private static final String TEST_STRING_VALUE2 = "test_value2";
  private static final String TEST_OBJECT_NAME = "testObject";
  private static final String TEST_CUSTOM_SERVICE_KEY = "testCustomServiceImpl";
  private static final String NON_EXISTENT_SERVICE_KEY = "nonExistentService";

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
    context = muleContextFactory.createMuleContext(testServicesConfigurationBuilder, new MinimalConfigurationBuilder());

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

    assertEquals("testValue3", ((MuleContextWithRegistry) context).getRegistry().lookupObject("testKey3"));
    assertEquals("testValue4", ((MuleContextWithRegistry) context).getRegistry().lookupObject("testKey4"));

    assertNoDefaults(context);
  }

  @Test
  public void notifiesMuleContextEvents() throws InitialisationException, ConfigurationException {
    MuleContextListener listener = mock(MuleContextListener.class);
    ConfigurationBuilder configurationBuilder = mock(ConfigurationBuilder.class);
    context = mockMuleContext();
    context = mockMuleContext();
    doAnswer(invocation -> {
      listener.onInitialization(context, mock(Registry.class));
      return null;
    }).when(context).initialise();
    MuleContextBuilder contextBuilder = mock(MuleContextBuilder.class);
    when(contextBuilder.buildMuleContext()).thenReturn(context);

    muleContextFactory.addListener(listener);

    muleContextFactory.createMuleContext(configurationBuilder, contextBuilder);

    InOrder inOrder = inOrder(listener);
    inOrder.verify(listener).onCreation(context);
    inOrder.verify(listener).onInitialization(eq(context), any(Registry.class));
  }

  @Test
  public void testCreateMuleContextWithCustomServices() throws InitialisationException, ConfigurationException {
    MinimalConfigurationBuilder minimalConfigurationBuilder = new MinimalConfigurationBuilder();

    Object testCustomServiceImpl = new Object();
    Object customQueueManagerImpl = new Object();
    Object customSecurityManagerImpl = new Object();
    Map<String, Consumer<ServiceInterceptor>> interceptors = new HashMap<>();
    // replace the default Security Manager
    interceptors.put(OBJECT_SECURITY_MANAGER, serviceInterceptor -> {
      assertThat(serviceInterceptor.getDefaultServiceImpl().isPresent(), is(true));
      serviceInterceptor.newServiceImpl(customSecurityManagerImpl);
    });
    // avoid registering the Cluster service
    interceptors.put(OBJECT_CLUSTER_SERVICE, serviceInterceptor -> {
      assertThat(serviceInterceptor.getDefaultServiceImpl().isPresent(), is(true));
      serviceInterceptor.skip();
    });
    // attempt intercepting a non-existent service
    interceptors.put(NON_EXISTENT_SERVICE_KEY,
                     serviceInterceptor -> assertThat(serviceInterceptor.getDefaultServiceImpl().isPresent(), is(false)));
    // replace the default Queue Manager and register a custom service
    minimalConfigurationBuilder.addServiceConfigurator(new TestServiceConfigurator(
                                                                                   singletonMap(TEST_CUSTOM_SERVICE_KEY,
                                                                                                testCustomServiceImpl),
                                                                                   singletonMap(OBJECT_QUEUE_MANAGER,
                                                                                                customQueueManagerImpl),
                                                                                   interceptors));
    context = muleContextFactory.createMuleContext(testServicesConfigurationBuilder, minimalConfigurationBuilder);

    assertMuleContextConfiguration(context);
    assertThat(((MuleContextWithRegistry) context).getRegistry().lookupObject(TEST_CUSTOM_SERVICE_KEY),
               is(testCustomServiceImpl));
    assertThat(((MuleContextWithRegistry) context).getRegistry().lookupObject(OBJECT_QUEUE_MANAGER), is(customQueueManagerImpl));
    assertThat(((MuleContextWithRegistry) context).getRegistry().lookupObject(OBJECT_SECURITY_MANAGER),
               is(customSecurityManagerImpl));
    assertThat(((MuleContextWithRegistry) context).getRegistry().lookupObject(OBJECT_CLUSTER_SERVICE), is(nullValue()));
    assertThat(((MuleContextWithRegistry) context).getRegistry().lookupObject(NON_EXISTENT_SERVICE_KEY), is(nullValue()));
  }

  private void assertDefaults(MuleContext context) {
    // Assert existance of defaults in registry
    assertNotNull(((MuleContextWithRegistry) context).getRegistry().lookupObject(OBJECT_QUEUE_MANAGER));
    assertNotNull(((MuleContextWithRegistry) context).getRegistry().lookupObject(OBJECT_SECURITY_MANAGER));
    assertNotNull(((MuleContextWithRegistry) context).getRegistry().lookupObject(BASE_IN_MEMORY_OBJECT_STORE_KEY));
  }

  private void assertNoDefaults(MuleContext context) {
    // Assert non-existance of defaults in registry
    assertNull(((MuleContextWithRegistry) context).getRegistry().lookupObject(OBJECT_QUEUE_MANAGER));
    assertNull(((MuleContextWithRegistry) context).getRegistry().lookupObject(OBJECT_SECURITY_MANAGER));
    assertNull(((MuleContextWithRegistry) context).getRegistry().lookupObject(BASE_IN_MEMORY_OBJECT_STORE_KEY));
    assertNull(((MuleContextWithRegistry) context).getRegistry().lookupObject(OBJECT_MULE_SIMPLE_REGISTRY_BOOTSTRAP));
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
    assertEquals(TEST_STRING_VALUE, ((MuleContextWithRegistry) context).getRegistry().lookupObject(TEST_STRING_KEY));

    Object obj = ((MuleContextWithRegistry) context).getRegistry().lookupObject(TEST_OBJECT_NAME);
    assertNotNull(obj);
    assertEquals(Banana.class, obj.getClass());
  }

  private void assertConfigurationBuilder2Objects(MuleContext context) {
    // Test Registry contents for existance of object configured by
    // TestConfigurationBuilder2
    assertEquals(TEST_STRING_VALUE2, ((MuleContextWithRegistry) context).getRegistry().lookupObject(TEST_STRING_KEY2));
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

  private static class TestServiceConfigurator implements ServiceConfigurator {

    private final Map<String, Object> customServices;
    private final Map<String, Object> defaultServices;
    private final Map<String, Consumer<ServiceInterceptor>> interceptedServices;

    public TestServiceConfigurator(Map<String, Object> customServices,
                                   Map<String, Object> defaultServices,
                                   Map<String, Consumer<ServiceInterceptor>> interceptedServices) {
      this.customServices = customServices;
      this.defaultServices = defaultServices;
      this.interceptedServices = interceptedServices;
    }

    @Override
    public void configure(CustomizationService customizationService) {
      customServices.forEach(customizationService::registerCustomServiceImpl);
      defaultServices.forEach(customizationService::overrideDefaultServiceImpl);
      interceptedServices.forEach(customizationService::interceptDefaultServiceImpl);
    }

  }

}
