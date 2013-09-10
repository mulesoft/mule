/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.endpoint;

import org.mule.api.EndpointAnnotationParser;
import org.mule.api.MessageProcessorAnnotationParser;
import org.mule.api.MuleContext;
import org.mule.api.MuleRuntimeException;
import org.mule.api.context.MuleContextAware;
import org.mule.api.expression.ExpressionAnnotationParser;
import org.mule.api.registry.ObjectProcessor;
import org.mule.api.registry.RegistrationException;
import org.mule.config.AnnotationsParserFactory;
import org.mule.config.i18n.CoreMessages;

import java.lang.annotation.Annotation;
import java.lang.reflect.Member;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Loads the default expression, router and endpoint parsers provided by Mule.  Mule modules can add to these by
 * registering the additional parsers in the 'registry-bootstrap.properties' for custom module.
 */
public class RegistryBackedAnnotationsParserFactory implements AnnotationsParserFactory, MuleContextAware
{
    /**
     * logger used by this class
     */
    protected transient final Log logger = LogFactory.getLog(RegistryBackedAnnotationsParserFactory.class);

    protected MuleContext muleContext;

    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
    }

    public EndpointAnnotationParser getEndpointParser(Annotation annotation, Class aClass, Member member)
    {
        Collection<EndpointAnnotationParser> parsers = muleContext.getRegistry().lookupObjects(EndpointAnnotationParser.class);
        for (EndpointAnnotationParser parser : parsers)
        {
            if (parser.supports(annotation, aClass, member))
            {
                return parser;
            }
        }
        return null;
    }

    public ExpressionAnnotationParser getExpressionParser(Annotation annotation)
    {
        Collection<ExpressionAnnotationParser> parsers = muleContext.getRegistry().lookupObjects(ExpressionAnnotationParser.class);
        for (ExpressionAnnotationParser parser : parsers)
        {
            if (parser.supports(annotation))
            {
                return parser;
            }
        }
        return null;
    }

    public MessageProcessorAnnotationParser getRouterParser(Annotation annotation, Class aClass, Member member)
    {
        Collection<MessageProcessorAnnotationParser> parsers = muleContext.getRegistry().lookupObjects(MessageProcessorAnnotationParser.class);
        for (MessageProcessorAnnotationParser parser : parsers)
        {
            if (parser.supports(annotation, aClass, member))
            {
                return parser;
            }
        }
        return null;
    }

    protected void registerObjectProcessor(ObjectProcessor processor)
    {
        try
        {
            muleContext.getRegistry().registerObject("_" + processor.getClass().getSimpleName(), processor);
        }
        catch (RegistrationException e)
        {
            throw new MuleRuntimeException(CoreMessages.failedToCreate(processor.getClass().getName()), e);
        }
    }

}
