/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.enricher;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mule.runtime.api.meta.ExpressionSupport.SUPPORTED;
import static org.mule.runtime.api.meta.model.parameter.ParameterGroupModel.DEFAULT_GROUP_NAME;
import static org.mule.runtime.api.metadata.MediaType.ANY;
import static org.mule.runtime.extension.api.loader.DeclarationEnricherPhase.STRUCTURE;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.toMetadataFormat;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.ADVANCED_TAB_NAME;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.ENCODING_PARAMETER_NAME;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.MIME_TYPE_PARAMETER_NAME;
import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.annotation.EnumAnnotation;
import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.builder.BinaryTypeBuilder;
import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.BinaryType;
import org.mule.metadata.api.model.MetadataFormat;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.StringType;
import org.mule.metadata.api.visitor.MetadataTypeVisitor;
import org.mule.metadata.java.api.annotation.ClassInformationAnnotation;
import org.mule.runtime.api.meta.model.declaration.fluent.ExecutableComponentDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.OperationDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.OutputDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterGroupDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.SourceDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.WithSourcesDeclaration;
import org.mule.runtime.api.meta.model.display.LayoutModel;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.api.streaming.CursorProvider;
import org.mule.runtime.api.streaming.bytes.CursorStreamProvider;
import org.mule.runtime.extension.api.declaration.fluent.util.IdempotentDeclarationWalker;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.loader.DeclarationEnricher;
import org.mule.runtime.extension.api.loader.DeclarationEnricherPhase;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.extension.api.runtime.streaming.PagingProvider;
import org.mule.runtime.extension.api.util.NameUtils;
import org.mule.runtime.module.extension.api.loader.java.type.Type;
import org.mule.runtime.module.extension.internal.ExtensionProperties;
import org.mule.runtime.module.extension.internal.loader.java.property.MediaTypeModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.type.property.ExtensionOperationDescriptorModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.type.property.ExtensionTypeDescriptorModelProperty;

import java.io.InputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;
import java.util.function.Function;

/**
 * Enriches operations which return types are {@link InputStream}, {@link String} or {@link Object} by adding two parameters: A
 * {@link ExtensionProperties#MIME_TYPE_PARAMETER_NAME} that allows configuring the mimeType to the output operation payload and a
 * {@link ExtensionProperties#ENCODING_PARAMETER_NAME} that allows configuring the encoding to the output operation payload.
 * <p>
 * Both added attributes are optional without default value and accept expressions.
 *
 * @since 4.0
 */
public final class MimeTypeParametersDeclarationEnricher implements DeclarationEnricher {

  @Override
  public DeclarationEnricherPhase getExecutionPhase() {
    return STRUCTURE;
  }

  @Override
  public void enrich(ExtensionLoadingContext extensionLoadingContext) {
    new EnricherDelegate().enrich(extensionLoadingContext);
  }

  private class EnricherDelegate extends AbstractAnnotatedDeclarationEnricher {

    private final ClassTypeLoader typeLoader = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader();

    @Override
    public void enrich(ExtensionLoadingContext extensionLoadingContext) {
      final ExtensionDeclaration declaration = extensionLoadingContext.getExtensionDeclarer().getDeclaration();
      new IdempotentDeclarationWalker() {

        @Override
        protected void onOperation(OperationDeclaration declaration) {
          declareMimeTypeParameters(declaration, getOperationReturnType(declaration));
        }

        private Optional<Type> getOperationReturnType(OperationDeclaration declaration) {
          Optional<ExtensionOperationDescriptorModelProperty> extensionOperationDescriptorModelProperty =
              declaration.getModelProperty(ExtensionOperationDescriptorModelProperty.class);
          if (extensionOperationDescriptorModelProperty.isPresent()) {
            return of(extensionOperationDescriptorModelProperty.get().getOperationMethod().getReturnType());
          } else {
            return empty();
          }
        }

        @Override
        protected void onSource(WithSourcesDeclaration owner, SourceDeclaration declaration) {
          declareMimeTypeParameters(declaration, getSourceOutputType(declaration));
        }

        private Optional<Type> getSourceOutputType(SourceDeclaration declaration) {

          Optional<ExtensionTypeDescriptorModelProperty> extensionTypeDescriptorModelProperty =
              declaration.getModelProperty(ExtensionTypeDescriptorModelProperty.class);
          if (extensionTypeDescriptorModelProperty.isPresent()) {
            return of(extensionTypeDescriptorModelProperty.get().getType().getSuperTypeGenerics(Source.class).get(0));
          } else {
            return empty();
          }
        }

      }.walk(declaration);
    }

    private void declareOutputEncodingParameter(ParameterGroupDeclaration group) {
      group.addParameter(newParameter(ENCODING_PARAMETER_NAME, "The encoding of the payload that this operation outputs."));
    }

    private void declareOutputMimeTypeParameter(ParameterGroupDeclaration group) {
      group.addParameter(newParameter(MIME_TYPE_PARAMETER_NAME, "The mime type of the payload that this operation outputs."));
    }

    private void declareMimeTypeParameters(ExecutableComponentDeclaration<?> declaration, Optional<Type> outputType) {
      MediaTypeModelProperty property = declaration.getModelProperty(MediaTypeModelProperty.class).orElse(null);
      declaration.getOutput().getType().accept(new MetadataTypeVisitor() {

        @Override
        public void visitString(StringType stringType) {
          if (stringType.getAnnotation(EnumAnnotation.class).isPresent()) {
            return;
          }

          if (property == null) {
            String componentType = NameUtils.getDeclarationTypeName(declaration);
            throw new IllegalModelDefinitionException(String.format(
                                                                    "%s '%s' has a String output but doesn't specify a default mime type. Please annotate it with @%s",
                                                                    componentType, declaration.getName(),
                                                                    org.mule.runtime.extension.api.annotation.param.MediaType.class
                                                                        .getSimpleName()));
          }

          if (!property.isStrict()) {
            declareOutputMimeTypeParameter(declaration.getParameterGroup(DEFAULT_GROUP_NAME));
          }

          replaceOutputType(declaration, property, format -> BaseTypeBuilder.create(format).stringType().build());
        }

        @Override
        public void visitArrayType(ArrayType arrayType) {
          if (!outputType.isPresent()) {
            return;
          }

          Type type = outputType.get();
          Type itemType = getItemType(type);

          if (itemType == null) {
            return;
          }

          Type resultPayloadType = getResultPayloadType(itemType);

          if (resultPayloadType == null) {
            return;
          }
          ParameterGroupDeclaration group = declaration.getParameterGroup(DEFAULT_GROUP_NAME);
          if (resultPayloadType.isAssignableTo(String.class)) {
            declareOutputMimeTypeParameter(group);
          } else if (resultPayloadType.isAssignableTo(CursorProvider.class)
              || resultPayloadType.isAssignableTo(InputStream.class)) {
            declareOutputMimeTypeParameter(group);
            declareOutputEncodingParameter(group);
          }
        }

        private Type getResultPayloadType(Type itemType) {
          if (itemType.isAssignableTo(Result.class)) {
            return itemType.getSuperTypeGenerics(Result.class).get(0);
          }
          return null;
        }

        private Type getItemType(Type type) {
          if (type.isAssignableTo(Collection.class)) {
            return type.getSuperTypeGenerics(Collection.class).get(0);
          } else if (type.isAssignableTo(Iterator.class)) {
            return type.getSuperTypeGenerics(Iterator.class).get(0);
          } else if (type.isAssignableTo(PagingProvider.class)) {
            return type.getSuperTypeGenerics(PagingProvider.class).get(1);
          } else {
            return null;
          }
        }

        @Override
        public void visitBinaryType(BinaryType binaryType) {
          if (property == null) {
            String componentType = NameUtils.getDeclarationTypeName(declaration);
            throw new IllegalModelDefinitionException(String.format(
                                                                    "%s '%s' has a binary output but doesn't specify a default mime type. Please annotate it with @%s",
                                                                    componentType, declaration.getName(),
                                                                    org.mule.runtime.extension.api.annotation.param.MediaType.class
                                                                        .getSimpleName()));
          }

          if (!property.isStrict()) {
            ParameterGroupDeclaration group = declaration.getParameterGroup(DEFAULT_GROUP_NAME);
            declareOutputMimeTypeParameter(group);
            declareOutputEncodingParameter(group);
          }

          replaceOutputType(declaration, property, format -> {
            BinaryTypeBuilder builder = BaseTypeBuilder.create(format).binaryType();
            declaration.getOutput().getType().getAnnotation(ClassInformationAnnotation.class).ifPresent(builder::with);

            return builder.build();
          });
        }
      });
    }

    private void replaceOutputType(ExecutableComponentDeclaration<?> declaration, MediaTypeModelProperty property,
                                   Function<MetadataFormat, MetadataType> type) {

      final MediaType mediaType = property.getMediaType();
      if (mediaType.matches(ANY)) {
        return;
      }

      final OutputDeclaration output = declaration.getOutput();
      output.setType(type.apply(toMetadataFormat(mediaType)), output.hasDynamicType());
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
}
