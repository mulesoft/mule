/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.json.transformers;

import org.mule.api.MuleContext;
import org.mule.config.transformer.AbstractAnnotatedTransformerArgumentResolver;

import org.codehaus.jackson.map.ObjectMapper;

/**
 * This resolver is used by the transform engine to inject a {@link org.codehaus.jackson.map.ObjectMapper} into a method that requires it.
 * A shared ObjectMapper context can be created for the application and stored in the registry, this will get injected
 * into any transform methods that add {@link org.codehaus.jackson.map.ObjectMapper} to the method signature.
 * <p/>
 * If there is no shared Object Mapper one will be created for the transformer using the return type as the Json root element.
 *
 * @since 3.0
 */
public class JsonMapperResolver extends AbstractAnnotatedTransformerArgumentResolver
{
    public static final String ANNOTATIONS_PACKAGE_NAME = "org.codehaus.jackson";

    /**
     * {@inheritDoc}
     */
    @Override
    protected Class<?> getArgumentClass()
    {
        return ObjectMapper.class;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Object createArgument(Class<?> annotatedType, MuleContext context) throws Exception
    {
        return new ObjectMapper();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getAnnotationsPackageName()
    {
        return ANNOTATIONS_PACKAGE_NAME;
    }
}
