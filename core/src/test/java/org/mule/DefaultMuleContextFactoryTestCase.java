/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule;

import org.mule.api.MuleContext;
import org.mule.api.config.ConfigurationBuilder;
import org.mule.api.config.ConfigurationException;
import org.mule.api.config.MuleConfiguration;
import org.mule.api.config.MuleProperties;
import org.mule.api.context.WorkManager;
import org.mule.api.model.Model;
import org.mule.config.DefaultMuleConfiguration;
import org.mule.config.builders.AbstractConfigurationBuilder;
import org.mule.config.builders.SimpleConfigurationBuilder;
import org.mule.context.DefaultMuleContextBuilder;
import org.mule.context.DefaultMuleContextFactory;
import org.mule.context.notification.ServerNotificationManager;
import org.mule.lifecycle.MuleContextLifecycleManager;
import org.mule.model.seda.SedaModel;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.testmodels.fruit.Banana;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.resource.spi.work.WorkListener;

public class DefaultMuleContextFactoryTestCase extends AbstractMuleTestCase
{
    private DefaultMuleContextFactory muleContextFactory = new DefaultMuleContextFactory();
    private static String TEST_STRING_KEY = "test";
    private static String TEST_STRING_VALUE = "test_value";
    private static String TEST_STRING_KEY2 = "test2";
    private static String TEST_STRING_VALUE2 = "test_value2";
    private static String TEST_OBJECT_NAME = "testObject";
    private static String TEST_MODEL_NAME = "testModel";

    /**
     * Override, we don't want a {@link MuleContext} created for this test case.
     */
    @Override
    protected MuleContext createMuleContext() throws Exception
    {
        return null;
    }

    public void testCreateMuleContext() throws Exception
    {
        MuleContext context = muleContextFactory.createMuleContext();

        assertMuleContextConfiguration(context);
        assertDefaults(context);
    }

    public void testCreateMuleContextConfigurationBuilder() throws Exception
    {
        MuleContext context = muleContextFactory.createMuleContext(new TestConfigurationBuilder());

        assertMuleContextConfiguration(context);
        assertConfigurationBuilder1Objects(context);
        assertNoDefaults(context);
    }

    public void testCreateMuleContextListMuleContextBuilder() throws Exception
    {
        List<ConfigurationBuilder> configBuilders = new ArrayList<ConfigurationBuilder>();
        configBuilders.add(new TestConfigurationBuilder());
        configBuilders.add(new TestConfigurationBuilder2());

        TestMuleContextBuilder muleContextBuilder = new TestMuleContextBuilder();
        MuleContext context = muleContextFactory.createMuleContext(configBuilders, muleContextBuilder);

        assertCustomMuleContext(context);
        assertConfigurationBuilder1Objects(context);
        assertConfigurationBuilder2Objects(context);
        assertNoDefaults(context);
    }

    public void testCreateMuleContextMuleContextBuilder() throws Exception
    {
        TestMuleContextBuilder muleContextBuilder = new TestMuleContextBuilder();
        MuleContext context = muleContextFactory.createMuleContext(new SimpleConfigurationBuilder(null), muleContextBuilder);

        assertCustomMuleContext(context);
        assertNoDefaults(context);
    }

    public void testCreateMuleContextConfigurationBuilderMuleContextBuilder() throws Exception
    {
        TestMuleContextBuilder muleContextBuilder = new TestMuleContextBuilder();
        MuleContext context = muleContextFactory.createMuleContext(new TestConfigurationBuilder2(),
            muleContextBuilder);

        assertCustomMuleContext(context);
        assertConfigurationBuilder2Objects(context);
        assertNoDefaults(context);
    }

    public void testCreateMuleContextString() throws Exception
    {
        MuleContext context = null;
        try
        {
            context = muleContextFactory.createMuleContext("log4j.properties");
        }
        catch (ConfigurationException e)
        {
            assertEquals(
                "No suitable configuration builder for resource \"[ConfigResource{resourceName='log4j.properties'}]\" found.  "
                                + "Check you have configuration module on your classpath and are using correct file extension. "
                                + "(org.mule.api.config.ConfigurationException)", e.getMessage());
        }
        assertNull(context);
    }

    public void testCreateMuleContextStringProperties() throws Exception
    {
        Properties properties = new Properties();
        properties.put("testKey1", "testValue1");
        properties.put("testKey2", "testValue2");

        MuleContext context = null;
        try
        {
            context = muleContextFactory.createMuleContext("log4j.properties", properties);
        }
        catch (ConfigurationException e)
        {
            assertEquals(
                "No suitable configuration builder for resource \"[ConfigResource{resourceName='log4j.properties'}]\" found.  "
                                + "Check you have configuration module on your classpath and are using correct file extension. "
                                + "(org.mule.api.config.ConfigurationException)", e.getMessage());
        }

        assertNull(context);
    }

    public void testCreateMuleContextConfigurationBuilderProperties() throws Exception
    {
        Properties properties = new Properties();
        properties.put("testKey3", "testValue3");
        properties.put("testKey4", "testValue4");

        MuleContext context = muleContextFactory.createMuleContext(new TestConfigurationBuilder(), properties);

        assertMuleContextConfiguration(context);
        assertConfigurationBuilder1Objects(context);

        assertEquals("testValue3", context.getRegistry().lookupObject("testKey3"));
        assertEquals("testValue4", context.getRegistry().lookupObject("testKey4"));

        assertNoDefaults(context);
    }

    private void assertDefaults(MuleContext context)
    {
        // Asert existance of defauts in registry
        assertNotNull(context.getRegistry().lookupObject(MuleProperties.OBJECT_QUEUE_MANAGER));
        assertNotNull(context.getRegistry().lookupObject(MuleProperties.OBJECT_SECURITY_MANAGER));
        assertNotNull(context.getRegistry().lookupObject(MuleProperties.OBJECT_STORE));
        assertNotNull(context.getRegistry().lookupObject(MuleProperties.OBJECT_SYSTEM_MODEL));
        assertNotNull(context.getRegistry().lookupObject(MuleProperties.OBJECT_MULE_ENDPOINT_FACTORY));
        assertNotNull(context.getRegistry().lookupObject(MuleProperties.OBJECT_MULE_SIMPLE_REGISTRY_BOOTSTRAP));
    }

    private void assertNoDefaults(MuleContext context)
    {
        // Asert non-existance of defauts in registry
        assertNull(context.getRegistry().lookupObject(MuleProperties.OBJECT_QUEUE_MANAGER));
        assertNull(context.getRegistry().lookupObject(MuleProperties.OBJECT_SECURITY_MANAGER));
        assertNull(context.getRegistry().lookupObject(MuleProperties.OBJECT_STORE));
        assertNull(context.getRegistry().lookupObject(MuleProperties.OBJECT_SYSTEM_MODEL));
        assertNull(context.getRegistry().lookupObject(MuleProperties.OBJECT_MULE_ENDPOINT_FACTORY));
        assertNull(context.getRegistry().lookupObject(MuleProperties.OBJECT_MULE_SIMPLE_REGISTRY_BOOTSTRAP));
    }

    private void assertMuleContextConfiguration(MuleContext context)
    {
        assertNotNull(context);
        assertEquals(DefaultMuleContext.class, context.getClass());
        assertTrue(context.isInitialised());
        assertNotNull(context.getConfiguration());
        assertEquals(DefaultMuleConfiguration.class, context.getConfiguration().getClass());
        assertNotNull(context.getLifecycleManager().getClass());
        assertNotNull(context.getNotificationManager());
        assertNotNull(context.getWorkManager());
    }

    private void assertCustomMuleContext(MuleContext context)
    {
        assertNotNull(context);
        assertEquals(TestMuleContext.class, context.getClass());
        assertTrue(context.isInitialised());
        assertNotNull(context.getConfiguration());
        assertNotNull(context.getLifecycleManager().getClass());
        assertNotNull(context.getNotificationManager());
        assertNotNull(context.getWorkManager());
    }

    private void assertConfigurationBuilder1Objects(MuleContext context)
    {
        // Test Registry contents for existance of object configured by
        // TestConfigurationBuilder
        assertEquals(TEST_STRING_VALUE, context.getRegistry().lookupObject(TEST_STRING_KEY));

        Object obj = context.getRegistry().lookupObject(TEST_OBJECT_NAME);
        assertNotNull(obj);
        assertEquals(Banana.class, obj.getClass());
    }

    private void assertConfigurationBuilder2Objects(MuleContext context)
    {
        // Test Registry contents for existance of object configured by
        // TestConfigurationBuilder2
        assertEquals(TEST_STRING_VALUE2, context.getRegistry().lookupObject(TEST_STRING_KEY2));
        assertNotNull(context.getRegistry().lookupModel(TEST_MODEL_NAME));
        assertEquals(TEST_MODEL_NAME, context.getRegistry().lookupModel(TEST_MODEL_NAME).getName());
    }

    static class TestConfigurationBuilder extends AbstractConfigurationBuilder
    {
        @Override
        protected void doConfigure(MuleContext context) throws Exception
        {
            context.getRegistry().registerObject(TEST_STRING_KEY, TEST_STRING_VALUE);
            context.getRegistry().registerObject(TEST_OBJECT_NAME, new Banana());
        }
    }

    static class TestConfigurationBuilder2 extends AbstractConfigurationBuilder
    {
        @Override
        protected void doConfigure(MuleContext context) throws Exception
        {
            context.getRegistry().registerObject(TEST_STRING_KEY2, TEST_STRING_VALUE2);
            Model testModel = new SedaModel();
            testModel.setName(TEST_MODEL_NAME);
            context.getRegistry().registerModel(testModel);
        }
    }

    static class TestMuleContextBuilder extends DefaultMuleContextBuilder
    {
        @Override
        public MuleContext buildMuleContext()
        {
            MuleContextLifecycleManager manager = getLifecycleManager();

            MuleContext context = new TestMuleContext(getMuleConfiguration(), getWorkManager(),
                getWorkListener(), manager, getNotificationManager());
            manager.setMuleContext(context);
            return context;
        }
    }

    static class TestMuleContext extends DefaultMuleContext
    {
        public TestMuleContext(MuleConfiguration config, WorkManager workManager,
            WorkListener workListener, MuleContextLifecycleManager lifecycleManager,
            ServerNotificationManager notificationManager)
        {
            super(config, workManager, workListener, lifecycleManager, notificationManager);
        }
    }

    static class TestMuleConfiguration extends DefaultMuleConfiguration
    {
        // just a skeleton
    }
}
