/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.validation;

import static java.lang.String.format;
import static org.mule.metadata.java.api.utils.JavaTypeUtils.getType;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.isMap;
import static org.mule.runtime.module.extension.internal.loader.validation.ModelValidationUtils.isCompiletime;
import static org.springframework.util.ClassUtils.isPrimitiveWrapper;
import static java.lang.reflect.Modifier.isAbstract;

import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.BooleanType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.model.UnionType;
import org.mule.metadata.api.visitor.MetadataTypeVisitor;
import org.mule.metadata.java.api.annotation.ClassInformationAnnotation;
import org.mule.runtime.api.meta.NamedObject;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.meta.model.util.ExtensionWalker;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.extension.api.annotation.metadata.MetadataKeyId;
import org.mule.runtime.extension.api.annotation.metadata.MetadataKeyPart;
import org.mule.runtime.extension.api.annotation.param.ConfigOverride;
import org.mule.runtime.extension.api.annotation.param.stereotype.ComponentId;
import org.mule.runtime.extension.api.loader.ExtensionModelValidator;
import org.mule.runtime.extension.api.loader.Problem;
import org.mule.runtime.extension.api.loader.ProblemsReporter;
import org.mule.runtime.module.extension.api.loader.java.type.FieldElement;
import org.mule.runtime.module.extension.api.loader.java.type.Type;
import org.mule.runtime.module.extension.internal.loader.java.type.property.ExtensionParameterDescriptorModelProperty;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Validates that the parameter types are valid
 * </ul>
 *
 * @since 4.0
 */
public final class ParameterTypeModelValidator implements ExtensionModelValidator {

  @Override
  public void validate(ExtensionModel extensionModel, ProblemsReporter problemsReporter) {
    if (!isCompiletime(extensionModel)) {
      // TODO MULE-14517: Validations for types will be added to 4.2,
      // so we need to keep backwards compatibility somehow for now.
      return;
    }

    new ExtensionWalker() {

      @Override
      protected void onParameter(ParameterizedModel owner, ParameterGroupModel groupModel, ParameterModel model) {
        validateParameterType(model, problemsReporter);
      }
    }.walk(extensionModel);
  }

  private void validateParameterType(ParameterModel parameter, ProblemsReporter problemsReporter) {
    parameter.getType().accept(new MetadataTypeVisitor() {

      private Set<MetadataType> visitedTypes = new HashSet<>();

      @Override
      public void visitUnion(UnionType unionType) {
        unionType.getTypes().forEach(t -> t.accept(this));
      }

      @Override
      public void visitArrayType(ArrayType arrayType) {
        arrayType.getType().accept(this);
      }

      @Override
      public void visitObject(ObjectType objectType) {
        if (!visitedTypes.add(objectType)) {
          return;
        }

        if (isMap(objectType)) {
          objectType.getAnnotation(ClassInformationAnnotation.class)
              .filter(classInformation -> !classInformation.getGenericTypes().isEmpty())
              .filter(classInformation -> !Objects.equals(classInformation.getGenericTypes().get(0), String.class.getName()))
              .ifPresent(classInformation -> problemsReporter.addError(new Problem(parameter,
                                                                                   format(
                                                                                          "Parameter '%s' is of type '%s' and its key type is not %s ",
                                                                                          parameter.getName(),
                                                                                          getType(objectType).getName(),
                                                                                          String.class.getName()))));

          objectType.getOpenRestriction().get().accept(this);
        } else {
          parameter.getModelProperty(ExtensionParameterDescriptorModelProperty.class)
              .map(descriptor -> descriptor.getExtensionParameter().getType())
              .ifPresent(type -> {
                final String typeName = type.getName();

                if (!type.getDeclaringClass().isPresent()) {
                  return;
                }

                Class<?> clazz = type.getDeclaringClass().get().equals(TypedValue.class)
                    ? getFirstGenericClass(type).orElse(Object.class)
                    : type.getDeclaringClass().get();

                if (isPojoWithoutDefaultConstructor(clazz)) {
                  problemsReporter
                      .addError(new Problem(parameter, format("Type '%s' does not have a default constructor", typeName)));
                }

                type.getFields()
                    .forEach(field -> checkInvalidFieldAnnotations(parameter, typeName, field,
                                                                   ConfigOverride.class, ComponentId.class,
                                                                   MetadataKeyId.class, MetadataKeyPart.class));
              });
        }

        objectType.getFields().forEach(f -> f.getValue().accept(this));
      }

      @Override
      public void visitBoolean(BooleanType booleanType) {
        if (isPrimitiveWrapper(getType(booleanType))) {
          problemsReporter
              .addError(new Problem(parameter, format("Parameter '%s' is of type '%s'. Use primitive type boolean instead.",
                                                      parameter.getName(), Boolean.class.getName())));
        }
      }

      private void checkInvalidFieldAnnotations(NamedObject model, String typeName, FieldElement field,
                                                Class<? extends Annotation>... invalidAnnotations) {
        for (Class<? extends Annotation> annotation : invalidAnnotations) {
          if (field.getAnnotation(annotation).isPresent()) {
            problemsReporter.addError(new Problem(model,
                                                  format(
                                                         "Type '%s' has a field with name '%s' declared as '%s', which is not allowed.",
                                                         typeName, field.getName(), annotation.getSimpleName())));
          }
        }
      }

      private boolean isPojoWithoutDefaultConstructor(Class<?> clazz) {
        return (!clazz.isInterface() && !isAbstract(clazz.getModifiers())
            && Stream.of(clazz.getConstructors())
                .noneMatch(c -> c.getParameterCount() == 0));
      }

      private Optional<Class<?>> getFirstGenericClass(Type type) {
        return type.getGenerics().stream().findFirst()
            .map(typeGeneric -> typeGeneric.getConcreteType())
            .map(concreteType -> concreteType.getDeclaringClass()
                .orElse(Object.class));
      }
    });
  }
}
