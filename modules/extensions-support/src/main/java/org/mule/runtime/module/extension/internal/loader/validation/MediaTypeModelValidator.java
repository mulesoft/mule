/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.validation;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.capitalize;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.toMetadataFormat;
import static org.mule.runtime.module.extension.internal.loader.validation.ModelValidationUtils.isCompiletime;

import org.mule.metadata.api.annotation.EnumAnnotation;
import org.mule.metadata.api.model.AnyType;
import org.mule.metadata.api.model.BinaryType;
import org.mule.metadata.api.model.MetadataFormat;
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
import org.mule.runtime.module.extension.api.loader.java.type.OperationElement;
import org.mule.runtime.module.extension.api.loader.java.type.SourceElement;
import org.mule.runtime.module.extension.internal.loader.annotations.CustomDefinedStaticTypeAnnotation;
import org.mule.runtime.module.extension.internal.loader.java.property.MediaTypeModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.type.property.ExtensionOperationDescriptorModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.type.property.ExtensionTypeDescriptorModelProperty;

import java.util.Optional;

/**
 * {@link ExtensionModelValidator} which verifies if a {@link org.mule.runtime.extension.api.annotation.param.MediaType} is
 * missing or if a {@link org.mule.runtime.extension.api.annotation.param.MediaType} is present with a value, and a value is not
 * permitted in such case.
 *
 * <p>
 * This validator goes through all operations and sources and verifies that:
 * <ul>
 * <li>If no OutputStaticResolver is given and the outputType is a String Type or Binary Type, it must have the MediaType
 * annotation with a valid value</li>
 * <li>If it has an OutputStaticResolver it must NOT have the MediaType annotation with a value</li>
 * </ul>
 *
 * @since 4.2.0
 */
public class MediaTypeModelValidator implements ExtensionModelValidator {

  @Override
  public void validate(ExtensionModel extensionModel, ProblemsReporter problemsReporter) {
    if (!isCompiletime(extensionModel)) {
      return;
    }
    new IdempotentExtensionWalker() {

      @Override
      protected void onOperation(OperationModel model) {
        Optional<MetadataType> outputType = model.getModelProperty(ExtensionOperationDescriptorModelProperty.class)
            .map(ExtensionOperationDescriptorModelProperty::getOperationElement)
            .map(OperationElement::getOperationReturnMetadataType);
        validateMediaType(model, outputType);
      }

      @Override
      protected void onSource(SourceModel model) {
        Optional<MetadataType> outputType = model.getModelProperty(ExtensionTypeDescriptorModelProperty.class)
            .filter(mp -> mp.getType() instanceof SourceElement)
            .map(mp -> (SourceElement) mp.getType())
            .map(SourceElement::getReturnMetadataType);
        validateMediaType(model, outputType);
      }

      private void validateMediaType(ConnectableComponentModel model, Optional<MetadataType> outputType) {
        MetadataType outputMetadataType = outputType.orElse(model.getOutput().getType());

        if (mediaTypeAnnotationIsMissing(model, outputMetadataType)) {
          String componentType = NameUtils.getComponentModelTypeName(model);
          String message =
              format("%s '%s' has a %s type output but doesn't specify a default mime type. Please annotate it with @%s",
                     capitalize(componentType), model.getName(),
                     outputMetadataType instanceof StringType ? "String" : "InputStream",
                     org.mule.runtime.extension.api.annotation.param.MediaType.class.getSimpleName());
          problemsReporter.addWarning(new Problem(model, message));
        } else if (staticResolverClashesWithMediaTypeAnnotationValue(model, outputMetadataType)) {
          String componentType = NameUtils.getComponentModelTypeName(model);
          String message =
              String
                  .format("%s '%s' is declaring both a custom output Type using a Static MetadataResolver, and a custom" +
                      " media type through the @%s annotation. Enforce 'MediaType' you want using the '%s' " +
                      "given to your Type Builder. You can still use the @%s annotation to set `strict=false` " +
                      "and keep letting the user configure the outputMimeType parameter.",
                          capitalize(componentType), model.getName(), MediaType.class.getSimpleName(),
                          org.mule.metadata.api.model.MetadataFormat.class.getName(), MediaType.class.getSimpleName());
          problemsReporter.addWarning(new Problem(model, message));
        } else if (mediaTypeAnnotationIsMissingValue(model, outputMetadataType)) {
          String componentType = NameUtils.getComponentModelTypeName(model);
          String message =
              format("%s '%s' has a %s type output but doesn't specify a default mime type. " +
                  "Please give a value to the @%s annotation",
                     capitalize(componentType), model.getName(),
                     outputMetadataType instanceof StringType ? "String" : "InputStream",
                     org.mule.runtime.extension.api.annotation.param.MediaType.class.getSimpleName());
          problemsReporter.addWarning(new Problem(model, message));
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

      private boolean staticResolverClashesWithMediaTypeAnnotationValue(ConnectableComponentModel model,
                                                                        MetadataType outputMetadataType) {
        MediaTypeModelProperty mediaTypeModelProperty = model.getModelProperty(MediaTypeModelProperty.class).orElse(null);
        return outputTypeNeedsMediaTypeAnnotation(outputMetadataType) &&
            hasMediaTypeModelProperty(model) &&
            mediaTypeModelPropertyIsStrict(mediaTypeModelProperty) &&
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
        return model.getType().getMetadataFormat().getId().equals(MetadataFormat.JAVA.getId());
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
        return mediaTypeModelProperty != null && !mediaTypeModelProperty.getMediaType().isPresent();
      }

      private boolean mediaTypeModelPropertyIsStrict(MediaTypeModelProperty mediaTypeModelProperty) {
        return mediaTypeModelProperty != null && mediaTypeModelProperty.isStrict();
      }

      private boolean outputTypeNeedsMediaTypeAnnotation(MetadataType metadataType) {
        return (metadataType instanceof StringType && !metadataType.getAnnotation(EnumAnnotation.class).isPresent())
            || metadataType instanceof BinaryType
            || metadataType instanceof AnyType;
      }

    }.walk(extensionModel);


  }
}
