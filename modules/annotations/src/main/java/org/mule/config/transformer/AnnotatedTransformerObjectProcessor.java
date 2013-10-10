/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
 */
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
        List<AnnotationMetaData> annos = AnnotationUtils.getMethodAnnotations(clazz, Transformer.class);

        if (annos.size() == 0)
        {
            return object;
        }
        for (AnnotationMetaData data : annos)
        {
            try
            {
                Transformer anno = (Transformer) data.getAnnotation();
                String sourceMimeType = anno.sourceMimeType().equals(MimeTypes.ANY) ? null : anno.sourceMimeType();
                String resultMimeType = anno.resultMimeType().equals(MimeTypes.ANY) ? null : anno.resultMimeType();
                AnnotatedTransformerProxy trans = new AnnotatedTransformerProxy(
                        anno.priorityWeighting(),
                        object, (Method) data.getMember(), anno.sourceTypes(),
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
