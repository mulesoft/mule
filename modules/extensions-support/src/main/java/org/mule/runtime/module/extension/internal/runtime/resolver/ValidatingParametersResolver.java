/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import static org.mule.runtime.api.util.collection.SmallMap.forSize;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.isFlattenedParameterGroup;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getMemberName;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.checkParameterGroupExclusiveness;

import org.mule.metadata.api.model.ObjectFieldType;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.core.api.Injector;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.api.el.ExtendedExpressionManager;
import org.mule.runtime.module.extension.api.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.api.runtime.resolver.ValueResolver;
import org.mule.runtime.module.extension.internal.loader.java.type.property.ExtensionParameterDescriptorModelProperty;
import org.mule.runtime.module.extension.internal.runtime.exception.RequiredParameterNotSetException;
import org.mule.runtime.module.extension.internal.runtime.objectbuilder.DefaultObjectBuilder;
import org.mule.runtime.module.extension.internal.util.ReflectionCache;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * This supports some scenarios where the validations have to be done at runtime because the validation is done on dynamically
 * fetched data rather that on data provided in the DSL.
 *
 * @since 4.0
 */
final class ValidatingParametersResolver extends ParametersResolver {

  ValidatingParametersResolver(MuleContext muleContext,
                               Injector injector,
                               Map<String, ?> parameters,
                               ReflectionCache reflectionCache,
                               ExtendedExpressionManager expressionManager,
                               String parameterOwner) {
    super(muleContext, injector, parameters, reflectionCache, expressionManager, parameterOwner);
  }

  @Override
  protected ResolverSet getResolverSet(Optional<ParameterizedModel> model, List<ParameterGroupModel> groups,
                                       List<ParameterModel> parameterModels, ResolverSet resolverSet)
      throws ConfigurationException {
    final ResolverSet populatedResolverSet = super.getResolverSet(model, groups, parameterModels, resolverSet);

    Map<String, String> aliasedParameterNames = forSize(parameterModels.size());
    parameterModels
        .stream()
        .filter(p -> !p.isComponentId()
            // This model property exists only for non synthetic parameters, in which case the value resolver has to be created,
            // regardless of the parameter being the componentId
            || p.getModelProperty(ExtensionParameterDescriptorModelProperty.class).isPresent())
        .forEach(p -> {
          final String parameterName = getMemberName(p, p.getName());
          if (!parameterName.equals(p.getName())) {
            aliasedParameterNames.put(parameterName, p.getName());
          }
        });

    checkParameterGroupExclusiveness(model, groups, getParameters(), aliasedParameterNames);
    return populatedResolverSet;
  }

  @Override
  protected void addToResolverSet(ParameterModel paramModel, final ResolverSet resolverSet, ValueResolver<?> resolver) {
    if (paramModel.isRequired() && resolver == null) {
      throw new RequiredParameterNotSetException(paramModel);
    }

    super.addToResolverSet(paramModel, resolverSet, resolver);
  }

  @Override
  protected void addPropertyResolver(DefaultObjectBuilder builder, ValueResolver<?> valueResolver, ObjectFieldType field,
                                     Field objectField) {
    if (field.isRequired() && !isFlattenedParameterGroup(field)) {
      throw new RequiredParameterNotSetException(objectField.getName());
    }

    super.addPropertyResolver(builder, valueResolver, field, objectField);
  }

}
