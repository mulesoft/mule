/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.util;

import static org.apache.commons.lang.StringUtils.EMPTY;
import static org.mule.module.extension.internal.introspection.MuleExtensionAnnotationParser.getMemberName;
import static org.mule.util.Preconditions.checkArgument;
import static org.reflections.ReflectionUtils.getAllFields;
import static org.reflections.ReflectionUtils.getAllMethods;
import static org.reflections.ReflectionUtils.withAnnotation;
import static org.reflections.ReflectionUtils.withModifier;
import static org.reflections.ReflectionUtils.withName;
import static org.reflections.ReflectionUtils.withParameters;
import static org.reflections.ReflectionUtils.withTypeAssignableTo;
import org.mule.api.NestedProcessor;
import org.mule.extension.annotations.param.Ignore;
import org.mule.extension.annotations.param.Optional;
import org.mule.extension.introspection.DataType;
import org.mule.extension.introspection.Operation;
import org.mule.extension.introspection.Parameter;
import org.mule.extension.introspection.declaration.fluent.ParameterDeclaration;
import org.mule.util.ArrayUtils;
import org.mule.util.ClassUtils;
import org.mule.util.CollectionUtils;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.springframework.core.ResolvableType;

/**
 * Set of utility operations to get insights about objects and their operations
 *
 * @since 3.7.0
 */
public class IntrospectionUtils
{

    private IntrospectionUtils()
    {
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

    public static Field getField(Class<?> clazz, Parameter parameter)
    {
        return getField(clazz, getMemberName(parameter, parameter.getName()), parameter.getType().getRawType());
    }

    public static Field getField(Class<?> clazz, ParameterDeclaration parameterDeclaration)
    {
        return getField(clazz, getMemberName(parameterDeclaration, parameterDeclaration.getName()), parameterDeclaration.getType().getRawType());
    }

    public static Field getField(Class<?> clazz, String name, Class<?> type)
    {
        Collection<Field> candidates = getAllFields(clazz, withName(name), withTypeAssignableTo(type));
        return CollectionUtils.isEmpty(candidates) ? null : candidates.iterator().next();
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
        checkInstantiable(declaringClass, true);
    }

    public static void checkInstantiable(Class<?> declaringClass, boolean requireDefaultConstructor)
    {
        checkArgument(declaringClass != null, "declaringClass cannot be null");

        if (requireDefaultConstructor)
        {
            checkArgument(hasDefaultConstructor(declaringClass), String.format("Class %s cannot be instantiated since it doesn't have a default constructor", declaringClass.getName()));
        }

        checkArgument(!declaringClass.isInterface(), String.format("Class %s cannot be instantiated since it's an interface", declaringClass.getName()));
        checkArgument(!Modifier.isAbstract(declaringClass.getModifiers()), String.format("Class %s cannot be instantiated since it's abstract", declaringClass.getName()));
    }

    public static boolean isIgnored(AccessibleObject object)
    {
        return object.getAnnotation(Ignore.class) != null;
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
        org.mule.extension.annotations.Parameter parameter = object.getAnnotation(org.mule.extension.annotations.Parameter.class);
        return parameter != null ? parameter.isDynamic() : true;
    }

    public static boolean isVoid(Method method)
    {
        Class<?> returnType = method.getReturnType();
        return returnType.equals(void.class) || returnType.equals(Void.class);
    }

    public static Collection<Field> getParameterFields(Class<?> extensionType)
    {
        return getAllFields(extensionType, withAnnotation(org.mule.extension.annotations.Parameter.class));
    }

    public static Collection<Field> getParameterGroupFields(Class<?> extensionType)
    {
        return getAllFields(extensionType, withAnnotation(org.mule.extension.annotations.ParameterGroup.class));
    }

    public static Collection<Method> getOperationMethods(Class<?> declaringClass)
    {
        return getAllMethods(declaringClass, withAnnotation(org.mule.extension.annotations.Operation.class), withModifier(Modifier.PUBLIC));
    }

    public static Method getOperationMethod(Class<?> declaringClass, Operation operation)
    {
        Class<?>[] parameterTypes;
        if (operation.getParameters().isEmpty())
        {
            parameterTypes = org.apache.commons.lang.ArrayUtils.EMPTY_CLASS_ARRAY;
        }
        else
        {
            parameterTypes = new Class<?>[operation.getParameters().size()];
            int i = 0;
            for (Parameter parameter : operation.getParameters())
            {
                parameterTypes[i++] = parameter.getType().getRawType();
            }
        }

        Collection<Method> methods = getAllMethods(declaringClass,
                                                   withAnnotation(org.mule.extension.annotations.Operation.class),
                                                   withModifier(Modifier.PUBLIC),
                                                   withName(operation.getName()),
                                                   withParameters(parameterTypes));

        checkArgument(!methods.isEmpty(), String.format("Could not find method %s in class %s", operation.getName(), declaringClass.getName()));
        checkArgument(methods.size() == 1, String.format("More than one matching method was found in class %s for operation %s", declaringClass.getName(), operation.getName()));

        return methods.iterator().next();
    }

    public static String getAlias(Field field)
    {
        org.mule.extension.annotations.Parameter parameter = field.getAnnotation(org.mule.extension.annotations.Parameter.class);
        String alias = parameter != null ? parameter.alias() : EMPTY;
        return StringUtils.isEmpty(alias) ? field.getName() : alias;
    }
}