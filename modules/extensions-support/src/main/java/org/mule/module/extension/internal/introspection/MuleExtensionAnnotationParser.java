/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.introspection;

import static org.mule.module.extension.internal.util.CapabilityUtils.getSingleCapability;
import static org.mule.module.extension.internal.util.IntrospectionUtils.getFieldDataType;
import static org.mule.module.extension.internal.util.MuleExtensionUtils.getDefaultValue;
import static org.mule.util.Preconditions.checkState;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.extension.annotations.Extension;
import org.mule.extension.annotations.Parameter;
import org.mule.extension.annotations.RestrictedTo;
import org.mule.extension.annotations.param.UseConfig;
import org.mule.extension.annotations.param.Optional;
import org.mule.extension.annotations.param.Payload;
import org.mule.extension.introspection.Capable;
import org.mule.extension.introspection.DataType;
import org.mule.extension.introspection.declaration.fluent.CapableDeclaration;
import org.mule.module.extension.internal.capability.metadata.MemberNameCapability;
import org.mule.module.extension.internal.util.CapabilityUtils;
import org.mule.module.extension.internal.util.IntrospectionUtils;
import org.mule.util.ClassUtils;
import org.mule.util.ParamReader;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
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

    static String getParameterName(Field field, Parameter parameterAnnotation)
    {
        return getParameterName(field.getName(), parameterAnnotation);
    }

    static String getParameterName(String defaultName, Parameter parameterAnnotation)
    {
        String alias = parameterAnnotation != null ? parameterAnnotation.alias() : null;
        return StringUtils.isEmpty(alias) ? defaultName : alias;
    }

    static String getMemberName(CapableDeclaration<?> capable, String defaultName)
    {
        MemberNameCapability memberNameCapability = CapabilityUtils.getSingleCapability(capable.getCapabilities(), MemberNameCapability.class);
        return memberNameCapability != null ? memberNameCapability.getName() : defaultName;
    }

    public static String getMemberName(Capable capable, String defaultName)
    {
        MemberNameCapability memberNameCapability = getSingleCapability(capable, MemberNameCapability.class);
        return memberNameCapability != null ? memberNameCapability.getName() : defaultName;
    }

    static Extension getExtension(Class<?> extensionType)
    {
        Extension extension = extensionType.getAnnotation(Extension.class);
        checkState(extension != null, String.format("%s is not a Mule extension since it's not annotated with %s",
                                                    extensionType.getName(), Extension.class.getName()));

        return extension;
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
        for (Field field : IntrospectionUtils.getParameterFields(parameterType.getRawType()))
        {
            if (field.getAnnotation(org.mule.extension.annotations.ParameterGroup.class) != null)
            {
                parseGroupParameters(getFieldDataType(field), parameterDescriptors);
            }
            else
            {
                ParameterDescriptor parameterDescriptor = doParseParameter(field.getName(), getFieldDataType(field), toMap(field.getAnnotations()));
                if (parameterDescriptor != null)
                {
                    parameterDescriptors.add(parameterDescriptor);
                }
            }
        }
    }

    private static ParameterDescriptor doParseParameter(String paramName, DataType parameterType, Map<Class<? extends Annotation>, Annotation> annotations)
    {
        if (IMPLICIT_ARGUMENT_TYPES.contains(parameterType.getRawType()) || annotations.containsKey(UseConfig.class))
        {
            return null;
        }

        DataType dataType = parameterType;

        ParameterDescriptor parameter = new ParameterDescriptor();
        parameter.setName(getParameterName(paramName, (Parameter) annotations.get(Parameter.class)));
        parameter.setType(dataType);

        Optional optional = (Optional) annotations.get(Optional.class);
        if (optional != null)
        {
            parameter.setRequired(false);
            parameter.setDefaultValue(getDefaultValue(optional));
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
                    String.format("Could not read parameter names from method '%s' of class '%s'", method.getName(), method.getDeclaringClass().getName())
                    , e);
        }

        return paramNames;
    }

    public static Map<Class<? extends Annotation>, Annotation> toMap(Annotation[] annotations)
    {

        Map<Class<? extends Annotation>, Annotation> map = new HashMap<>();

        for (Annotation annotation : annotations)
        {
            map.put(ClassUtils.resolveAnnotationClass(annotation), annotation);
        }

        return map;
    }


}
