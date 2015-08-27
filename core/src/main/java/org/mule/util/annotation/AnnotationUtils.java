/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util.annotation;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
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

    @Deprecated
    public static List<AnnotationMetaData> getParamAnnotations(Method method)
    {
        return getParamAnnotationsWithMeta(method, null);
    }

    public static List<AnnotationMetaData> getParamAnnotationsWithMeta(Method method, Class<? extends Annotation> metaAnnotation)
    {
        List<AnnotationMetaData> annos = new ArrayList<AnnotationMetaData>();

        for (int i = 0; i < method.getParameterAnnotations().length; i++)
        {
            for (int j = 0; j < method.getParameterAnnotations()[i].length; j++)
            {
                Annotation annotation = method.getParameterAnnotations()[i][j];
                if(metaAnnotation==null || annotation.annotationType().isAnnotationPresent(metaAnnotation))
                {
                    annos.add(new AnnotationMetaData(method.getDeclaringClass(), method, ElementType.PARAMETER, annotation));
                }
            }
        }
        return annos;
    }

    @Deprecated
    public static List<AnnotationMetaData> getClassAndMethodAnnotations(Class<?> c)
    {
        List<AnnotationMetaData> annotations = new ArrayList<AnnotationMetaData>();

        for (int i = 0; i < c.getAnnotations().length; i++)
        {
            annotations.add(new AnnotationMetaData(c, null, ElementType.TYPE, c.getAnnotations()[i]));
        }

        annotations.addAll(getAllMethodAnnotations(c));
        return annotations;
    }

    @Deprecated
    public static List<AnnotationMetaData> getAllMethodAnnotations(Class<?> c)
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

    public static List<AnnotationMetaData> getMethodAnnotations(Class<?> c, Class<? extends Annotation> ann)
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

    @Deprecated
    public static List<AnnotationMetaData> getMethodMetaAnnotations(Class<?> c, Class<? extends Annotation> metaAnn)
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

    @Deprecated
    public static List<AnnotationMetaData> getAllFieldAnnotations(Class<?> c)
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

    @Deprecated
    public static List<AnnotationMetaData> getFieldAnnotations(Class<?> c, Class<? extends Annotation> annotation)
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

    /**
     * @deprecated use getClassAnnotationInHierarchy(Class, Class)
     */
    @Deprecated
    public static AnnotationMetaData getClassAnnotationInHeirarchy(Class<? extends Annotation> annotation, Class<?> bottom)
    {
        return getClassAnnotationInHierarchy(annotation, bottom);
    }

    @Deprecated
    public static AnnotationMetaData getClassAnnotationInHierarchy(Class<? extends Annotation> annotation, Class<?> bottom)
    {
        AnnotationMetaData anno = getClassAnnotationForSuperClasses(annotation, bottom);
        if (anno == null)
        {
            for (int i = 0; i < bottom.getInterfaces().length; i++)
            {
                Class<?> aClass = bottom.getInterfaces()[i];
                if (aClass.isAnnotationPresent(annotation))
                {
                    anno = new AnnotationMetaData(aClass, null, ElementType.TYPE, aClass.getAnnotation(annotation));
                    break;
                }
            }
        }
        return anno;
    }

    /**
     * @deprecated use getClassAnnotationInHierarchy(Class)
     */
    @Deprecated
    public static List<AnnotationMetaData> getClassAnnotationInHeirarchy(Class<?> bottom)
    {
        return getClassAnnotationInHierarchy(bottom);
    }

    @Deprecated
    public static List<AnnotationMetaData> getClassAnnotationInHierarchy(Class<?> bottom)
    {
        List<AnnotationMetaData> annos = new ArrayList<AnnotationMetaData>();

        getClassAnnotationForSuperClasses(bottom, annos);
        getClassAnnotationForInterfaces(bottom, annos);

        return annos;
    }

    protected static AnnotationMetaData getClassAnnotationForSuperClasses(Class<? extends Annotation> annotation, Class<?> bottom)
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

    protected static void getClassAnnotationForSuperClasses(Class<?> bottom, List<AnnotationMetaData> annos)
    {
        for (Annotation annotation : bottom.getAnnotations())
        {
            annos.add(new AnnotationMetaData(bottom, null, ElementType.TYPE, annotation));
        }

        if (bottom.getSuperclass() != null && !bottom.getSuperclass().equals(Object.class))
        {
            getClassAnnotationForSuperClasses(bottom.getSuperclass(), annos);
        }
    }

    protected static void getClassAnnotationForInterfaces(Class<?> bottom, List<AnnotationMetaData> annos)
    {
        for (Class<?> aClass : bottom.getInterfaces())
        {
            for (Annotation annotation : aClass.getAnnotations())
            {
                annos.add(new AnnotationMetaData(bottom, null, ElementType.TYPE, annotation));
            }
        }
    }

    /**
     * @deprecated use getFieldAnnotationsForHierarchy(Class)
     */
    @Deprecated
    public static Set<AnnotationMetaData> getFieldAnnotationsForHeirarchy(Class<?> bottom)
    {
        return getFieldAnnotationsForHierarchy(bottom);
    }

    @Deprecated
    public static Set<AnnotationMetaData> getFieldAnnotationsForHierarchy(Class<?> bottom)
    {
        Set<AnnotationMetaData> annos = new HashSet<AnnotationMetaData>();
        getFieldAnnotationsForSuperClasses(bottom, annos);
        getFieldAnnotationsForInterfaces(bottom, annos);
        return annos;
    }

    @Deprecated
    public static void getFieldAnnotationsForInterfaces(Class<?> clazz, Set<AnnotationMetaData> annos)
    {
        for (int i = 0; i < clazz.getInterfaces().length; i++)
        {
            Class<?> aClass = clazz.getInterfaces()[i];
            annos.addAll(getAllFieldAnnotations(aClass));
            getFieldAnnotationsForInterfaces(aClass, annos);
        }
    }

    protected static void getFieldAnnotationsForSuperClasses(Class<?> bottom, Set<AnnotationMetaData> annos)
    {
        annos.addAll(getAllFieldAnnotations(bottom));

        if (bottom.getSuperclass() != null && !bottom.getSuperclass().equals(Object.class))
        {
            getFieldAnnotationsForSuperClasses(bottom.getSuperclass(), annos);
        }
    }

    /**
     * @deprecated use getFieldAnnotationsForHierarchy(Class, Class)
     */
    @Deprecated
    public static Set<AnnotationMetaData> getFieldAnnotationsForHeirarchy(Class<?> bottom, Class<? extends Annotation> annotation)
    {
        return getFieldAnnotationsForHierarchy(bottom, annotation);
    }

    @Deprecated
    public static Set<AnnotationMetaData> getFieldAnnotationsForHierarchy(Class<?> bottom, Class<? extends Annotation> annotation)
    {
        Set<AnnotationMetaData> annos = new HashSet<AnnotationMetaData>();
        getFieldAnnotationsForSuperClasses(bottom, annos, annotation);
        getFieldAnnotationsForInterfaces(bottom, annos, annotation);
        return annos;
    }

    @Deprecated
    public static void getFieldAnnotationsForInterfaces(Class<?> clazz, Set<AnnotationMetaData> annos, Class<? extends Annotation> annotation)
    {
        for (int i = 0; i < clazz.getInterfaces().length; i++)
        {
            Class<?> aClass = clazz.getInterfaces()[i];
            annos.addAll(getFieldAnnotations(aClass, annotation));
            getFieldAnnotationsForInterfaces(aClass, annos, annotation);
        }
    }

    protected static void getFieldAnnotationsForSuperClasses(Class<?> bottom, Set<AnnotationMetaData> annos, Class<? extends Annotation> annotation)
    {
        annos.addAll(getFieldAnnotations(bottom, annotation));

        if (bottom.getSuperclass() != null && !bottom.getSuperclass().equals(Object.class))
        {
            getFieldAnnotationsForSuperClasses(bottom.getSuperclass(), annos, annotation);
        }
    }


    @Deprecated
    public static boolean hasAnnotation(Class<? super Annotation> annotation, Class<?> clazz) throws IOException
    {
        for (Annotation anno : clazz.getDeclaredAnnotations())
        {
            if (anno.annotationType().getName().equals(clazz.getName()))
            {
                return true;
            }
        }

        for (Field field : clazz.getDeclaredFields())
        {
            for (Annotation anno : field.getDeclaredAnnotations())
            {
                if (anno.annotationType().getName().equals(clazz.getName()))
                {
                    return true;
                }
            }
        }

        for (Method method : clazz.getDeclaredMethods())
        {
            for (Annotation anno : method.getDeclaredAnnotations())
            {
                if (anno.annotationType().getName().equals(clazz.getName()))
                {
                    return true;
                }
            }
        }

        return false;
    }

    public static boolean hasAnnotationWithPackage(String packageName, Class<?> clazz) throws IOException
    {
        for (Annotation anno : clazz.getDeclaredAnnotations())
        {
            if (anno.annotationType().getPackage().getName().startsWith(packageName))
            {
                return true;
            }
        }

        for (Field field : clazz.getDeclaredFields())
        {
            for (Annotation anno : field.getDeclaredAnnotations())
            {
                if (anno.annotationType().getPackage().getName().startsWith(packageName))
                {
                    return true;
                }
            }
        }

        for (Method method : clazz.getDeclaredMethods())
        {
            for (Annotation anno : method.getDeclaredAnnotations())
            {
                if (anno.annotationType().getPackage().getName().startsWith(packageName))
                {
                    return true;
                }
            }
        }

        return false;
    }

}
