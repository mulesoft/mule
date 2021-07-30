/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser.java;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static org.mule.runtime.api.meta.ExpressionSupport.SUPPORTED;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.isMap;
import static org.mule.runtime.extension.api.util.ExtensionModelUtils.roleOf;
import static org.mule.runtime.module.extension.internal.loader.java.MuleExtensionAnnotationParser.parseLayoutAnnotations;

import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.visitor.BasicTypeMetadataVisitor;
import org.mule.metadata.java.api.utils.JavaTypeUtils;
import org.mule.runtime.api.meta.ExpressionSupport;
import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.api.meta.model.ParameterDslConfiguration;
import org.mule.runtime.api.meta.model.display.LayoutModel;
import org.mule.runtime.api.meta.model.parameter.ParameterRole;
import org.mule.runtime.api.meta.model.stereotype.StereotypeModel;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.dsl.xml.ParameterDsl;
import org.mule.runtime.extension.api.annotation.param.ConfigOverride;
import org.mule.runtime.extension.api.annotation.param.Content;
import org.mule.runtime.extension.api.annotation.param.NullSafe;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.declaration.type.annotation.StereotypeTypeAnnotation;
import org.mule.runtime.extension.api.exception.IllegalParameterModelDefinitionException;
import org.mule.runtime.extension.api.property.DefaultImplementingTypeModelProperty;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionParameter;
import org.mule.runtime.module.extension.api.loader.java.type.Type;
import org.mule.runtime.module.extension.internal.loader.java.property.DeclaringMemberModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ImplementingParameterModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.NullSafeModelProperty;
import org.mule.runtime.module.extension.internal.loader.parser.ParameterModelParser;
import org.mule.runtime.module.extension.internal.util.IntrospectionUtils;

import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public class JavaParameterModelParser implements ParameterModelParser {

  private final ExtensionParameter parameter;
  private final MetadataType type;
  private final List<ModelProperty> modelProperties = new LinkedList<>();

  public JavaParameterModelParser(ExtensionParameter parameter) {
    this.parameter = parameter;
    type = parameter.getType().asMetadataType();
  }

  @Override
  public String getName() {
    return parameter.getAlias();
  }

  @Override
  public String getDescription() {
    return parameter.getDescription();
  }

  @Override
  public MetadataType getType() {
    return type;
  }

  @Override
  public List<StereotypeModel> getAllowedStereotypes() {
    return type.getAnnotation(StereotypeTypeAnnotation.class)
        .map(StereotypeTypeAnnotation::getAllowedStereotypes)
        .orElse(emptyList());
  }

  @Override
  public ParameterRole getRole() {
    return roleOf(parameter.getAnnotation(Content.class));
  }

  @Override
  public ExpressionSupport getExpressionSupport() {
    return parameter.getAnnotation(Expression.class)
        .map(expression -> IntrospectionUtils.getExpressionSupport(expression))
        .orElse(SUPPORTED);
  }

  @Override
  public Optional<LayoutModel> getLayoutModel() {
    return parseLayoutAnnotations(parameter, LayoutModel.builder());
  }

  @Override
  public Optional<ParameterDslConfiguration> getDslConfiguration() {
    return parameter.getAnnotation(ParameterDsl.class).map(parameterDsl ->
        ParameterDslConfiguration.builder()
            .allowsInlineDefinition(parameterDsl.allowInlineDefinition())
            .allowsReferences(parameterDsl.allowReferences())
            .build());
  }

  @Override
  public List<ModelProperty> getAdditionalModelProperties() {
    collectAdditionalModelProperties();
    return modelProperties;
  }

  private void collectAdditionalModelProperties() {
    if (modelProperties.isEmpty()) {
      collectImplementingTypeProperties();
      collectNullSafeProperties();
    }
  }

  private void collectImplementingTypeProperties() {
    parameter.getDeclaringElement().ifPresent(element -> {
      if (element instanceof Field) {
        modelProperties.add(new DeclaringMemberModelProperty(((Field) element)));
      } else {
        modelProperties.add(new ImplementingParameterModelProperty((java.lang.reflect.Parameter) element));
      }
    });
  }

  private void collectNullSafeProperties() {
    if (parameter.isAnnotatedWith(NullSafe.class)) {
      if (parameter.isAnnotatedWith(ConfigOverride.class)) {
        throw new IllegalParameterModelDefinitionException(
            format("Parameter '%s' is annotated with '@%s' and also marked as a config override, which is redundant. "
                    + "The default value for this parameter will come from the configuration parameter",
                parameter.getName(), NullSafe.class.getSimpleName()));
      }
      if (parameter.isRequired() && !parameter.isAnnotatedWith(ParameterGroup.class)) {
        throw new IllegalParameterModelDefinitionException(
            format("Parameter '%s' is required but annotated with '@%s', which is redundant",
                parameter.getName(), NullSafe.class.getSimpleName()));
      }

      Type nullSafeAnnotationType =
          parameter.getValueFromAnnotation(NullSafe.class).get().getClassValue(NullSafe::defaultImplementingType);
      final boolean hasDefaultOverride = !nullSafeAnnotationType.isSameType(Object.class);

      MetadataType nullSafeType =
          hasDefaultOverride ? nullSafeAnnotationType.asMetadataType() : type;

      boolean isInstantiable =
          hasDefaultOverride ? nullSafeAnnotationType.isInstantiable() : parameter.getType().isInstantiable();

      type.accept(new BasicTypeMetadataVisitor() {

        @Override
        protected void visitBasicType(MetadataType metadataType) {
          throw new IllegalParameterModelDefinitionException(
              format("Parameter '%s' is annotated with '@%s' but is of type '%s'. That annotation can only be "
                      + "used with complex types (Pojos, Lists, Maps)",
                  parameter.getName(), NullSafe.class.getSimpleName(),
                  parameter.getType().getName()));
        }

        @Override
        public void visitArrayType(ArrayType arrayType) {
          if (hasDefaultOverride) {
            throw new IllegalParameterModelDefinitionException(format("Parameter '%s' is annotated with '@%s' is of type '%s'"
                    + " but a 'defaultImplementingType' was provided."
                    + " Type override is not allowed for Collections",
                parameter.getName(),
                NullSafe.class.getSimpleName(),
                parameter.getType().getName()));
          }
        }

        @Override
        public void visitObject(ObjectType objectType) {
          if (hasDefaultOverride && isMap(objectType)) {
            throw new IllegalParameterModelDefinitionException(format("Parameter '%s' is annotated with '@%s' is of type '%s'"
                    + " but a 'defaultImplementingType' was provided."
                    + " Type override is not allowed for Maps",
                parameter.getName(),
                NullSafe.class.getSimpleName(),
                parameter.getType().getName()));
          }

          if (hasDefaultOverride && parameter.getType().isInstantiable()) {
            throw new IllegalParameterModelDefinitionException(
                format("Parameter '%s' is annotated with '@%s' is of concrete type '%s',"
                        + " but a 'defaultImplementingType' was provided."
                        + " Type override is not allowed for concrete types",
                    parameter.getName(),
                    NullSafe.class.getSimpleName(),
                    parameter.getType().getName()));
          }

          if (!isInstantiable && !isMap(nullSafeType)) {
            throw new IllegalParameterModelDefinitionException(
                format("Parameter '%s' is annotated with '@%s' but is of type '%s'. That annotation can only be "
                        + "used with complex instantiable types (Pojos, Lists, Maps)",
                    parameter.getName(),
                    NullSafe.class.getSimpleName(),
                    parameter.getType().getName()));
          }

          if (hasDefaultOverride && !parameter.getType().isAssignableFrom(nullSafeAnnotationType)) {
            throw new IllegalParameterModelDefinitionException(
                format("Parameter '%s' is annotated with '@%s' of type '%s', but provided type '%s"
                        + " is not a subtype of the parameter's type",
                    parameter.getName(),
                    NullSafe.class.getSimpleName(),
                    parameter.getType().getName(),
                    JavaTypeUtils.getType(nullSafeType).getName()));
          }
        }
      });

      modelProperties.add(new NullSafeModelProperty(nullSafeType));
      if (hasDefaultOverride) {
        modelProperties.add(new DefaultImplementingTypeModelProperty(nullSafeType));
      }
    }
  }
}
