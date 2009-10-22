/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.impl.concept;

import org.mule.api.MuleException;
import org.mule.api.RouterAnnotationParser;
import org.mule.api.routing.Router;
import org.mule.config.annotations.concept.Splitter;
import org.mule.routing.outbound.ExpressionMessageSplitter;

import java.lang.annotation.Annotation;
import java.lang.reflect.Member;

/**
 * Parses a {@link org.mule.config.annotations.concept.Splitter} annotation into a Mule {@link org.mule.routing.outbound.ExpressionMessageSplitter}
 * and registers it with the service it is configured on.
 */
public class SplitterRouterParser implements RouterAnnotationParser
{
    public Router parseRouter(Annotation annotation) throws MuleException
    {
        Splitter splitter = (Splitter) annotation;
        ExpressionMessageSplitter router = new ExpressionMessageSplitter();
        router.setEvaluator(splitter.evaluator());
        router.setExpression(splitter.expression());
        router.setDeterministic(splitter.deterministic());
        router.setDisableRoundRobin(splitter.disableRoundRobin());
        router.setFailIfNoMatch(splitter.failIfNoMatch());
        return router;
    }

    public boolean supports(Annotation annotation, Class clazz, Member member)
    {
        return annotation instanceof Splitter;
    }
}