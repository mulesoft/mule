/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.mule.metadata.java.api.utils.JavaTypeUtils.getType;
import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.api.meta.model.parameter.ParameterGroupModel.DEFAULT_GROUP_NAME;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.isMap;
import static org.mule.runtime.extension.api.util.ExtensionModelUtils.roleOf;
import static org.mule.runtime.extension.api.util.NameUtils.getComponentDeclarationTypeName;
import static org.mule.runtime.module.extension.internal.loader.java.MuleExtensionAnnotationParser.parseLayoutAnnotations;
import static org.mule.runtime.module.extension.internal.loader.utils.ModelLoaderUtils.isProcessorChain;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getExpressionSupport;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getFieldsWithGetters;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.visitor.BasicTypeMetadataVisitor;
import org.mule.runtime.api.meta.model.ParameterDslConfiguration;
import org.mule.runtime.api.meta.model.declaration.fluent.Declarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ExclusiveParametersDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.HasNestedComponentsDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.HasParametersDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.NamedDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterGroupDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterGroupDeclarer;
import org.mule.runtime.api.meta.model.display.DisplayModel;
import org.mule.runtime.api.meta.model.display.LayoutModel;
import org.mule.runtime.api.meta.model.parameter.ExclusiveParametersModel;
import org.mule.runtime.connectivity.api.platform.schema.extension.ExcludeFromConnectivitySchemaModelProperty;
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
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.stereotype.ComponentId;
import org.mule.runtime.extension.api.declaration.type.annotation.StereotypeTypeAnnotation;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.exception.IllegalParameterModelDefinitionException;
import org.mule.runtime.extension.api.model.parameter.ImmutableExclusiveParametersModel;
import org.mule.runtime.extension.api.property.DefaultImplementingTypeModelProperty;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionParameter;
import org.mule.runtime.module.extension.api.loader.java.type.FieldElement;
import org.mule.runtime.module.extension.api.loader.java.type.Type;
import org.mule.runtime.module.extension.api.loader.java.type.WithAlias;
import org.mule.runtime.module.extension.internal.loader.ParameterGroupDescriptor;
import org.mule.runtime.module.extension.internal.loader.java.contributor.ParameterDeclarerContributor;
import org.mule.runtime.module.extension.internal.loader.java.property.DeclaringMemberModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ExclusiveOptionalModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ImplementingParameterModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.NullSafeModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ParameterGroupModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.type.property.ExtensionParameterDescriptorModelProperty;
import org.mule.runtime.module.extension.internal.loader.parser.ParameterGroupModelParser;
import org.mule.runtime.module.extension.internal.loader.utils.ParameterDeclarationContext;
import org.mule.sdk.api.annotation.semantics.connectivity.ExcludeFromConnectivitySchema;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public final class ParameterModelsLoaderDelegate {

  private final List<ParameterDeclarerContributor> contributors;
  private final ClassTypeLoader typeLoader;

  public ParameterModelsLoaderDelegate(List<ParameterDeclarerContributor> contributors, ClassTypeLoader loader) {
    this.contributors = contributors;
    this.typeLoader = loader;
  }

  public List<ParameterDeclarer> declare(HasParametersDeclarer component,
                                         List<ParameterGroupModelParser> groupParsers,
                                         ParameterDeclarationContext declarationContext) {

    final List<ParameterDeclarer> declarerList = new LinkedList<>();
    groupParsers.forEach(group -> {
      ParameterGroupDeclarer groupDeclarer;

      if (DEFAULT_GROUP_NAME.equals(group.getName())) {
        groupDeclarer = component.onDefaultParameterGroup();
      } else {
        groupDeclarer = component.onParameterGroup(group.getName())
            .withDslInlineRepresentation(group.showsInDsl());

        group.getDisplayModel().ifPresent(groupDeclarer::withDisplayModel);
        group.getAdditionalModelProperties().forEach(groupDeclarer::withModelProperty);
        group.getExclusiveOptionals().ifPresent(descriptor -> groupDeclarer.withExclusiveOptionals(descriptor.getExclusiveOptionals(), descriptor.isOneRequired()));
        groupDeclarer.getDeclaration().setDescription(group.getDescription());
        group.getLayoutModel().ifPresent(groupDeclarer::withLayout);
      }
    });


    boolean supportsNestedElements = component instanceof HasNestedComponentsDeclarer;
    for (ExtensionParameter extensionParameter : parameters) {


      if (!extensionParameter.shouldBeAdvertised()) {
        continue;
      }

      ParameterDeclarer parameter;
      if (extensionParameter.isRequired()) {
        parameter = groupDeclarer.withRequiredParameter(extensionParameter.getAlias());
      } else {
        parameter = groupDeclarer.withOptionalParameter(extensionParameter.getAlias())
            .defaultingTo(extensionParameter.defaultValue().isPresent() ? extensionParameter.defaultValue().get() : null);
      }

      final MetadataType metadataType = extensionParameter.getType().asMetadataType();
      parameter.ofType(metadataType).describedAs(extensionParameter.getDescription());
      metadataType.getAnnotation(StereotypeTypeAnnotation.class).ifPresent(st -> {
        parameter.withAllowedStereotypes(st.getAllowedStereotypes());
      });
      parseParameterRole(extensionParameter, parameter);
      parseExpressionSupport(extensionParameter, parameter);
      parseConfigOverride(extensionParameter, parameter);
      parseComponentId(extensionParameter, parameter);
      parseNullSafe(extensionParameter, parameter);
      parseLayout(extensionParameter, parameter);
      parseExclusiveOptional(extensionParameter, groupDeclarer, parameter);
      parseExcludeFromConnectivitySchema(extensionParameter, parameter);
      parameter.withModelProperty(new ExtensionParameterDescriptorModelProperty(extensionParameter));
      extensionParameter.getDeclaringElement().ifPresent(element -> addImplementingTypeModelProperty(element, parameter));
      parseParameterDsl(extensionParameter, parameter);
      contributors.forEach(contributor -> contributor.contribute(extensionParameter, parameter, declarationContext));
      declarerList.add(parameter);
    }

    if (declarerList.stream().noneMatch(p -> p.getDeclaration().isComponentId())) {
      declarerList.stream()
          .filter(p -> p.getDeclaration().getName().equals("name")
              && p.getDeclaration().isRequired()
              && p.getDeclaration().getExpressionSupport() == NOT_SUPPORTED
              && p.getDeclaration().getType().equals(typeLoader.load(String.class))
              && p.getDeclaration().getAllowedStereotypeModels().isEmpty())
          .forEach(p -> p.asComponentId());
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

  private void parseExclusiveOptional(ExtensionParameter extensionParameter, ParameterGroupDeclarer parameterGroupDeclarer,
                                      ParameterDeclarer parameter) {
    ParameterGroupDeclaration groupDeclaration = (ParameterGroupDeclaration) parameterGroupDeclarer.getDeclaration();
    List<ExclusiveParametersDeclaration> exclusiveParameters = groupDeclaration.getExclusiveParameters();
    exclusiveParameters.stream()
        .filter(group -> group.getParameterNames().contains(extensionParameter.getAlias()))
        .findFirst()
        .ifPresent(exclusiveParametersDeclaration -> {
          ExclusiveParametersModel exclusiveParametersModel =
              new ImmutableExclusiveParametersModel(exclusiveParametersDeclaration.getParameterNames(),
                                                    exclusiveParametersDeclaration.isRequiresOne());
          parameter.withModelProperty(new ExclusiveOptionalModelProperty(exclusiveParametersModel));
        });
  }

  private void parseExcludeFromConnectivitySchema(ExtensionParameter extensionParameter, ParameterDeclarer parameter) {
    extensionParameter.getAnnotation(ExcludeFromConnectivitySchema.class)
        .ifPresent(a -> parameter.withModelProperty(new ExcludeFromConnectivitySchemaModelProperty()));
  }

  private void parseConfigOverride(ExtensionParameter extensionParameter, ParameterDeclarer parameter) {
    extensionParameter.getAnnotation(ConfigOverride.class)
        .ifPresent(a -> parameter.asConfigOverride());
  }

  private void parseComponentId(ExtensionParameter extensionParameter, ParameterDeclarer parameter) {
    extensionParameter.getAnnotation(ComponentId.class)
        .ifPresent(a -> parameter.asComponentId());
  }

  private void parseParameterRole(ExtensionParameter extensionParameter, ParameterDeclarer parameter) {
    parameter.withRole(roleOf(extensionParameter.getAnnotation(Content.class)));
  }

  private void parseExpressionSupport(ExtensionParameter extensionParameter, ParameterDeclarer parameter) {
    extensionParameter.getAnnotation(Expression.class)
        .ifPresent(expression -> parameter.withExpressionSupport(getExpressionSupport(expression)));
  }



  private void parseLayout(ExtensionParameter extensionParameter, ParameterDeclarer parameter) {
    parseLayoutAnnotations(extensionParameter, LayoutModel.builder())
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



  private void addImplementingTypeModelProperty(AnnotatedElement element, ParameterDeclarer parameter) {
    parameter.withModelProperty(element instanceof Field
        ? new DeclaringMemberModelProperty(((Field) element))
        : new ImplementingParameterModelProperty((java.lang.reflect.Parameter) element));
  }



}
