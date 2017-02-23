/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.validation;

import static java.lang.String.format;
import static org.mule.metadata.api.model.MetadataFormat.JAVA;
import static org.mule.metadata.api.utils.MetadataTypeUtils.getTypeId;
import com.google.common.collect.ImmutableSet;
import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.util.IdempotentExtensionWalker;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.loader.ExtensionModelValidator;
import org.mule.runtime.extension.api.loader.Problem;
import org.mule.runtime.extension.api.loader.ProblemsReporter;

import java.util.Set;

/**
 * Validates that all {@link OperationModel operations} parameters are from a valid type.
 * <p>
 * A valid type is considered to be one that is not an {@link Event} nor a {@link Message}
 *
 * @since 4.0
 */
public class OperationParametersTypeModelValidator implements ExtensionModelValidator {

  private final ClassTypeLoader typeLoader = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader();
  private final Set<MetadataType> forbiddenTypes = ImmutableSet.<MetadataType>builder()
      .add(typeLoader.load(Event.class))
      .add(typeLoader.load(Message.class))
      .build();

  @Override
  public void validate(ExtensionModel extensionModel, ProblemsReporter problemsReporter) {
    new IdempotentExtensionWalker() {

      @Override
      protected void onOperation(OperationModel model) {
        model.getAllParameterModels().forEach(parameterModel -> {
          MetadataType parameterType = parameterModel.getType();
          if (parameterType.getMetadataFormat().equals(JAVA)) {
            if (isForbiddenType(parameterType)) {
              problemsReporter
                  .addError(new Problem(model, format("Operation '%s' contains parameter '%s' of type '%s' which is forbidden",
                                                      model.getName(), parameterModel.getName(),
                                                      getTypeId(parameterType).get())));
            }
          }
        });
      }

      private boolean isForbiddenType(MetadataType parameterType) {
        return forbiddenTypes.contains(parameterType);
      }

    }.walk(extensionModel);
  }
}
