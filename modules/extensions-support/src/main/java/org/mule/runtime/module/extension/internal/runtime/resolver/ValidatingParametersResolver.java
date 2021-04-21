/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import static java.lang.String.format;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.collections.CollectionUtils.intersection;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.util.collection.SmallMap.forSize;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.isFlattenedParameterGroup;
import static org.mule.runtime.extension.api.util.NameUtils.getComponentModelTypeName;
import static org.mule.runtime.extension.api.util.NameUtils.getModelName;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getMemberName;

import org.mule.metadata.api.model.ObjectFieldType;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.model.parameter.ExclusiveParametersModel;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.module.extension.internal.loader.java.type.property.ExtensionParameterDescriptorModelProperty;
import org.mule.runtime.module.extension.internal.runtime.ValueResolvingException;
import org.mule.runtime.module.extension.internal.runtime.exception.RequiredParameterNotSetException;
import org.mule.runtime.module.extension.internal.runtime.objectbuilder.DefaultObjectBuilder;
import org.mule.runtime.module.extension.internal.util.ReflectionCache;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import com.google.common.base.Joiner;

/**
 * This supports some scenarios where the validations have to be done at runtime because the validation is done on dynamically
 * fetched data rather that on data provided in the DSL.
 *
 * @since 4.0
 */
final class ValidatingParametersResolver extends ParametersResolver {

  ValidatingParametersResolver(MuleContext muleContext, Map<String, ?> parameters,
                               ReflectionCache reflectionCache, ExpressionManager expressionManager,
                               String parameterOwner) {
    super(muleContext, parameters, reflectionCache, expressionManager, parameterOwner);
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

  public void checkParameterGroupExclusiveness(Optional<ParameterizedModel> model,
                                               List<ParameterGroupModel> groups,
                                               Map<String, ?> parameters, Map<String, String> aliasedParameterNames)
      throws ConfigurationException {
    Set<String> parameterValueResolvers = new HashSet<>();
    Set<String> parameterNames = parameters.entrySet().stream()
        .flatMap(entry -> {
          if (entry.getValue() instanceof ParameterValueResolver) {
            try {
              parameterValueResolvers.add(aliasedParameterNames.getOrDefault(entry.getKey(), entry.getKey()));
              return ((ParameterValueResolver) entry.getValue()).getParameters().keySet()
                  .stream().map(k -> aliasedParameterNames.getOrDefault(k, k));
            } catch (ValueResolvingException e) {
              throw new MuleRuntimeException(e);
            }
          } else {
            String key = entry.getKey();
            aliasedParameterNames.getOrDefault(key, key);
            return Stream.of(key);
          }
        })
        .collect(toSet());
    parameterNames.addAll(parameterValueResolvers);

    for (ParameterGroupModel group : groups) {
      for (ExclusiveParametersModel exclusiveModel : group.getExclusiveParametersModels()) {
        Collection<String> definedExclusiveParameters = intersection(exclusiveModel.getExclusiveParameterNames(), parameterNames);
        if (definedExclusiveParameters.isEmpty() && exclusiveModel.isOneRequired()) {
          throw new ConfigurationException((createStaticMessage(format(
                                                                       "Parameter group '%s' requires that one of its optional parameters should be set but all of them are missing. "
                                                                           + "One of the following should be set: [%s]",
                                                                       group.getName(),
                                                                       Joiner.on(", ")
                                                                           .join(exclusiveModel
                                                                               .getExclusiveParameterNames())))));
        } else if (definedExclusiveParameters.size() > 1) {
          if (model.isPresent()) {
            throw new ConfigurationException(createStaticMessage(format("In %s '%s', the following parameters cannot be set at the same time: [%s]",
                                                                        getComponentModelTypeName(model.get()),
                                                                        getModelName(model.get()),
                                                                        Joiner.on(", ").join(definedExclusiveParameters))));
          } else {
            throw new ConfigurationException(createStaticMessage(format("The following parameters cannot be set at the same time: [%s]",
                                                                        Joiner.on(", ").join(definedExclusiveParameters))));
          }
        }
      }
    }
  }

}
