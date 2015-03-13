/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.bootstrap;

import org.mule.api.MuleException;
import org.mule.api.registry.ObjectProcessor;
import org.mule.api.registry.RegistrationException;
import org.mule.api.registry.TransformerResolver;
import org.mule.api.transformer.Converter;
import org.mule.api.transformer.DiscoverableTransformer;
import org.mule.api.transformer.Transformer;
import org.mule.api.util.StreamCloser;
import org.mule.config.i18n.CoreMessages;
import org.mule.registry.MuleRegistryHelper;
import org.mule.registry.SimpleRegistry;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.util.ClassUtils;

import java.util.Map;

/**
 * An implementation of {@link AbstractRegistryBootstrap} to populate instances
 * of {@link SimpleRegistry}
 *
 * @deprecated as of 3.7.0. Try to use {@link org.mule.config.spring.SpringRegistryBootstrap} instead.
 */
@Deprecated
public class SimpleRegistryBootstrap extends AbstractRegistryBootstrap
{

    public SimpleRegistryBootstrap()
    {
        super();
    }

    public SimpleRegistryBootstrap(RegistryBootstrapDiscoverer discoverer)
    {
        super(discoverer);
    }

    @Override
    protected void doRegisterTransformer(String name, Class<?> returnClass, Class<? extends Transformer> transformerClass, String mime, boolean optional) throws Exception
    {
        Transformer trans = ClassUtils.instanciateClass(transformerClass);
        if (!(trans instanceof DiscoverableTransformer))
        {
            throw new RegistrationException(CoreMessages.transformerNotImplementDiscoverable(trans));
        }
        if (returnClass != null)
        {
            trans.setReturnDataType(DataTypeFactory.create(returnClass, mime));
        }
        if (name != null)
        {
            trans.setName(name);
        }
        else
        {
            //This will generate a default name for the transformer
            name = trans.getName();
            //We then prefix the name to ensure there is less chance of conflict if the user registers
            // the transformer with the same name
            trans.setName("_" + name);
        }
        muleContext.getRegistry().registerTransformer(trans);
    }

    @Override
    protected void registerTransformers() throws MuleException
    {
        MuleRegistryHelper registry = (MuleRegistryHelper) muleContext.getRegistry();
        Map<String, Converter> converters = registry.lookupByType(Converter.class);
        for (Converter converter : converters.values())
        {
            registry.notifyTransformerResolvers(converter, TransformerResolver.RegistryAction.ADDED);
        }
    }

    @Override
    protected void doRegisterObject(String key, String className, boolean optional) throws Exception
    {
        Object o = ClassUtils.instanciateClass(className);
        Class<?> meta = Object.class;

        if (o instanceof ObjectProcessor)
        {
            meta = ObjectProcessor.class;
        }
        else if (o instanceof StreamCloser)
        {
            meta = StreamCloser.class;
        }
        else if (o instanceof BootstrapObjectFactory)
        {
            o = ((BootstrapObjectFactory) o).create();
        }

        muleContext.getRegistry().registerObject(key, o, meta);
    }
}
