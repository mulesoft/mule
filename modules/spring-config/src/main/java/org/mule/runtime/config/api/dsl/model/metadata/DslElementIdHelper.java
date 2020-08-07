/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.api.dsl.model.metadata;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mule.runtime.api.functional.Either.left;
import static org.mule.runtime.api.functional.Either.right;
import static org.mule.runtime.core.api.el.ExpressionManager.DEFAULT_EXPRESSION_PREFIX;
import static org.mule.runtime.core.api.util.StringUtils.isBlank;
import static org.mule.runtime.internal.dsl.DslConstants.CONFIG_ATTRIBUTE_NAME;
import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.model.StringType;
import org.mule.metadata.api.visitor.MetadataTypeVisitor;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.functional.Either;
import org.mule.runtime.api.meta.NamedObject;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.config.api.dsl.model.DslElementModel;
import org.mule.runtime.core.internal.locator.ComponentLocator;
import org.mule.runtime.dsl.api.component.config.ComponentConfiguration;
import org.mule.runtime.extension.api.declaration.type.annotation.TypeDslAnnotation;
import org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils;

import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DslElementIdHelper {

  private static final Logger LOGGER = LoggerFactory.getLogger(DslElementIdHelper.class);

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
        .orElseGet(() -> element.getIdentifier().map(Object::toString).orElse(isBlank(element.getDsl().getElementName())
            ? element.getDsl().getAttributeName() : element.getDsl().getElementName()));
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

  static Optional<Either<DslElementModel<?>, String>> resolveSimpleValue(DslElementModel<?> element,
                                                                         ComponentLocator<DslElementModel<?>> locator) {
    if (element == null || !element.getValue().isPresent()) {
      return empty();
    }

    final String value = element.getValue().get();
    final String sourceElementName = sourceElementNameFromSimpleValue(element);

    if (value.contains(DEFAULT_EXPRESSION_PREFIX)) {
      return of(right(value));
    }

    Reference<DslElementModel<?>> reference = new Reference<>();
    if (element.getModel() instanceof ParameterModel) {
      ParameterModel model = (ParameterModel) element.getModel();
      model.getType()
          .accept(new MetadataTypeVisitor() {

            @Override
            public void visitString(StringType stringType) {
              if (!model.getAllowedStereotypes().isEmpty()) {
                getGlobalElement(value, locator).ifPresent(reference::set);
              }
            }

            @Override
            public void visitArrayType(ArrayType arrayType) {
              if (model.getDslConfiguration().allowsReferences()) {
                getGlobalElement(value, locator).ifPresent(reference::set);
              }
            }

            @Override
            public void visitObject(ObjectType objectType) {
              if (model.getDslConfiguration().allowsReferences()) {
                getGlobalElement(value, locator).ifPresent(reference::set);
              }
            }

          });

    } else if (element.getModel() instanceof MetadataType) {
      ((MetadataType) element.getModel()).accept(new MetadataTypeVisitor() {

        @Override
        public void visitArrayType(ArrayType arrayType) {
          getGlobalElement(value, locator).ifPresent(reference::set);
        }

        @Override
        public void visitObject(ObjectType objectType) {
          boolean canBeGlobal = objectType.getAnnotation(TypeDslAnnotation.class)
              .map(TypeDslAnnotation::allowsTopLevelDefinition).orElse(false);

          if (canBeGlobal) {
            getGlobalElement(value, locator).ifPresent(reference::set);
          }
        }

      });
    } else {
      LOGGER.warn(format("Unknown model type '%s' found for element '%s'", String.valueOf(element.getModel()),
                         element.getIdentifier().map(Object::toString).orElse(sourceElementName)));
    }

    return of(reference.get() == null ? right(value) : left(reference.get()));
  }

  static Optional<DslElementModel<?>> getGlobalElement(String name, ComponentLocator<DslElementModel<?>> locator) {
    if (!isBlank(name)) {
      return locator.get(Location.builder().globalName(name).build());
    }
    return empty();
  }

}
