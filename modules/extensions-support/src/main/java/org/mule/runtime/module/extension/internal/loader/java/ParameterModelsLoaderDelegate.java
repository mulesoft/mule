/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java;

import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.api.meta.model.parameter.ParameterGroupModel.DEFAULT_GROUP_NAME;
<<<<<<< HEAD
=======
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.isMap;
import static org.mule.runtime.extension.api.util.ExtensionModelUtils.roleOf;
import static org.mule.runtime.extension.api.util.NameUtils.getComponentDeclarationTypeName;
import static org.mule.runtime.module.extension.internal.loader.java.MuleExtensionAnnotationParser.parseLayoutAnnotations;
import static org.mule.runtime.module.extension.internal.loader.utils.ModelLoaderUtils.isProcessorChain;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getExpressionSupport;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getFieldsWithGetters;
import static org.mule.runtime.module.extension.internal.util.ParameterGroupUtils.getParameterGroupInfo;
>>>>>>> daf9920d2e3 (MULE-18561:Adjust sdk-api implicit argument handling)

import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.StringType;
import org.mule.runtime.api.meta.model.declaration.fluent.HasParametersDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterGroupDeclarer;
import org.mule.runtime.connectivity.api.platform.schema.extension.ExcludeFromConnectivitySchemaModelProperty;
<<<<<<< HEAD
import org.mule.runtime.module.extension.internal.loader.parser.ParameterGroupModelParser;

=======
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
import org.mule.runtime.module.extension.internal.loader.utils.ParameterDeclarationContext;
import org.mule.sdk.api.annotation.semantics.connectivity.ExcludeFromConnectivitySchema;
import org.mule.runtime.module.extension.internal.loader.utils.ParameterGroupInfo;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.util.ArrayList;
>>>>>>> daf9920d2e3 (MULE-18561:Adjust sdk-api implicit argument handling)
import java.util.LinkedList;
import java.util.List;

public final class ParameterModelsLoaderDelegate {


<<<<<<< HEAD
  public List<ParameterDeclarer> declare(HasParametersDeclarer component, List<ParameterGroupModelParser> groupParsers) {
=======
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
    checkAnnotationsNotUsedMoreThanOnce(parameters, Connection.class, org.mule.sdk.api.annotation.param.Connection.class,
                                        Config.class, org.mule.sdk.api.annotation.param.Config.class, MetadataKeyId.class);

    boolean supportsNestedElements = component instanceof HasNestedComponentsDeclarer;
    for (ExtensionParameter extensionParameter : parameters) {
>>>>>>> daf9920d2e3 (MULE-18561:Adjust sdk-api implicit argument handling)

    final List<ParameterDeclarer> declarerList = new LinkedList<>();
    groupParsers.forEach(group -> {
      ParameterGroupDeclarer groupDeclarer;

      if (DEFAULT_GROUP_NAME.equals(group.getName())) {
        groupDeclarer = component.onDefaultParameterGroup();
      } else {
        groupDeclarer = component.onParameterGroup(group.getName())
            .withDslInlineRepresentation(group.showsInDsl());

        group.getDisplayModel().ifPresent(groupDeclarer::withDisplayModel);
        group.getLayoutModel().ifPresent(groupDeclarer::withLayout);
        group.getAdditionalModelProperties().forEach(groupDeclarer::withModelProperty);
        group.getExclusiveOptionals().ifPresent(descriptor -> groupDeclarer
            .withExclusiveOptionals(descriptor.getExclusiveOptionals(), descriptor.isOneRequired()));
        groupDeclarer.getDeclaration().setDescription(group.getDescription());
        group.getLayoutModel().ifPresent(groupDeclarer::withLayout);
      }

      group.getParameterParsers().forEach(extensionParameter -> {
        ParameterDeclarer parameter;
        if (extensionParameter.isRequired()) {
          parameter = groupDeclarer.withRequiredParameter(extensionParameter.getName());
        } else {
          parameter = groupDeclarer.withOptionalParameter(extensionParameter.getName())
              .defaultingTo(extensionParameter.getDefaultValue());
        }

        final MetadataType metadataType = extensionParameter.getType();
        parameter.ofType(metadataType)
            .describedAs(extensionParameter.getDescription())
            .withAllowedStereotypes(extensionParameter.getAllowedStereotypes())
            .withRole(extensionParameter.getRole())
            .withExpressionSupport(extensionParameter.getExpressionSupport());

        if (extensionParameter.isComponentId()) {
          parameter.asComponentId();
        }

        if (extensionParameter.isConfigOverride()) {
          parameter.asConfigOverride();
        }

        if (extensionParameter.isExcludedFromConnectivitySchema()) {
          parameter.withModelProperty(new ExcludeFromConnectivitySchemaModelProperty());
        }

        extensionParameter.getLayoutModel().ifPresent(parameter::withLayout);
        extensionParameter.getDslConfiguration().ifPresent(parameter::withDsl);
        extensionParameter.getDeprecationModel().ifPresent(parameter::withDeprecation);
        extensionParameter.getAdditionalModelProperties().forEach(parameter::withModelProperty);
        declarerList.add(parameter);
      });

      if (declarerList.stream().noneMatch(p -> p.getDeclaration().isComponentId())) {
        declarerList.stream()
            .filter(p -> p.getDeclaration().getName().equals("name")
                && p.getDeclaration().isRequired()
                && p.getDeclaration().getExpressionSupport() == NOT_SUPPORTED
                && p.getDeclaration().getType() instanceof StringType
                && p.getDeclaration().getAllowedStereotypeModels().isEmpty())
            .forEach(p -> p.asComponentId());
      }
    });

    return declarerList;
  }
}
