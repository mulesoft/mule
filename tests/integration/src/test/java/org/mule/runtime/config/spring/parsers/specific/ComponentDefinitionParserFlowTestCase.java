/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.parsers.specific;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.mule.runtime.config.spring.SpringXmlConfigurationBuilder;
import org.mule.runtime.config.spring.parsers.specific.ComponentDelegatingDefinitionParser.CheckExclusiveClassAttributeObjectFactoryException;
import org.mule.runtime.config.spring.util.SpringBeanLookup;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.component.JavaComponent;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.api.context.MuleContextFactory;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.component.AbstractJavaComponent;
import org.mule.runtime.core.component.DefaultInterfaceBinding;
import org.mule.runtime.core.component.DefaultJavaComponent;
import org.mule.runtime.core.component.PooledJavaComponent;
import org.mule.runtime.core.config.PoolingProfile;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.core.context.DefaultMuleContextFactory;
import org.mule.runtime.core.model.resolvers.ArrayEntryPointResolver;
import org.mule.runtime.core.model.resolvers.CallableEntryPointResolver;
import org.mule.runtime.core.model.resolvers.DefaultEntryPointResolverSet;
import org.mule.runtime.core.model.resolvers.ExplicitMethodEntryPointResolver;
import org.mule.runtime.core.model.resolvers.MethodHeaderPropertyEntryPointResolver;
import org.mule.runtime.core.model.resolvers.NoArgumentsEntryPointResolver;
import org.mule.runtime.core.model.resolvers.ReflectionEntryPointResolver;
import org.mule.runtime.core.object.PrototypeObjectFactory;
import org.mule.runtime.core.object.SingletonObjectFactory;
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
        Flow flow = muleContext.getRegistry().lookupObject("service1");
        validateCorrectServiceCreation(flow);
        assertEquals(DefaultJavaComponent.class, flow.getMessageProcessors().get(0).getClass());
        assertEquals(PrototypeObjectFactory.class, ((AbstractJavaComponent) flow.getMessageProcessors().get(0)).getObjectFactory()
            .getClass());
        assertEquals(1, ((JavaComponent) flow.getMessageProcessors().get(0)).getInterfaceBindings().size());
        assertNotNull(((JavaComponent) flow.getMessageProcessors().get(0)).getEntryPointResolverSet());
        assertEquals(
            2,
            (((DefaultEntryPointResolverSet) ((JavaComponent) flow.getMessageProcessors().get(0)).getEntryPointResolverSet()).getEntryPointResolvers().size()));
        assertTrue((((DefaultEntryPointResolverSet) ((JavaComponent) flow.getMessageProcessors().get(0)).getEntryPointResolverSet()).getEntryPointResolvers()
            .toArray()[0] instanceof ArrayEntryPointResolver));
        assertTrue((((DefaultEntryPointResolverSet) ((JavaComponent) flow.getMessageProcessors().get(0)).getEntryPointResolverSet()).getEntryPointResolvers()
            .toArray()[1] instanceof CallableEntryPointResolver));
    }

    @Test
    public void testDefaultJavaComponentPrototype() throws Exception
    {
        ConfigurationBuilder configBuilder = new SpringXmlConfigurationBuilder(
            "org/mule/config/spring/parsers/specific/component-ok-test-flow.xml");
        muleContext = muleContextFactory.createMuleContext(configBuilder);
        Flow flow = muleContext.getRegistry().lookupObject("service2");
        validateCorrectServiceCreation(flow);
        assertEquals(DefaultJavaComponent.class, flow.getMessageProcessors().get(0).getClass());
        assertEquals(PrototypeObjectFactory.class, ((AbstractJavaComponent) flow.getMessageProcessors().get(0)).getObjectFactory()
            .getClass());
        assertEquals(1, ((JavaComponent) flow.getMessageProcessors().get(0)).getInterfaceBindings().size());
        assertNotNull(((JavaComponent) flow.getMessageProcessors().get(0)).getEntryPointResolverSet());
        assertEquals(
            1,
            (((DefaultEntryPointResolverSet) ((JavaComponent) flow.getMessageProcessors().get(0)).getEntryPointResolverSet()).getEntryPointResolvers().size()));
        assertTrue((((DefaultEntryPointResolverSet) ((JavaComponent) flow.getMessageProcessors().get(0)).getEntryPointResolverSet()).getEntryPointResolvers()
            .toArray()[0] instanceof CallableEntryPointResolver));
    }

    @Test
    public void testDefaultJavaComponentSingleton() throws Exception
    {
        ConfigurationBuilder configBuilder = new SpringXmlConfigurationBuilder(
            "org/mule/config/spring/parsers/specific/component-ok-test-flow.xml");
        muleContext = muleContextFactory.createMuleContext(configBuilder);
        Flow flow = muleContext.getRegistry().lookupObject("service3");
        validateCorrectServiceCreation(flow);
        assertEquals(DefaultJavaComponent.class, flow.getMessageProcessors().get(0).getClass());
        assertEquals(SingletonObjectFactory.class, ((AbstractJavaComponent) flow.getMessageProcessors().get(0)).getObjectFactory()
            .getClass());
        assertEquals(1, ((JavaComponent) flow.getMessageProcessors().get(0)).getInterfaceBindings().size());
        assertNotNull(((JavaComponent) flow.getMessageProcessors().get(0)).getEntryPointResolverSet());
        assertEquals(
            1,
            (((DefaultEntryPointResolverSet) ((JavaComponent) flow.getMessageProcessors().get(0)).getEntryPointResolverSet()).getEntryPointResolvers().size()));
        assertTrue((((DefaultEntryPointResolverSet) ((JavaComponent) flow.getMessageProcessors().get(0)).getEntryPointResolverSet()).getEntryPointResolvers()
            .toArray()[0] instanceof ExplicitMethodEntryPointResolver));

    }

    @Test
    public void testDefaultJavaComponentSpringBean() throws Exception
    {
        ConfigurationBuilder configBuilder = new SpringXmlConfigurationBuilder(
            "org/mule/config/spring/parsers/specific/component-ok-test-flow.xml");
        muleContext = muleContextFactory.createMuleContext(configBuilder);
        Flow flow = muleContext.getRegistry().lookupObject("service4");
        validateCorrectServiceCreation(flow);
        assertEquals(DefaultJavaComponent.class, flow.getMessageProcessors().get(0).getClass());
        assertEquals(SpringBeanLookup.class, ((AbstractJavaComponent) flow.getMessageProcessors().get(0)).getObjectFactory()
            .getClass());
        assertEquals(1, ((JavaComponent) flow.getMessageProcessors().get(0)).getInterfaceBindings().size());
        assertNotNull(((JavaComponent) flow.getMessageProcessors().get(0)).getEntryPointResolverSet());
        assertEquals(
            1,
            (((DefaultEntryPointResolverSet) ((JavaComponent) flow.getMessageProcessors().get(0)).getEntryPointResolverSet()).getEntryPointResolvers().size()));
        assertTrue((((DefaultEntryPointResolverSet) ((JavaComponent) flow.getMessageProcessors().get(0)).getEntryPointResolverSet()).getEntryPointResolvers()
            .toArray()[0] instanceof NoArgumentsEntryPointResolver));
    }

    private void validatePoolingProfile(Flow flow)
    {
        assertNotNull(((PooledJavaComponent) flow.getMessageProcessors().get(0)).getPoolingProfile());
        assertNotNull(((PooledJavaComponent) flow.getMessageProcessors().get(0)).getPoolingProfile());

        PoolingProfile profile = ((PooledJavaComponent) flow.getMessageProcessors().get(0)).getPoolingProfile();
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
        Flow flow = muleContext.getRegistry().lookupObject("service5");
        validateCorrectServiceCreation(flow);
        assertEquals(PooledJavaComponent.class, flow.getMessageProcessors().get(0).getClass());
        assertEquals(PrototypeObjectFactory.class, ((AbstractJavaComponent) flow.getMessageProcessors().get(0)).getObjectFactory()
            .getClass());
        assertEquals(1, ((JavaComponent) flow.getMessageProcessors().get(0)).getInterfaceBindings().size());
        validatePoolingProfile(flow);
        assertNotNull(((JavaComponent) flow.getMessageProcessors().get(0)).getEntryPointResolverSet());
        assertEquals(
            1,
            (((DefaultEntryPointResolverSet) ((JavaComponent) flow.getMessageProcessors().get(0)).getEntryPointResolverSet()).getEntryPointResolvers().size()));
        assertTrue((((DefaultEntryPointResolverSet) ((JavaComponent) flow.getMessageProcessors().get(0)).getEntryPointResolverSet()).getEntryPointResolvers()
            .toArray()[0] instanceof MethodHeaderPropertyEntryPointResolver));

    }

    @Test
    public void testPooledJavaComponentPrototype() throws Exception
    {
        ConfigurationBuilder configBuilder = new SpringXmlConfigurationBuilder(
            "org/mule/config/spring/parsers/specific/component-ok-test-flow.xml");
        muleContext = muleContextFactory.createMuleContext(configBuilder);
        Flow flow = muleContext.getRegistry().lookupObject("service6");
        validateCorrectServiceCreation(flow);
        assertEquals(PooledJavaComponent.class, flow.getMessageProcessors().get(0).getClass());
        assertEquals(PrototypeObjectFactory.class, ((AbstractJavaComponent) flow.getMessageProcessors().get(0)).getObjectFactory()
            .getClass());
        assertEquals(1, ((JavaComponent) flow.getMessageProcessors().get(0)).getInterfaceBindings().size());
        validatePoolingProfile(flow);
        assertNotNull(((JavaComponent) flow.getMessageProcessors().get(0)).getEntryPointResolverSet());
        assertEquals(
            1,
            (((DefaultEntryPointResolverSet) ((JavaComponent) flow.getMessageProcessors().get(0)).getEntryPointResolverSet()).getEntryPointResolvers().size()));
        assertTrue((((DefaultEntryPointResolverSet) ((JavaComponent) flow.getMessageProcessors().get(0)).getEntryPointResolverSet()).getEntryPointResolvers()
            .toArray()[0] instanceof ReflectionEntryPointResolver));

    }

    @Test
    public void testPooledJavaComponentSingleton() throws Exception
    {
        ConfigurationBuilder configBuilder = new SpringXmlConfigurationBuilder(
            "org/mule/config/spring/parsers/specific/component-ok-test-flow.xml");
        muleContext = muleContextFactory.createMuleContext(configBuilder);
        Flow flow = muleContext.getRegistry().lookupObject("service7");
        validateCorrectServiceCreation(flow);
        assertEquals(PooledJavaComponent.class, flow.getMessageProcessors().get(0).getClass());
        assertEquals(SingletonObjectFactory.class, ((AbstractJavaComponent) flow.getMessageProcessors().get(0)).getObjectFactory()
            .getClass());
        assertEquals(1, ((JavaComponent) flow.getMessageProcessors().get(0)).getInterfaceBindings().size());
        validatePoolingProfile(flow);
        assertNotNull(((JavaComponent) flow.getMessageProcessors().get(0)).getEntryPointResolverSet());
        assertEquals(
            1,
            (((DefaultEntryPointResolverSet) ((JavaComponent) flow.getMessageProcessors().get(0)).getEntryPointResolverSet()).getEntryPointResolvers().size()));
        assertTrue((((DefaultEntryPointResolverSet) ((JavaComponent) flow.getMessageProcessors().get(0)).getEntryPointResolverSet()).getEntryPointResolvers()
            .toArray()[0] instanceof ReflectionEntryPointResolver));

    }

    @Test
    public void testPooledJavaComponentSpringBean() throws Exception
    {
        ConfigurationBuilder configBuilder = new SpringXmlConfigurationBuilder(
            "org/mule/config/spring/parsers/specific/component-ok-test-flow.xml");
        muleContext = muleContextFactory.createMuleContext(configBuilder);
        Flow flow = muleContext.getRegistry().lookupObject("service8");
        validateCorrectServiceCreation(flow);
        assertEquals(PooledJavaComponent.class, flow.getMessageProcessors().get(0).getClass());
        assertEquals(SpringBeanLookup.class, ((AbstractJavaComponent) flow.getMessageProcessors().get(0)).getObjectFactory()
            .getClass());
        assertEquals(1, ((JavaComponent) flow.getMessageProcessors().get(0)).getInterfaceBindings().size());
        validatePoolingProfile(flow);
        assertNull(((JavaComponent) flow.getMessageProcessors().get(0)).getEntryPointResolverSet());
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

    protected void validateCorrectServiceCreation(Flow flow) throws Exception
    {
        assertNotNull(flow);
        assertNotNull(flow.getMessageProcessors().get(0));
        assertTrue(flow.getMessageProcessors().get(0) instanceof JavaComponent);
        assertEquals(DummyComponentWithBinding.class, ((JavaComponent) flow.getMessageProcessors().get(0)).getObjectType());
        assertNotNull(((JavaComponent) flow.getMessageProcessors().get(0)).getInterfaceBindings());
        assertEquals(1, ((JavaComponent) flow.getMessageProcessors().get(0)).getInterfaceBindings().size());
        assertTrue(((JavaComponent) flow.getMessageProcessors().get(0)).getInterfaceBindings().get(0) instanceof DefaultInterfaceBinding);
        assertTrue(((JavaComponent) flow.getMessageProcessors().get(0)).getLifecycleAdapterFactory() instanceof TestComponentLifecycleAdapterFactory);
    }

    protected MuleContext createMuleContext() throws Exception
    {
        return null;
    }
}
