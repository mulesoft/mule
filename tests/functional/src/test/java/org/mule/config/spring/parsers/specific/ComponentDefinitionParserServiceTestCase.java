/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers.specific;

import org.mule.api.MuleContext;
import org.mule.api.component.JavaComponent;
import org.mule.api.config.ConfigurationBuilder;
import org.mule.api.config.ConfigurationException;
import org.mule.api.context.MuleContextFactory;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.service.Service;
import org.mule.component.AbstractJavaComponent;
import org.mule.component.DefaultInterfaceBinding;
import org.mule.component.DefaultJavaComponent;
import org.mule.component.PooledJavaComponent;
import org.mule.component.simple.PassThroughComponent;
import org.mule.config.PoolingProfile;
import org.mule.config.spring.SpringXmlConfigurationBuilder;
import org.mule.config.spring.parsers.specific.ComponentDelegatingDefinitionParser.CheckExclusiveClassAttributeObjectFactoryException;
import org.mule.config.spring.util.SpringBeanLookup;
import org.mule.context.DefaultMuleContextFactory;
import org.mule.model.resolvers.ArrayEntryPointResolver;
import org.mule.model.resolvers.CallableEntryPointResolver;
import org.mule.model.resolvers.DefaultEntryPointResolverSet;
import org.mule.model.resolvers.ExplicitMethodEntryPointResolver;
import org.mule.model.resolvers.MethodHeaderPropertyEntryPointResolver;
import org.mule.model.resolvers.NoArgumentsEntryPointResolver;
import org.mule.model.resolvers.ReflectionEntryPointResolver;
import org.mule.object.PrototypeObjectFactory;
import org.mule.object.SingletonObjectFactory;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.testmodels.mule.TestComponentLifecycleAdapterFactory;

import org.junit.Test;
import org.springframework.beans.factory.BeanDefinitionStoreException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ComponentDefinitionParserServiceTestCase extends AbstractMuleTestCase
{

    private MuleContextFactory muleContextFactory = new DefaultMuleContextFactory();
    private MuleContext muleContext;

    @Test
    public void testDefaultJavaComponentShortcut() throws Exception
    {
        ConfigurationBuilder configBuilder = new SpringXmlConfigurationBuilder(
            "org/mule/config/spring/parsers/specific/component-ok-test-service.xml");
        muleContext = muleContextFactory.createMuleContext(configBuilder);
        Service service = muleContext.getRegistry().lookupService("service1");
        validateCorrectServiceCreation(service);
        assertEquals(DefaultJavaComponent.class, service.getComponent().getClass());
        assertEquals(PrototypeObjectFactory.class, ((AbstractJavaComponent) service.getComponent()).getObjectFactory()
            .getClass());
        assertEquals(1, ((JavaComponent) service.getComponent()).getInterfaceBindings().size());
        assertNotNull(((JavaComponent) service.getComponent()).getEntryPointResolverSet());
        assertEquals(
            2,
            (((DefaultEntryPointResolverSet) ((JavaComponent) service.getComponent()).getEntryPointResolverSet()).getEntryPointResolvers().size()));
        assertTrue((((DefaultEntryPointResolverSet) ((JavaComponent) service.getComponent()).getEntryPointResolverSet()).getEntryPointResolvers()
            .toArray()[0] instanceof ArrayEntryPointResolver));
        assertTrue((((DefaultEntryPointResolverSet) ((JavaComponent) service.getComponent()).getEntryPointResolverSet()).getEntryPointResolvers()
            .toArray()[1] instanceof CallableEntryPointResolver));
    }

    @Test
    public void testDefaultJavaComponentPrototype() throws Exception
    {
        ConfigurationBuilder configBuilder = new SpringXmlConfigurationBuilder(
            "org/mule/config/spring/parsers/specific/component-ok-test-service.xml");
        muleContext = muleContextFactory.createMuleContext(configBuilder);
        Service service = muleContext.getRegistry().lookupService("service2");
        validateCorrectServiceCreation(service);
        assertEquals(DefaultJavaComponent.class, service.getComponent().getClass());
        assertEquals(PrototypeObjectFactory.class, ((AbstractJavaComponent) service.getComponent()).getObjectFactory()
            .getClass());
        assertEquals(1, ((JavaComponent) service.getComponent()).getInterfaceBindings().size());
        assertNotNull(((JavaComponent) service.getComponent()).getEntryPointResolverSet());
        assertEquals(
            1,
            (((DefaultEntryPointResolverSet) ((JavaComponent) service.getComponent()).getEntryPointResolverSet()).getEntryPointResolvers().size()));
        assertTrue((((DefaultEntryPointResolverSet) ((JavaComponent) service.getComponent()).getEntryPointResolverSet()).getEntryPointResolvers()
            .toArray()[0] instanceof CallableEntryPointResolver));
    }

    @Test
    public void testDefaultJavaComponentSingleton() throws Exception
    {
        ConfigurationBuilder configBuilder = new SpringXmlConfigurationBuilder(
            "org/mule/config/spring/parsers/specific/component-ok-test-service.xml");
        muleContext = muleContextFactory.createMuleContext(configBuilder);
        Service service = muleContext.getRegistry().lookupService("service3");
        validateCorrectServiceCreation(service);
        assertEquals(DefaultJavaComponent.class, service.getComponent().getClass());
        assertEquals(SingletonObjectFactory.class, ((AbstractJavaComponent) service.getComponent()).getObjectFactory()
            .getClass());
        assertEquals(1, ((JavaComponent) service.getComponent()).getInterfaceBindings().size());
        assertNotNull(((JavaComponent) service.getComponent()).getEntryPointResolverSet());
        assertEquals(
            1,
            (((DefaultEntryPointResolverSet) ((JavaComponent) service.getComponent()).getEntryPointResolverSet()).getEntryPointResolvers().size()));
        assertTrue((((DefaultEntryPointResolverSet) ((JavaComponent) service.getComponent()).getEntryPointResolverSet()).getEntryPointResolvers()
            .toArray()[0] instanceof ExplicitMethodEntryPointResolver));

    }

    @Test
    public void testDefaultJavaComponentSpringBean() throws Exception
    {
        ConfigurationBuilder configBuilder = new SpringXmlConfigurationBuilder(
            "org/mule/config/spring/parsers/specific/component-ok-test-service.xml");
        muleContext = muleContextFactory.createMuleContext(configBuilder);
        Service service = muleContext.getRegistry().lookupService("service4");
        validateCorrectServiceCreation(service);
        assertEquals(DefaultJavaComponent.class, service.getComponent().getClass());
        assertEquals(SpringBeanLookup.class, ((AbstractJavaComponent) service.getComponent()).getObjectFactory()
            .getClass());
        assertEquals(1, ((JavaComponent) service.getComponent()).getInterfaceBindings().size());
        assertNotNull(((JavaComponent) service.getComponent()).getEntryPointResolverSet());
        assertEquals(
            1,
            (((DefaultEntryPointResolverSet) ((JavaComponent) service.getComponent()).getEntryPointResolverSet()).getEntryPointResolvers().size()));
        assertTrue((((DefaultEntryPointResolverSet) ((JavaComponent) service.getComponent()).getEntryPointResolverSet()).getEntryPointResolvers()
            .toArray()[0] instanceof NoArgumentsEntryPointResolver));
    }

    private void validatePoolingProfile(Service service)
    {
        assertNotNull(((PooledJavaComponent) service.getComponent()).getPoolingProfile());
        assertNotNull(((PooledJavaComponent) service.getComponent()).getPoolingProfile());

        PoolingProfile profile = ((PooledJavaComponent) service.getComponent()).getPoolingProfile();
        assertNotNull(profile);
        assertEquals("exhausted:", PoolingProfile.WHEN_EXHAUSTED_FAIL, profile.getExhaustedAction());
        assertEquals("initialisation:", PoolingProfile.INITIALISE_ALL, profile.getInitialisationPolicy());
        assertEquals("active:", 1, profile.getMaxActive());
        assertEquals("idle:", 2, profile.getMaxIdle());
        assertEquals("wait:", 3, profile.getMaxWait());
    }

    @Test
    public void testPooledJavaComponentShortcut() throws Exception
    {
        ConfigurationBuilder configBuilder = new SpringXmlConfigurationBuilder(
            "org/mule/config/spring/parsers/specific/component-ok-test-service.xml");
        muleContext = muleContextFactory.createMuleContext(configBuilder);
        Service service = muleContext.getRegistry().lookupService("service5");
        validateCorrectServiceCreation(service);
        assertEquals(PooledJavaComponent.class, service.getComponent().getClass());
        assertEquals(PrototypeObjectFactory.class, ((AbstractJavaComponent) service.getComponent()).getObjectFactory()
            .getClass());
        assertEquals(1, ((JavaComponent) service.getComponent()).getInterfaceBindings().size());
        validatePoolingProfile(service);
        assertNotNull(((JavaComponent) service.getComponent()).getEntryPointResolverSet());
        assertEquals(
            1,
            (((DefaultEntryPointResolverSet) ((JavaComponent) service.getComponent()).getEntryPointResolverSet()).getEntryPointResolvers().size()));
        assertTrue((((DefaultEntryPointResolverSet) ((JavaComponent) service.getComponent()).getEntryPointResolverSet()).getEntryPointResolvers()
            .toArray()[0] instanceof MethodHeaderPropertyEntryPointResolver));

    }

    @Test
    public void testPooledJavaComponentPrototype() throws Exception
    {
        ConfigurationBuilder configBuilder = new SpringXmlConfigurationBuilder(
            "org/mule/config/spring/parsers/specific/component-ok-test-service.xml");
        muleContext = muleContextFactory.createMuleContext(configBuilder);
        Service service = muleContext.getRegistry().lookupService("service6");
        validateCorrectServiceCreation(service);
        assertEquals(PooledJavaComponent.class, service.getComponent().getClass());
        assertEquals(PrototypeObjectFactory.class, ((AbstractJavaComponent) service.getComponent()).getObjectFactory()
            .getClass());
        assertEquals(1, ((JavaComponent) service.getComponent()).getInterfaceBindings().size());
        validatePoolingProfile(service);
        assertNotNull(((JavaComponent) service.getComponent()).getEntryPointResolverSet());
        assertEquals(
            1,
            (((DefaultEntryPointResolverSet) ((JavaComponent) service.getComponent()).getEntryPointResolverSet()).getEntryPointResolvers().size()));
        assertTrue((((DefaultEntryPointResolverSet) ((JavaComponent) service.getComponent()).getEntryPointResolverSet()).getEntryPointResolvers()
            .toArray()[0] instanceof ReflectionEntryPointResolver));

    }

    @Test
    public void testPooledJavaComponentSingleton() throws Exception
    {
        ConfigurationBuilder configBuilder = new SpringXmlConfigurationBuilder(
            "org/mule/config/spring/parsers/specific/component-ok-test-service.xml");
        muleContext = muleContextFactory.createMuleContext(configBuilder);
        Service service = muleContext.getRegistry().lookupService("service7");
        validateCorrectServiceCreation(service);
        assertEquals(PooledJavaComponent.class, service.getComponent().getClass());
        assertEquals(SingletonObjectFactory.class, ((AbstractJavaComponent) service.getComponent()).getObjectFactory()
            .getClass());
        assertEquals(1, ((JavaComponent) service.getComponent()).getInterfaceBindings().size());
        validatePoolingProfile(service);
        assertNotNull(((JavaComponent) service.getComponent()).getEntryPointResolverSet());
        assertEquals(
            1,
            (((DefaultEntryPointResolverSet) ((JavaComponent) service.getComponent()).getEntryPointResolverSet()).getEntryPointResolvers().size()));
        assertTrue((((DefaultEntryPointResolverSet) ((JavaComponent) service.getComponent()).getEntryPointResolverSet()).getEntryPointResolvers()
            .toArray()[0] instanceof ReflectionEntryPointResolver));

    }

    @Test
    public void testPooledJavaComponentSpringBean() throws Exception
    {
        ConfigurationBuilder configBuilder = new SpringXmlConfigurationBuilder(
            "org/mule/config/spring/parsers/specific/component-ok-test-service.xml");
        muleContext = muleContextFactory.createMuleContext(configBuilder);
        Service service = muleContext.getRegistry().lookupService("service8");
        validateCorrectServiceCreation(service);
        assertEquals(PooledJavaComponent.class, service.getComponent().getClass());
        assertEquals(SpringBeanLookup.class, ((AbstractJavaComponent) service.getComponent()).getObjectFactory()
            .getClass());
        assertEquals(1, ((JavaComponent) service.getComponent()).getInterfaceBindings().size());
        validatePoolingProfile(service);
        assertNull(((JavaComponent) service.getComponent()).getEntryPointResolverSet());
    }

    @Test
    public void testClassAttributeAndObjectFactory() throws Exception
    {
        try
        {
            ConfigurationBuilder configBuilder = new SpringXmlConfigurationBuilder(
                "org/mule/config/spring/parsers/specific/component-bad-test-service.xml");
            muleContextFactory.createMuleContext(configBuilder);
            throw new IllegalStateException("Expected config to fail");
        }
        catch (Exception e)
        {
            assertEquals(ConfigurationException.class, e.getClass());
            assertEquals(InitialisationException.class, e.getCause().getClass());
            assertEquals(BeanDefinitionStoreException.class, e.getCause().getCause().getClass());
            assertEquals(CheckExclusiveClassAttributeObjectFactoryException.class, 
                e.getCause().getCause().getCause().getClass());
        }
    }

    protected void validateCorrectServiceCreation(Service service) throws Exception
    {
        assertNotNull(service);
        assertNotNull(service.getComponent());
        assertFalse(service.getComponent() instanceof PassThroughComponent);
        assertTrue(service.getComponent() instanceof JavaComponent);
        assertEquals(DummyComponentWithBinding.class, ((JavaComponent) service.getComponent()).getObjectType());
        assertNotNull(((JavaComponent) service.getComponent()).getInterfaceBindings());
        assertEquals(1, ((JavaComponent) service.getComponent()).getInterfaceBindings().size());
        assertTrue(((JavaComponent) service.getComponent()).getInterfaceBindings().get(0) instanceof DefaultInterfaceBinding);
        assertTrue(((JavaComponent) service.getComponent()).getLifecycleAdapterFactory() instanceof TestComponentLifecycleAdapterFactory);

    }

    protected MuleContext createMuleContext() throws Exception
    {
        return null;
    }
}
