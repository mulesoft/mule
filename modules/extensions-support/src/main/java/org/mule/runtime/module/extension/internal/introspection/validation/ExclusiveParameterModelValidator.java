/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.introspection.validation;

import static java.lang.String.format;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getFieldMetadataType;
import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.SimpleType;
import org.mule.metadata.api.visitor.MetadataTypeVisitor;
import org.mule.runtime.extension.api.annotation.ExclusiveOptionals;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.introspection.EnrichableModel;
import org.mule.runtime.extension.api.introspection.ExtensionModel;
import org.mule.runtime.extension.api.introspection.config.ConfigurationModel;
import org.mule.runtime.extension.api.introspection.connection.ConnectionProviderModel;
import org.mule.runtime.extension.api.introspection.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.introspection.operation.OperationModel;
import org.mule.runtime.extension.api.introspection.source.SourceModel;
import org.mule.runtime.module.extension.internal.exception.IllegalParameterModelDefinitionException;
import org.mule.runtime.module.extension.internal.introspection.ParameterGroup;
import org.mule.runtime.module.extension.internal.model.property.ParameterGroupModelProperty;
import org.mule.runtime.module.extension.internal.util.IdempotentExtensionWalker;

import java.util.Optional;

/**
 * This validator makes sure that all the {@link OperationModel}, {@link SourceModel}, {@link ConnectionProviderModel}
 * and {@link ConfigurationModel} which contains any {@link ParameterGroup} using exclusion complies the following condition:
 * <p>
 * The class of the {@link ParameterGroup} doesn't contain any nested {@link ParameterGroup} or any other parameter of a complex type.
 *
 * @since 4.0
 */
public final class ExclusiveParameterModelValidator implements ModelValidator
{

    private ClassTypeLoader typeLoader = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader();
    private final static String SOURCE = "source";
    private final static String CONFIG = "configuration";
    private final static String OPERATION = "operation";
    private final static String PROVIDER = "provider";

    /**
     * {@inheritDoc}
     */
    @Override
    public void validate(ExtensionModel model) throws IllegalModelDefinitionException
    {
        new IdempotentExtensionWalker()
        {
            @Override
            protected void onOperation(OperationModel model)
            {
                validateExclusiveParameterGroups(model, OPERATION, model.getName());
            }

            @Override
            public void onConfiguration(ConfigurationModel model)
            {
                validateExclusiveParameterGroups(model, CONFIG, model.getName());
            }

            @Override
            protected void onConnectionProvider(ConnectionProviderModel model)
            {
                validateExclusiveParameterGroups(model, PROVIDER, model.getName());
            }

            @Override
            protected void onSource(SourceModel model)
            {
                validateExclusiveParameterGroups(model, SOURCE, model.getName());
            }
        }.walk(model);
    }

    /**
     * @param model         to be validated
     * @param componentName of the model
     * @throws IllegalModelDefinitionException if there is a nested {@link ParameterGroup} or
     *                                         the parameter {@link MetadataType} is not a {@link SimpleType}
     */
    private void validateExclusiveParameterGroups(EnrichableModel model, String component, String componentName) throws IllegalModelDefinitionException
    {

        model.getModelProperty(ParameterGroupModelProperty.class).filter(mp -> mp.hasExclusion()).ifPresent(property -> {
            for (ParameterGroup pg : property.getGroups())
            {
                Optional<ParameterGroupModelProperty> nestedParameterGroup = pg.getModelProperty(ParameterGroupModelProperty.class);
                if (nestedParameterGroup.isPresent())
                {
                    throw new IllegalModelDefinitionException(format("Parameter group of class '%s' is annotated with '%s' so it cannot contain any nested parameter group on its inside.",
                                                                     pg.getType().getName(), ExclusiveOptionals.class.getName(), componentName));
                }

                long optionalParametersCount = pg.getParameters().stream().filter(p -> p.getAnnotation(org.mule.runtime.extension.api.annotation.param.Optional.class) != null).count();
                if (optionalParametersCount <= 1)
                {
                    throw new IllegalParameterModelDefinitionException(format("In %s '%s', parameter group '%s' should contain more than one field marked as optional inside but %d was/were found", component, componentName, pg.getType().getName(), optionalParametersCount));
                }

                pg.getParameters().stream()
                        .filter(p -> p.getAnnotation(org.mule.runtime.extension.api.annotation.param.Optional.class) != null)
                        .forEach(f -> getFieldMetadataType(f, typeLoader).accept(new MetadataTypeVisitor()
                        {
                            @Override
                            protected void defaultVisit(MetadataType metadataType)
                            {
                                throw new IllegalModelDefinitionException(format("In %s '%s', parameter group '%s' uses exclusion and cannot contain any complex field inside but '%s' was found", component, componentName, pg.getType().getName(), f.getType().getName()));
                            }

                            @Override
                            public void visitSimpleType(SimpleType simpleType)
                            {
                            }
                        }));
            }
        });
    }
}