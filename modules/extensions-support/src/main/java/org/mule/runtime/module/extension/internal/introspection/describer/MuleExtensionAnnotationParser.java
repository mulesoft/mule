/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.introspection.describer;

import static java.util.Arrays.stream;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.mule.metadata.java.api.utils.JavaTypeUtils.getType;
import static org.mule.runtime.core.util.Preconditions.checkState;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getFieldMetadataType;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getMethodArgumentTypes;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.isParameterContainer;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getDefaultValue;
import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.runtime.api.message.MuleMessage;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.util.ClassUtils;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.ParameterGroup;
import org.mule.runtime.extension.api.annotation.RestrictedTo;
import org.mule.runtime.extension.api.annotation.metadata.Content;
import org.mule.runtime.extension.api.annotation.metadata.MetadataKeyId;
import org.mule.runtime.extension.api.annotation.metadata.MetadataKeyPart;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.UseConfig;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Password;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.annotation.param.display.Text;
import org.mule.runtime.extension.api.introspection.declaration.fluent.BaseDeclaration;
import org.mule.runtime.extension.api.introspection.declaration.fluent.HasModelProperties;
import org.mule.runtime.extension.api.introspection.declaration.fluent.ParameterDeclarer;
import org.mule.runtime.extension.api.introspection.property.DisplayModelProperty;
import org.mule.runtime.extension.api.introspection.property.DisplayModelPropertyBuilder;
import org.mule.runtime.extension.api.introspection.property.MetadataContentModelProperty;
import org.mule.runtime.extension.api.introspection.property.MetadataKeyPartModelProperty;
import org.mule.runtime.module.extension.internal.model.property.ConfigTypeModelProperty;
import org.mule.runtime.module.extension.internal.model.property.ConnectionTypeModelProperty;
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
            .add(MuleMessage.class)
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

        if (isEmpty(paramNames))
        {
            return ImmutableList.of();
        }

        MetadataType[] parameterTypes = getMethodArgumentTypes(method, typeLoader);
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();

        List<ParsedParameter> parsedParameters = new LinkedList<>();
        for (int i = 0; i < paramNames.size(); i++)
        {
            Map<Class<? extends Annotation>, Annotation> annotations = toMap(parameterAnnotations[i]);
            if (isParameterContainer(annotations.keySet(), parameterTypes[i]))
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

    public static <T extends Annotation> List<T> parseRepeatableAnnotation(Class<?> extensionType, Class<T> annotation,
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
        stream(getType(parameterType).getDeclaredFields())
                .filter(MuleExtensionAnnotationParser::isParameterOrParameterContainer)
                .forEach(field ->
                         {
                             final Map<Class<? extends Annotation>, Annotation> annotations = toMap(field.getAnnotations());
                             final MetadataType fieldType = typeLoader.load(field.getType());

                             if (isParameterContainer(annotations.keySet(), fieldType))
                             {
                                 parseGroupParameters(getFieldMetadataType(field, typeLoader), parsedParameters, typeLoader);
                             }
                             else
                             {
                                 parsedParameters.add(doParseParameter(field.getName(), fieldType, annotations, typeLoader.getClassLoader()));
                             }
                         });
    }

    private static boolean isParameterOrParameterContainer(Field paramField)
    {
        return paramField.isAnnotationPresent(ParameterGroup.class) || paramField.isAnnotationPresent(MetadataKeyId.class)
               || paramField.isAnnotationPresent(Parameter.class) || paramField.isAnnotationPresent(MetadataKeyPart.class);
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
        return !(IMPLICIT_ARGUMENT_TYPES.contains(getType(parameterType, classLoader)) ||
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

    static DisplayModelProperty parseDisplayAnnotations(AnnotatedElement annotatedElement, String name)
    {
        return parseDisplayAnnotations(annotatedElement, name, DisplayModelPropertyBuilder.create());
    }

    static DisplayModelProperty parseDisplayAnnotations(AnnotatedElement annotatedElement, String name, DisplayModelPropertyBuilder builder)
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
     * Enriches the {@link ParameterDeclarer} with a {@link MetadataKeyPartModelProperty} or a {@link MetadataContentModelProperty} if the parsedParameter is
     * annotated either as {@link MetadataKeyId}, {@link MetadataKeyPart} or {@link Content} respectibly.
     *
     * @param element                    the method annotated parameter parsed
     * @param elementWithModelProperties the {@link ParameterDeclarer} associated to the parsed parameter
     */
    public static void parseMetadataAnnotations(AnnotatedElement element, HasModelProperties elementWithModelProperties)
    {
        if (element.isAnnotationPresent(Content.class))
        {
            elementWithModelProperties.withModelProperty(new MetadataContentModelProperty());
        }

        if (element.isAnnotationPresent(MetadataKeyId.class))
        {
            elementWithModelProperties.withModelProperty(new MetadataKeyPartModelProperty(1));
        }

        if (element.isAnnotationPresent(MetadataKeyPart.class))
        {
            MetadataKeyPart metadataKeyPart = element.getAnnotation(MetadataKeyPart.class);
            elementWithModelProperties.withModelProperty(new MetadataKeyPartModelProperty(metadataKeyPart.order()));
        }
    }

    private static boolean isParameterFieldContainer(Set<Class<? extends Annotation>> annotations, MetadataType parameterType)
    {
        return (annotations.contains(ParameterGroup.class) || annotations.contains(MetadataKeyId.class)) && parameterType instanceof ObjectType;
    }

    static void addConnectionTypeModelProperty(MetadataType annotatedFieldClass, HasModelProperties parameter)
    {
        parameter.withModelProperty(new ConnectionTypeModelProperty(annotatedFieldClass));
    }

    static void addConfigTypeModelProperty(MetadataType annotatedFieldClass, HasModelProperties parameter)
    {
        parameter.withModelProperty(new ConfigTypeModelProperty(annotatedFieldClass));
    }
}
