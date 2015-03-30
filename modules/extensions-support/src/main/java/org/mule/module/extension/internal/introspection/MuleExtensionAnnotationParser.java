/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.introspection;

import static org.mule.util.Preconditions.checkArgument;
import static org.mule.util.Preconditions.checkState;
import static org.reflections.ReflectionUtils.getAllFields;
import static org.reflections.ReflectionUtils.getAllMethods;
import static org.reflections.ReflectionUtils.withAnnotation;
import static org.reflections.ReflectionUtils.withModifier;
import static org.reflections.ReflectionUtils.withName;
import static org.reflections.ReflectionUtils.withParameters;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.extension.annotations.Extension;
import org.mule.extension.annotations.RestrictedTo;
import org.mule.extension.annotations.param.Optional;
import org.mule.extension.annotations.param.Payload;
import org.mule.extension.introspection.DataQualifier;
import org.mule.extension.introspection.DataType;
import org.mule.extension.introspection.Operation;
import org.mule.extension.introspection.Parameter;
import org.mule.module.extension.internal.util.IntrospectionUtils;
import org.mule.util.ParamReader;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

/**
 * Utilities for reading annotations as a mean to
 * describe extensions
 *
 * @since 3.7.0
 */
public final class MuleExtensionAnnotationParser
{

    private static final Set<Class<?>> IMPLICIT_ARGUMENT_TYPES = ImmutableSet.<Class<?>>builder()
            .add(MuleEvent.class)
            .add(MuleMessage.class)
            .build();

    static Extension getExtension(Class<?> extensionType)
    {
        Extension extension = extensionType.getAnnotation(Extension.class);
        checkState(extension != null, String.format("%s is not a Mule extension since it's not annotated with %s",
                                                    extensionType.getName(), Extension.class.getName()));

        return extension;
    }

    public static Collection<Field> getParameterFields(Class<?> extensionType)
    {
        return getAllFields(extensionType, withAnnotation(org.mule.extension.annotations.Parameter.class));
    }

    public static Collection<Field> getParameterGroupFields(Class<?> extensionType)
    {
        return getAllFields(extensionType, withAnnotation(org.mule.extension.annotations.ParameterGroup.class));
    }

    static Collection<Method> getOperationMethods(Class<?> declaringClass)
    {
        return getAllMethods(declaringClass, withAnnotation(org.mule.extension.annotations.Operation.class), withModifier(Modifier.PUBLIC));
    }

    public static Method getOperationMethod(Class<?> declaringClass, Operation operation)
    {
        Class<?>[] parameterTypes;
        if (operation.getParameters().isEmpty())
        {
            parameterTypes = ArrayUtils.EMPTY_CLASS_ARRAY;
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

    static List<ParameterDescriptor> parseParameters(Method method)
    {
        String[] paramNames = getParamNames(method);

        if (ArrayUtils.isEmpty(paramNames))
        {
            return ImmutableList.of();
        }

        DataType[] parameterTypes = IntrospectionUtils.getMethodArgumentTypes(method);
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();

        List<ParameterDescriptor> parameterDescriptors = new LinkedList<>();

        for (int i = 0; i < paramNames.length; i++)
        {
            Map<Class<? extends Annotation>, Annotation> annotations = toMap(parameterAnnotations[i]);

            if (annotations.containsKey(org.mule.extension.annotations.ParameterGroup.class))
            {
                parseGroupParameters(parameterTypes[i], parameterDescriptors);
            }
            else
            {
                ParameterDescriptor parameterDescriptor = doParseParameter(paramNames[i], parameterTypes[i], annotations);
                if (parameterDescriptor != null)
                {
                    parameterDescriptors.add(parameterDescriptor);
                }
            }
        }

        return parameterDescriptors;
    }

    private static void parseGroupParameters(DataType parameterType, List<ParameterDescriptor> parameterDescriptors)
    {
        for (Field field : getParameterFields(parameterType.getRawType()))
        {
            if (field.getAnnotation(org.mule.extension.annotations.ParameterGroup.class) != null)
            {
                parseGroupParameters(DataType.of(field.getType()), parameterDescriptors);
            }
            else
            {
                ParameterDescriptor parameterDescriptor = doParseParameter(field.getName(), DataType.of(field.getType()), toMap(field.getAnnotations()));
                if (parameterDescriptor != null)
                {
                    parameterDescriptors.add(parameterDescriptor);
                }
            }
        }
    }

    private static ParameterDescriptor doParseParameter(String paramName, DataType parameterType, Map<Class<? extends Annotation>, Annotation> annotations)
    {
        if (IMPLICIT_ARGUMENT_TYPES.contains(parameterType.getRawType()))
        {
            return null;
        }

        DataType dataType = parameterType;

        ParameterDescriptor parameter = new ParameterDescriptor();
        parameter.setName(paramName);
        parameter.setType(dataType);

        Optional optional = (Optional) annotations.get(Optional.class);
        if (optional != null)
        {
            parameter.setRequired(false);
            parameter.setDefaultValue(getDefaultValue(optional, dataType));
        }
        else
        {
            parameter.setRequired(true);
        }

        Payload payload = (Payload) annotations.get(Payload.class);
        if (payload != null)
        {
            parameter.setRequired(false);
            parameter.setDefaultValue("#[payload]");
            parameter.setHidden(true);
        }

        RestrictedTo typeRestriction = (RestrictedTo) annotations.get(RestrictedTo.class);
        if (typeRestriction != null)
        {
            parameter.setTypeRestriction(typeRestriction.value());
        }
        return parameter;
    }

    protected static Object getDefaultValue(Optional optional, DataType dataType)
    {
        if (optional == null)
        {
            return null;
        }

        String defaultValue = optional.defaultValue();
        if (DataQualifier.STRING.equals(dataType.getQualifier()))
        {
            return defaultValue;
        }
        else
        {
            return StringUtils.isEmpty(defaultValue) ? null : defaultValue;
        }
    }

    private static void checkParametrizable(DataType type)
    {
        if (IMPLICIT_ARGUMENT_TYPES.contains(type.getRawType()))
        {
            throw new IllegalArgumentException(
                    String.format("Type %s is not allowed as an operation parameter. Use dependency injection instead", type.getRawType().getName()));
        }
    }

    public static String[] getParamNames(Method method)
    {
        String[] paramNames;
        try
        {
            paramNames = new ParamReader(method.getDeclaringClass()).getParameterNames(method);
        }
        catch (IOException e)
        {
            throw new IllegalStateException(
                    String.format("Could not read parameter names from method %s of class %s", method.getName(), method.getDeclaringClass().getName())
                    , e);
        }

        return paramNames;
    }

    public static Map<Class<? extends Annotation>, Annotation> toMap(Annotation[] annotations)
    {

        Map<Class<? extends Annotation>, Annotation> map = new HashMap<>();

        for (Annotation annotation : annotations)
        {
            map.put(resolveAnnotationClass(annotation), annotation);
        }

        return map;
    }

    private static Class<? extends Annotation> resolveAnnotationClass(Annotation annotation)
    {
        if (Proxy.isProxyClass(annotation.getClass()))
        {
            return (Class<Annotation>) annotation.getClass().getInterfaces()[0];
        }
        else
        {
            return annotation.getClass();
        }
    }


}
