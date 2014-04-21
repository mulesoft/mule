/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.bootstrap;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.config.ConfigurationBuilder;
import org.mule.api.registry.ResolverException;
import org.mule.api.registry.TransformerResolver;
import org.mule.api.transformer.DataType;
import org.mule.api.transformer.Transformer;
import org.mule.api.transformer.TransformerException;
import org.mule.config.builders.DefaultsConfigurationBuilder;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.transformer.AbstractDiscoverableTransformer;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.junit.Test;

public class SimpleRegistryBootstrapTransformersTest extends AbstractMuleContextTestCase
{

    @Test
    public void registeringTransformersWithCustomKey() throws MuleException
    {
        Properties properties = new Properties();
        properties.setProperty("core.transformer.1", ExpectedKeyTransformer.class.getName());
        properties.setProperty("custom1", CustomKeyTransformer.class.getName());

        TestTransformerResolver transformerResolver = new TestTransformerResolver();
        muleContext.getRegistry().registerObject("testTransformerResolver", transformerResolver);

        SimpleRegistryBootstrap registryBootstrap = new SimpleRegistryBootstrap(new SinglePropertiesRegistryBootstrapDiscoverer(properties));
        registryBootstrap.setMuleContext(muleContext);
        registryBootstrap.initialise();

        assertEquals(2, transformerResolver.getTransformersCount());

        assertTrue(transformerResolver.contains(ExpectedKeyTransformer.class));

        assertTrue(transformerResolver.contains(CustomKeyTransformer.class));
    }

    @Override
    protected ConfigurationBuilder getBuilder() throws Exception
    {
        // Avoid to use DefaultsConfigurationBuilder because it registers a new instance of SimpleRegistryBootstrap
        // that conflicts with the one being tested in the test
        return new DummyConfigurationBuilder();
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

        @Override
        protected Object doTransform(Object src, String enc) throws TransformerException
        {
            return null;
        }
    }

    public static class CustomKeyTransformer extends AbstractDiscoverableTransformer
    {

        @Override
        protected Object doTransform(Object src, String enc) throws TransformerException
        {
            return null;
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

        public int getTransformersCount()
        {
            return transformers.size();
        }

        private boolean contains(Class<? extends Transformer> transformerClass)
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
    }
}
