/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config;

import org.mule.api.EndpointAnnotationParser;
import org.mule.api.expression.ExpressionParser;
import org.mule.api.registry.ObjectProcessor;

import java.lang.annotation.Annotation;
import java.lang.reflect.Member;
import java.util.List;

/**
 * Defines a common interface to find all Endpoint annotation parsers for a context.  Endpoint parsers may be
 * customized depending on the underlying platform.
 * This is not an interface that users should ever need to use or customize, but iBeans on different platforms
 * can customize how the endpoints are created from the annotations.
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
    EndpointAnnotationParser getEndpointParser(Annotation annotation, Class aClass, Member member);

    /**
     * Retrieves a parser for the given annotation, where the annotation is an Expression annotation; one annotated with
     * the {@link org.mule.config.annotations.expressions.Evaluator} annotation. The parameters passed in can be used
     * to validate the use of the annotation, i.e. you may want to restrict annotations to only be configured on
     * concrete classes.
     * <p/>
     * the annotation being processed
     *
     * @param aClass the class on which  the annotation is defined
     * @param member the class member on which the annotation was defined, such as Field, Method, Constructor, or
     *               null if a Type-level annotation.
     * @return the expression annotation parser that can parse the supplied annotation or null if a matching parser
     *         not found
     */
    ExpressionParser getExpressionParser(Annotation annotation, Class aClass, Member member);

    /**
     * A list of Object processors that should be registered for this factory.  typicially, these will be object
     * processors that process annotations on objects that get put ni the registry.
     *
     * @return the list of processors to be used or an empty list.
     */
    List<ObjectProcessor> getProcessors();

}
