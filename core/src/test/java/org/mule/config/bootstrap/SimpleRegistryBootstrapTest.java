/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.bootstrap;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.registry.ResolverException;
import org.mule.api.registry.TransformerResolver;
import org.mule.api.transaction.TransactionFactory;
import org.mule.api.transformer.DataType;
import org.mule.api.transformer.Transformer;
import org.mule.api.transformer.TransformerException;
import org.mule.config.builders.DefaultsConfigurationBuilder;
import org.mule.context.DefaultMuleContextFactory;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.transformer.AbstractDiscoverableTransformer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.junit.Test;


public class SimpleRegistryBootstrapTest extends AbstractMuleContextTestCase
{

    public static final String TEST_TRANSACTION_FACTORY_CLASS = "javax.jms.Connection";

    public static final String CUSTOM_TRANSFORMER_KEY = "custom1";

    public void initRegistryBootstrap() throws InitialisationException
    {
        SimpleRegistryBootstrap simpleRegistryBootstrap = new SimpleRegistryBootstrap();
        simpleRegistryBootstrap.setMuleContext(muleContext);
        simpleRegistryBootstrap.initialise();
    }

    @Test(expected = ClassNotFoundException.class)
    public void testRegisteringOptionalTransaction() throws ClassNotFoundException, InitialisationException
    {
        initRegistryBootstrap();
        muleContext.getTransactionFactoryManager().getTransactionFactoryFor(Class.forName(TEST_TRANSACTION_FACTORY_CLASS));
    }

    @Test
    public void testExistingNotOptionalTransaction() throws Exception
    {
        initRegistryBootstrap();
        TransactionFactory transactionFactoryFor = muleContext.getTransactionFactoryManager().getTransactionFactoryFor(FakeTransactionResource.class);
        assertNotNull(transactionFactoryFor);

    }

    @Test
    public void testTransformersWithCustomKeyRegistration() throws MuleException
    {
        disposeContext();
        muleContext = new DefaultMuleContextFactory().createMuleContext(new DummyConfigurationBuilder());

        Properties properties = new Properties();
        properties.setProperty("core.transformer.1", ExpectedKeyTransformer.class.getName());
        properties.setProperty(CUSTOM_TRANSFORMER_KEY, CustomKeyTransformer.class.getName());

        SimpleRegistryBootstrap customRegistryBootstrap = new SimpleRegistryBootstrap(new TestRegistryBootstrapDiscoverer(properties));
        customRegistryBootstrap.setMuleContext(muleContext);

        TestTransformerResolver transformerResolver = new TestTransformerResolver();
        muleContext.getRegistry().registerObject("testTransformerResolver", transformerResolver);

        customRegistryBootstrap.initialise();

        List<? extends Transformer> transformers = transformerResolver.getTransformers();

        assertEquals(2, transformers.size());

        assertTrue(contains(transformers, ExpectedKeyTransformer.class));

        assertTrue(contains(transformers, CustomKeyTransformer.class));
    }

    private boolean contains(List<? extends Transformer> transformers, Class<? extends Transformer> transformerClass)
    {
        for (Transformer transformer : transformers)
        {
            if (transformerClass.isAssignableFrom(transformer.getClass()))
            {
                return true;
            }
        }
        return false;
    }

    private class DummyConfigurationBuilder extends DefaultsConfigurationBuilder
    {

        @Override
        protected void doConfigure(MuleContext muleContext) throws Exception
        {
            // Do nothing
        }
    }

    public static class ExpectedKeyTransformer extends AbstractDiscoverableTransformer
    {

        public ExpectedKeyTransformer()
        {
            super();
        }

        @Override
        protected Object doTransform(Object src, String enc) throws TransformerException
        {
            return null;
        }
    }

    public static class CustomKeyTransformer extends AbstractDiscoverableTransformer
    {

        public CustomKeyTransformer()
        {
            super();
        }

        @Override
        protected Object doTransform(Object src, String enc) throws TransformerException
        {
            return null;
        }
    }

    private static class TestRegistryBootstrapDiscoverer implements RegistryBootstrapDiscoverer
    {

        private final Properties properties;

        public TestRegistryBootstrapDiscoverer(Properties properties)
        {
            this.properties = properties;
        }

        @Override
        public List<Properties> discover() throws IOException
        {
            return Arrays.asList(properties);
        }
    }

    private static class TestTransformerResolver implements TransformerResolver
    {

        private List<Transformer> transformers = new ArrayList<Transformer>();

        @Override
        public Transformer resolve(DataType<?> source, DataType<?> result) throws ResolverException
        {
            return null;
        }

        @Override
        public void transformerChange(Transformer transformer, RegistryAction registryAction)
        {
            transformers.add(transformer);
        }

        public List<Transformer> getTransformers()
        {
            return transformers;
        }
    }
}
