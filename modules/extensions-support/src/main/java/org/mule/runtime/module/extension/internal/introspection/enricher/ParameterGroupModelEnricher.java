/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.introspection.enricher;

import static java.util.stream.Collectors.toList;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getFieldsWithGetterAndSetters;

import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.introspection.declaration.DescribingContext;
import org.mule.runtime.extension.api.introspection.declaration.fluent.BaseDeclaration;
import org.mule.runtime.extension.api.introspection.declaration.fluent.ConfigurationDeclaration;
import org.mule.runtime.extension.api.introspection.declaration.fluent.ConnectionProviderDeclaration;
import org.mule.runtime.extension.api.introspection.declaration.fluent.ExtensionDeclaration;
import org.mule.runtime.extension.api.introspection.declaration.fluent.SourceDeclaration;
import org.mule.runtime.extension.api.introspection.declaration.fluent.WithSourcesDeclaration;
import org.mule.runtime.extension.api.introspection.declaration.spi.ModelEnricher;
import org.mule.runtime.module.extension.internal.introspection.ParameterGroup;
import org.mule.runtime.module.extension.internal.introspection.describer.model.FieldWrapper;
import org.mule.runtime.module.extension.internal.introspection.describer.model.TypeBasedComponent;
import org.mule.runtime.module.extension.internal.introspection.describer.model.TypeWrapper;
import org.mule.runtime.module.extension.internal.model.property.ImplementingTypeModelProperty;
import org.mule.runtime.module.extension.internal.model.property.ParameterGroupModelProperty;
import org.mule.runtime.module.extension.internal.util.IdempotentDeclarationWalker;

import java.lang.reflect.Field;
import java.util.List;

/**
 * {@link ModelEnricher} implementation that walks through a {@link ExtensionDeclaration} and looks for annotated
 * component parameters with {@link org.mule.runtime.extension.api.annotation.ParameterGroup}
 * <p>
 * The containers of the parameters considered as parameter groups a {@link ParameterGroupModelProperty} will be added
 * describing the structure of this parameter group.
 *
 * @since 4.0
 */
public class ParameterGroupModelEnricher implements ModelEnricher
{

    @Override
    public void enrich(DescribingContext describingContext)
    {
        new IdempotentDeclarationWalker()
        {
            @Override
            public void onSource(WithSourcesDeclaration owner, SourceDeclaration declaration)
            {
                enrich(declaration);
            }

            @Override
            public void onConfiguration(ConfigurationDeclaration declaration)
            {
                enrich(declaration);
            }

            @Override
            protected void onConnectionProvider(ConnectionProviderDeclaration declaration)
            {
                enrich(declaration);
            }
        }.walk(describingContext.getExtensionDeclarer().getDeclaration());
    }

    /**
     * For each {@link org.mule.runtime.extension.api.annotation.ParameterGroup} annotated parameter the
     * {@link ParameterGroupModelProperty} will be added
     *
     * @param baseDeclaration declaration of a Source, Configuration or ConnectionProvider
     */
    private void enrich(BaseDeclaration<?> baseDeclaration)
    {
        baseDeclaration
                .getModelProperty(ImplementingTypeModelProperty.class)
                .ifPresent(implementing ->
                           {
                               final Class<?> type = implementing.getType();
                               final List<FieldWrapper> annotatedFields = new TypeBasedComponent<>(type).getAnnotatedFields(org.mule.runtime.extension.api.annotation.ParameterGroup.class);
                               baseDeclaration.addModelProperty(new ParameterGroupModelProperty(annotatedFields
                                                                                                        .stream()
                                                                                                        .map(this::toParameterGroup)
                                                                                                        .collect(toList())));
                           });
    }

    /**
     * Given a {@link FieldWrapper} representing a {@link Field} based parameter, introspect it and returns the
     * {@link ParameterGroup} of this field.
     *
     * @param fieldWrapper Wrapper of the field based parameter
     * @return A {@link ParameterGroup} representing the structure of the given parameter
     */
    private ParameterGroup toParameterGroup(FieldWrapper fieldWrapper)
    {
        final TypeWrapper<?> paramGroupType = fieldWrapper.getType();
        final ParameterGroup parameterGroup = new ParameterGroup(paramGroupType.getDeclaredClass(), fieldWrapper.getField());
        getParameterGroupFields(paramGroupType).forEach(field -> parameterGroup.addParameter(field.getField()));

        final List<FieldWrapper> parameterGroups = paramGroupType.getAnnotatedFields(org.mule.runtime.extension.api.annotation.ParameterGroup.class);
        if (!parameterGroups.isEmpty())
        {
            parameterGroup.addModelProperty(new ParameterGroupModelProperty(parameterGroups
                                                                                    .stream()
                                                                                    .map(this::toParameterGroup)
                                                                                    .collect(toList())));
        }

        return parameterGroup;
    }

    /**
     * Given a {@link TypeWrapper} representing the type of a parameter group, returns all the fields that are
     * considered as parameters:
     * <p>
     * <ul>
     * <li>The ones that are annotated with {@link Parameter}</li>
     * <li>The ones that have getters and setters</li>
     * </ul>
     * <p>
     * The above conditions are exclusive, so if a field is annotated with {@link Parameter} no field that just have
     * getters and setters will be considered.
     *
     * @param paramGroupType type of the parameter group
     * @return a {@link List} of {@link FieldWrapper} of the parameters in the parameter group
     */
    private List<FieldWrapper> getParameterGroupFields(TypeWrapper<?> paramGroupType)
    {
        List<FieldWrapper> annotatedFields = paramGroupType.getAnnotatedFields(Parameter.class);
        if (annotatedFields.isEmpty())
        {
            annotatedFields = getFieldsWithGetterAndSetters(paramGroupType.getDeclaredClass())
                    .stream()
                    .map(FieldWrapper::new)
                    .collect(toList());
        }
        return annotatedFields;
    }
}
