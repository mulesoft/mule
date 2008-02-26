
package org.mule;

import org.mule.api.MuleContext;
import org.mule.api.config.MuleProperties;
import org.mule.api.service.Service;
import org.mule.config.spring.SpringOsgiXmlConfigurationBuilder;
import org.mule.config.spring.SpringXmlConfigurationBuilder;
import org.mule.context.DefaultMuleContextFactory;
import org.mule.context.OsgiMuleContextBuilder;

import junit.framework.TestCase;

import org.knopflerfish.framework.Framework;
import org.osgi.framework.BundleContext;

/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

public class MuleStartupTestCase extends TestCase
{
    private final String USER_CONFIG = "hello-config.xml";
    private final String USER_SERVICE = "GreeterUMO";

    MuleContext muleContext;
    // OSGi Framework
    Framework framework;
    BundleContext bundleContext;
    
    protected void setUp() throws Exception
    {
        // empty
    }

    // Start up OSGi Framework
    protected void setUpOsgiFramework() throws Exception
    {
        System.getProperties().put("org.osgi.framework.dir", "/tmp");
        framework = new Framework(this);
        framework.launch(0);
        bundleContext = framework.getSystemBundleContext();
        assertNotNull(bundleContext);
    }

    protected void tearDown() throws Exception
    {
        if (muleContext != null)
        {
            muleContext.dispose();
        }
        muleContext = null;
        MuleServer.setMuleContext(null);
        
        if (framework != null)
        {
            framework.shutdown();
            framework = null;
            bundleContext = null;
        }
    }

    protected boolean verifyDefaultConfig()
    {
        boolean verified = false;
        if (muleContext != null)
        {
            if (muleContext.getRegistry().lookupObject(MuleProperties.OBJECT_QUEUE_MANAGER) != null
                && muleContext.getRegistry().lookupObject(MuleProperties.OBJECT_SECURITY_MANAGER) != null
                && muleContext.getRegistry().lookupObject(MuleProperties.OBJECT_MULE_ENDPOINT_FACTORY) != null)
            {
                verified = true;
            }
        }
        return verified;
    }

    protected boolean verifyUserConfig()
    {
        boolean verified = false;
        if (muleContext != null)
        {
            if (muleContext.getRegistry().lookupService(USER_SERVICE) != null)
            {
                verified = true;
            }
        }
        return verified;
    }

    protected boolean verifyStarted()
    {
        boolean verified = false;
        if (muleContext != null)
        {
            Service svc = muleContext.getRegistry().lookupService(USER_SERVICE);
            if (svc != null && svc.isStarted())
            {
                verified = true;
            }
        }
        return verified;
    }

    public void testProgrammaticDefaults() throws Exception
    {
        muleContext = new DefaultMuleContextFactory().createMuleContext();
        muleContext.start();

        assertTrue("Default configuration not found in registry", verifyDefaultConfig());
    }

    public void testSpringXmlImplicitDefaults() throws Exception
    {
        muleContext = new DefaultMuleContextFactory().createMuleContext(new SpringXmlConfigurationBuilder(""));
        muleContext.start();

        assertTrue("Default configuration not found in registry", verifyDefaultConfig());
    }

    public void testSpringXmlImplicitDefaultsWithConfig() throws Exception
    {
        muleContext = new DefaultMuleContextFactory().createMuleContext(new SpringXmlConfigurationBuilder(
            USER_CONFIG));
        muleContext.start();

        assertTrue("Default configuration not found in registry", verifyDefaultConfig());
        assertTrue("User configuration not found in registry", verifyUserConfig());
        assertTrue("Service not started", verifyStarted());
    }

    public void testSpringXmlExplicitDefaultsWithConfig() throws Exception
    {
        SpringXmlConfigurationBuilder builder1 = new SpringXmlConfigurationBuilder(
            SpringXmlConfigurationBuilder.MULE_DEFAULTS_CONFIG + ", " + USER_CONFIG);
        builder1.setUseDefaultConfigResource(false);
        muleContext = new DefaultMuleContextFactory().createMuleContext(builder1);
        muleContext.start();

        assertTrue("Default configuration not found in registry", verifyDefaultConfig());
        assertTrue("User configuration not found in registry", verifyUserConfig());
        assertTrue("Service not started", verifyStarted());
    }

    public void testProgrammaticDefaultsThenSpringXml() throws Exception
    {
        // Start up Mule core with defaults
        muleContext = new DefaultMuleContextFactory().createMuleContext();

        // Start up user config
        SpringXmlConfigurationBuilder builder2 = new SpringXmlConfigurationBuilder(USER_CONFIG);
        builder2.setUseDefaultConfigResource(false);
        builder2.configure(muleContext);
        muleContext.start();

        assertTrue("Default configuration not found in registry", verifyDefaultConfig());
        assertTrue("User configuration not found in registry", verifyUserConfig());
        assertTrue("Service not started", verifyStarted());
    }

    public void testSpringXmlDefaultsThenSpringXml() throws Exception
    {
        // Start up Mule core with defaults
        SpringXmlConfigurationBuilder builder1 = new SpringXmlConfigurationBuilder(
            SpringXmlConfigurationBuilder.MULE_DEFAULTS_CONFIG);
        builder1.setUseDefaultConfigResource(false);
        muleContext = new DefaultMuleContextFactory().createMuleContext(builder1);

        // Start up user config
        SpringXmlConfigurationBuilder builder2 = new SpringXmlConfigurationBuilder(USER_CONFIG);
        builder2.setUseDefaultConfigResource(false);
        builder2.configure(muleContext);
        muleContext.start();

        assertTrue("Default configuration not found in registry", verifyDefaultConfig());
        assertTrue("User configuration not found in registry", verifyUserConfig());
        assertTrue("Service not started", verifyStarted());
    }

//    public void testProgrammaticDefaultsThenStartThenSpringXml() throws Exception
//    {
//        // Start up Mule core with defaults
//        muleContext = new DefaultMuleContextFactory().createMuleContext();
//        muleContext.start();
//
//        // Start up user config
//        SpringXmlConfigurationBuilder builder2 = new SpringXmlConfigurationBuilder(USER_CONFIG);
//        builder2.setUseDefaultConfigResource(false);
//        builder2.configure(muleContext);
//
//        assertTrue("Default configuration not found in registry", verifyDefaultConfig());
//        assertTrue("User configuration not found in registry", verifyUserConfig());
//        assertTrue("Service not started", verifyStarted());
//    }
//
//    public void testSpringXmlDefaultsThenStartThenSpringXml() throws Exception
//    {
//        // Start up Mule core with defaults
//        SpringXmlConfigurationBuilder builder1 = new SpringXmlConfigurationBuilder(
//            SpringXmlConfigurationBuilder.MULE_DEFAULTS_CONFIG);
//        builder1.setUseDefaultConfigResource(false);
//        muleContext = new DefaultMuleContextFactory().createMuleContext(builder1);
//        muleContext.start();
//
//        // Start up user config
//        SpringXmlConfigurationBuilder builder2 = new SpringXmlConfigurationBuilder(USER_CONFIG);
//        builder2.setUseDefaultConfigResource(false);
//        builder2.configure(muleContext);
//
//        assertTrue("Default configuration not found in registry", verifyDefaultConfig());
//        assertTrue("User configuration not found in registry", verifyUserConfig());
//        assertTrue("Service not started", verifyStarted());
//    }

    public void testProgrammaticDefaultsThenSpringOsgiXml() throws Exception
    {
        // Start up OSGi Framework
        setUpOsgiFramework();
        
        // Start up Mule core with defaults
        muleContext = new DefaultMuleContextFactory().createMuleContext(
            new OsgiMuleContextBuilder(bundleContext));

        // Make the MuleContext available via the OSGi ServiceRegistry 
        // (done by the MuleContextActivator normally)
        bundleContext.registerService(new String[]{OsgiMuleContext.class.getName(), MuleContext.class.getName()}, muleContext, null);
        
        // Start up user config
        new SpringOsgiXmlConfigurationBuilder(new String[]{"classpath:" + USER_CONFIG}, bundleContext).configure(muleContext);
        muleContext.start();

        assertTrue("Default configuration not found in registry", verifyDefaultConfig());
        assertTrue("User configuration not found in registry", verifyUserConfig());
        assertTrue("Service not started", verifyStarted());
    }

}
