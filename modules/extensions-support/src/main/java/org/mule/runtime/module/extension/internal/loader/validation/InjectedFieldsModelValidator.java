/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.validation;

import static java.lang.String.format;
import static org.mule.metadata.api.model.MetadataFormat.JAVA;
import static org.mule.metadata.java.api.utils.JavaTypeUtils.getType;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getAnnotatedFields;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getImplementingType;

import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.visitor.MetadataTypeVisitor;
import org.mule.runtime.api.meta.MuleVersion;
import org.mule.runtime.api.meta.NamedObject;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.api.meta.model.connection.HasConnectionProviderModels;
import org.mule.runtime.api.meta.model.operation.HasOperationModels;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.meta.model.source.HasSourceModels;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.api.meta.model.util.ExtensionWalker;
import org.mule.runtime.extension.api.annotation.param.DefaultEncoding;
import org.mule.runtime.extension.api.annotation.param.RefName;
import org.mule.runtime.extension.api.declaration.type.annotation.InfrastructureTypeAnnotation;
import org.mule.runtime.extension.api.loader.ExtensionModelValidator;
import org.mule.runtime.extension.api.loader.Problem;
import org.mule.runtime.extension.api.loader.ProblemsReporter;
import org.mule.runtime.extension.api.property.ClassLoaderModelProperty;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionParameter;
import org.mule.runtime.module.extension.api.loader.java.type.MethodElement;
import org.mule.runtime.module.extension.internal.loader.java.type.property.ExtensionOperationDescriptorModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.type.property.ExtensionTypeDescriptorModelProperty;
import org.mule.sdk.api.annotation.param.RuntimeVersion;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Validates that the fields which are annotated with {@link RefName} or {@link DefaultEncoding} honor that:
 * <ul>
 * <li>The annotated field is of {@link String} type</li>
 * <li>There is at most one field annotated per type</li>
 * </ul>
 * <p>
 * It also validates the aforementioned rules for all the {@link OperationModel} method's arguments.
 *
 * @since 4.0
 */
public final class InjectedFieldsModelValidator implements ExtensionModelValidator {

  @Override
  public void validate(ExtensionModel extensionModel, ProblemsReporter problemsReporter) {
    final Set<Class<?>> validatedTypes = new HashSet<>();
    // TODO - MULE-14401 - Make InjectedFieldsModelValidator work in AST Mode
    boolean isASTMode = !extensionModel.getModelProperty(ExtensionTypeDescriptorModelProperty.class)
        .map(mp -> mp.getType().getDeclaringClass().isPresent())
        .orElse(false);

    if (!isASTMode) {
      extensionModel.getModelProperty(ClassLoaderModelProperty.class).ifPresent(classLoaderModelProperty -> {
        new ExtensionWalker() {

          @Override
          protected void onSource(HasSourceModels owner, SourceModel model) {
            Optional<Class> implementingType = getImplementingType(model);
            validateFields(model, implementingType, DefaultEncoding.class, String.class);
            validateFields(model, implementingType, RuntimeVersion.class, MuleVersion.class);
          }

          @Override
          protected void onConfiguration(ConfigurationModel model) {
            Optional<Class> implementingType = getImplementingType(model);
            validateFields(model, implementingType, DefaultEncoding.class, String.class);
            validateFields(model, implementingType, RefName.class, String.class);
            validateFields(model, implementingType, RuntimeVersion.class, MuleVersion.class);
          }

          @Override
          protected void onOperation(HasOperationModels owner, OperationModel model) {
            validateArguments(model, model.getModelProperty(ExtensionOperationDescriptorModelProperty.class),
                              DefaultEncoding.class);
          }

          @Override
          protected void onConnectionProvider(HasConnectionProviderModels owner, ConnectionProviderModel model) {
            Optional<Class> implementingType = getImplementingType(model);
            validateFields(model, implementingType, DefaultEncoding.class, String.class);
            validateFields(model, implementingType, RefName.class, String.class);
            validateFields(model, implementingType, RuntimeVersion.class, MuleVersion.class);
          }

          @Override
          protected void onParameter(ParameterizedModel owner, ParameterGroupModel groupModel, ParameterModel model) {
            if (model.getType().getMetadataFormat().equals(JAVA)) {
              model.getType().accept(new MetadataTypeVisitor() {

                @Override
                public void visitObject(ObjectType objectType) {
                  if (!objectType.getAnnotation(InfrastructureTypeAnnotation.class).isPresent()) {
                    try {
                      Class<?> type = getType(objectType, classLoaderModelProperty.getClassLoader());
                      if (validatedTypes.add(type)) {
                        validateType(model, type, DefaultEncoding.class, String.class);
                      }
                    } catch (Exception e) {
                      problemsReporter.addWarning(new Problem(model, "Could not validate Class: " + e.getMessage()));
                    }
                  }
                }
              });
            }
          }

          private void validateArguments(NamedObject model, Optional<ExtensionOperationDescriptorModelProperty> modelProperty,
                                         Class<? extends Annotation> annotationClass) {
            modelProperty.ifPresent(operationDescriptorModelProperty -> {
              MethodElement operation = operationDescriptorModelProperty.getOperationElement();
              int size = operation.getParametersAnnotatedWith(annotationClass).size();

              if (size == 0) {
                return;
              } else if (size > 1) {
                problemsReporter
                    .addError(new Problem(model,
                                          format("Operation method '%s' has %d arguments annotated with @%s. Only one argument may carry that annotation",
                                                 operation.getName(), size,
                                                 annotationClass.getSimpleName())));
              }

              ExtensionParameter argument = operation.getParametersAnnotatedWith(annotationClass).get(0);
              if (!argument.getType().isSameType(String.class)) {
                problemsReporter
                    .addError(new Problem(model,
                                          format("Operation method '%s' declares an argument '%s' which is annotated with @%s and is of type '%s'. Only "
                                              + "arguments of type String are allowed to carry such annotation",
                                                 operation.getName(),
                                                 argument.getName(), annotationClass.getSimpleName(),
                                                 argument.getType().getName())));
              }
            });
          }

          private void validateFields(NamedObject model, Optional<Class> implementingType,
                                      Class<? extends Annotation> annotationClass, Class implementingClass) {
            implementingType.ifPresent(type -> validateType(model, type, annotationClass, implementingClass));
          }

          private void validateType(NamedObject model, Class<?> type, Class<? extends Annotation> annotationClass,
                                    Class implementingClass) {
            List<Field> fields = getAnnotatedFields(type, annotationClass);
            if (fields.isEmpty()) {
              return;
            } else if (fields.size() > 1) {
              problemsReporter
                  .addError(new Problem(model,
                                        format("Class '%s' has %d fields annotated with @%s. Only one field may carry that annotation",
                                               type.getName(), fields.size(), annotationClass.getSimpleName())));
            }

            Field field = fields.get(0);
            if (!implementingClass.equals(field.getType())) {
              problemsReporter
                  .addError(new Problem(model,
                                        format("Class '%s' declares the field '%s' which is annotated with @%s and is of type '%s'. Only "
                                            + "fields of type %s are allowed to carry such annotation", type.getName(),
                                               field.getName(), annotationClass.getSimpleName(), field.getType().getName(),
                                               implementingClass)));
            }
          }
        }.walk(extensionModel);
      });
    }
  }
}
