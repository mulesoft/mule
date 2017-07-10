/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.enricher;

import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.mule.runtime.api.meta.model.parameter.ElementReference.ElementType.CONFIG;
import static org.mule.runtime.api.meta.model.parameter.ElementReference.ElementType.FLOW;

import org.mule.runtime.api.meta.model.declaration.fluent.ParameterDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterGroupDeclaration;
import org.mule.runtime.api.meta.model.parameter.ElementReference;
import org.mule.runtime.extension.api.annotation.ConfigReference;
import org.mule.runtime.extension.api.annotation.ElementReferences;
import org.mule.runtime.extension.api.annotation.FlowReference;
import org.mule.runtime.extension.api.declaration.fluent.util.IdempotentDeclarationWalker;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.module.extension.internal.loader.java.property.DeclaringMemberModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ImplementingParameterModelProperty;
import java.lang.reflect.AnnotatedElement;
import java.util.List;

/**
 * Enriches the {@link ParameterDeclaration}s of an extension model with a {@link List} of {@link ElementReferences} if they
 * are marked as a reference to at least some element.
 *
 * @since 4.0
 */
public final class ElementReferenceDeclarionEnricher extends AbstractAnnotatedDeclarationEnricher {

  /**
   * {@inheritDoc}
   * <p>
   * Checks all the declared parameters if someone is annotated with {@link ElementReferences} to create the references and set
   * them up.
   */
  @Override
  public void enrich(ExtensionLoadingContext extensionLoadingContext) {
    new IdempotentDeclarationWalker() {

      @Override
      protected void onParameter(ParameterGroupDeclaration parameterGroup, ParameterDeclaration declaration) {
        declaration.getModelProperty(ImplementingParameterModelProperty.class)
            .ifPresent(param -> declaration.setElementReferences(getReferences(param.getParameter())));
        declaration.getModelProperty(DeclaringMemberModelProperty.class)
            .ifPresent(field -> declaration.setElementReferences(getReferences(field.getDeclaringField())));
      }
    }.walk(extensionLoadingContext.getExtensionDeclarer().getDeclaration());
  }

  private List<ElementReference> getReferences(AnnotatedElement element) {
    ElementReferences references = element.getAnnotation(ElementReferences.class);
    if (references != null) {
      return stream(references.value()).map(ref -> new ElementReference(ref.namespace(), ref.name(), CONFIG)).collect(toList());
    }
    ConfigReference ref = element.getAnnotation(ConfigReference.class);
    if (ref != null) {
      return singletonList(new ElementReference(ref.namespace(), ref.name(), CONFIG));
    }
    FlowReference flowRef = element.getAnnotation(FlowReference.class);
    if (flowRef != null) {
      return singletonList(new ElementReference("mule", "flow", FLOW));
    }
    return emptyList();
  }
}
