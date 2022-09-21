/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.validation;

import static org.mule.runtime.ast.api.ArtifactType.MULE_EXTENSION;
import static org.mule.runtime.ast.api.validation.Validation.Level.ERROR;
import static org.mule.runtime.ast.api.validation.ValidationResultItem.create;
import static org.mule.runtime.module.extension.mule.api.extension.MuleSdkExtensionExtensionModelDeclarer.EXTENSION_CONSTRUCT_NAME;
import static org.mule.runtime.module.extension.mule.internal.dsl.processor.xml.MuleSdkExtensionDslNamespaceInfoProvider.MULE_EXTENSION_DSL_NAMESPACE;
import static org.mule.runtime.module.extension.mule.internal.dsl.processor.xml.MuleSdkExtensionDslNamespaceInfoProvider.MULE_EXTENSION_DSL_NAMESPACE_URI;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import static java.util.Optional.of;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.validation.ArtifactValidation;
import org.mule.runtime.ast.api.validation.ValidationResultItem;

import java.util.List;
import java.util.Optional;

public class ExtensionStructureValidations implements ArtifactValidation {

  private static final ComponentIdentifier EXTENSION_ROOT_IDENTIFIER = ComponentIdentifier.builder()
      .namespace(MULE_EXTENSION_DSL_NAMESPACE)
      .namespaceUri(MULE_EXTENSION_DSL_NAMESPACE_URI)
      .name(EXTENSION_CONSTRUCT_NAME)
      .build();

  @Override
  public String getName() {
    return "Mule Extension has expected root element";
  }

  @Override
  public String getDescription() {
    return "Validates that a Mule Extension has the expected root element.";
  }

  @Override
  public Level getLevel() {
    return ERROR;
  }

  public boolean applicable(ArtifactAst artifactAst) {
    return artifactAst.getArtifactType().equals(MULE_EXTENSION);
  }

  private Optional<ValidationResultItem> validateContainsExtension(ArtifactAst artifactAst) {
    List<ComponentAst> topLevelComponents = artifactAst.topLevelComponents();
    if (topLevelComponents.size() != 1) {
      return of(create(emptyList(), this,
                       format("Expected a single top level component matching identifier [%s]", EXTENSION_ROOT_IDENTIFIER)));
    }

    ComponentAst rootComponent = topLevelComponents.get(0);
    if (!rootComponent.getIdentifier().equals(EXTENSION_ROOT_IDENTIFIER)) {
      return of(create(rootComponent, this,
                       format("Expected a single top level component matching identifier [%s], but got: [%s]",
                              EXTENSION_ROOT_IDENTIFIER, rootComponent.getIdentifier())));
    }

    return empty();
  }

  @Override
  public Optional<ValidationResultItem> validate(ArtifactAst artifact) {
    if (!applicable(artifact)) {
      return empty();
    }

    return validateContainsExtension(artifact);
  }
}
