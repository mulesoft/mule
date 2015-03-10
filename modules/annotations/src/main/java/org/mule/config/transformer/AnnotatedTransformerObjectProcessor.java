/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.transformer;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.annotations.ContainsTransformerMethods;
import org.mule.api.annotations.Transformer;
import org.mule.api.context.MuleContextAware;
import org.mule.api.registry.PreInitProcessor;
import org.mule.transformer.types.MimeTypes;
import org.mule.util.annotation.AnnotationMetaData;
import org.mule.util.annotation.AnnotationUtils;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Will check all method level annotations to see if there are any {@link org.mule.api.annotations.Transformer} annotations present.
 * For each method annotated with {@link org.mule.api.annotations.Transformer} a Mule transformer will be created.  When the
 * transformer is used, the method will get invoked
 *
 * @see org.mule.api.annotations.Transformer
 * @deprecated as of 3.7.0 since these are only used by {@link org.mule.registry.TransientRegistry} which is also deprecated. Use post processors
 * for currently supported registries instead (i.e: {@link org.mule.config.spring.SpringRegistry})
 */
@Deprecated
public class AnnotatedTransformerObjectProcessor implements PreInitProcessor, MuleContextAware
{

    private MuleContext muleContext;

    public AnnotatedTransformerObjectProcessor()
    {
    }

    public AnnotatedTransformerObjectProcessor(MuleContext muleContext)
    {
        setMuleContext(muleContext);
    }

    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
    }

    public Object process(Object object)
    {
        Class<? extends Object> clazz = object.getClass();
        if (clazz.getAnnotation(ContainsTransformerMethods.class) == null)
        {
            return object;
        }
        List<AnnotationMetaData> annotations = AnnotationUtils.getMethodAnnotations(clazz, Transformer.class);

        if (annotations.size() == 0)
        {
            return object;
        }
        for (AnnotationMetaData data : annotations)
        {
            try
            {
                Transformer annotation = (Transformer) data.getAnnotation();
                String sourceMimeType = annotation.sourceMimeType().equals(MimeTypes.ANY) ? null : annotation.sourceMimeType();
                String resultMimeType = annotation.resultMimeType().equals(MimeTypes.ANY) ? null : annotation.resultMimeType();
                AnnotatedTransformerProxy trans = new AnnotatedTransformerProxy(
                        annotation.priorityWeighting(),
                        object, (Method) data.getMember(), annotation.sourceTypes(),
                        sourceMimeType, resultMimeType);

                muleContext.getRegistry().registerTransformer(trans);
            }
            catch (MuleException e)
            {
                throw new RuntimeException(e);
            }
        }
        return object;
    }
}
