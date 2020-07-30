/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.model.type.decorator;

import static java.util.Optional.empty;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.extension.api.property.QNameModelProperty;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * {@link QNameModelProperty} are used by the schema generator to place references to the abstract definitions of infrastructure
 * types in the core schema. But when trying to match a DSL element to a model definition with this property, there is a mismatch.
 * <p>
 * Using this decorator allows to strip the {@link QNameModelProperty} from the model that only makes sense for schema generation.
 *
 * @since 4.0
 */
public final class IgnoreCustomQNameParameterModelDecorator extends BaseParameterModelDecorator {

  public static ParameterizedModel ignoreCustomQName(ParameterizedModel model) {

    return new BaseParameterizedModelDecorator(model) {

      @Override
      public List<ParameterModel> getAllParameterModels() {
        return super.getAllParameterModels()
            .stream()
            .map(pm -> ignoreCustomQName(pm))
            .collect(toList());
      }


      @Override
      public List<ParameterGroupModel> getParameterGroupModels() {
        return super.getParameterGroupModels()
            .stream()
            .map(pgm -> ignoreCustomQName(pgm))
            .collect(toList());
      }
    };
  }

  public static ParameterModel unwrap(ParameterModel model) {
    return (model instanceof BaseParameterModelDecorator)
        ? ((BaseParameterModelDecorator) model).getDecorated()
        : model;
  }

  private static ParameterGroupModel ignoreCustomQName(ParameterGroupModel model) {
    return new BaseParameterGroupModelDecorator(model) {

      @Override
      public Optional<ParameterModel> getParameter(String name) {
        return super.getParameter(name)
            .map(pm -> ignoreCustomQName(pm));
      }

      @Override
      public List<ParameterModel> getParameterModels() {
        return super.getParameterModels()
            .stream()
            .map(pm -> ignoreCustomQName(pm))
            .collect(toList());
      }
    };
  }

  private static ParameterModel ignoreCustomQName(ParameterModel model) {
    return new IgnoreCustomQNameParameterModelDecorator(model);
  }

  public IgnoreCustomQNameParameterModelDecorator(ParameterModel decorated) {
    super(decorated);
  }

  @Override
  public Set<ModelProperty> getModelProperties() {
    return super.getModelProperties()
        .stream()
        .filter(mp -> !(mp instanceof QNameModelProperty))
        .collect(toSet());
  }

  @Override
  public <T extends ModelProperty> Optional<T> getModelProperty(Class<T> propertyType) {
    if (QNameModelProperty.class.isAssignableFrom(propertyType)) {
      return empty();
    } else {
      return super.getModelProperty(propertyType);
    }
  }
}
