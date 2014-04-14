/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.bootstrap;

import org.mule.api.MuleContext;
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

import junit.framework.Assert;
import org.junit.Test;

public class SimpleRegistryBootstrapTransformerWithCustomKeyTest extends AbstractMuleContextTestCase
{
    @Override
    protected MuleContext createMuleContext() throws Exception
    {
        return null;
    }

    @Test
    public void testTransformersWithCustomKeyRegistration() throws InitialisationException, RegistrationException
    {
        Map<String, Transformer> previousTransformers = muleContext.getRegistry().lookupByType(Transformer.class);

        SimpleRegistryBootstrap customRegistryBootstrap = new SimpleRegistryBootstrap(new RegistryBoostrapDiscoverer()
        {
            @Override
            public List<Properties> discover() throws IOException
            {
                Properties properties = new Properties();
                properties.setProperty("core.transformer.1", "org.mule.config.bootstrap.SimpleRegistryBootstrapTransformerWithCustomKeyTest$MyTransformer1");
                properties.setProperty("custom1", "org.mule.config.bootstrap.SimpleRegistryBootstrapTransformerWithCustomKeyTest$MyTransformer2,name=MyCustomTransformer");
                return Arrays.asList(properties);
            }
        });
        customRegistryBootstrap.setMuleContext(muleContext);
        final List<Transformer> transformers = new ArrayList<Transformer>();
        muleContext.getRegistry().registerObject("aaa", new TransformerResolver()
        {
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
        });
        customRegistryBootstrap.initialise();

        transformers.removeAll(previousTransformers.values());

        Assert.assertEquals(2, transformers.size());

        Assert.assertTrue(findByName(transformers, "MyCustomTransformer") instanceof MyTransformer2);

        Assert.assertTrue(findByName(transformers, "_SimpleRegistryBootstrapTransformerWithCustomKeyTest$MyTransformer1") instanceof MyTransformer1);

    }

    private Transformer findByName(List<Transformer> transformers, String name)
    {
        for (Transformer t : transformers)
        {
            if(name.equals(t.getName()))
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

