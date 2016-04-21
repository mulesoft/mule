/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.introspection.describer;

import static org.mule.runtime.core.util.Preconditions.checkState;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getFieldMetadataType;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getDefaultValue;
import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.java.utils.JavaTypeUtils;
import org.mule.runtime.api.temporary.MuleMessage;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.util.ClassUtils;
import org.mule.runtime.core.util.CollectionUtils;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.ParameterGroup;
import org.mule.runtime.extension.api.annotation.RestrictedTo;
import org.mule.runtime.extension.api.annotation.metadata.Content;
import org.mule.runtime.extension.api.annotation.metadata.MetadataKeyParam;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.UseConfig;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Password;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.annotation.param.display.Text;
import org.mule.runtime.extension.api.introspection.EnrichableModel;
import org.mule.runtime.extension.api.introspection.declaration.fluent.BaseDeclaration;
import org.mule.runtime.extension.api.introspection.declaration.fluent.HasModelProperties;
import org.mule.runtime.extension.api.introspection.declaration.fluent.ParameterDeclarer;
import org.mule.runtime.extension.api.introspection.property.DisplayModelProperty;
import org.mule.runtime.extension.api.introspection.property.DisplayModelPropertyBuilder;
import org.mule.runtime.extension.api.introspection.property.MetadataModelProperty;
import org.mule.runtime.module.extension.internal.model.property.DeclaringMemberModelProperty;
import org.mule.runtime.module.extension.internal.util.IntrospectionUtils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import java.lang.annotation.Annotation;
import java.lang.annotation.Repeatable;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

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
            .add(org.mule.runtime.api.temporary.MuleMessage.class)
            .add(MuleMessage.class)
            .build();

    static String getAliasName(Field field)
    {
        return IntrospectionUtils.getAliasName(field.getName(), field.getAnnotation(Alias.class));
    }

    public static String getMemberName(BaseDeclaration<?> declaration, String defaultName)
    {
        return declaration.getModelProperty(DeclaringMemberModelProperty.class).map(p -> p.getDeclaringField().getName()).orElse(defaultName);
    }

    public static String getMemberName(EnrichableModel enrichableModel, String defaultName)
    {
        return enrichableModel.getModelProperty(DeclaringMemberModelProperty.class).map(p -> p.getDeclaringField().getName()).orElse(defaultName);
    }

    public static Extension getExtension(Class<?> extensionType)
    {
        Extension extension = extensionType.getAnnotation(Extension.class);
        checkState(extension != null, String.format("%s is not a Mule extension since it's not annotated with %s",
                                                    extensionType.getName(), Extension.class.getName()));

        return extension;
    }

    static List<ParsedParameter> parseParameters(Method method, ClassTypeLoader typeLoader)
    {
        List<String> paramNames = getParamNames(method);

        if (CollectionUtils.isEmpty(paramNames))
        {
            return ImmutableList.of();
        }

        MetadataType[] parameterTypes = IntrospectionUtils.getMethodArgumentTypes(method, typeLoader);
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();

        List<ParsedParameter> parsedParameters = new LinkedList<>();


        for (int i = 0; i < paramNames.size(); i++)
        {
            Map<Class<? extends Annotation>, Annotation> annotations = toMap(parameterAnnotations[i]);

            if (annotations.containsKey(ParameterGroup.class))
            {
                parseGroupParameters(parameterTypes[i], parsedParameters, typeLoader);
            }
            else
            {
                ParsedParameter parsedParameter = doParseParameter(paramNames.get(i), parameterTypes[i], annotations, typeLoader.getClassLoader());
                parsedParameters.add(parsedParameter);
            }
        }

        return parsedParameters;
    }

    static <T extends Annotation> List<T> parseRepeatableAnnotation(Class<?> extensionType, Class<T> annotation,
                                                                    Function<Annotation, T[]> containerConsumer)
    {
        List<T> annotationDeclarations = ImmutableList.of();

        Repeatable repeatableContainer = annotation.getAnnotation(Repeatable.class);
        if (repeatableContainer != null)
        {
            Annotation container = IntrospectionUtils.getAnnotation(extensionType, repeatableContainer.value());
            if (container != null)
            {
                annotationDeclarations = ImmutableList.copyOf(containerConsumer.apply(container));
            }
        }

        T singleDeclaration = IntrospectionUtils.getAnnotation(extensionType, annotation);
        if (singleDeclaration != null)
        {
            annotationDeclarations = ImmutableList.of(singleDeclaration);
        }

        return annotationDeclarations;
    }

    private static void parseGroupParameters(MetadataType parameterType, List<ParsedParameter> parsedParameters, ClassTypeLoader typeLoader)
    {
        for (Field field : IntrospectionUtils.getParameterFields(JavaTypeUtils.getType(parameterType)))
        {
            if (field.getAnnotation(org.mule.runtime.extension.api.annotation.ParameterGroup.class) != null)
            {
                parseGroupParameters(getFieldMetadataType(field, typeLoader), parsedParameters, typeLoader);
            }
            else
            {
                ParsedParameter parsedParameter = doParseParameter(field.getName(),
                                                                   getFieldMetadataType(field, typeLoader),
                                                                   toMap(field.getAnnotations()), typeLoader.getClassLoader());
                if (parsedParameter != null)
                {
                    parsedParameters.add(parsedParameter);
                }
            }
        }
    }

    private static ParsedParameter doParseParameter(String paramName,
                                                    MetadataType metadataType,
                                                    Map<Class<? extends Annotation>, Annotation> annotations, ClassLoader classLoader)
    {
        ParsedParameter parameter = new ParsedParameter(annotations);
        parameter.setAdvertised(shouldAdvertise(metadataType, annotations, classLoader));

        parameter.setName(IntrospectionUtils.getAliasName(paramName, (Alias) annotations.get(Alias.class)));
        parameter.setType(metadataType);

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

    private static boolean shouldAdvertise(MetadataType parameterType, Map<Class<? extends Annotation>, Annotation> annotations, ClassLoader classLoader)
    {
        return !(IMPLICIT_ARGUMENT_TYPES.contains(JavaTypeUtils.getType(parameterType, classLoader)) ||
                 annotations.containsKey(UseConfig.class) ||
                 annotations.containsKey(Connection.class));
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

    private static void parseDisplayAnnotations(AnnotatedElement annotatedElement, DisplayModelPropertyBuilder builder)
    {
        Password passwordAnnotation = annotatedElement.getAnnotation(Password.class);
        if (passwordAnnotation != null)
        {
            builder.withPassword(true);
        }
        Text textAnnotation = annotatedElement.getAnnotation(Text.class);
        if (textAnnotation != null)
        {
            builder.withText(true);
        }
    }

    private static void parsePlacementAnnotation(AnnotatedElement annotatedElement, DisplayModelPropertyBuilder builder)
    {
        Placement placementAnnotation = annotatedElement.getAnnotation(Placement.class);
        if (placementAnnotation != null)
        {
            builder.order(placementAnnotation.order()).
                    groupName(placementAnnotation.group()).
                    tabName(placementAnnotation.tab());
        }
    }

    private static void parseDisplayNameAnnotation(AnnotatedElement annotatedElement, String fieldName, DisplayModelPropertyBuilder builder)
    {
        DisplayName displayNameAnnotation = annotatedElement.getAnnotation(DisplayName.class);
        String displayName = (displayNameAnnotation != null) ? displayNameAnnotation.value() : getFormattedDisplayName(fieldName);
        builder.displayName(displayName);
    }

    private static String getFormattedDisplayName(String fieldName)
    {
        return StringUtils.join(StringUtils.splitByCharacterTypeCamelCase(StringUtils.capitalize(fieldName)), ' ');
    }

    public static DisplayModelProperty parseDisplayAnnotations(AnnotatedElement annotatedElement, String name)
    {
        return parseDisplayAnnotations(annotatedElement, name, DisplayModelPropertyBuilder.create());
    }

    public static DisplayModelProperty parseDisplayAnnotations(AnnotatedElement annotatedElement, String name, DisplayModelPropertyBuilder builder)
    {
        if (isDisplayAnnotationPresent(annotatedElement))
        {
            parseDisplayAnnotations(annotatedElement, builder);
            parsePlacementAnnotation(annotatedElement, builder);
            parseDisplayNameAnnotation(annotatedElement, name, builder);
            return builder.build();
        }
        return null;
    }

    private static boolean isDisplayAnnotationPresent(AnnotatedElement annotatedElement)
    {
        List<Class> displayAnnotations = Arrays.asList(Password.class, Text.class, DisplayName.class, Placement.class);
        return displayAnnotations.stream().anyMatch(annotation -> annotatedElement.getAnnotation(annotation) != null);
    }

    /**
     * Enriches the {@link ParameterDeclarer} with a {@link MetadataModelProperty} if the parsedParameter is
     * annotated either as {@link Content} or {@link MetadataKeyParam}
     *
     * @param parsedParameter the method annotated parameter parsed
     * @param parameter       the {@link ParameterDeclarer} associated to the parsed parameter
     */
    public static void parseMetadataAnnotations(AnnotatedElement parsedParameter, HasModelProperties parameter)
    {
        if (parsedParameter.getAnnotation(Content.class) != null)
        {
            parameter.withModelProperty(new MetadataModelProperty(false, true));
        }
        else if (parsedParameter.getAnnotation(MetadataKeyParam.class) != null)
        {
            parameter.withModelProperty(new MetadataModelProperty(true, false));
        }
    }
}
