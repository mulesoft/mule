/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.util;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang.StringUtils.EMPTY;
import static org.mule.extension.api.introspection.ExpressionSupport.SUPPORTED;
import static org.mule.module.extension.internal.introspection.MuleExtensionAnnotationParser.getMemberName;
import static org.mule.util.Preconditions.checkArgument;
import static org.reflections.ReflectionUtils.getAllFields;
import static org.reflections.ReflectionUtils.getAllMethods;
import static org.reflections.ReflectionUtils.withAnnotation;
import static org.reflections.ReflectionUtils.withModifier;
import static org.reflections.ReflectionUtils.withName;
import static org.reflections.ReflectionUtils.withTypeAssignableTo;
import org.mule.api.NestedProcessor;
import org.mule.extension.annotation.api.Alias;
import org.mule.extension.annotation.api.Expression;
import org.mule.extension.annotation.api.Operation;
import org.mule.extension.annotation.api.Parameter;
import org.mule.extension.annotation.api.ParameterGroup;
import org.mule.extension.annotation.api.param.Ignore;
import org.mule.extension.annotation.api.param.Optional;
import org.mule.extension.api.exception.IllegalModelDefinitionException;
import org.mule.extension.api.introspection.DataType;
import org.mule.extension.api.introspection.ExpressionSupport;
import org.mule.extension.api.introspection.OperationModel;
import org.mule.extension.api.introspection.ParameterModel;
import org.mule.extension.api.introspection.declaration.fluent.ParameterDeclaration;
import org.mule.extension.api.runtime.source.Source;
import org.mule.util.ArrayUtils;
import org.mule.util.ClassUtils;
import org.mule.util.CollectionUtils;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ResolvableType;

/**
 * Set of utility operations to get insights about objects and their operations
 *
 * @since 3.7.0
 */
public class IntrospectionUtils
{

    private static final Logger logger = LoggerFactory.getLogger(IntrospectionUtils.class);

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

    public static Field getField(Class<?> clazz, ParameterModel parameterModel)
    {
        return getField(clazz, getMemberName(parameterModel, parameterModel.getName()), parameterModel.getType().getRawType());
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
            return DataType.of(OperationModel.class);
        }
        else if (ArrayUtils.isEmpty(generics))
        {
            return DataType.of(rawClass);
        }
        else
        {
            return DataType.of(rawClass, toDataTypes(generics));
        }
    }

    public static List<Class<?>> getInterfaceGenerics(Class<?> type, Class<?> implementedInterface)
    {
        ResolvableType interfaceType = null;
        Class<?> searchClass = type;

        while (!Object.class.equals(searchClass))
        {
            for (ResolvableType iType : ResolvableType.forClass(searchClass).getInterfaces())
            {
                if (iType.getRawClass().equals(implementedInterface))
                {
                    interfaceType = iType;
                    break;
                }
            }
            searchClass = searchClass.getSuperclass();
        }

        if (interfaceType == null)
        {
            throw new IllegalArgumentException(String.format("Class '%s' does not implement the '%s' interface", type.getName(), implementedInterface.getName()));
        }

        return Arrays.stream(interfaceType.getGenerics()).map(ResolvableType::getRawClass).collect(toList());
    }

    public static List<Class<?>> getSuperClassGenerics(Class<?> type, Class<?> superClass)
    {
        Class<?> searchClass = type;

        while (!Object.class.equals(searchClass))
        {
            if (searchClass.getSuperclass().equals(superClass))
            {
                Type superType = searchClass.getGenericSuperclass();
                if (superType instanceof ParameterizedType)
                {
                    return Arrays.stream(((ParameterizedType) superType).getActualTypeArguments())
                            .map(generic -> (Class<?>) generic)
                            .collect(toList());
                }
            }
            searchClass = searchClass.getSuperclass();
        }

        throw new IllegalArgumentException(String.format("Class '%s' does not extend the '%s' class", type.getName(), superClass.getName()));
    }

    private static boolean isOperation(Class<?> rawClass)
    {
        return NestedProcessor.class.isAssignableFrom(rawClass);
    }

    private static DataType[] toDataTypes(ResolvableType[] resolvableTypes)
    {
        DataType[] types = new DataType[resolvableTypes.length];
        for (int i = 0; i < resolvableTypes.length; i++)
        {
            if (logger.isWarnEnabled() && resolvableTypes[i].getRawClass() == null)
            {
                logger.warn(String.format("WildCard used in extension definition: '%s'", resolvableTypes[i].toString()));
            }
            Class<?> rawClass = resolvableTypes[i].resolve(Object.class);
            if (isOperation(rawClass))
            {
                rawClass = OperationModel.class;
            }

            if (resolvableTypes[i].getGenerics().length > 0)
            {
                types[i] = DataType.of(rawClass, toDataTypes(resolvableTypes[i].getGenerics()));
            }
            else
            {
                types[i] = DataType.of(rawClass);
            }
        }
        return types;
    }

    public static void checkInstantiable(Class<?> declaringClass)
    {
        checkInstantiable(declaringClass, true);
    }

    public static void checkInstantiable(Class<?> declaringClass, boolean requireDefaultConstructor)
    {
        if (!isInstantiable(declaringClass, requireDefaultConstructor))
        {
            throw new IllegalArgumentException(String.format("Class %s cannot be instantiated.", declaringClass));
        }
    }

    public static boolean isInstantiable(Class<?> declaringClass)
    {
        return isInstantiable(declaringClass, true);
    }

    public static boolean isInstantiable(Class<?> declaringClass, boolean requireDefaultConstructor)
    {
        return declaringClass != null
               && (!requireDefaultConstructor || hasDefaultConstructor(declaringClass))
               && !declaringClass.isInterface()
               && !Modifier.isAbstract(declaringClass.getModifiers());
    }

    public static boolean isIgnored(AccessibleObject object)
    {
        return object.getAnnotation(Ignore.class) != null;
    }

    public static boolean isRequired(AccessibleObject object)
    {
        return object.getAnnotation(Optional.class) == null;
    }

    public static boolean isRequired(ParameterModel parameterModel, boolean forceOptional)
    {
        return !forceOptional && parameterModel.isRequired();
    }

    public static ExpressionSupport getExpressionSupport(AnnotatedElement object)
    {
        Expression expression = object.getAnnotation(Expression.class);
        return expression != null ? expression.value() : SUPPORTED;
    }

    public static boolean isVoid(Method method)
    {
        return isVoid(method.getReturnType());
    }

    public static boolean isVoid(OperationModel operationModel)
    {
        return isVoid(operationModel.getReturnType().getRawType());
    }

    private static boolean isVoid(Class<?> type)
    {
        return type.equals(void.class) || type.equals(Void.class);
    }

    public static Collection<Field> getParameterFields(Class<?> extensionType)
    {
        return getAllFields(extensionType, withAnnotation(Parameter.class));
    }

    public static Collection<Field> getParameterGroupFields(Class<?> extensionType)
    {
        return getAllFields(extensionType, withAnnotation(ParameterGroup.class));
    }

    public static Collection<Method> getOperationMethods(Class<?> declaringClass)
    {
        return getAllMethods(declaringClass, withAnnotation(Operation.class), withModifier(Modifier.PUBLIC));
    }

    public static Collection<Field> getExposedFields(Class<?> extensionType)
    {
        Collection<Field> allFields = getParameterFields(extensionType);
        if (!allFields.isEmpty())
        {
            return allFields;
        }
        try
        {
            PropertyDescriptor[] propertyDescriptors = Introspector.getBeanInfo(extensionType).getPropertyDescriptors();
            return Arrays.stream(propertyDescriptors)
                    .filter(p -> getField(extensionType, p.getName(), p.getPropertyType()) != null)
                    .map(p -> getField(extensionType, p.getName(), p.getPropertyType()))
                    .collect(toSet());
        }
        catch (IntrospectionException e)
        {
            throw new IllegalModelDefinitionException("Could not introspect POJO: " + extensionType.getName(), e);
        }
    }

    public static String getAlias(Field field)
    {
        Parameter parameter = field.getAnnotation(Parameter.class);
        String alias = parameter != null ? parameter.alias() : EMPTY;
        return StringUtils.isEmpty(alias) ? field.getName() : alias;
    }

    public static String getSourceName(Class<? extends Source> sourceType)
    {
        Alias alias = sourceType.getAnnotation(Alias.class);
        if (alias != null)
        {
            return alias.value();
        }

        return sourceType.getSimpleName();
    }

}