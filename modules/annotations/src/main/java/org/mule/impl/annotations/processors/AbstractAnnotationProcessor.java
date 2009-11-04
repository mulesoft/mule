/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.impl.annotations.processors;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.MuleRuntimeException;
import org.mule.api.context.MuleContextAware;
import org.mule.api.registry.PreInitProcessor;
import org.mule.impl.endpoint.AnnotatedEndpointBuilder;
import org.mule.impl.registry.RegistryMap;
import org.mule.util.TemplateParser;

/**
 * TODO
 */
public abstract class AbstractAnnotationProcessor implements PreInitProcessor, MuleContextAware
{
    protected MuleContext context;
    protected AnnotatedEndpointBuilder builder;
    private final TemplateParser parser = TemplateParser.createAntStyleParser();
    protected RegistryMap regProps;

    public void setMuleContext(MuleContext context)
    {
        this.context = context;
        regProps = new RegistryMap(context.getRegistry());
        try
        {
            builder = new AnnotatedEndpointBuilder(context);
        }
        catch (MuleException e)
        {
            throw new MuleRuntimeException(e.getI18nMessage(), e);
        }
    }

    protected String getValue(String key)
    {
        return parser.parse(regProps, key);
    }

}