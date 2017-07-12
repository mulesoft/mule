/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.enricher;

import static org.mule.runtime.api.meta.ExpressionSupport.SUPPORTED;
import static org.mule.runtime.api.meta.model.parameter.ParameterGroupModel.DEFAULT_GROUP_NAME;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.getId;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.ADVANCED_TAB_NAME;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.ENCODING_PARAMETER_NAME;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.MIME_TYPE_PARAMETER_NAME;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getMethodReturnType;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getSourceReturnType;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getSuperClassGenerics;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.isVoid;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getImplementingMethod;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getImplementingType;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.annotation.EnumAnnotation;
import org.mule.metadata.api.model.BinaryType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.model.StringType;
import org.mule.metadata.api.visitor.MetadataTypeVisitor;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.OperationDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterGroupDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterizedDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.SourceDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.WithSourcesDeclaration;
import org.mule.runtime.api.meta.model.display.LayoutModel;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.extension.api.declaration.fluent.util.IdempotentDeclarationWalker;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.module.extension.internal.ExtensionProperties;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.List;

/**
 * Enriches operations which return types are {@link InputStream}, {@link String} or {@link Object} by adding two parameters:
 * A {@link ExtensionProperties#MIME_TYPE_PARAMETER_NAME} that allows configuring the mimeType to the output operation payload
 * and a {@link ExtensionProperties#ENCODING_PARAMETER_NAME} that allows configuring the encoding to the output operation payload.
 * <p>
 * Both added attributes are optional without default value and accept expressions.
 *
 * @since 4.0
 */
public final class MimeTypeParametersDeclarationEnricher extends AbstractAnnotatedDeclarationEnricher {

  private final ClassTypeLoader typeLoader = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader();

  @Override
  public void enrich(ExtensionLoadingContext extensionLoadingContext) {
    final ExtensionDeclaration declaration = extensionLoadingContext.getExtensionDeclarer().getDeclaration();
    new IdempotentDeclarationWalker() {

      @Override
      protected void onOperation(OperationDeclaration declaration) {
        getImplementingMethod(declaration).ifPresent(method -> {
          if (!isVoid(method) && acceptsMimeTypeParameters(getMethodReturnType(method, typeLoader))) {
            declareMimeTypeParameters(declaration);
          }
        });
      }

      @Override
      protected void onSource(WithSourcesDeclaration owner, SourceDeclaration declaration) {
        getImplementingType(declaration).ifPresent(type -> {
          List<Type> generics = getSuperClassGenerics(type, Source.class);
          if (generics.isEmpty()) {
            declareMimeTypeParameters(declaration);
          } else {
            if (acceptsMimeTypeParameters(getSourceReturnType(generics.get(0), typeLoader))) {
              declareMimeTypeParameters(declaration);
            }
          }
        });
      }
    }.walk(declaration);
  }

  private void declareMimeTypeParameters(ParameterizedDeclaration<?> declaration) {
    ParameterGroupDeclaration group = declaration.getParameterGroup(DEFAULT_GROUP_NAME);
    group.addParameter(newParameter(MIME_TYPE_PARAMETER_NAME, "The mime type of the payload that this operation outputs."));
    group.addParameter(newParameter(ENCODING_PARAMETER_NAME, "The encoding of the payload that this operation outputs."));
  }

  private boolean acceptsMimeTypeParameters(MetadataType type) {
    Reference<Boolean> holder = new Reference<>();
    type.accept(new MetadataTypeVisitor() {

      @Override
      public void visitString(StringType stringType) {
        holder.set(!stringType.getAnnotation(EnumAnnotation.class).isPresent());
      }

      @Override
      public void visitBinaryType(BinaryType binaryType) {
        holder.set(true);
      }

      @Override
      public void visitObject(ObjectType objectType) {
        holder.set(getId(objectType).equals(Object.class.getName()));
      }

      @Override
      protected void defaultVisit(MetadataType metadataType) {
        holder.set(false);
      }
    });
    return holder.get();
  }

  private ParameterDeclaration newParameter(String name, String description) {
    ParameterDeclaration parameter = new ParameterDeclaration(name);
    parameter.setRequired(false);
    parameter.setExpressionSupport(SUPPORTED);
    parameter.setType(typeLoader.load(String.class), false);
    parameter.setDescription(description);
    parameter.setLayoutModel(LayoutModel.builder().tabName(ADVANCED_TAB_NAME).build());
    return parameter;
  }
}
