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
import org.mule.api.RouterAnnotationParser;
import org.mule.api.context.MuleContextAware;
import org.mule.api.expression.ExpressionParser;
import org.mule.api.registry.ObjectProcessor;
import org.mule.api.registry.RegistrationException;
import org.mule.config.AnnotationsParserFactory;
import org.mule.config.i18n.CoreMessages;
import org.mule.impl.annotations.processors.AnnotatedServiceObjectProcessor;
import org.mule.impl.annotations.processors.DirectBindAnnotationProcessor;
import org.mule.impl.concept.SplitterRouterParser;
import org.mule.impl.expression.parsers.BeanAnnotationParser;
import org.mule.impl.expression.parsers.CustomEvaluatorAnnotationParser;
import org.mule.impl.expression.parsers.ExpressionFilterAnnotationParser;
import org.mule.impl.expression.parsers.FunctionAnnotationParser;
import org.mule.impl.expression.parsers.GroovyAnnotationParser;
import org.mule.impl.expression.parsers.MuleAnnotationParser;
import org.mule.impl.expression.parsers.OgnlAnnotationParser;
import org.mule.impl.expression.parsers.XPathAnnotationParser;
import org.mule.impl.routing.IdempotentRouterParser;
import org.mule.impl.routing.WireTapRouterParser;

import java.lang.annotation.Annotation;
import java.lang.reflect.Member;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Loads the default endpoint parsers provided by the annotations module.  Currently, there is only one,
 * the {@link org.mule.impl.endpoint.ReplyAnnotationParser}, the others are currently supplied by iBeans.
 */
public class DefaultAnnotationsParserFactory implements AnnotationsParserFactory, MuleContextAware
{
    /**
     * logger used by this class
     */
    protected transient final Log logger = LogFactory.getLog(DefaultAnnotationsParserFactory.class);

    protected MuleContext muleContext;

    protected List<EndpointAnnotationParser> endpointParsers = new ArrayList<EndpointAnnotationParser>();
    protected List<ExpressionParser> expressionParsers = new ArrayList<ExpressionParser>();
    protected List<RouterAnnotationParser> routerParsers = new ArrayList<RouterAnnotationParser>();
    protected List<ObjectProcessor> processors = new ArrayList<ObjectProcessor>();

    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
        addDefaultParsers();

        for (ObjectProcessor processor : getProcessors())
        {
            try
            {
                muleContext.getRegistry().registerObject("_" + processor.getClass().getSimpleName(), processor, ObjectProcessor.class);
            }
            catch (RegistrationException e)
            {
                logger.warn(e.getMessage(), e);
            }
        }
    }

    protected void addDefaultParsers()
    {
        //Endpoint parsers
        //Note that currently iBeans provides all the other endpoint annotations
        registerEndpointParser(new ReplyAnnotationParser());

        //Router parsers
        registerRouterParser(new WireTapRouterParser());
        registerRouterParser(new IdempotentRouterParser());
        registerRouterParser(new ExpressionFilterAnnotationParser());
        //Splitter is Experimental, has limited use
        registerRouterParser(new SplitterRouterParser());

        //Expression parsers
        registerExpressionParser(new MuleAnnotationParser());
        registerExpressionParser(new CustomEvaluatorAnnotationParser());
        registerExpressionParser(new FunctionAnnotationParser());
        registerExpressionParser(new XPathAnnotationParser());
        registerExpressionParser(new BeanAnnotationParser());
        registerExpressionParser(new OgnlAnnotationParser());
        registerExpressionParser(new GroovyAnnotationParser());

        //Processors
        processors.add(new AnnotatedServiceObjectProcessor());
        processors.add(new DirectBindAnnotationProcessor());
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

    public ExpressionParser getExpressionParser(Annotation annotation)
    {
        for (ExpressionParser parser : expressionParsers)
        {
            if (parser.supports(annotation))
            {
                return parser;
            }
        }
        return null;
    }

    public RouterAnnotationParser getRouterParser(Annotation annotation, Class aClass, Member member)
    {
        for (RouterAnnotationParser parser : routerParsers)
        {
            if (parser.supports(annotation, aClass, member))
            {
                return parser;
            }
        }
        return null;
    }

    public List<ObjectProcessor> getProcessors()
    {
        return processors;
    }

    protected void registerEndpointParser(EndpointAnnotationParser parser)
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

    protected void registerExpressionParser(ExpressionParser parser)
    {
        try
        {
            muleContext.getRegistry().registerObject("_" + parser.getClass().getSimpleName(), parser);
            expressionParsers.add(parser);
        }
        catch (RegistrationException e)
        {
            throw new MuleRuntimeException(CoreMessages.failedToCreate(parser.getClass().getName()), e);
        }
    }

    protected void registerRouterParser(RouterAnnotationParser parser)
    {
        try
        {
            muleContext.getRegistry().registerObject("_" + parser.getClass().getSimpleName(), parser);
            routerParsers.add(parser);
        }
        catch (RegistrationException e)
        {
            throw new MuleRuntimeException(CoreMessages.failedToCreate(parser.getClass().getName()), e);
        }
    }

}
