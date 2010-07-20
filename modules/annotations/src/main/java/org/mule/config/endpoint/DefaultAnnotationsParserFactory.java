/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.endpoint;

import org.mule.api.EndpointAnnotationParser;
import org.mule.api.MuleContext;
import org.mule.api.MuleRuntimeException;
import org.mule.api.RouterAnnotationParser;
import org.mule.api.context.MuleContextAware;
import org.mule.api.expression.ExpressionParser;
import org.mule.api.registry.ObjectProcessor;
import org.mule.api.registry.RegistrationException;
import org.mule.config.AnnotationsParserFactory;
import org.mule.config.expression.BeanAnnotationParser;
import org.mule.config.expression.CustomEvaluatorAnnotationParser;
import org.mule.config.expression.ExpressionFilterAnnotationParser;
import org.mule.config.expression.FunctionAnnotationParser;
import org.mule.config.expression.GroovyAnnotationParser;
import org.mule.config.expression.MuleAnnotationParser;
import org.mule.config.expression.OgnlAnnotationParser;
import org.mule.config.expression.XPathAnnotationParser;
import org.mule.config.i18n.CoreMessages;
import org.mule.config.parsers.InboundAttachmentsAnnotationParser;
import org.mule.config.parsers.InboundHeadersAnnotationParser;
import org.mule.config.parsers.OutboundAttachmentsAnnotationParser;
import org.mule.config.parsers.PayloadAnnotationParser;
import org.mule.config.processors.AnnotatedServiceObjectProcessor;
import org.mule.config.processors.DirectBindAnnotationProcessor;
import org.mule.config.processors.InjectAnnotationProcessor;
import org.mule.config.processors.NamedAnnotationProcessor;
import org.mule.config.routing.IdempotentRouterParser;
import org.mule.config.routing.SplitterRouterParser;
import org.mule.config.routing.WireTapRouterParser;

import java.lang.annotation.Annotation;
import java.lang.reflect.Member;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Loads the default endpoint parsers provided by the annotations module.  Currently, there is only one,
 * the {@link org.mule.config.endpoint.ReplyAnnotationParser}, the others are currently supplied by iBeans.
 */
public class DefaultAnnotationsParserFactory implements AnnotationsParserFactory, MuleContextAware
{
    /**
     * logger used by this class
     */
    protected transient final Log logger = LogFactory.getLog(DefaultAnnotationsParserFactory.class);

    protected MuleContext muleContext;

    private List<EndpointAnnotationParser> endpointParsers = new ArrayList<EndpointAnnotationParser>();
    private List<ExpressionParser> expressionParsers = new ArrayList<ExpressionParser>();
    private List<RouterAnnotationParser> routerParsers = new ArrayList<RouterAnnotationParser>();
    private List<ObjectProcessor> processors = new ArrayList<ObjectProcessor>();

    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
        addDefaultParsers();
        addDefaultProcessors();
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
        registerExpressionParser(new PayloadAnnotationParser());
        registerExpressionParser(new InboundHeadersAnnotationParser());
        registerExpressionParser(new CustomEvaluatorAnnotationParser());
        registerExpressionParser(new InboundAttachmentsAnnotationParser());
        registerExpressionParser(new OutboundAttachmentsAnnotationParser());
        registerExpressionParser(new FunctionAnnotationParser());
        registerExpressionParser(new XPathAnnotationParser());
        registerExpressionParser(new BeanAnnotationParser());
        registerExpressionParser(new OgnlAnnotationParser());
        registerExpressionParser(new GroovyAnnotationParser());
    }

    protected void addDefaultProcessors()
    {
        //Processors
        registerObjectProcessor(new AnnotatedServiceObjectProcessor());
        registerObjectProcessor(new DirectBindAnnotationProcessor());
        registerObjectProcessor(new InjectAnnotationProcessor());//Add support for JSR-330
        registerObjectProcessor(new NamedAnnotationProcessor());//Add support for JSR-330
    }

    public EndpointAnnotationParser getEndpointParser(Annotation annotation, Class<?> aClass, Member member)
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

    public RouterAnnotationParser getRouterParser(Annotation annotation, Class<?> aClass, Member member)
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

    protected void registerObjectProcessor(ObjectProcessor processor)
    {
        try
        {
            muleContext.getRegistry().registerObject("_" + processor.getClass().getSimpleName(), processor);
            processors.add(processor);
        }
        catch (RegistrationException e)
        {
            throw new MuleRuntimeException(CoreMessages.failedToCreate(processor.getClass().getName()), e);
        }
    }
}
