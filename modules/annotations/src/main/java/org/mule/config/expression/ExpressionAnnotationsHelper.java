/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.expression;

import org.mule.api.MuleContext;
import org.mule.api.expression.ExpressionAnnotationParser;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.registry.RegistrationException;
import org.mule.api.transformer.TransformerException;
import org.mule.config.AnnotationsParserFactory;
import org.mule.config.i18n.AnnotationsMessages;
import org.mule.expression.transformers.ExpressionArgument;
import org.mule.expression.transformers.ExpressionTransformer;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * TODO
 */
public class ExpressionAnnotationsHelper
{
    protected static Log logger = LogFactory.getLog(ExpressionAnnotationsHelper.class);

    public static ExpressionTransformer getTransformerForMethodWithAnnotations(Method method, MuleContext context) throws TransformerException, InitialisationException
    {
        ExpressionTransformer trans = new ExpressionTransformer();
        trans.setMuleContext(context);

        Annotation[][] annotations = method.getParameterAnnotations();

        for (int i = 0; i < annotations.length; i++)
        {
            Annotation[] annotation = annotations[i];
            for (int j = 0; j < annotation.length; j++)
            {
                Annotation ann = annotation[j];
                ExpressionArgument arg = parseAnnotation(ann, method.getParameterTypes()[i], context);

                if (arg != null)
                {
                    trans.addArgument(arg);
                }
            }
        }
        trans.initialise();
        return trans;
    }

    static synchronized ExpressionArgument parseAnnotation(Annotation annotation, 
        Class<?> paramType, MuleContext muleContext)
    {
        AnnotationsParserFactory factory;
        try
        {
            factory = muleContext.getRegistry().lookupObject(AnnotationsParserFactory.class);
        }
        catch (RegistrationException e)
        {
            //TODO better exception message
            throw new IllegalArgumentException(AnnotationsMessages.noParserFoundForAnnotation(annotation).getMessage());
        }
        
        ExpressionAnnotationParser parser = factory.getExpressionParser(annotation);
        if (parser == null)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug(AnnotationsMessages.noParserFoundForAnnotation(annotation).getMessage());
            }
            return null;
        }
        return parser.parse(annotation, paramType);
    }
}
