/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.config;

import org.mule.MuleServer;
import org.mule.api.MuleContext;
import org.mule.api.ThreadSafeAccess;
import org.mule.api.config.MuleConfiguration;
import org.mule.api.context.MuleContextBuilder;
import org.mule.config.DefaultMuleConfiguration;
import org.mule.context.DefaultMuleContextBuilder;
import org.mule.context.DefaultMuleContextFactory;

import junit.framework.TestCase;

public class MuleConfigurationTestCase extends TestCase
{
    
    private boolean failOnMessageScribbling;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        
        // fiddling with ThreadSafeAccess must not have side effects on later tests. Store
        // the current state here and restore it in tearDown
        failOnMessageScribbling = ThreadSafeAccess.AccessControl.isFailOnMessageScribbling();
    }

    @Override
    protected void tearDown() throws Exception
    {
        muleContext = null;
        MuleServer.setMuleContext(null);
        ThreadSafeAccess.AccessControl.setFailOnMessageScribbling(failOnMessageScribbling);
    } 
    
    private MuleContext muleContext;
    
    /** Test for MULE-3092 */
    public void testConfigureProgramatically() throws Exception
    {
        DefaultMuleConfiguration config = new DefaultMuleConfiguration();
        config.setDefaultEncoding("UTF-16");
        config.setDefaultSynchronousEndpoints(true);
        config.setSystemModelType("direct");
        config.setDefaultResponseTimeout(30000);
        config.setDefaultTransactionTimeout(60000);
        config.setWorkingDirectory("/some/directory");
        config.setClientMode(true);
        ThreadSafeAccess.AccessControl.setFailOnMessageScribbling(false);
        config.setId("MY_SERVER");
        config.setClusterId("MY_CLUSTER");
        config.setDomainId("MY_DOMAIN");
        config.setCacheMessageAsBytes(false);
        config.setCacheMessageOriginalPayload(false);
        config.setEnableStreaming(false);
        ThreadSafeAccess.AccessControl.setAssertMessageAccess(false);
        config.setAutoWrapMessageAwareTransform(false);
        
        MuleContextBuilder contextBuilder = new DefaultMuleContextBuilder();
        contextBuilder.setMuleConfiguration(config);
        muleContext = new DefaultMuleContextFactory().createMuleContext(contextBuilder);
        
        muleContext.start();
        
        verifyConfiguration();
    }

    /** Test for MULE-3092 */
    public void testConfigureWithSystemProperties() throws Exception
    {
        System.setProperty(MuleConfiguration.SYSTEM_PROPERTY_PREFIX + "encoding", "UTF-16");
        System.setProperty(MuleConfiguration.SYSTEM_PROPERTY_PREFIX + "endpoints.synchronous", "true");
        System.setProperty(MuleConfiguration.SYSTEM_PROPERTY_PREFIX + "systemModelType", "direct");
        System.setProperty(MuleConfiguration.SYSTEM_PROPERTY_PREFIX + "timeout.synchronous", "30000");
        System.setProperty(MuleConfiguration.SYSTEM_PROPERTY_PREFIX + "timeout.transaction", "60000");
        System.setProperty(MuleConfiguration.SYSTEM_PROPERTY_PREFIX + "remoteSync", "true");
        System.setProperty(MuleConfiguration.SYSTEM_PROPERTY_PREFIX + "workingDirectory", "/some/directory");
        System.setProperty(MuleConfiguration.SYSTEM_PROPERTY_PREFIX + "clientMode", "true");
        
        // this is just to make the test work for now. Since the initialization of the threadsafe
        // check behaviour in ThreadSafeAccess.AccessControl has already happened at this point in
        // time (we touched ThreadSafeAccess.AccessControl in setUp) setting the system property 
        // won't have any effect here
        // System.setProperty(MuleConfiguration.SYSTEM_PROPERTY_PREFIX + "disable.threadsafemessages", "true");
        ThreadSafeAccess.AccessControl.setFailOnMessageScribbling(false);
        
        System.setProperty(MuleConfiguration.SYSTEM_PROPERTY_PREFIX + "serverId", "MY_SERVER");
        System.setProperty(MuleConfiguration.SYSTEM_PROPERTY_PREFIX + "clusterId", "MY_CLUSTER");
        System.setProperty(MuleConfiguration.SYSTEM_PROPERTY_PREFIX + "domainId", "MY_DOMAIN");
        System.setProperty(MuleConfiguration.SYSTEM_PROPERTY_PREFIX + "message.cacheBytes", "false");
        System.setProperty(MuleConfiguration.SYSTEM_PROPERTY_PREFIX + "message.cacheOriginal", "false");
        System.setProperty(MuleConfiguration.SYSTEM_PROPERTY_PREFIX + "streaming.enable", "false");
        System.setProperty(MuleConfiguration.SYSTEM_PROPERTY_PREFIX + "message.assertAccess", "false");
        System.setProperty(MuleConfiguration.SYSTEM_PROPERTY_PREFIX + "transform.autoWrap", "false");
        
        muleContext = new DefaultMuleContextFactory().createMuleContext();
        muleContext.start();

        verifyConfiguration();

        System.clearProperty(MuleConfiguration.SYSTEM_PROPERTY_PREFIX + "encoding");
        System.clearProperty(MuleConfiguration.SYSTEM_PROPERTY_PREFIX + "endpoints.synchronous");
        System.clearProperty(MuleConfiguration.SYSTEM_PROPERTY_PREFIX + "systemModelType");
        System.clearProperty(MuleConfiguration.SYSTEM_PROPERTY_PREFIX + "timeout.synchronous");
        System.clearProperty(MuleConfiguration.SYSTEM_PROPERTY_PREFIX + "timeout.transaction");
        System.clearProperty(MuleConfiguration.SYSTEM_PROPERTY_PREFIX + "remoteSync");
        System.clearProperty(MuleConfiguration.SYSTEM_PROPERTY_PREFIX + "workingDirectory");
        System.clearProperty(MuleConfiguration.SYSTEM_PROPERTY_PREFIX + "clientMode");
        System.clearProperty(MuleConfiguration.SYSTEM_PROPERTY_PREFIX + "disable.threadsafemessages");
        System.clearProperty(MuleConfiguration.SYSTEM_PROPERTY_PREFIX + "serverId");
        System.clearProperty(MuleConfiguration.SYSTEM_PROPERTY_PREFIX + "clusterId");
        System.clearProperty(MuleConfiguration.SYSTEM_PROPERTY_PREFIX + "domainId");
        System.clearProperty(MuleConfiguration.SYSTEM_PROPERTY_PREFIX + "message.cacheBytes");
        System.clearProperty(MuleConfiguration.SYSTEM_PROPERTY_PREFIX + "message.cacheOriginal");
        System.clearProperty(MuleConfiguration.SYSTEM_PROPERTY_PREFIX + "streaming.enable");
        System.clearProperty(MuleConfiguration.SYSTEM_PROPERTY_PREFIX + "message.assertAccess");
        System.clearProperty(MuleConfiguration.SYSTEM_PROPERTY_PREFIX + "transform.autoWrap");
    }

    /** Test for MULE-3110 */
    public void testConfigureAfterInitFails() throws Exception
    {
        muleContext = new DefaultMuleContextFactory().createMuleContext();        

        DefaultMuleConfiguration mutableConfig = ((DefaultMuleConfiguration) muleContext.getConfiguration());
        
        // These are OK to change after init but before start
        mutableConfig.setDefaultSynchronousEndpoints(true);
        mutableConfig.setSystemModelType("direct");
        mutableConfig.setDefaultResponseTimeout(30000);
        mutableConfig.setDefaultTransactionTimeout(60000);
        mutableConfig.setClientMode(true);

        // These are not OK to change after init
        mutableConfig.setDefaultEncoding("UTF-16");
        mutableConfig.setWorkingDirectory("/some/directory");
        mutableConfig.setId("MY_SERVER");
        mutableConfig.setClusterId("MY_CLUSTER");
        mutableConfig.setDomainId("MY_DOMAIN");

        MuleConfiguration config = muleContext.getConfiguration();

        // These are OK to change after init but before start
        assertTrue(config.isDefaultSynchronousEndpoints());
        assertEquals("direct", config.getSystemModelType());
        assertEquals(30000, config.getDefaultResponseTimeout());
        assertEquals(60000, config.getDefaultTransactionTimeout());
        assertTrue(config.isClientMode());
        
        // These are not OK to change after init
        assertFalse("UTF-16".equals(config.getDefaultEncoding()));
        assertFalse("/some/directory".equals(config.getWorkingDirectory()));
        assertFalse("MY_SERVER".equals(config.getId()));
        assertFalse("MY_CLUSTER".equals(config.getClusterId()));
        assertFalse("MY_DOMAIN".equals(config.getDomainId()));
    }

    /** Test for MULE-3110 */
    public void testConfigureAfterStartFails() throws Exception
    {
        muleContext = new DefaultMuleContextFactory().createMuleContext();        
        muleContext.start();

        DefaultMuleConfiguration mutableConfig = ((DefaultMuleConfiguration) muleContext.getConfiguration());
        mutableConfig.setDefaultSynchronousEndpoints(true);
        mutableConfig.setSystemModelType("direct");
        mutableConfig.setDefaultResponseTimeout(30000);
        mutableConfig.setDefaultTransactionTimeout(60000);
        mutableConfig.setClientMode(true);

        MuleConfiguration config = muleContext.getConfiguration();
        assertFalse(config.isDefaultSynchronousEndpoints());
        assertFalse("direct".equals(config.getSystemModelType()));
        assertFalse(30000 == config.getDefaultResponseTimeout());
        assertFalse(60000 == config.getDefaultTransactionTimeout());
        assertFalse(config.isClientMode());
    }

    protected void verifyConfiguration()
    {
        MuleConfiguration config = muleContext.getConfiguration();
        assertEquals("UTF-16", config.getDefaultEncoding());
        assertTrue(config.isDefaultSynchronousEndpoints());
        assertEquals("direct", config.getSystemModelType());
        assertEquals(30000, config.getDefaultResponseTimeout());
        assertEquals(60000, config.getDefaultTransactionTimeout());
        // on windows this ends up with a c:/ in it
        assertTrue(config.getWorkingDirectory().indexOf("/some/directory") != -1);
        assertTrue(config.isClientMode());
        assertFalse(ThreadSafeAccess.AccessControl.isFailOnMessageScribbling());
        assertEquals("MY_SERVER", config.getId());
        assertEquals("MY_CLUSTER", config.getClusterId());
        assertEquals("MY_DOMAIN", config.getDomainId());
        assertFalse(config.isCacheMessageAsBytes());
        assertFalse(config.isCacheMessageOriginalPayload());
        assertFalse(config.isEnableStreaming());
        assertFalse(ThreadSafeAccess.AccessControl.isAssertMessageAccess());
        assertFalse(config.isAutoWrapMessageAwareTransform());
    }
}


