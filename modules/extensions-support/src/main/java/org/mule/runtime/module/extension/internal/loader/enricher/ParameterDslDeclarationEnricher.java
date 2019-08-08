/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.enricher;

import static org.mule.runtime.extension.api.dsl.syntax.DslSyntaxUtils.supportsInlineDeclaration;
import static org.mule.runtime.extension.api.dsl.syntax.DslSyntaxUtils.typeRequiresWrapperElement;
import static org.mule.runtime.extension.api.loader.DeclarationEnricherPhase.STRUCTURE;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.isMap;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.isReferableType;

import org.mule.metadata.api.model.AnyType;
import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.model.StringType;
import org.mule.metadata.api.model.UnionType;
import org.mule.metadata.api.visitor.MetadataTypeVisitor;
import org.mule.runtime.api.meta.model.ParameterDslConfiguration;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterGroupDeclaration;
import org.mule.runtime.api.meta.model.parameter.ParameterRole;
import org.mule.runtime.api.meta.type.TypeCatalog;
import org.mule.runtime.extension.api.declaration.fluent.util.IdempotentDeclarationWalker;
import org.mule.runtime.extension.api.loader.DeclarationEnricher;
import org.mule.runtime.extension.api.loader.DeclarationEnricherPhase;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.api.property.InfrastructureParameterModelProperty;

/**
 * Enhances the declaration of the {@link ParameterDslConfiguration} taking into account
 * the type of the parameter as well as the context in which the type is being used.
 *
 * @since 4.1.3, 4.2.0
 */
public class ParameterDslDeclarationEnricher implements DeclarationEnricher {

  /**
   * This has to run in the same phase as {@link SubTypesDeclarationEnricher}.
   */
  @Override
  public DeclarationEnricherPhase getExecutionPhase() {
    return STRUCTURE;
  }

  @Override
  public void enrich(ExtensionLoadingContext extensionLoadingContext) {
    new EnricherDelegate().apply(extensionLoadingContext);
  }


  private static class EnricherDelegate {

    private ExtensionDeclaration extensionDeclaration;

    public void apply(ExtensionLoadingContext extensionLoadingContext) {
      extensionDeclaration = extensionLoadingContext.getExtensionDeclarer().getDeclaration();
      TypeCatalog typeCatalog = extensionLoadingContext.getDslResolvingContext().getTypeCatalog();
      new IdempotentDeclarationWalker() {

        @Override
        protected void onParameter(ParameterGroupDeclaration parameterGroup, ParameterDeclaration declaration) {

          ParameterDslConfiguration.Builder builder = ParameterDslConfiguration.builder();
          boolean isContent = !declaration.getRole().equals(ParameterRole.BEHAVIOUR);
          ParameterDslConfiguration dslConfiguration = declaration.getDslConfiguration();
          declaration.getType().accept(new MetadataTypeVisitor() {

            @Override
            protected void defaultVisit(MetadataType metadataType) {
              builder.allowsInlineDefinition(dslConfiguration.allowsInlineDefinition() && isContent)
                  .allowTopLevelDefinition(false)
                  .allowsReferences(false);
            }

            @Override
            public void visitString(StringType stringType) {
              boolean isText = declaration.getLayoutModel() != null && declaration.getLayoutModel().isText();
              builder.allowsInlineDefinition(dslConfiguration.allowsInlineDefinition() && (isText || isContent))
                  .allowTopLevelDefinition(false)
                  .allowsReferences(false);
            }

            @Override
            public void visitArrayType(ArrayType arrayType) {
              MetadataType genericType = arrayType.getType();
              boolean supportsInline = supportsInlineDeclaration(arrayType, declaration.getExpressionSupport(),
                                                                 dslConfiguration, isContent);
              boolean isWrapped = allowsInlineAsWrappedType(genericType, typeCatalog);

              builder.allowsInlineDefinition(dslConfiguration.allowsInlineDefinition() && (supportsInline || isWrapped))
                  .allowTopLevelDefinition(dslConfiguration.allowTopLevelDefinition())
                  .allowsReferences(dslConfiguration.allowsReferences());
            }

            @Override
            public void visitAnyType(AnyType anyType) {
              if (isReferableType(anyType)) {
                builder.allowsReferences(dslConfiguration.allowsReferences())
                    .allowTopLevelDefinition(false)
                    .allowsInlineDefinition(false);
              } else {
                defaultVisit(anyType);
              }
            }

            @Override
            public void visitObject(ObjectType objectType) {
              if (isMap(objectType)) {
                builder.allowsInlineDefinition(dslConfiguration.allowsInlineDefinition());

              } else if (!declaration.getModelProperty(InfrastructureParameterModelProperty.class).isPresent()) {

                boolean supportsInline = supportsInlineDeclaration(objectType, declaration.getExpressionSupport(),
                                                                   dslConfiguration, isContent);

                boolean isWrapped = allowsInlineAsWrappedType(objectType, typeCatalog);

                builder.allowsInlineDefinition(dslConfiguration.allowsInlineDefinition() &&
                    (supportsInline || isWrapped));
              }

              builder.allowTopLevelDefinition(dslConfiguration.allowTopLevelDefinition())
                  .allowsReferences(dslConfiguration.allowsReferences());
            }

            @Override
            public void visitUnion(UnionType unionType) {
              unionType.getTypes().forEach(type -> type.accept(this));
            }

          });

          declaration.setDslConfiguration(builder.build());
        }
      }.walk(extensionDeclaration);

    }

    boolean allowsInlineAsWrappedType(MetadataType type, TypeCatalog typeCatalog) {
      return extensionDeclaration.getSubTypes().stream().anyMatch(s -> s.getBaseType().equals(type))
          || typeRequiresWrapperElement(type, typeCatalog);
    }

  }

}
