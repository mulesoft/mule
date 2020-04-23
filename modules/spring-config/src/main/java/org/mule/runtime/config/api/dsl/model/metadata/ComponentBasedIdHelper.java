/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.api.dsl.model.metadata;

import static java.util.Optional.empty;
import static org.mule.runtime.internal.dsl.DslConstants.CONFIG_ATTRIBUTE_NAME;

import org.mule.runtime.api.meta.NamedObject;
import org.mule.runtime.api.meta.Typed;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentParameterAst;
import org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils;

import java.util.Optional;

public class ComponentBasedIdHelper {

  static String getSourceElementName(ComponentAst elementModel) {
    return elementModel.getIdentifier().toString()
        + getModelNameAst(elementModel).orElse(elementModel.getComponentId().map(n -> "[" + n + "]").orElse(""));
  }

  static Optional<String> getModelNameAst(ComponentAst component) {
    final Optional<NamedObject> namedObjectModel = component.getModel(NamedObject.class);
    if (namedObjectModel.isPresent()) {
      return namedObjectModel.map(n -> n.getName());
    }

    final Optional<Typed> typedObjectModel = component.getModel(Typed.class);
    if (typedObjectModel.isPresent()) {
      return typedObjectModel.map(t -> ExtensionMetadataTypeUtils.getId(t.getType()).toString());
    }

    return empty();
  }

  static String sourceElementNameFromSimpleValue(ComponentAst element) {
    return getModelNameAst(element)
        .map(modelName -> element.getIdentifier().getNamespace() + ":" + modelName)
        .orElseGet(() -> element.getIdentifier().toString());
  }

  static String sourceElementNameFromSimpleValue(ComponentAst owner, ComponentParameterAst element) {
    return owner.getIdentifier().getNamespace() + ":" + element.getModel().getName();
  }

  static Optional<String> resolveConfigName(ComponentAst elementModel) {
    // TODO MULE-18327 Migrate to Stereotypes when config-ref is part of model
    // There seems to be something missing in the mock model from the unit tests and this fails.
    // return MuleAstUtils.parameterOfType(elementModel, MuleStereotypes.CONFIG)
    // .map(p -> p.getValue().reduce(identity(), v -> v.toString()));
    return elementModel.getRawParameterValue(CONFIG_ATTRIBUTE_NAME);
  }

}
