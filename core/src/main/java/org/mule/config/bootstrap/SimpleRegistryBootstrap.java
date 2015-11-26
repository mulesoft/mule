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

    @Override
    protected void doRegisterTransformer(TransformerBootstrapProperty bootstrapProperty, Class<?> returnClass, Class<? extends Transformer> transformerClass) throws Exception
    {
        Transformer trans = ClassUtils.instanciateClass(transformerClass);
        if (!(trans instanceof DiscoverableTransformer))
        {
            throw new RegistrationException(CoreMessages.transformerNotImplementDiscoverable(trans));
        }
        if (returnClass != null)
        {
            trans.setReturnDataType(DataTypeFactory.create(returnClass, bootstrapProperty.getMimeType()));
        }
        if (bootstrapProperty.getName() != null)
        {
            trans.setName(bootstrapProperty.getName());
        }
        else
        {
            // Prefixes the generated default name to ensure there is less chance of conflict if the user registers
            // the transformer with the same name
            trans.setName("_" + trans.getName());
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
    protected void doRegisterObject(ObjectBootstrapProperty bootstrapProperty) throws Exception
    {
        Object value = bootstrapProperty.getService().instantiateClass(bootstrapProperty.getClassName());
        Class<?> meta = Object.class;

        if (value instanceof ObjectProcessor)
        {
            meta = ObjectProcessor.class;
        }
        else if (value instanceof StreamCloser)
        {
            meta = StreamCloser.class;
        }
        else if (value instanceof BootstrapObjectFactory)
        {
            value = ((BootstrapObjectFactory) value).create();
        }

        muleContext.getRegistry().registerObject(bootstrapProperty.getKey(), value, meta);
    }
}
