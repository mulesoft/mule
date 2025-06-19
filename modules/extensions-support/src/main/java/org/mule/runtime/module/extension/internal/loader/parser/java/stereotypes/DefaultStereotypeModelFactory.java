/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser.java.stereotypes;

import static java.util.Optional.ofNullable;
import static org.mule.runtime.api.meta.model.stereotype.StereotypeModelBuilder.newStereotype;
import static org.mule.runtime.api.util.FunctionalUtils.computeIfAbsent;
import static org.mule.runtime.core.api.util.StringUtils.isBlank;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.PROCESSOR;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.SOURCE;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.VALIDATOR_DEFINITION;
import static org.mule.runtime.module.extension.internal.loader.parser.java.stereotypes.JavaStereotypeModelParserUtils.asDefinition;
import static org.mule.sdk.api.stereotype.MuleStereotypes.VALIDATOR;

import org.mule.runtime.api.meta.model.stereotype.StereotypeModel;
import org.mule.runtime.api.meta.model.stereotype.StereotypeModelBuilder;
import org.mule.runtime.extension.api.loader.parser.StereotypeModelFactory;
import org.mule.sdk.api.stereotype.StereotypeDefinition;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Default implementation for {@link StereotypeModelFactory}
 *
 * @since 4.5.0
 */
public class DefaultStereotypeModelFactory implements StereotypeModelFactory {


  private final Map<StereotypeDefinition, StereotypeModel> stereotypesCache = new HashMap<>();

  private String namespace;
  private StereotypeModel sourceParent;
  private StereotypeModel processorParent;
  private StereotypeModel validatorStereotype;

  @Override
  public StereotypeModel createStereotype(StereotypeDefinition stereotypeDefinition) {
    return createStereotype(stereotypeDefinition, namespace);
  }

  @Override
  public StereotypeModel createStereotype(StereotypeDefinition stereotypeDefinition, String namespace) {
    return computeIfAbsent(stereotypesCache, stereotypeDefinition, definition -> {
      String resolvedNamespace = isBlank(stereotypeDefinition.getNamespace()) ? namespace : stereotypeDefinition.getNamespace();
      final StereotypeModelBuilder builder = newStereotype(stereotypeDefinition.getName(), resolvedNamespace);
      stereotypeDefinition.getParent().ifPresent(parent -> {
        String parentNamespace = parent.getNamespace();
        if (isBlank(parentNamespace)) {
          parentNamespace = namespace;
        }
        builder.withParent(createStereotype(parent, parentNamespace));
      });

      return builder.build();
    });
  }

  @Override
  public StereotypeModel createStereotype(String name, StereotypeModel parent) {
    return createStereotype(name, namespace, parent);
  }

  @Override
  public StereotypeModel createStereotype(String name, String namespace, StereotypeModel parent) {
    final String effectiveNamespace = namespace.toUpperCase();
    return createStereotype(new StereotypeDefinition() {

      @Override
      public String getName() {
        return name;
      }

      @Override
      public String getNamespace() {
        return effectiveNamespace;
      }

      @Override
      public Optional<StereotypeDefinition> getParent() {
        return ofNullable(parent).map(p -> asDefinition(p));
      }
    });
  }

  @Override
  public StereotypeModel getProcessorParentStereotype() {
    return processorParent;
  }

  @Override
  public StereotypeModel getSourceParentStereotype() {
    return sourceParent;
  }

  @Override
  public StereotypeModel getValidatorStereotype() {
    return validatorStereotype;
  }

  public void setNamespace(String namespace) {
    this.namespace = namespace;
    processorParent = newStereotype(PROCESSOR.getType(), namespace).withParent(PROCESSOR).build();
    sourceParent = newStereotype(SOURCE.getType(), namespace).withParent(SOURCE).build();
    validatorStereotype = newStereotype(VALIDATOR_DEFINITION.getName(), namespace)
        .withParent(VALIDATOR)
        .build();
  }
}
