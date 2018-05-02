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
import static org.mule.runtime.api.meta.model.stereotype.StereotypeModelBuilder.newStereotype;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.CONFIG;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.FLOW;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.OBJECT_STORE;
import static org.mule.runtime.module.extension.internal.loader.enricher.stereotypes.StereotypeResolver.createCustomStereotype;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterGroupDeclaration;
import org.mule.runtime.api.meta.model.stereotype.StereotypeModel;
import org.mule.runtime.extension.api.annotation.ConfigReferences;
import org.mule.runtime.extension.api.annotation.param.reference.ConfigReference;
import org.mule.runtime.extension.api.annotation.param.reference.FlowReference;
import org.mule.runtime.extension.api.annotation.param.reference.ObjectStoreReference;
import org.mule.runtime.extension.api.annotation.param.stereotype.AllowedStereotypes;
import org.mule.runtime.extension.api.declaration.fluent.util.IdempotentDeclarationWalker;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.api.stereotype.StereotypeDefinition;
import org.mule.runtime.module.extension.internal.loader.java.property.DeclaringMemberModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ImplementingParameterModelProperty;

import java.lang.reflect.AnnotatedElement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Enriches the {@link ParameterDeclaration}s of an extension model with a {@link List} of {@link StereotypeModel} if they
 * are marked as a reference to at least some element.
 *
 * @since 4.0
 */
public final class ParameterAllowedStereotypesDeclarionEnricher extends AbstractAnnotatedDeclarationEnricher {

  /**
   * {@inheritDoc}
   * <p>
   * Checks all the declared parameters if someone is annotated with {@link ConfigReferences} to create the references and set
   * them up.
   */
  @Override
  public void enrich(ExtensionLoadingContext extensionLoadingContext) {
    new Enricher().enrich(extensionLoadingContext.getExtensionDeclarer().getDeclaration());
  }

  private static class Enricher {

    private Map<StereotypeDefinition, StereotypeModel> stereotypesCache = new HashMap<>();
    private String defaultNamespace;

    void enrich(ExtensionDeclaration extension) {
      defaultNamespace = extension.getXmlDslModel().getPrefix().toUpperCase();

      new IdempotentDeclarationWalker() {

        @Override
        protected void onParameter(ParameterGroupDeclaration parameterGroup, ParameterDeclaration declaration) {
          declaration.getModelProperty(ImplementingParameterModelProperty.class)
              .ifPresent(param -> declaration.setAllowedStereotypeModels(getStereotypes(param.getParameter())));
          declaration.getModelProperty(DeclaringMemberModelProperty.class)
              .ifPresent(field -> declaration.setAllowedStereotypeModels(getStereotypes(field.getDeclaringField())));
        }
      }.walk(extension);
    }

    private List<StereotypeModel> getStereotypes(AnnotatedElement element) {
      ConfigReferences references = element.getAnnotation(ConfigReferences.class);
      if (references != null) {
        return stream(references.value()).map(ref -> newStereotype(ref.name(), ref.namespace())
            .withParent(CONFIG)
            .build())
            .collect(toList());
      }

      ConfigReference ref = element.getAnnotation(ConfigReference.class);
      if (ref != null) {
        return singletonList(newStereotype(ref.name(), ref.namespace()).withParent(CONFIG).build());
      }

      if (element.getAnnotation(FlowReference.class) != null) {
        return singletonList(FLOW);
      }

      if (element.getAnnotation(ObjectStoreReference.class) != null) {
        return singletonList(OBJECT_STORE);
      }

      AllowedStereotypes allowedStereotypes = element.getAnnotation(AllowedStereotypes.class);
      if (allowedStereotypes != null) {
        return stream(allowedStereotypes.value())
            .map(definition -> createCustomStereotype(definition, defaultNamespace, stereotypesCache))
            .collect(toList());
      }

      return emptyList();
    }

  }
}
