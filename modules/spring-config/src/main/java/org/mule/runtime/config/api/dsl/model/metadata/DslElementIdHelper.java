/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.api.dsl.model.metadata;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mule.runtime.core.api.util.StringUtils.isBlank;
import static org.mule.runtime.internal.dsl.DslConstants.CONFIG_ATTRIBUTE_NAME;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.runtime.api.meta.NamedObject;
import org.mule.runtime.config.api.dsl.model.DslElementModel;
import org.mule.runtime.dsl.api.component.config.ComponentConfiguration;
import org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils;

import java.util.Objects;
import java.util.Optional;

public class DslElementIdHelper {

  static String getSourceElementName(DslElementModel<?> elementModel) {
    return elementModel.getDsl().getPrefix() + ":" +
        getModelName(elementModel.getModel()).orElse(elementModel.getDsl().getElementName()) +
        elementModel.getConfiguration()
            .map(c -> c.getParameters().get("name")).filter(Objects::nonNull)
            .map(n -> "[" + n + "]").orElse("");
  }

  static Optional<String> getModelName(Object model) {
    if (model instanceof NamedObject) {
      return of(((NamedObject) model).getName());
    }

    if (model instanceof ObjectType) {
      return ExtensionMetadataTypeUtils.getId((MetadataType) model);
    }

    return empty();
  }

  static String sourceElementNameFromSimpleValue(DslElementModel<?> element) {
    return getModelName(element.getModel())
        .map(modelName -> isBlank(element.getDsl().getPrefix()) ? modelName : element.getDsl().getPrefix() + ":" + modelName)
        .orElseGet(() -> element.getIdentifier().map(Object::toString).orElse(null));
  }

  static Optional<String> resolveConfigName(DslElementModel<?> elementModel) {
    // TODO Migrate to Stereotypes when config-ref is part of model
    Optional<ComponentConfiguration> configuration = elementModel.getConfiguration();
    if (configuration.isPresent()) {
      String configRef = configuration.get().getParameters().get(CONFIG_ATTRIBUTE_NAME);
      if (!isBlank(configRef)) {
        return of(configRef);
      }
    }
    return empty();
  }

}
