/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.validation;

import org.mule.metadata.api.annotation.EnumAnnotation;
import org.mule.metadata.api.model.BinaryType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.StringType;
import org.mule.runtime.api.meta.model.ConnectableComponentModel;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.OutputModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.api.meta.model.util.IdempotentExtensionWalker;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.extension.api.loader.ExtensionModelValidator;
import org.mule.runtime.extension.api.loader.Problem;
import org.mule.runtime.extension.api.loader.ProblemsReporter;
import org.mule.runtime.extension.api.util.NameUtils;
import org.mule.runtime.module.extension.api.loader.java.type.Type;
import org.mule.runtime.module.extension.internal.loader.annotations.CustomDefinedStaticTypeAnnotation;
import org.mule.runtime.module.extension.internal.loader.java.property.MediaTypeModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.type.property.ExtensionOperationDescriptorModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.type.property.ExtensionTypeDescriptorModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.type.runtime.SourceTypeWrapper;

import java.util.Optional;

import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.toMetadataFormat;
import static org.mule.runtime.module.extension.internal.loader.validation.ModelValidationUtils.isCompiletime;

/**
 * {@link ExtensionModelValidator} which verifies if a {@link org.mule.runtime.extension.api.annotation.param.MediaType} is
 * missing or if a {@link org.mule.runtime.extension.api.annotation.param.MediaType} is present with a value, and a value is not
 * permited in such case.
 *
 * <p>
 * This validator goes through all operations and sources and verifies that:
 * <ul>
 * <li>If no OutputStaticResolver is given and the outputType is a String Type or Binary Type, it must have the MediaType
 * annotation with a valid value</li>
 * <li>If it has an OutputStaticResolver it must NOT have the MediaType annotation with a value</li>
 * </ul>
 *
 * @since 4.1.3
 */
public class MediaTypeModelValidator implements ExtensionModelValidator {

  private static String JAVA_METADATA_FORMAT_ID = "java";

  @Override
  public void validate(ExtensionModel extensionModel, ProblemsReporter problemsReporter) {

    new IdempotentExtensionWalker() {

      @Override
      protected void onOperation(OperationModel model) {
        validateMediaType(model, getOperationReturnType(model));
      }

      private Optional<Type> getOperationReturnType(OperationModel operationModel) {
        return operationModel.getModelProperty(ExtensionOperationDescriptorModelProperty.class)
            .map(mp -> mp.getOperationMethod().getReturnType());
      }

      @Override
      protected void onSource(SourceModel model) {
        validateMediaType(model, getSourceOutputType(model));
      }

      private Optional<Type> getSourceOutputType(SourceModel sourceModel) {
        return sourceModel.getModelProperty(ExtensionTypeDescriptorModelProperty.class)
            .map(mp -> (Type) ((SourceTypeWrapper) (mp.getType())).getSuperClassGenerics().get(0));
      }

      private void validateMediaType(ConnectableComponentModel model, Optional<Type> outputType) {
        MetadataType outputMetadataType = outputType.map(type -> type.asMetadataType()).orElse(model.getOutput().getType());

        if (mediaTypeAnnotationIsMissing(model, outputMetadataType)) {
          String componentType = NameUtils.getComponentModelTypeName(model);
          String message =
              String
                  .format("%s '%s' has a String or InputStream output but doesn't specify a default mime type. Please annotate it with @%s",
                          componentType, model.getName(),
                          org.mule.runtime.extension.api.annotation.param.MediaType.class.getSimpleName());
          problemsReporter.addError(new Problem(model, message));
        } else if (staticResolverClashesWithMediaTypeAnnotationValue(model)) {
          if (isCompiletime(extensionModel)) {
            String componentType = NameUtils.getComponentModelTypeName(model);
            String message =
                String
                    .format("%s '%s' is both using a Static Metadata Resolver for the output, and enforcing a media type through"
                        +
                        " the @%s annotation. You can use the default value for the annotation to keep letting the" +
                        " user configure the outputMimeType parameter.",
                            componentType, model.getName(),
                            org.mule.runtime.extension.api.annotation.param.MediaType.class.getSimpleName());
            problemsReporter.addError(new Problem(model, message));
          }
        } else if (mediaTypeAnnotationIsMissingValue(model, outputMetadataType)) {
          String componentType = NameUtils.getComponentModelTypeName(model);
          String message =
              String
                  .format("%s '%s' has a String or InputStream output but doesn't specify a default mime type. " +
                      "Please give a value to the @%s annotation",
                          componentType, model.getName(),
                          org.mule.runtime.extension.api.annotation.param.MediaType.class.getSimpleName());
          problemsReporter.addError(new Problem(model, message));
        }

      }

      private boolean mediaTypeAnnotationIsMissingValue(ConnectableComponentModel model, MetadataType outputMetadataType) {
        Optional<MediaTypeModelProperty> mediaTypeModelProperty = model.getModelProperty(MediaTypeModelProperty.class);
        return outputTypeNeedsMediaTypeAnnotation(outputMetadataType) &&
            hasMediaTypeModelProperty(model) &&
            mediaTypeModelPropertyHasDefaultValue(mediaTypeModelProperty.get()) &&
        // Because the value is defaulted, the mediaType in the model property is null
            !hasStaticMetadataDefined(model, null);
      }

      private boolean staticResolverClashesWithMediaTypeAnnotationValue(ConnectableComponentModel model) {
        MediaTypeModelProperty mediaTypeModelProperty = model.getModelProperty(MediaTypeModelProperty.class).orElse(null);
        return hasMediaTypeModelProperty(model) &&
            !mediaTypeModelPropertyHasDefaultValue(mediaTypeModelProperty) &&
            hasStaticMetadataDefined(model, mediaTypeModelProperty.getMediaType().get());
      }

      private boolean mediaTypeAnnotationIsMissing(ConnectableComponentModel model, MetadataType outputMetadataType) {
        return outputTypeNeedsMediaTypeAnnotation(outputMetadataType) &&
            !hasMediaTypeModelProperty(model) &&
        // Since the model property is missing, there is no media type
            !hasStaticMetadataDefined(model, null);
      }

      private boolean hasStaticMetadataDefined(ConnectableComponentModel model, MediaType mediaTypeFromModelProperty) {
        if (mediaTypeFromModelProperty == null) {
          return hasCustomStaticMetadataDefined(model.getOutput()) || !hasJavaAsMetadataFormat(model.getOutput());
        } else {
          return hasCustomStaticMetadataDefined(model.getOutput()) ||
              modelMediaTypeDiffersFromMediaType(model.getOutput(), mediaTypeFromModelProperty);
        }
      }

      private boolean hasJavaAsMetadataFormat(OutputModel model) {
        return model.getType().getMetadataFormat().getId().equals(JAVA_METADATA_FORMAT_ID);
      }

      private boolean hasMediaTypeModelProperty(ConnectableComponentModel model) {
        return model.getModelProperty(MediaTypeModelProperty.class).isPresent();
      }

      private boolean hasCustomStaticMetadataDefined(OutputModel model) {
        return model.getType().getAnnotation(CustomDefinedStaticTypeAnnotation.class).isPresent();
      }

      private boolean modelMediaTypeDiffersFromMediaType(OutputModel model, MediaType mediaType) {
        return !model.getType().getMetadataFormat().equals(toMetadataFormat(mediaType));
      }

      private boolean mediaTypeModelPropertyHasDefaultValue(MediaTypeModelProperty mediaTypeModelProperty) {
        return !mediaTypeModelProperty.getMediaType().isPresent();
      }

      private boolean outputTypeNeedsMediaTypeAnnotation(MetadataType metadataType) {
        return (metadataType instanceof StringType && !metadataType.getAnnotation(EnumAnnotation.class).isPresent())
            || metadataType instanceof BinaryType;
      }

    }.walk(extensionModel);


  }
}
