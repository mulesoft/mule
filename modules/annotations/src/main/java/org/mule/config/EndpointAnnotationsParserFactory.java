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

import java.lang.annotation.Annotation;
import java.lang.reflect.Member;

/**
 * Defines a common interface to find all Endpoint annotation parsers for a context.  Endpoint parsers may be
 * customized depending on the underlying platform.
 * This is not an interface that users should ever need to use or customize, but iBeans on different platforms
 * can customize how the endpoints are created from the annotations.
 */
public interface EndpointAnnotationsParserFactory
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
}
