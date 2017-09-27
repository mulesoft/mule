/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.mule.metadata.java.api.utils.JavaTypeUtils.getType;
import static org.mule.runtime.api.meta.model.parameter.ParameterGroupModel.DEFAULT_GROUP_NAME;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.isMap;
import static org.mule.runtime.extension.api.util.ExtensionModelUtils.roleOf;
import static org.mule.runtime.extension.api.util.NameUtils.getComponentDeclarationTypeName;
import static org.mule.runtime.module.extension.internal.loader.utils.ModelLoaderUtils.isProcessorChain;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getExpressionSupport;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getFieldsWithGetters;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.isInstantiable;
import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.visitor.BasicTypeMetadataVisitor;
import org.mule.runtime.api.meta.model.ParameterDslConfiguration;
import org.mule.runtime.api.meta.model.declaration.fluent.Declarer;
import org.mule.runtime.api.meta.model.declaration.fluent.HasNestedComponentsDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.HasParametersDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.NamedDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterGroupDeclarer;
import org.mule.runtime.api.meta.model.display.LayoutModel;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.dsl.xml.ParameterDsl;
import org.mule.runtime.extension.api.annotation.metadata.MetadataKeyId;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.ConfigOverride;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.Content;
import org.mule.runtime.extension.api.annotation.param.ExclusiveOptionals;
import org.mule.runtime.extension.api.annotation.param.NullSafe;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.exception.IllegalParameterModelDefinitionException;
import org.mule.runtime.extension.api.property.DefaultImplementingTypeModelProperty;
import org.mule.runtime.module.extension.internal.loader.ParameterGroupDescriptor;
import org.mule.runtime.module.extension.internal.loader.java.contributor.ParameterDeclarerContributor;
import org.mule.runtime.module.extension.internal.loader.java.property.DeclaringMemberModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ImplementingParameterModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.NullSafeModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ParameterGroupModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.type.ExtensionParameter;
import org.mule.runtime.module.extension.internal.loader.java.type.FieldElement;
import org.mule.runtime.module.extension.internal.loader.java.type.Type;
import org.mule.runtime.module.extension.internal.loader.java.type.WithAlias;
import org.mule.runtime.module.extension.internal.loader.java.type.runtime.FieldWrapper;
import org.mule.runtime.module.extension.internal.loader.utils.ParameterDeclarationContext;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class ParameterModelsLoaderDelegate {

  private List<ParameterDeclarerContributor> contributors;
  private ClassTypeLoader typeLoader;

  public ParameterModelsLoaderDelegate(List<ParameterDeclarerContributor> contributors, ClassTypeLoader loader) {
    this.contributors = contributors;
    this.typeLoader = loader;
  }

  public List<ParameterDeclarer> declare(HasParametersDeclarer component,
                                         List<? extends ExtensionParameter> parameters,
                                         ParameterDeclarationContext declarationContext) {
    return declare(component, parameters, declarationContext, null);
  }

  public List<ParameterDeclarer> declare(HasParametersDeclarer component,
                                         List<? extends ExtensionParameter> parameters,
                                         ParameterDeclarationContext declarationContext,
                                         ParameterGroupDeclarer parameterGroupDeclarer) {
    List<ParameterDeclarer> declarerList = new ArrayList<>();
    checkAnnotationsNotUsedMoreThanOnce(parameters, Connection.class, Config.class, MetadataKeyId.class);

    boolean supportsNestedElements = component instanceof HasNestedComponentsDeclarer;
    for (ExtensionParameter extensionParameter : parameters) {

      // If the element being resolved accepts components to be declared as NestableElements, like any ComponentModel,
      // then we will parse it as a component instead of a parameter.
      // Both nested components and parameters are declared using the @Parameter annotation in order to simplify the API
      if (supportsNestedElements && declaredAsNestedComponent((HasNestedComponentsDeclarer) component, extensionParameter)) {
        continue;
      }

      if (!extensionParameter.shouldBeAdvertised()) {
        continue;
      }

      if (declaredAsGroup(component, declarationContext, extensionParameter)) {
        continue;
      }

      ParameterGroupDeclarer groupDeclarer =
          parameterGroupDeclarer != null ? parameterGroupDeclarer : component.onDefaultParameterGroup();

      ParameterDeclarer parameter;
      if (extensionParameter.isRequired()) {
        parameter = groupDeclarer.withRequiredParameter(extensionParameter.getAlias());
      } else {
        parameter = groupDeclarer.withOptionalParameter(extensionParameter.getAlias())
            .defaultingTo(extensionParameter.defaultValue().isPresent() ? extensionParameter.defaultValue().get() : null);
      }

      parameter.ofType(extensionParameter.getMetadataType(typeLoader)).describedAs(extensionParameter.getDescription());
      parseParameterRole(extensionParameter, parameter);
      parseExpressionSupport(extensionParameter, parameter);
      parseConfigOverride(extensionParameter, parameter);
      parseNullSafe(extensionParameter, parameter);
      parseLayout(extensionParameter, parameter);
      addImplementingTypeModelProperty(extensionParameter, parameter);
      parseParameterDsl(extensionParameter, parameter);
      contributors.forEach(contributor -> contributor.contribute(extensionParameter, parameter, declarationContext));
      declarerList.add(parameter);
    }

    return declarerList;
  }

  private boolean declaredAsNestedComponent(HasNestedComponentsDeclarer component, ExtensionParameter extensionParameter) {
    if (isProcessorChain(extensionParameter)) {
      component.withChain(extensionParameter.getAlias())
          .setRequired(extensionParameter.isRequired())
          .describedAs(extensionParameter.getDescription());

      return true;
    }

    return false;
  }

  private void parseConfigOverride(ExtensionParameter extensionParameter, ParameterDeclarer parameter) {
    if (extensionParameter.getAnnotation(ConfigOverride.class).isPresent()) {
      parameter.asConfigOverride();
    }
  }

  private boolean declaredAsGroup(HasParametersDeclarer component,
                                  ParameterDeclarationContext declarationContext,
                                  ExtensionParameter groupParameter)
      throws IllegalParameterModelDefinitionException {

    ParameterGroup groupAnnotation = groupParameter.getAnnotation(ParameterGroup.class).orElse(null);
    if (groupAnnotation == null) {
      return false;
    }

    final String groupName = groupAnnotation.name();
    if (DEFAULT_GROUP_NAME.equals(groupName)) {
      throw new IllegalParameterModelDefinitionException(
                                                         format("%s '%s' defines parameter group of name '%s' which is the default one. "
                                                             + "@%s cannot be used with the default group name",
                                                                getComponentDeclarationTypeName(((Declarer) component)
                                                                    .getDeclaration()),
                                                                ((NamedDeclaration) ((Declarer) component).getDeclaration())
                                                                    .getName(),
                                                                groupName,
                                                                ParameterGroup.class.getSimpleName()));
    }

    final Type type = groupParameter.getType();

    final List<FieldElement> nestedGroups = type.getAnnotatedFields(ParameterGroup.class);
    if (!nestedGroups.isEmpty()) {
      throw new IllegalParameterModelDefinitionException(format(
                                                                "Class '%s' is used as a @%s but contains fields which also hold that annotation. Nesting groups is not allowed. "
                                                                    + "Offending fields are: [%s]",
                                                                type.getName(),
                                                                ParameterGroup.class.getSimpleName(),
                                                                nestedGroups.stream().map(element -> element.getName())
                                                                    .collect(joining(","))));
    }

    if (groupParameter.isAnnotatedWith(org.mule.runtime.extension.api.annotation.param.Optional.class)) {
      throw new IllegalParameterModelDefinitionException(format(
                                                                "@%s can not be applied alongside with @%s. Affected parameter is [%s].",
                                                                org.mule.runtime.extension.api.annotation.param.Optional.class
                                                                    .getSimpleName(),
                                                                ParameterGroup.class.getSimpleName(),
                                                                groupParameter.getName()));
    }

    ParameterGroupDeclarer declarer = component.onParameterGroup(groupName);
    if (declarer.getDeclaration().getModelProperty(ParameterGroupModelProperty.class).isPresent()) {
      throw new IllegalParameterModelDefinitionException(format("Parameter group '%s' has already been declared on %s '%s'",
                                                                groupName,
                                                                getComponentDeclarationTypeName(((Declarer) component)
                                                                    .getDeclaration()),
                                                                ((NamedDeclaration) ((Declarer) component).getDeclaration())
                                                                    .getName()));
    } else {
      declarer.withModelProperty(new ParameterGroupModelProperty(
                                                                 new ParameterGroupDescriptor(groupName, type,
                                                                                              groupParameter
                                                                                                  .getMetadataType(typeLoader),
                                                                                              groupParameter
                                                                                                  .getDeclaringElement())));
    }

    final List<FieldElement> annotatedParameters = type.getAnnotatedFields(Parameter.class);
    type.getAnnotation(ExclusiveOptionals.class).ifPresent(annotation -> {
      Set<String> optionalParamNames = annotatedParameters.stream()
          .filter(f -> !f.isRequired())
          .map(WithAlias::getAlias)
          .collect(toSet());

      declarer.withExclusiveOptionals(optionalParamNames, annotation.isOneRequired());
    });


    declarer.withDslInlineRepresentation(groupAnnotation.showInDsl());

    MuleExtensionAnnotationParser.parseLayoutAnnotations(groupParameter, LayoutModel.builder()).ifPresent(declarer::withLayout);

    if (!annotatedParameters.isEmpty()) {
      declare(component, annotatedParameters, declarationContext, declarer);
    } else {
      Class declaringClass = type.getDeclaringClass();
      List<FieldWrapper> fields = getFieldsWithGetters(declaringClass).stream().map(FieldWrapper::new).collect(toList());
      declare(component, fields, declarationContext, declarer);
    }

    return true;
  }

  private void parseParameterRole(ExtensionParameter extensionParameter, ParameterDeclarer parameter) {
    parameter.withRole(roleOf(extensionParameter.getAnnotation(Content.class)));
  }

  private void parseExpressionSupport(ExtensionParameter extensionParameter, ParameterDeclarer parameter) {
    extensionParameter.getAnnotation(Expression.class)
        .ifPresent(expression -> parameter.withExpressionSupport(getExpressionSupport(expression)));
  }

  private void parseNullSafe(ExtensionParameter extensionParameter, ParameterDeclarer parameter) {
    if (extensionParameter.isAnnotatedWith(NullSafe.class)) {
      if (extensionParameter.isAnnotatedWith(ConfigOverride.class)) {
        throw new IllegalParameterModelDefinitionException(
                                                           format("Parameter '%s' is annotated with '@%s' and also marked as a config override, which is redundant. "
                                                               + "The default value for this parameter will come from the configuration parameter",
                                                                  extensionParameter.getName(), NullSafe.class.getSimpleName()));
      }
      if (extensionParameter.isRequired() && !extensionParameter.isAnnotatedWith(ParameterGroup.class)) {
        throw new IllegalParameterModelDefinitionException(
                                                           format("Parameter '%s' is required but annotated with '@%s', which is redundant",
                                                                  extensionParameter.getName(), NullSafe.class.getSimpleName()));
      }

      Class<?> defaultType = extensionParameter.getAnnotation(NullSafe.class).get().defaultImplementingType();
      final boolean hasDefaultOverride = !defaultType.equals(Object.class);

      MetadataType nullSafeType = hasDefaultOverride ? typeLoader.load(defaultType) : parameter.getDeclaration().getType();

      parameter.getDeclaration().getType().accept(new BasicTypeMetadataVisitor() {

        @Override
        protected void visitBasicType(MetadataType metadataType) {
          throw new IllegalParameterModelDefinitionException(
                                                             format("Parameter '%s' is annotated with '@%s' but is of type '%s'. That annotation can only be "
                                                                 + "used with complex types (Pojos, Lists, Maps)",
                                                                    extensionParameter.getName(), NullSafe.class.getSimpleName(),
                                                                    extensionParameter.getType().getName()));
        }

        @Override
        public void visitArrayType(ArrayType arrayType) {
          if (hasDefaultOverride) {
            throw new IllegalParameterModelDefinitionException(format("Parameter '%s' is annotated with '@%s' is of type '%s'"
                + " but a 'defaultImplementingType' was provided."
                + " Type override is not allowed for Collections",
                                                                      extensionParameter.getName(),
                                                                      NullSafe.class.getSimpleName(),
                                                                      extensionParameter.getType().getName()));
          }
        }

        @Override
        public void visitObject(ObjectType objectType) {
          if (hasDefaultOverride && isMap(objectType)) {
            throw new IllegalParameterModelDefinitionException(format("Parameter '%s' is annotated with '@%s' is of type '%s'"
                + " but a 'defaultImplementingType' was provided."
                + " Type override is not allowed for Maps",
                                                                      extensionParameter.getName(),
                                                                      NullSafe.class.getSimpleName(),
                                                                      extensionParameter.getType().getName()));
          }

          if (hasDefaultOverride && isInstantiable(objectType)) {
            throw new IllegalParameterModelDefinitionException(
                                                               format("Parameter '%s' is annotated with '@%s' is of concrete type '%s',"
                                                                   + " but a 'defaultImplementingType' was provided."
                                                                   + " Type override is not allowed for concrete types",
                                                                      extensionParameter.getName(),
                                                                      NullSafe.class.getSimpleName(),
                                                                      extensionParameter.getType().getName()));
          }

          if (!isInstantiable(nullSafeType) && !isMap(nullSafeType)) {
            throw new IllegalParameterModelDefinitionException(
                                                               format("Parameter '%s' is annotated with '@%s' but is of type '%s'. That annotation can only be "
                                                                   + "used with complex instantiable types (Pojos, Lists, Maps)",
                                                                      extensionParameter.getName(),
                                                                      NullSafe.class.getSimpleName(),
                                                                      extensionParameter.getType().getName()));
          }

          if (hasDefaultOverride && !getType(parameter.getDeclaration().getType()).isAssignableFrom(getType(nullSafeType))) {
            throw new IllegalParameterModelDefinitionException(
                                                               format("Parameter '%s' is annotated with '@%s' of type '%s', but provided type '%s"
                                                                   + " is not a subtype of the parameter's type",
                                                                      extensionParameter.getName(),
                                                                      NullSafe.class.getSimpleName(),
                                                                      extensionParameter.getType().getName(),
                                                                      getType(nullSafeType).getName()));
          }
        }
      });

      parameter.withModelProperty(new NullSafeModelProperty(nullSafeType));
      if (hasDefaultOverride) {
        parameter.withModelProperty(new DefaultImplementingTypeModelProperty(nullSafeType));

      }
    }
  }

  private void parseLayout(ExtensionParameter extensionParameter, ParameterDeclarer parameter) {
    MuleExtensionAnnotationParser.parseLayoutAnnotations(extensionParameter, LayoutModel.builder())
        .ifPresent(parameter::withLayout);
  }

  private void parseParameterDsl(ExtensionParameter extensionParameter, ParameterDeclarer parameter) {
    extensionParameter.getAnnotation(ParameterDsl.class).ifPresent(
                                                                   parameterDsl -> parameter
                                                                       .withDsl(ParameterDslConfiguration.builder()
                                                                           .allowsInlineDefinition(parameterDsl
                                                                               .allowInlineDefinition())
                                                                           .allowsReferences(parameterDsl.allowReferences())
                                                                           .build()));
  }

  private void checkAnnotationsNotUsedMoreThanOnce(List<? extends ExtensionParameter> parameters,
                                                   Class<? extends Annotation>... annotations) {
    for (Class<? extends Annotation> annotation : annotations) {
      final long count = parameters.stream().filter(param -> param.isAnnotatedWith(annotation)).count();
      if (count > 1) {
        throw new IllegalModelDefinitionException(
                                                  format("The defined parameters %s from %s, uses the annotation @%s more than once",
                                                         parameters.stream().map(p -> p.getName()).collect(toList()),
                                                         parameters.iterator().next().getOwnerDescription(),
                                                         annotation.getSimpleName()));
      }
    }
  }

  private void addImplementingTypeModelProperty(ExtensionParameter extensionParameter, ParameterDeclarer parameter) {
    AnnotatedElement element = extensionParameter.getDeclaringElement();
    parameter.withModelProperty(element instanceof Field
        ? new DeclaringMemberModelProperty(((Field) element))
        : new ImplementingParameterModelProperty((java.lang.reflect.Parameter) element));
  }



}
