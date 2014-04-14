/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.bootstrap;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.registry.RegistrationException;
import org.mule.api.registry.ResolverException;
import org.mule.api.registry.TransformerResolver;
import org.mule.api.transformer.DataType;
import org.mule.api.transformer.Transformer;
import org.mule.api.transformer.TransformerException;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.transformer.AbstractDiscoverableTransformer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.junit.Test;

public class SimpleRegistryBootstrapTransformerWithCustomKeyTest extends AbstractMuleContextTestCase
{

    public static final String CUSTOM_TRANSFORMER_NAME = "MyCustomTransformer";
    public static final String TRANSFORMER1_CLASS = "SimpleRegistryBootstrapTransformerWithCustomKeyTest$MyTransformer1";
    public static final String TRANSFORMER2_CLASS = "SimpleRegistryBootstrapTransformerWithCustomKeyTest$MyTransformer2";

    @Test
    public void testTransformersWithCustomKeyRegistration() throws InitialisationException, RegistrationException
    {
        Map<String, Transformer> previousTransformers = muleContext.getRegistry().lookupByType(Transformer.class);

        Properties properties = new Properties();
        properties.setProperty("core.transformer.1", String.format("org.mule.config.bootstrap.%s", TRANSFORMER1_CLASS));
        properties.setProperty("custom1", String.format("org.mule.config.bootstrap.%s,name=%s", TRANSFORMER2_CLASS, CUSTOM_TRANSFORMER_NAME));

        SimpleRegistryBootstrap customRegistryBootstrap = new SimpleRegistryBootstrap(new AdHocRegistryBootstrapDiscoverer(properties));
        customRegistryBootstrap.setMuleContext(muleContext);

        AdHocTransformerResolver transformerResolver = new AdHocTransformerResolver();
        muleContext.getRegistry().registerObject("adhocTransformerResolver", transformerResolver);

        customRegistryBootstrap.initialise();

        transformerResolver.getTransformers().removeAll(previousTransformers.values());

        assertEquals(2, transformerResolver.getTransformers().size());

        assertTrue(findByName(transformerResolver.getTransformers(), CUSTOM_TRANSFORMER_NAME) instanceof MyTransformer2);

        assertTrue(findByName(transformerResolver.getTransformers(), "_" + TRANSFORMER1_CLASS) instanceof MyTransformer1);

    }

    private Transformer findByName(List<Transformer> transformers, String name)
    {
        for (Transformer t : transformers)
        {
            if (name.equals(t.getName()))
            {
                return t;
            }
        }
        return null;
    }

    public static class MyTransformer1 extends AbstractDiscoverableTransformer
    {

        public MyTransformer1()
        {
            super();
        }

        @Override
        protected Object doTransform(Object src, String enc) throws TransformerException
        {
            return null;
        }
    }

    public static class MyTransformer2 extends AbstractDiscoverableTransformer
    {

        public MyTransformer2()
        {
            super();
        }

        @Override
        protected Object doTransform(Object src, String enc) throws TransformerException
        {
            return null;
        }
    }

}

class AdHocRegistryBootstrapDiscoverer implements RegistryBootstrapDiscoverer
{

    private final Properties properties;

    public AdHocRegistryBootstrapDiscoverer(Properties properties)
    {
        this.properties = properties;
    }

    @Override
    public List<Properties> discover() throws IOException
    {
        return Arrays.asList(properties);
    }
}

class AdHocTransformerResolver implements TransformerResolver
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