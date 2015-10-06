/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.introspection;

import static org.mule.module.extension.internal.util.IntrospectionUtils.getFieldDataType;
import static org.mule.module.extension.internal.util.MuleExtensionUtils.getDefaultValue;
import static org.mule.util.Preconditions.checkState;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.extension.annotation.api.Extension;
import org.mule.extension.annotation.api.Parameter;
import org.mule.extension.annotation.api.ParameterGroup;
import org.mule.extension.annotation.api.RestrictedTo;
import org.mule.extension.annotation.api.param.Connection;
import org.mule.extension.annotation.api.param.Optional;
import org.mule.extension.annotation.api.param.UseConfig;
import org.mule.extension.api.introspection.DataType;
import org.mule.extension.api.introspection.EnrichableModel;
import org.mule.extension.api.introspection.declaration.fluent.BaseDeclaration;
import org.mule.extension.api.runtime.ContentType;
import org.mule.module.extension.internal.model.property.MemberNameModelProperty;
import org.mule.module.extension.internal.util.IntrospectionUtils;
import org.mule.util.ClassUtils;
import org.mule.util.CollectionUtils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

/**
 * Utilities for reading annotations as a mean to describe extensions
 *
 * @since 3.7.0
 */
public final class MuleExtensionAnnotationParser
{

    private static final Set<Class<?>> IMPLICIT_ARGUMENT_TYPES = ImmutableSet.<Class<?>>builder()
            .add(MuleEvent.class)
            .add(MuleMessage.class)
            .add(ContentType.class)
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

    public static String getMemberName(BaseDeclaration<?> declaration, String defaultName)
    {
        MemberNameModelProperty memberNameModelProperty = declaration.getModelProperty(MemberNameModelProperty.KEY);
        return memberNameModelProperty != null ? memberNameModelProperty.getName() : defaultName;
    }

    public static String getMemberName(EnrichableModel enrichableModel, String defaultName)
    {
        MemberNameModelProperty memberNameModelProperty = enrichableModel.getModelProperty(MemberNameModelProperty.KEY);
        return memberNameModelProperty != null ? memberNameModelProperty.getName() : defaultName;
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
        List<String> paramNames = getParamNames(method);

        if (CollectionUtils.isEmpty(paramNames))
        {
            return ImmutableList.of();
        }

        DataType[] parameterTypes = IntrospectionUtils.getMethodArgumentTypes(method);
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();

        List<ParameterDescriptor> parameterDescriptors = new LinkedList<>();


        for (int i = 0; i < paramNames.size(); i++)
        {
            Map<Class<? extends Annotation>, Annotation> annotations = toMap(parameterAnnotations[i]);

            if (annotations.containsKey(ParameterGroup.class))
            {
                parseGroupParameters(parameterTypes[i], parameterDescriptors);
            }
            else
            {
                ParameterDescriptor parameterDescriptor = doParseParameter(paramNames.get(i), parameterTypes[i], annotations);
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
            if (field.getAnnotation(org.mule.extension.annotation.api.ParameterGroup.class) != null)
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
        if (shouldAdvertise(parameterType, annotations))
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

        RestrictedTo typeRestriction = (RestrictedTo) annotations.get(RestrictedTo.class);
        if (typeRestriction != null)
        {
            parameter.setTypeRestriction(typeRestriction.value());
        }
        return parameter;
    }

    private static boolean shouldAdvertise(DataType parameterType, Map<Class<? extends Annotation>, Annotation> annotations)
    {
        return IMPLICIT_ARGUMENT_TYPES.contains(parameterType.getRawType()) ||
               annotations.containsKey(UseConfig.class) ||
               annotations.containsKey(Connection.class);
    }

    public static List<String> getParamNames(Method method)
    {
        ImmutableList.Builder<String> paramNames = ImmutableList.builder();
        for (java.lang.reflect.Parameter parameter : method.getParameters())
        {
            paramNames.add(parameter.getName());
        }

        return paramNames.build();
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
