/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.impl.endpoint;

import org.mule.api.EndpointAnnotationParser;
import org.mule.api.MuleContext;
import org.mule.api.MuleRuntimeException;
import org.mule.api.context.MuleContextAware;
import org.mule.api.registry.RegistrationException;
import org.mule.config.EndpointAnnotationsParserFactory;
import org.mule.config.i18n.CoreMessages;

import java.lang.annotation.Annotation;
import java.lang.reflect.Member;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads the default endpoint parsers provided by the annotations module.  Currently, there is only one,
 * the {@link org.mule.impl.endpoint.ReplyAnnotationParser}, the others are currently supplied by iBeans.
 * <p/>
 * TODO Note that this class is not loaded by default, which means endpoint parsers are currently not supported in Mule
 */
public class DefaultEndpointAnnotationsParserFactory implements EndpointAnnotationsParserFactory, MuleContextAware
{
    protected MuleContext muleContext;

    private List<EndpointAnnotationParser> endpointParsers = new ArrayList<EndpointAnnotationParser>();

    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
        addDefaultParsers();
    }

    protected void addDefaultParsers()
    {
        //Endpoint parsers
        //Note that currently iBeans provides all the other endpoint annotations
        registerEndpointParser(new ReplyAnnotationParser());
    }


    public EndpointAnnotationParser getEndpointParser(Annotation annotation, Class aClass, Member member)
    {
        for (EndpointAnnotationParser parser : endpointParsers)
        {
            if (parser.supports(annotation, aClass, member))
            {
                return parser;
            }
        }
        return null;
    }

    public void registerEndpointParser(EndpointAnnotationParser parser)
    {
        try
        {
            muleContext.getRegistry().registerObject("_" + parser.getClass().getSimpleName(), parser);
            endpointParsers.add(parser);
        }
        catch (RegistrationException e)
        {
            throw new MuleRuntimeException(CoreMessages.failedToCreate(parser.getClass().getName()), e);
        }
    }
}
