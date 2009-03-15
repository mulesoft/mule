/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule;

import org.mule.api.MuleContext;
import org.mule.api.config.ConfigurationException;
import org.mule.api.config.MuleConfiguration;
import org.mule.api.config.MuleProperties;
import org.mule.api.context.WorkManager;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.LifecycleManager;
import org.mule.api.model.Model;
import org.mule.config.DefaultMuleConfiguration;
import org.mule.config.builders.AbstractConfigurationBuilder;
import org.mule.config.builders.SimpleConfigurationBuilder;
import org.mule.context.DefaultMuleContextBuilder;
import org.mule.context.DefaultMuleContextFactory;
import org.mule.context.notification.ServerNotificationManager;
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

    public void testCreateMuleContext() throws InitialisationException, ConfigurationException
    {
        MuleContext muleContext = muleContextFactory.createMuleContext();

        // Assert MuleContext config
        testMuleContext(muleContext);

        testDefaults(muleContext);

    }

    public void testCreateMuleContextConfigurationBuilder() throws InitialisationException, ConfigurationException
    {
        MuleContext muleContext = muleContextFactory.createMuleContext(new TestConfigurationBuilder());

        // Assert MuleContext config
        testMuleContext(muleContext);

        testConfigurationBuilder1Objects(muleContext);

        testNoDefaults(muleContext);

    }

    public void testCreateMuleContextListMuleContextBuilder() throws InitialisationException, ConfigurationException
    {
        List configBuilders = new ArrayList();
        configBuilders.add(new TestConfigurationBuilder());
        configBuilders.add(new TestConfigurationBuilder2());

        TestMuleContextBuilder muleContextBuilder = new TestMuleContextBuilder();
        MuleContext muleContext = muleContextFactory.createMuleContext(configBuilders, muleContextBuilder);

        // Assert MuleContext config
        testCustomMuleContext(muleContext);

        // Assert configured objects
        testConfigurationBuilder1Objects(muleContext);
        testConfigurationBuilder2Objects(muleContext);

        testNoDefaults(muleContext);

    }

    public void testCreateMuleContextMuleContextBuilder() throws InitialisationException, ConfigurationException
    {
        TestMuleContextBuilder muleContextBuilder = new TestMuleContextBuilder();
        MuleContext muleContext = muleContextFactory.createMuleContext(new SimpleConfigurationBuilder(null), muleContextBuilder);

        // Assert MuleContext config
        testCustomMuleContext(muleContext);

        testNoDefaults(muleContext);
    }

    public void testCreateMuleContextConfigurationBuilderMuleContextBuilder()
        throws InitialisationException, ConfigurationException
    {

        TestMuleContextBuilder muleContextBuilder = new TestMuleContextBuilder();
        MuleContext muleContext = muleContextFactory.createMuleContext(new TestConfigurationBuilder2(),
            muleContextBuilder);

        // Assert MuleContext config
        testCustomMuleContext(muleContext);

        testConfigurationBuilder2Objects(muleContext);

        testNoDefaults(muleContext);
    }

    public void testCreateMuleContextString() throws InitialisationException, ConfigurationException
    {
        MuleContext muleContext = null;
        try
        {
            muleContext = muleContextFactory.createMuleContext("log4j.properties");

        }
        catch (ConfigurationException e)
        {
            assertEquals(
                "No suitable configuration builder for resource \"[ConfigResource{resourceName='log4j.properties'}]\" found.  "
                                + "Check you have configuration module on your classpath and are using correct file extension. "
                                + "(org.mule.api.config.ConfigurationException)", e.getMessage());
        }
        assertNull(muleContext);

    }

    public void testCreateMuleContextStringProperties() throws InitialisationException, ConfigurationException
    {
        Properties properties = new Properties();
        properties.put("testKey1", "testValue1");
        properties.put("testKey2", "testValue2");

        MuleContext muleContext = null;
        try
        {
            muleContext = muleContextFactory.createMuleContext("log4j.properties", properties);

        }
        catch (ConfigurationException e)
        {
            assertEquals(
                "No suitable configuration builder for resource \"[ConfigResource{resourceName='log4j.properties'}]\" found.  "
                                + "Check you have configuration module on your classpath and are using correct file extension. "
                                + "(org.mule.api.config.ConfigurationException)", e.getMessage());
        }

        assertNull(muleContext);
    }

    public void testCreateMuleContextConfigurationBuilderProperties()
        throws InitialisationException, ConfigurationException
    {
        Properties properties = new Properties();
        properties.put("testKey3", "testValue3");
        properties.put("testKey4", "testValue4");

        MuleContext muleContext = muleContextFactory.createMuleContext(new TestConfigurationBuilder(), properties);

        // Assert MuleContext config
        testMuleContext(muleContext);

        testConfigurationBuilder1Objects(muleContext);
        assertEquals("testValue3", muleContext.getRegistry().lookupObject("testKey3"));
        assertEquals("testValue4", muleContext.getRegistry().lookupObject("testKey4"));

        testNoDefaults(muleContext);
    }

    private void testDefaults(MuleContext muleContext)
    {
        // Asert existance of defauts in registry
        assertNotNull(muleContext.getRegistry().lookupObject(MuleProperties.OBJECT_QUEUE_MANAGER));
        assertNotNull(muleContext.getRegistry().lookupObject(MuleProperties.OBJECT_SECURITY_MANAGER));
        assertNotNull(muleContext.getRegistry().lookupObject(MuleProperties.OBJECT_SYSTEM_MODEL));
        assertNotNull(muleContext.getRegistry().lookupObject(MuleProperties.OBJECT_MULE_ENDPOINT_FACTORY));
        assertNotNull(muleContext.getRegistry().lookupObject(MuleProperties.OBJECT_MULE_SIMPLE_REGISTRY_BOOTSTRAP));
    }

    private void testNoDefaults(MuleContext muleContext)
    {
        // Asert non-existance of defauts in registry
        assertNull(muleContext.getRegistry().lookupObject(MuleProperties.OBJECT_QUEUE_MANAGER));
        assertNull(muleContext.getRegistry().lookupObject(MuleProperties.OBJECT_SECURITY_MANAGER));
        assertNull(muleContext.getRegistry().lookupObject(MuleProperties.OBJECT_SYSTEM_MODEL));
        assertNull(muleContext.getRegistry().lookupObject(MuleProperties.OBJECT_MULE_ENDPOINT_FACTORY));
        assertNull(muleContext.getRegistry().lookupObject(MuleProperties.OBJECT_MULE_SIMPLE_REGISTRY_BOOTSTRAP));
    }

    private void testMuleContext(MuleContext muleContext)
    {
        assertNotNull(muleContext);
        assertEquals(DefaultMuleContext.class, muleContext.getClass());
        assertTrue(muleContext.isInitialised());
        assertNotNull(muleContext.getConfiguration());
        assertEquals(DefaultMuleConfiguration.class, muleContext.getConfiguration().getClass());
        assertNotNull(muleContext.getLifecycleManager().getClass());
        assertNotNull(muleContext.getLifecycleManager().getLifecycles().toArray()[0]);
        assertNotNull(muleContext.getLifecycleManager().getLifecycles().toArray()[1]);
        assertNotNull(muleContext.getLifecycleManager().getLifecycles().toArray()[2]);
        assertNotNull(muleContext.getLifecycleManager().getLifecycles().toArray()[3]);
        assertNotNull(muleContext.getNotificationManager());
        assertNotNull(muleContext.getWorkManager());
    }

    private void testCustomMuleContext(MuleContext muleContext)
    {
        assertNotNull(muleContext);
        assertEquals(TestMuleContext.class, muleContext.getClass());
        assertTrue(muleContext.isInitialised());
        assertNotNull(muleContext.getConfiguration());
        //assertEquals(TestMuleConfiguration.class, muleContext.getConfiguration().getClass());
        assertNotNull(muleContext.getLifecycleManager().getClass());
        assertNotNull(muleContext.getLifecycleManager().getLifecycles().toArray()[0]);
        assertNotNull(muleContext.getLifecycleManager().getLifecycles().toArray()[1]);
        assertNotNull(muleContext.getLifecycleManager().getLifecycles().toArray()[2]);
        assertNotNull(muleContext.getLifecycleManager().getLifecycles().toArray()[3]);
        assertNotNull(muleContext.getNotificationManager());
        assertNotNull(muleContext.getWorkManager());
    }

    private void testConfigurationBuilder1Objects(MuleContext muleContext)
    {
        // Test Registry contents for existance of object configured by
        // TestConfigurationBuilder
        assertEquals(TEST_STRING_VALUE, muleContext.getRegistry().lookupObject(TEST_STRING_KEY));
        
        Object obj = muleContext.getRegistry().lookupObject(TEST_OBJECT_NAME);
        assertNotNull(obj);
        assertEquals(Banana.class, obj.getClass());
    }

    private void testConfigurationBuilder2Objects(MuleContext muleContext)
    {
        // Test Registry contents for existance of object configured by
        // TestConfigurationBuilder2
        assertEquals(TEST_STRING_VALUE2, muleContext.getRegistry().lookupObject(TEST_STRING_KEY2));
        assertNotNull(muleContext.getRegistry().lookupModel(TEST_MODEL_NAME));
        assertEquals(TEST_MODEL_NAME, muleContext.getRegistry().lookupModel(TEST_MODEL_NAME).getName());
    }

    /**
     * Override, we don't want a {@link MuleContext} created for this test case.
     */
    protected MuleContext createMuleContext() throws Exception
    {
        return null;
    }

    static class TestConfigurationBuilder extends AbstractConfigurationBuilder
    {
        protected void doConfigure(MuleContext muleContext) throws Exception
        {
            muleContext.getRegistry().registerObject(TEST_STRING_KEY, TEST_STRING_VALUE);
            muleContext.getRegistry().registerObject(TEST_OBJECT_NAME, new Banana());
        }
    }

    static class TestConfigurationBuilder2 extends AbstractConfigurationBuilder
    {
        protected void doConfigure(MuleContext muleContext) throws Exception
        {
            muleContext.getRegistry().registerObject(TEST_STRING_KEY2, TEST_STRING_VALUE2);
            Model testModel = new SedaModel();
            testModel.setName(TEST_MODEL_NAME);
            muleContext.getRegistry().registerModel(testModel);
        }
    }

    static class TestMuleContextBuilder extends DefaultMuleContextBuilder
    {
        public MuleContext buildMuleContext()
        {
            MuleContext muleContext = new TestMuleContext(getMuleConfiguration(),
                                                             getWorkManager(),
                                                             getWorkListener(),
                                                             getLifecycleManager(),
                                                             getNotificationManager());
            return muleContext;
        }
    }

    static class TestMuleContext extends DefaultMuleContext
    {
        public TestMuleContext(MuleConfiguration config,
                                  WorkManager workManager, 
                                  WorkListener workListener, 
                                  LifecycleManager lifecycleManager, 
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
