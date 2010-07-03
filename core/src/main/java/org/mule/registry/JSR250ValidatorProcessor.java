/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.registry;

import org.mule.api.registry.InjectProcessor;
import org.mule.config.i18n.CoreMessages;
import org.mule.util.annotation.AnnotationMetaData;
import org.mule.util.annotation.AnnotationUtils;

import java.lang.reflect.Method;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * Injects the MuleContext object for objects stored in the {@link TransientRegistry} where the object registered
 * implements {@link org.mule.api.context.MuleContextAware}.
 */
public class JSR250ValidatorProcessor implements InjectProcessor
{
    public Object process(Object object)
    {
        List<AnnotationMetaData> annos = AnnotationUtils.getMethodAnnotations(object.getClass(), PostConstruct.class);
        if (annos.size() > 1)
        {
            throw new IllegalArgumentException(CoreMessages.objectHasMoreThanOnePostConstructAnnotation(object.getClass()).getMessage());
        }
        else if(annos.size()==1)
        {
            validateMethod((Method)annos.get(0).getMember());
        }

        annos = AnnotationUtils.getMethodAnnotations(object.getClass(), PreDestroy.class);
        if (annos.size() > 1)
        {
            throw new IllegalArgumentException(CoreMessages.objectHasMoreThanOnePreDestroyAnnotation(object.getClass()).getMessage());
        }
        else if(annos.size()==1)
        {
            validateMethod((Method)annos.get(0).getMember());
        }

        return object;
    }

    protected void validateMethod(Method method)
    {
        if(method.getParameterTypes().length != 0)
        {
            throw new IllegalArgumentException(CoreMessages.lifecycleMewthodNotVoidOrHasParams(method).getMessage());
        }

        if(!method.getReturnType().equals(Void.TYPE))
        {
            throw new IllegalArgumentException(CoreMessages.lifecycleMewthodNotVoidOrHasParams(method).getMessage());
        }
    }
}