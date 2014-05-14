/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extensions.internal.util;

import static org.mule.util.Preconditions.checkArgument;
import static org.reflections.ReflectionUtils.getAllFields;
import static org.reflections.ReflectionUtils.getAllMethods;
import static org.reflections.ReflectionUtils.withAnnotation;
import static org.reflections.ReflectionUtils.withModifier;
import static org.reflections.ReflectionUtils.withName;
import static org.reflections.ReflectionUtils.withParameters;
import static org.reflections.ReflectionUtils.withParametersCount;
import static org.reflections.ReflectionUtils.withPrefix;
import static org.reflections.ReflectionUtils.withReturnType;
import org.mule.api.NestedProcessor;
import org.mule.extensions.annotations.param.Ignore;
import org.mule.extensions.annotations.param.Optional;
import org.mule.extensions.introspection.DataType;
import org.mule.extensions.introspection.Operation;
import org.mule.extensions.introspection.Parameter;
import org.mule.repackaged.internal.org.springframework.core.ResolvableType;
import org.mule.util.ArrayUtils;
import org.mule.util.ClassUtils;
import org.mule.util.CollectionUtils;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Set of utility operations to get insights about objects and their operations
 *
 * @since 3.7.0
 */
public class IntrospectionUtils
{

    /**
     * Returns a {@link DataType} representing the
     * given clazz
     */
    public static DataType getClassDataType(Class<?> clazz)
    {
        return toDataType(ResolvableType.forClass(clazz));
    }

    /**
     * Returns a {@link DataType} representing
     * the given {@link java.lang.reflect.Method}'s return type
     *
     * @return a {@link DataType}
     * @throws java.lang.IllegalArgumentException is method is {@code null}
     */
    public static DataType getMethodReturnType(Method method)
    {
        checkArgument(method != null, "Can't introspect a null method");
        return toDataType(ResolvableType.forMethodReturnType(method));
    }

    /**
     * Returns an array of {@link DataType}
     * representing each of the given {@link java.lang.reflect.Method}'s argument
     * types.
     *
     * @param method a not {@code null} {@link java.lang.reflect.Method}
     * @return an array of {@link DataType} matching
     * the method's arguments. If the method doesn't take any, then the array will be empty
     * @throws java.lang.IllegalArgumentException is method is {@code null}
     */
    public static DataType[] getMethodArgumentTypes(Method method)
    {
        checkArgument(method != null, "Can't introspect a null method");
        Class<?>[] parameters = method.getParameterTypes();
        if (ArrayUtils.isEmpty(parameters))
        {
            return new DataType[] {};
        }

        DataType[] types = new DataType[parameters.length];
        for (int i = 0; i < parameters.length; i++)
        {
            ResolvableType type = ResolvableType.forMethodParameter(method, i);
            types[i] = toDataType(type);
        }

        return types;
    }

    /**
     * Returns a {@link DataType} describing
     * the given {@link java.lang.reflect.Field}'s type
     *
     * @param field a not {@code null} {@link java.lang.reflect.Field}
     * @return a {@link DataType} matching the field's type
     * @throws java.lang.IllegalArgumentException if field is {@code null}
     */
    public static DataType getFieldDataType(Field field)
    {
        checkArgument(field != null, "Can't introspect a null field");
        return toDataType(ResolvableType.forField(field));
    }

    public static DataType getMethodDataType(Method method)
    {
        checkArgument(method != null, "Can't introspect a null method");
        return toDataType(ResolvableType.forMethodReturnType(method));
    }

    public static DataType getMethodArgumentDataType(Method method, int argumentIndex)
    {
        checkArgument(method != null, "Can't introspect a null method");
        return toDataType(ResolvableType.forMethodParameter(method, argumentIndex));
    }


    public static Map<Field, DataType> getAnnotatedFieldsDataTypes(Class<?> declaringClass, Class<? extends Annotation> annotationClass)
    {
        Set<Field> fields = getAllFields(declaringClass, withAnnotation(annotationClass));
        if (CollectionUtils.isEmpty(fields))
        {
            return ImmutableMap.of();
        }

        ImmutableMap.Builder<Field, DataType> map = ImmutableMap.builder();
        for (Field field : fields)
        {
            if (isIgnored(field))
            {
                continue;
            }

            map.put(field, getFieldDataType(field));
        }

        return map.build();
    }

    public static Map<Method, DataType> getSettersDataTypes(Class<?> declaringClass)
    {
        Set<Method> setters = getSetters(declaringClass);

        if (CollectionUtils.isEmpty(setters))
        {
            return ImmutableMap.of();
        }

        ImmutableMap.Builder<Method, DataType> map = ImmutableMap.builder();
        for (Method setter : setters)
        {
            if (isIgnored(setter))
            {
                continue;
            }

            map.put(setter, getMethodArgumentDataType(setter, 0));
        }

        return map.build();
    }

    public static Set<Field> getFieldsAnnotatedWith(Class<?> clazz, Class<? extends Annotation> annotation)
    {
        return getAllFields(clazz, withAnnotation(annotation));
    }

    public static Set<Class<?>> getImplementedTypes(Class<?> clazz, Class<?>... types)
    {
        ImmutableSet.Builder<Class<?>> implemented = ImmutableSet.builder();
        for (Class<?> type : types)
        {
            if (type.isAssignableFrom(clazz))
            {
                implemented.add(type);
            }
        }

        return implemented.build();
    }

    public static Set<Method> getSetters(Class<?> clazz)
    {
        return getAllMethods(clazz, withModifier(Modifier.PUBLIC),
                             withPrefix("set"),
                             withParametersCount(1),
                             withReturnType(void.class));
    }

    public static Method getSetter(Class<?> declaringClass, Parameter parameter)
    {
        return getSetter(declaringClass, parameter.getName(), parameter.getType().getRawType());
    }

    public static Method getSetter(Class<?> declaringClass, String attributeName, Class<?> attributeType)
    {
        Set<Method> setters = getAllMethods(declaringClass, withModifier(Modifier.PUBLIC),
                                            withName(NameUtils.getSetterName(attributeName)),
                                            withParametersCount(1),
                                            withParameters(attributeType),
                                            withReturnType(void.class));

        return CollectionUtils.isEmpty(setters) ? null : setters.iterator().next();
    }

    public static boolean hasSetter(Class<?> declaringClass, Parameter parameter)
    {
        return getSetter(declaringClass, parameter) != null;
    }

    public static boolean hasDefaultConstructor(Class<?> clazz)
    {
        return ClassUtils.getConstructor(clazz, new Class[] {}) != null;
    }

    private static DataType toDataType(ResolvableType type)
    {
        Class<?> rawClass = type.getRawClass();
        ResolvableType[] generics = type.getGenerics();

        if (isOperation(rawClass))
        {
            return DataType.of(Operation.class);
        }

        if (List.class.isAssignableFrom(rawClass))
        {
            if (!ArrayUtils.isEmpty(generics) && isOperation(generics[0].getRawClass()))
            {
                return DataType.of(rawClass, Operation.class);
            }
        }

        return DataType.of(rawClass, toRawTypes(generics));
    }

    private static boolean isOperation(Class<?> rawClass)
    {
        return NestedProcessor.class.isAssignableFrom(rawClass);
    }

    private static Class<?>[] toRawTypes(ResolvableType[] resolvableTypes)
    {
        Class<?>[] types = new Class<?>[resolvableTypes.length];
        for (int i = 0; i < resolvableTypes.length; i++)
        {
            types[i] = resolvableTypes[i].getRawClass();
        }

        return types;
    }

    public static void checkInstantiable(Class<?> declaringClass)
    {
        checkArgument(declaringClass != null, "declaringClass cannot be null");
        checkArgument(hasDefaultConstructor(declaringClass),
                      String.format("Class %s must have a default constructor", declaringClass.getName()));
        checkArgument(!declaringClass.isInterface(), String.format("Class %s cannot be instantiated since it's an interface", declaringClass.getName()));
        checkArgument(!Modifier.isAbstract(declaringClass.getModifiers()), String.format("Class %s cannot be instantiated since it's abstract", declaringClass.getName()));
        checkArgument(hasDefaultConstructor(declaringClass), String.format("Class %s cannot be instantiated since it doesn't have a default constructor", declaringClass.getName()));
    }

    public static boolean isIgnored(AccessibleObject object)
    {
        return object == null || object.getAnnotation(Ignore.class) != null;
    }

    public static boolean isRequired(AccessibleObject object)
    {
        return object.getAnnotation(Optional.class) == null;
    }

    public static boolean isRequired(Parameter parameter, boolean forceOptional)
    {
        return !forceOptional && parameter.isRequired();
    }

    public static boolean isDynamic(AccessibleObject object)
    {
        org.mule.extensions.annotations.Parameter parameter = object.getAnnotation(org.mule.extensions.annotations.Parameter.class);
        return parameter != null ? parameter.isDynamic() : true;
    }
}