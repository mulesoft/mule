/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api;

import org.mule.api.processor.MessageProcessor;

import java.lang.annotation.Annotation;
import java.lang.reflect.Member;

/**
 * An SPI interface used for creating Routers from Annotations. Note that any Router annotations must be themselves
 * annotated with the {@link org.mule.api.routing.OutboundRouter} annotation.
 */
public interface MessageProcessorAnnotationParser
{
    /**
     * Will create a Mule Router according to the annotation. Note that the annotation must
     * itself be annotated with the {@link org.mule.api.routing.OutboundRouter} annotation.
     *
     * @param annotation the current annotation being processed
     * @return a new Router configuration based on the current annotation
     * @throws MuleException if the inbound endpoint cannot be created. A Mule-specific error will be thrown.
     */
    public MessageProcessor parseMessageProcessor(Annotation annotation) throws MuleException;

    /**
     * Determines whether this parser can process the current annotation. The clazz and member params are passed in
     * so that further validation be done on the location, type or name of these elements.
     *
     * @param annotation the annotation being processed
     * @param clazz      the class on which the annotation was found
     * @param member     the member on which the annotation was found inside the class.  This is only set when the annotation
     *                   was either set on a {@link java.lang.reflect.Method}, {@link java.lang.reflect.Field} or {@link java.lang.reflect.Constructor}
     *                   class member, otherwise this value is null.
     * @return true if this parser supports the current annotation, false otherwise
     */
    public boolean supports(Annotation annotation, Class clazz, Member member);
}
