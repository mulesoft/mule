/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.introspection.validation;

import static java.lang.String.format;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getComponentModelTypeName;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getFieldMetadataType;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getModelName;
import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.model.SimpleType;
import org.mule.metadata.api.visitor.MetadataTypeVisitor;
import org.mule.runtime.extension.api.annotation.param.ExclusiveOptionals;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.api.meta.model.EnrichableModel;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.extension.api.exception.IllegalParameterModelDefinitionException;
import org.mule.runtime.module.extension.internal.introspection.ParameterGroup;
import org.mule.runtime.module.extension.internal.model.property.ParameterGroupModelProperty;
import org.mule.runtime.api.meta.model.util.IdempotentExtensionWalker;

import java.lang.reflect.Field;
import java.util.Set;

/**
 * This validator makes sure that all the {@link ParameterizedModel}s which contains any {@link ParameterGroup} using exclusion
 * complies the following conditions:
 * <p>
 *
 * The {@link ParameterGroup} doesn't have any nested {@link ParameterGroup} that contains exclusive optionals.
 * 
 * It must contain more than one optional parameter on its inside, and those optional parameter's {@link MetadataType} must be
 * either an {@link ObjectType} or a {@link SimpleType}.
 *
 * @since 4.0
 */
public final class ExclusiveParameterModelValidator implements ModelValidator {

  private ClassTypeLoader typeLoader = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader();

  /**
   * {@inheritDoc}
   */
  @Override
  public void validate(ExtensionModel model) throws IllegalModelDefinitionException {
    new IdempotentExtensionWalker() {

      @Override
      protected void onOperation(OperationModel model) {
        validateExclusiveParameterGroups(model);
      }

      @Override
      public void onConfiguration(ConfigurationModel model) {
        validateExclusiveParameterGroups(model);
      }

      @Override
      protected void onConnectionProvider(ConnectionProviderModel model) {
        validateExclusiveParameterGroups(model);
      }

      @Override
      protected void onSource(SourceModel model) {
        validateExclusiveParameterGroups(model);
      }
    }.walk(model);
  }

  /**
   * @param model to be validated
   * @throws IllegalModelDefinitionException if there is a nested {@link ParameterGroup} that has exclusive optionals on its
   *         inside or the parameter {@link MetadataType} is not a {@link SimpleType} nor a {@link ObjectType}
   */
  private void validateExclusiveParameterGroups(EnrichableModel model) throws IllegalModelDefinitionException {

    model.getModelProperty(ParameterGroupModelProperty.class).filter(mp -> mp.hasExclusiveOptionals()).ifPresent(property -> {
      for (ParameterGroup<?> pg : property.getGroups()) {
        pg.getModelProperty(ParameterGroupModelProperty.class)
            .filter(ParameterGroupModelProperty::hasExclusiveOptionals)
            .ifPresent(nestedParameterGroup -> {
              throw new IllegalModelDefinitionException(format("Parameter group of class '%s' is annotated with '%s' so it cannot contain any nested parameter group with exclusive optionals on its inside.",
                                                               pg.getType().getName(), ExclusiveOptionals.class.getName()));
            });

        Set<Field> optionalParametersFromGroup = pg.getOptionalParameters();

        pg.getModelProperty(ParameterGroupModelProperty.class).ifPresent(nestedParameterGroup -> nestedParameterGroup.getGroups()
            .stream().forEach(g -> ((ParameterGroup<?>) g).getOptionalParameters().stream()
                .forEach(p -> optionalParametersFromGroup.add(p))));

        long optionalParametersCount = optionalParametersFromGroup.stream().count();
        if (optionalParametersCount <= 1) {
          throw new IllegalParameterModelDefinitionException(format("In %s '%s', parameter group '%s' should contain more than one field marked as optional inside but %d was/were found",
                                                                    getComponentModelTypeName(model), getModelName(model),
                                                                    pg.getType().getName(), optionalParametersCount));
        }

        optionalParametersFromGroup.forEach(f -> getFieldMetadataType(f, typeLoader).accept(new MetadataTypeVisitor() {

          @Override
          protected void defaultVisit(MetadataType metadataType) {
            throw new IllegalModelDefinitionException(format("In %s '%s', parameter group '%s' uses exclusion and cannot contain any complex field inside but '%s' was found",
                                                             getComponentModelTypeName(model), getModelName(model),
                                                             pg.getType().getName(), f.getType().getName()));
          }

          @Override
          public void visitSimpleType(SimpleType simpleType) {}

          @Override
          public void visitObject(ObjectType objectType) {}
        }));
      }
    });
  }
}
