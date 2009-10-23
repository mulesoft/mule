/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.utils;

import org.mule.api.MuleContext;
import org.mule.api.expression.ExpressionParser;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.transformer.TransformerException;
import org.mule.config.annotations.i18n.AnnotationsMessages;
import org.mule.expression.transformers.ExpressionArgument;
import org.mule.expression.transformers.ExpressionTransformer;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * A helper class for reading annotations.
 */
public class AnnotationUtils
{
    public static boolean methodHasParamAnnotations(Method method)
    {
        for (int i = 0; i < method.getParameterAnnotations().length; i++)
        {
            if (method.getParameterAnnotations()[i].length > 0)
            {
                return true;
            }
        }
        return false;
    }

    public static List<AnnotationMetaData> getParamAnnotations(Method method)
    {
        List<AnnotationMetaData> annos = new ArrayList<AnnotationMetaData>();

        for (int i = 0; i < method.getParameterAnnotations().length; i++)
        {
            for (int j = 0; j < method.getParameterAnnotations()[i].length; j++)
            {
                Annotation annotation = method.getParameterAnnotations()[i][j];
                annos.add(new AnnotationMetaData(method.getDeclaringClass(), method, ElementType.PARAMETER, annotation));

            }
        }
        return annos;
    }

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

                trans.addArgument(arg);
            }
        }
        trans.initialise();
        return trans;
    }

    static synchronized ExpressionArgument parseAnnotation(Annotation annotation, Class paramType, MuleContext muleContext)
    {
        Collection c = muleContext.getRegistry().lookupObjects(ExpressionParser.class);
        for (Iterator iterator = c.iterator(); iterator.hasNext();)
        {
            ExpressionParser parser = (ExpressionParser) iterator.next();
            if (parser.supports(annotation))
            {
                return parser.parse(annotation, paramType);
            }
        }

        throw new IllegalArgumentException(AnnotationsMessages.noParserFoundForAnnotation(annotation).getMessage());
    }

    public static List<AnnotationMetaData> getClassAndMethodAnnotations(Class c)
    {
        List<AnnotationMetaData> annotations = new ArrayList<AnnotationMetaData>();

        for (int i = 0; i < c.getAnnotations().length; i++)
        {
            annotations.add(new AnnotationMetaData(c, null, ElementType.TYPE, c.getAnnotations()[i]));
        }

        annotations.addAll(getAllMethodAnnotations(c));
        return annotations;
    }

    public static List<AnnotationMetaData> getAllMethodAnnotations(Class c)
    {
        List<AnnotationMetaData> annotations = new ArrayList<AnnotationMetaData>();

        for (int i = 0; i < c.getMethods().length; i++)
        {
            Method method = c.getMethods()[i];
            for (int j = 0; j < method.getDeclaredAnnotations().length; j++)
            {
                annotations.add(new AnnotationMetaData(c, method, ElementType.METHOD, method.getDeclaredAnnotations()[j]));
            }
        }
        return annotations;
    }

    public static List<AnnotationMetaData> getMethodAnnotations(Class c, Class<? extends Annotation> ann)
    {
        List<AnnotationMetaData> annotations = new ArrayList<AnnotationMetaData>();

        for (int i = 0; i < c.getMethods().length; i++)
        {
            Method method = c.getMethods()[i];
            for (int j = 0; j < method.getDeclaredAnnotations().length; j++)
            {
                if (ann.isInstance(method.getDeclaredAnnotations()[j]))
                {
                    annotations.add(new AnnotationMetaData(c, method, ElementType.METHOD, method.getDeclaredAnnotations()[j]));
                }
            }
        }
        return annotations;
    }

    public static List<AnnotationMetaData> getMethodMetaAnnotations(Class c, Class<? extends Annotation> metaAnn)
    {
        List<AnnotationMetaData> annotations = new ArrayList<AnnotationMetaData>();

        for (int i = 0; i < c.getMethods().length; i++)
        {
            Method method = c.getMethods()[i];
            for (int j = 0; j < method.getDeclaredAnnotations().length; j++)
            {
                if (method.getDeclaredAnnotations()[j].annotationType().isAnnotationPresent(metaAnn))
                {
                    annotations.add(new AnnotationMetaData(c, method, ElementType.METHOD, method.getDeclaredAnnotations()[j]));
                }
            }
        }
        return annotations;
    }

    public static List<AnnotationMetaData> getAllFieldAnnotations(Class c)
    {
        List<AnnotationMetaData> annotations = new ArrayList<AnnotationMetaData>();

        for (int i = 0; i < c.getDeclaredFields().length; i++)
        {
            Field field = c.getDeclaredFields()[i];
            for (int j = 0; j < field.getDeclaredAnnotations().length; j++)
            {
                annotations.add(new AnnotationMetaData(c, field, ElementType.FIELD, field.getDeclaredAnnotations()[j]));
            }
        }
        return annotations;
    }

    public static List<AnnotationMetaData> getFieldAnnotations(Class c, Class<? extends Annotation> annotation)
    {
        List<AnnotationMetaData> annotations = new ArrayList<AnnotationMetaData>();

        for (int i = 0; i < c.getDeclaredFields().length; i++)
        {
            Field field = c.getDeclaredFields()[i];
            for (int j = 0; j < field.getDeclaredAnnotations().length; j++)
            {
                if (annotation.equals(field.getDeclaredAnnotations()[j].annotationType()))
                {
                    annotations.add(new AnnotationMetaData(c, field, ElementType.FIELD, field.getDeclaredAnnotations()[j]));
                }
            }
        }
        return annotations;
    }

    public static AnnotationMetaData getClassAnnotationInHeirarchy(Class<? extends Annotation> annotation, Class bottom)
    {
        AnnotationMetaData anno = getClassAnnotationForSuperClasses(annotation, bottom);
        if (anno == null)
        {
            for (int i = 0; i < bottom.getInterfaces().length; i++)
            {
                Class aClass = bottom.getInterfaces()[i];
                if (aClass.isAnnotationPresent(annotation))
                {
                    anno = new AnnotationMetaData(aClass, null, ElementType.TYPE, aClass.getAnnotation(annotation));
                    break;
                }
            }
        }
        return anno;
    }

    protected static AnnotationMetaData getClassAnnotationForSuperClasses(Class<? extends Annotation> annotation, Class bottom)
    {
        if (bottom.isAnnotationPresent(annotation))
        {
            return new AnnotationMetaData(bottom, null, ElementType.TYPE, bottom.getAnnotation(annotation));
        }
        if (bottom.getSuperclass() != null && !bottom.getSuperclass().equals(Object.class))
        {
            return getClassAnnotationForSuperClasses(annotation, bottom.getSuperclass());
        }
        return null;
    }

    public static Set<AnnotationMetaData> getFieldAnnotationsForHeirarchy(Class bottom)
    {
        Set<AnnotationMetaData> annos = new HashSet<AnnotationMetaData>();
        getFieldAnnotationsForSuperClasses(bottom, annos);
        getFieldAnnotationsForInterfaces(bottom, annos);
        return annos;
    }

    public static void getFieldAnnotationsForInterfaces(Class clazz, Set<AnnotationMetaData> annos)
    {
        for (int i = 0; i < clazz.getInterfaces().length; i++)
        {
            Class aClass = clazz.getInterfaces()[i];
            annos.addAll(getAllFieldAnnotations(aClass));
            getFieldAnnotationsForInterfaces(aClass, annos);
        }
    }

    protected static void getFieldAnnotationsForSuperClasses(Class bottom, Set<AnnotationMetaData> annos)
    {
        annos.addAll(getAllFieldAnnotations(bottom));

        if (bottom.getSuperclass() != null && !bottom.getSuperclass().equals(Object.class))
        {
            getFieldAnnotationsForSuperClasses(bottom.getSuperclass(), annos);
        }
    }

    
    public static Set<AnnotationMetaData> getFieldAnnotationsForHeirarchy(Class bottom, Class<? extends Annotation> annotation)
    {
        Set<AnnotationMetaData> annos = new HashSet<AnnotationMetaData>();
        getFieldAnnotationsForSuperClasses(bottom, annos, annotation);
        getFieldAnnotationsForInterfaces(bottom, annos, annotation);
        return annos;
    }

    public static void getFieldAnnotationsForInterfaces(Class clazz, Set<AnnotationMetaData> annos, Class<? extends Annotation> annotation)
    {
        for (int i = 0; i < clazz.getInterfaces().length; i++)
        {
            Class aClass = clazz.getInterfaces()[i];
            annos.addAll(getFieldAnnotations(aClass, annotation));
            getFieldAnnotationsForInterfaces(aClass, annos, annotation);
        }
    }

    protected static void getFieldAnnotationsForSuperClasses(Class bottom, Set<AnnotationMetaData> annos, Class<? extends Annotation> annotation)
    {
        annos.addAll(getFieldAnnotations(bottom, annotation));

        if (bottom.getSuperclass() != null && !bottom.getSuperclass().equals(Object.class))
        {
            getFieldAnnotationsForSuperClasses(bottom.getSuperclass(), annos, annotation);
        }
    }

}
