/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.introspection.validation;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.mule.runtime.module.extension.internal.util.ExtensionMetadataTypeUtils.isBasic;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getComponentModelTypeName;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getModelName;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.model.SimpleType;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.meta.model.util.ExtensionWalker;
import org.mule.runtime.core.util.StringUtils;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.exception.IllegalParameterModelDefinitionException;
import org.mule.runtime.module.extension.internal.introspection.ParameterGroupDescriptor;

import java.util.List;
import java.util.Set;

/**
 * This validator makes sure that all the {@link ParameterizedModel}s which contains any {@link ParameterGroupDescriptor} using exclusion
 * complies the following conditions:
 * <p>
 * <p>
 * The {@link ParameterGroupDescriptor} doesn't have any nested {@link ParameterGroupDescriptor} that contains exclusive optionals.
 * <p>
 * It must contain more than one optional parameter on its inside, and those optional parameter's {@link MetadataType} must be
 * either an {@link ObjectType} or a {@link SimpleType}.
 *
 * @since 4.0
 */
public final class ExclusiveParameterModelValidator implements ModelValidator {

  /**
   * {@inheritDoc}
   */
  @Override
  public void validate(ExtensionModel extensionModel) throws IllegalModelDefinitionException {
    new ExtensionWalker() {

      @Override
      public void onParameterGroup(ParameterizedModel owner, ParameterGroupModel model) {
        model.getExclusiveParametersModels().forEach(exclusiveParametersModel -> {

          final Set<String> exclusiveParameterNames = exclusiveParametersModel.getExclusiveParameterNames();
          if (exclusiveParameterNames.isEmpty()) {
            throw new IllegalParameterModelDefinitionException(format(
                                                                      "In %s '%s', parameter group '%s' defines an empty set of exclusive parameters",
                                                                      getComponentModelTypeName(owner),
                                                                      getModelName(owner),
                                                                      model.getName()));
          }

          List<ParameterModel> optionalParameters = model.getParameterModels().stream()
              .filter(p -> exclusiveParameterNames.contains(p.getName()))
              .collect(toList());

          if (optionalParameters.size() < 2) {
            throw new IllegalParameterModelDefinitionException(format(
                                                                      "In %s '%s', parameter group '%s' defines exclusive optional parameters, and thus should contain more than one "
                                                                          + "parameter marked as optional but %d was/were found",
                                                                      getComponentModelTypeName(owner),
                                                                      getModelName(owner),
                                                                      model.getName(),
                                                                      optionalParameters.size()));
          }

          String complexParameters = optionalParameters.stream()
              .filter(p -> !isBasic(p.getType()))
              .map(ParameterModel::getName)
              .collect(joining(","));

          if (!StringUtils.isBlank(complexParameters)) {
            throw new IllegalModelDefinitionException(format(
                                                             "In %s '%s', parameter group '%s' defines exclusive optional parameters and thus cannot contain any complex parameters,"
                                                                 + "but the following were found: [%s]",
                                                             getComponentModelTypeName(owner),
                                                             getModelName(owner),
                                                             model.getName(),
                                                             complexParameters));
          }
        });
      }
    }.walk(extensionModel);
  }
}
