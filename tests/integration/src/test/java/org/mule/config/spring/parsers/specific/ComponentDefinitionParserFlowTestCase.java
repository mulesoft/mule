/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers.specific;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.mule.api.MuleContext;
import org.mule.api.component.JavaComponent;
import org.mule.api.config.ConfigurationBuilder;
import org.mule.api.config.ConfigurationException;
import org.mule.api.context.MuleContextFactory;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.component.AbstractJavaComponent;
import org.mule.component.DefaultInterfaceBinding;
import org.mule.component.DefaultJavaComponent;
import org.mule.component.PooledJavaComponent;
import org.mule.component.simple.PassThroughComponent;
import org.mule.config.PoolingProfile;
import org.mule.config.spring.SpringXmlConfigurationBuilder;
import org.mule.config.spring.parsers.specific.ComponentDelegatingDefinitionParser.CheckExclusiveClassAttributeObjectFactoryException;
import org.mule.config.spring.util.SpringBeanLookup;
import org.mule.construct.Flow;
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

public class ComponentDefinitionParserFlowTestCase extends AbstractMuleTestCase
{

    private MuleContextFactory muleContextFactory = new DefaultMuleContextFactory();
    private MuleContext muleContext;

    @Test
    public void testDefaultJavaComponentShortcut() throws Exception
    {
        ConfigurationBuilder configBuilder = new SpringXmlConfigurationBuilder(
            "org/mule/config/spring/parsers/specific/component-ok-test-flow.xml");
        muleContext = muleContextFactory.createMuleContext(configBuilder);
        Flow service = muleContext.getRegistry().lookupObject("service1");
        validateCorrectServiceCreation(service);
        assertEquals(DefaultJavaComponent.class, service.getMessageProcessors().get(0).getClass());
        assertEquals(PrototypeObjectFactory.class, ((AbstractJavaComponent) service.getMessageProcessors().get(0)).getObjectFactory()
            .getClass());
        assertEquals(1, ((JavaComponent) service.getMessageProcessors().get(0)).getInterfaceBindings().size());
        assertNotNull(((JavaComponent) service.getMessageProcessors().get(0)).getEntryPointResolverSet());
        assertEquals(
            2,
            (((DefaultEntryPointResolverSet) ((JavaComponent) service.getMessageProcessors().get(0)).getEntryPointResolverSet()).getEntryPointResolvers().size()));
        assertTrue((((DefaultEntryPointResolverSet) ((JavaComponent) service.getMessageProcessors().get(0)).getEntryPointResolverSet()).getEntryPointResolvers()
            .toArray()[0] instanceof ArrayEntryPointResolver));
        assertTrue((((DefaultEntryPointResolverSet) ((JavaComponent) service.getMessageProcessors().get(0)).getEntryPointResolverSet()).getEntryPointResolvers()
            .toArray()[1] instanceof CallableEntryPointResolver));
    }

    @Test
    public void testDefaultJavaComponentPrototype() throws Exception
    {
        ConfigurationBuilder configBuilder = new SpringXmlConfigurationBuilder(
            "org/mule/config/spring/parsers/specific/component-ok-test-flow.xml");
        muleContext = muleContextFactory.createMuleContext(configBuilder);
        Flow service = muleContext.getRegistry().lookupObject("service2");
        validateCorrectServiceCreation(service);
        assertEquals(DefaultJavaComponent.class, service.getMessageProcessors().get(0).getClass());
        assertEquals(PrototypeObjectFactory.class, ((AbstractJavaComponent) service.getMessageProcessors().get(0)).getObjectFactory()
            .getClass());
        assertEquals(1, ((JavaComponent) service.getMessageProcessors().get(0)).getInterfaceBindings().size());
        assertNotNull(((JavaComponent) service.getMessageProcessors().get(0)).getEntryPointResolverSet());
        assertEquals(
            1,
            (((DefaultEntryPointResolverSet) ((JavaComponent) service.getMessageProcessors().get(0)).getEntryPointResolverSet()).getEntryPointResolvers().size()));
        assertTrue((((DefaultEntryPointResolverSet) ((JavaComponent) service.getMessageProcessors().get(0)).getEntryPointResolverSet()).getEntryPointResolvers()
            .toArray()[0] instanceof CallableEntryPointResolver));
    }

    @Test
    public void testDefaultJavaComponentSingleton() throws Exception
    {
        ConfigurationBuilder configBuilder = new SpringXmlConfigurationBuilder(
            "org/mule/config/spring/parsers/specific/component-ok-test-flow.xml");
        muleContext = muleContextFactory.createMuleContext(configBuilder);
        Flow service = muleContext.getRegistry().lookupObject("service3");
        validateCorrectServiceCreation(service);
        assertEquals(DefaultJavaComponent.class, service.getMessageProcessors().get(0).getClass());
        assertEquals(SingletonObjectFactory.class, ((AbstractJavaComponent) service.getMessageProcessors().get(0)).getObjectFactory()
            .getClass());
        assertEquals(1, ((JavaComponent) service.getMessageProcessors().get(0)).getInterfaceBindings().size());
        assertNotNull(((JavaComponent) service.getMessageProcessors().get(0)).getEntryPointResolverSet());
        assertEquals(
            1,
            (((DefaultEntryPointResolverSet) ((JavaComponent) service.getMessageProcessors().get(0)).getEntryPointResolverSet()).getEntryPointResolvers().size()));
        assertTrue((((DefaultEntryPointResolverSet) ((JavaComponent) service.getMessageProcessors().get(0)).getEntryPointResolverSet()).getEntryPointResolvers()
            .toArray()[0] instanceof ExplicitMethodEntryPointResolver));

    }

    @Test
    public void testDefaultJavaComponentSpringBean() throws Exception
    {
        ConfigurationBuilder configBuilder = new SpringXmlConfigurationBuilder(
            "org/mule/config/spring/parsers/specific/component-ok-test-flow.xml");
        muleContext = muleContextFactory.createMuleContext(configBuilder);
        Flow service = muleContext.getRegistry().lookupObject("service4");
        validateCorrectServiceCreation(service);
        assertEquals(DefaultJavaComponent.class, service.getMessageProcessors().get(0).getClass());
        assertEquals(SpringBeanLookup.class, ((AbstractJavaComponent) service.getMessageProcessors().get(0)).getObjectFactory()
            .getClass());
        assertEquals(1, ((JavaComponent) service.getMessageProcessors().get(0)).getInterfaceBindings().size());
        assertNotNull(((JavaComponent) service.getMessageProcessors().get(0)).getEntryPointResolverSet());
        assertEquals(
            1,
            (((DefaultEntryPointResolverSet) ((JavaComponent) service.getMessageProcessors().get(0)).getEntryPointResolverSet()).getEntryPointResolvers().size()));
        assertTrue((((DefaultEntryPointResolverSet) ((JavaComponent) service.getMessageProcessors().get(0)).getEntryPointResolverSet()).getEntryPointResolvers()
            .toArray()[0] instanceof NoArgumentsEntryPointResolver));
    }

    private void validatePoolingProfile(Flow service)
    {
        assertNotNull(((PooledJavaComponent) service.getMessageProcessors().get(0)).getPoolingProfile());
        assertNotNull(((PooledJavaComponent) service.getMessageProcessors().get(0)).getPoolingProfile());

        PoolingProfile profile = ((PooledJavaComponent) service.getMessageProcessors().get(0)).getPoolingProfile();
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
            "org/mule/config/spring/parsers/specific/component-ok-test-flow.xml");
        muleContext = muleContextFactory.createMuleContext(configBuilder);
        Flow service = muleContext.getRegistry().lookupObject("service5");
        validateCorrectServiceCreation(service);
        assertEquals(PooledJavaComponent.class, service.getMessageProcessors().get(0).getClass());
        assertEquals(PrototypeObjectFactory.class, ((AbstractJavaComponent) service.getMessageProcessors().get(0)).getObjectFactory()
            .getClass());
        assertEquals(1, ((JavaComponent) service.getMessageProcessors().get(0)).getInterfaceBindings().size());
        validatePoolingProfile(service);
        assertNotNull(((JavaComponent) service.getMessageProcessors().get(0)).getEntryPointResolverSet());
        assertEquals(
            1,
            (((DefaultEntryPointResolverSet) ((JavaComponent) service.getMessageProcessors().get(0)).getEntryPointResolverSet()).getEntryPointResolvers().size()));
        assertTrue((((DefaultEntryPointResolverSet) ((JavaComponent) service.getMessageProcessors().get(0)).getEntryPointResolverSet()).getEntryPointResolvers()
            .toArray()[0] instanceof MethodHeaderPropertyEntryPointResolver));

    }

    @Test
    public void testPooledJavaComponentPrototype() throws Exception
    {
        ConfigurationBuilder configBuilder = new SpringXmlConfigurationBuilder(
            "org/mule/config/spring/parsers/specific/component-ok-test-flow.xml");
        muleContext = muleContextFactory.createMuleContext(configBuilder);
        Flow service = muleContext.getRegistry().lookupObject("service6");
        validateCorrectServiceCreation(service);
        assertEquals(PooledJavaComponent.class, service.getMessageProcessors().get(0).getClass());
        assertEquals(PrototypeObjectFactory.class, ((AbstractJavaComponent) service.getMessageProcessors().get(0)).getObjectFactory()
            .getClass());
        assertEquals(1, ((JavaComponent) service.getMessageProcessors().get(0)).getInterfaceBindings().size());
        validatePoolingProfile(service);
        assertNotNull(((JavaComponent) service.getMessageProcessors().get(0)).getEntryPointResolverSet());
        assertEquals(
            1,
            (((DefaultEntryPointResolverSet) ((JavaComponent) service.getMessageProcessors().get(0)).getEntryPointResolverSet()).getEntryPointResolvers().size()));
        assertTrue((((DefaultEntryPointResolverSet) ((JavaComponent) service.getMessageProcessors().get(0)).getEntryPointResolverSet()).getEntryPointResolvers()
            .toArray()[0] instanceof ReflectionEntryPointResolver));

    }

    @Test
    public void testPooledJavaComponentSingleton() throws Exception
    {
        ConfigurationBuilder configBuilder = new SpringXmlConfigurationBuilder(
            "org/mule/config/spring/parsers/specific/component-ok-test-flow.xml");
        muleContext = muleContextFactory.createMuleContext(configBuilder);
        Flow service = muleContext.getRegistry().lookupObject("service7");
        validateCorrectServiceCreation(service);
        assertEquals(PooledJavaComponent.class, service.getMessageProcessors().get(0).getClass());
        assertEquals(SingletonObjectFactory.class, ((AbstractJavaComponent) service.getMessageProcessors().get(0)).getObjectFactory()
            .getClass());
        assertEquals(1, ((JavaComponent) service.getMessageProcessors().get(0)).getInterfaceBindings().size());
        validatePoolingProfile(service);
        assertNotNull(((JavaComponent) service.getMessageProcessors().get(0)).getEntryPointResolverSet());
        assertEquals(
            1,
            (((DefaultEntryPointResolverSet) ((JavaComponent) service.getMessageProcessors().get(0)).getEntryPointResolverSet()).getEntryPointResolvers().size()));
        assertTrue((((DefaultEntryPointResolverSet) ((JavaComponent) service.getMessageProcessors().get(0)).getEntryPointResolverSet()).getEntryPointResolvers()
            .toArray()[0] instanceof ReflectionEntryPointResolver));

    }

    @Test
    public void testPooledJavaComponentSpringBean() throws Exception
    {
        ConfigurationBuilder configBuilder = new SpringXmlConfigurationBuilder(
            "org/mule/config/spring/parsers/specific/component-ok-test-flow.xml");
        muleContext = muleContextFactory.createMuleContext(configBuilder);
        Flow service = muleContext.getRegistry().lookupObject("service8");
        validateCorrectServiceCreation(service);
        assertEquals(PooledJavaComponent.class, service.getMessageProcessors().get(0).getClass());
        assertEquals(SpringBeanLookup.class, ((AbstractJavaComponent) service.getMessageProcessors().get(0)).getObjectFactory()
            .getClass());
        assertEquals(1, ((JavaComponent) service.getMessageProcessors().get(0)).getInterfaceBindings().size());
        validatePoolingProfile(service);
        assertNull(((JavaComponent) service.getMessageProcessors().get(0)).getEntryPointResolverSet());
    }

    @Test
    public void testClassAttributeAndObjectFactory() throws Exception
    {
        try
        {
            ConfigurationBuilder configBuilder = new SpringXmlConfigurationBuilder(
                "org/mule/config/spring/parsers/specific/component-bad-test-flow.xml");
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

    protected void validateCorrectServiceCreation(Flow service) throws Exception
    {
        assertNotNull(service);
        assertNotNull(service.getMessageProcessors().get(0));
        assertFalse(service.getMessageProcessors().get(0) instanceof PassThroughComponent);
        assertTrue(service.getMessageProcessors().get(0) instanceof JavaComponent);
        assertEquals(DummyComponentWithBinding.class, ((JavaComponent) service.getMessageProcessors().get(0)).getObjectType());
        assertNotNull(((JavaComponent) service.getMessageProcessors().get(0)).getInterfaceBindings());
        assertEquals(1, ((JavaComponent) service.getMessageProcessors().get(0)).getInterfaceBindings().size());
        assertTrue(((JavaComponent) service.getMessageProcessors().get(0)).getInterfaceBindings().get(0) instanceof DefaultInterfaceBinding);
        assertTrue(((JavaComponent) service.getMessageProcessors().get(0)).getLifecycleAdapterFactory() instanceof TestComponentLifecycleAdapterFactory);

    }

    protected MuleContext createMuleContext() throws Exception
    {
        return null;
    }
}
