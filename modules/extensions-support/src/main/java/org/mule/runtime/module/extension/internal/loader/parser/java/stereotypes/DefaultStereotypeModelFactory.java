/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser.java.stereotypes;

import static java.lang.String.format;
import static org.mule.runtime.api.meta.model.stereotype.StereotypeModelBuilder.newStereotype;
import static org.mule.runtime.api.util.FunctionalUtils.computeIfAbsent;
import static org.mule.runtime.core.api.util.StringUtils.isBlank;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypeDefinition.NAMESPACE;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.PROCESSOR;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.SOURCE;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.VALIDATOR_DEFINITION;
import static org.mule.sdk.api.stereotype.MuleStereotypes.VALIDATOR;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.api.meta.model.stereotype.StereotypeModel;
import org.mule.runtime.api.meta.model.stereotype.StereotypeModelBuilder;
import org.mule.runtime.extension.api.declaration.type.DefaultExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.module.extension.internal.loader.parser.StereotypeModelFactory;
import org.mule.sdk.api.stereotype.StereotypeDefinition;

import java.util.HashMap;
import java.util.Map;

public class DefaultStereotypeModelFactory implements StereotypeModelFactory {


  private final Map<StereotypeDefinition, StereotypeModel> stereotypesCache = new HashMap<>();
  private final DslResolvingContext dslResolvingContext;

  private String namespace;
  private StereotypeModel sourceParent;
  private StereotypeModel processorParent;
  private StereotypeModel validatorStereotype;

  private final ClassTypeLoader typeLoader;
  public DefaultStereotypeModelFactory(ExtensionLoadingContext extensionLoadingContext) {
    dslResolvingContext = extensionLoadingContext.getDslResolvingContext();
    this.typeLoader =
        new DefaultExtensionsTypeLoaderFactory().createTypeLoader(extensionLoadingContext.getExtensionClassLoader());
//    resolveDeclaredTypesStereotypes(declaration, namespace);
  }

  @Override
  public StereotypeModel createStereotype(StereotypeDefinition stereotypeDefinition) {
    return createStereotype(stereotypeDefinition, namespace);
  }

  @Override
  public StereotypeModel createStereotype(String name, StereotypeModel parent) {
    return newStereotype(name, namespace).withParent(parent).build();
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

  private StereotypeModel createStereotype(StereotypeDefinition stereotypeDefinition, String namespace) {
    return computeIfAbsent(stereotypesCache, stereotypeDefinition, definition -> {

      if (!isValidStereotype(stereotypeDefinition, namespace)) {
        throw new IllegalModelDefinitionException(format(
            "Stereotype '%s' defines namespace '%s' which doesn't match extension stereotype '%s'. No extension can define "
                + "stereotypes on namespaces other than its own",
            stereotypeDefinition.getName(), stereotypeDefinition.getNamespace(),
            namespace));
      }

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

  private boolean isValidStereotype(StereotypeDefinition stereotypeDefinition, String namespace) {
    if (isBlank(stereotypeDefinition.getNamespace())) {
      return true;
    }

    return namespace.equals(stereotypeDefinition.getNamespace()) || NAMESPACE.equals(stereotypeDefinition.getNamespace());
  }

}
