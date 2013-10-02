/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config;

import org.mule.api.EndpointAnnotationParser;
import org.mule.api.MessageProcessorAnnotationParser;
import org.mule.api.expression.ExpressionAnnotationParser;

import java.lang.annotation.Annotation;
import java.lang.reflect.Member;

/**
 * Defines a common interface to find all Endpoint annotation parsers for a context.  Endpoint parsers may be
 * customized depending on the underlying platform.
 * This is not an interface that users should ever need to use or customize, but Mule on different platforms
 * can customize how the endpoints are created from the annotations.
 * <p/>
 * There are 3 types of annotation parser -
 * 1. Endpoint : translates into an endpoint configured on a service object.
 * 2. Router : translates into a router that will be configured on a service object, i.e. WireTap, Splitter, or Filter
 * 3. Expression : translates into an expression, usually these are configured on method parameters so that an expression
 * will get evaluated on the parameter before being passed into the method.
 *
 * @since 3.0
 *
 */
public interface AnnotationsParserFactory
{
    /**
     * Retrieves a parser for the given annotation, the parameters passed in can be used to validate the use of
     * the annotation, i.e. you may want to restrict annotations to only be configured on concrete classes
     *
     * @param annotation the annotation being processed
     * @param aClass     the class on which  the annotation is defined
     * @param member     the class member on which the annotation was defined, such as Field, Method, Constructor, or
     *                   null if a Type-level annotation.
     * @return the endpoint annotation parser that can parse the supplied annotation or null if a matching parser
     *         not found
     */
    EndpointAnnotationParser getEndpointParser(Annotation annotation, Class<?> aClass, Member member);

    /**
     * Retrieves a parser for the given annotation, where the annotation is an Expression annotation; one annotated with
     * the {@link org.mule.api.annotations.meta.Evaluator} annotation.
     * <p/>
     *
     * @param annotation the annotation being processed
     * @return the expression annotation parser that can parse the supplied annotation or null if a matching parser
     *         not found
     */
    ExpressionAnnotationParser getExpressionParser(Annotation annotation);

    /**
     * Retrieves a parser for the given annotation, where the annotation is a Router annotation; one annotated with
     * the {@link org.mule.api.annotations.meta.Router} annotation. the parameters passed in can be used to validate the use of
     * the annotation, i.e. you may want to restrict annotations to only be configured on concrete classes.
     * <p/>
     *
     * @param annotation the annotation being processed
     * @param aClass     the class on which  the annotation is defined
     * @param member     the class member on which the annotation was defined, such as Field, Method, Constructor, or
     *                   null if a Type-level annotation.
     * @return the router annotation parser that can parse the supplied annotation or null if a matching parser
     *         not found
     */
    MessageProcessorAnnotationParser getRouterParser(Annotation annotation, Class<?> aClass, Member member);

}
