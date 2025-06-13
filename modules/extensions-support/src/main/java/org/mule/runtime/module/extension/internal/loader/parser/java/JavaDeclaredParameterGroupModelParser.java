/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser.java;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;
import static org.mule.runtime.api.meta.model.parameter.ParameterGroupModel.DEFAULT_GROUP_NAME;
import static org.mule.runtime.module.extension.internal.loader.parser.java.MuleExtensionAnnotationParser.mapReduceAnnotation;
import static org.mule.runtime.module.extension.internal.loader.parser.java.MuleExtensionAnnotationParser.mapReduceSingleAnnotation;
import static org.mule.runtime.module.extension.internal.loader.parser.java.MuleExtensionAnnotationParser.parseLayoutAnnotations;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getFieldsWithGetters;

import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.api.meta.model.display.DisplayModel;
import org.mule.runtime.api.meta.model.display.LayoutModel;
import org.mule.runtime.extension.api.annotation.param.ExclusiveOptionals;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.exception.IllegalParameterModelDefinitionException;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionParameter;
import org.mule.runtime.module.extension.api.loader.java.type.FieldElement;
import org.mule.runtime.module.extension.api.loader.java.type.Type;
import org.mule.runtime.module.extension.api.loader.java.type.WithAlias;
import org.mule.runtime.module.extension.internal.loader.ParameterGroupDescriptor;
import org.mule.runtime.module.extension.internal.loader.java.property.ParameterGroupModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.type.property.ExtensionParameterDescriptorModelProperty;
import org.mule.runtime.extension.api.loader.parser.ParameterGroupModelParser;
import org.mule.runtime.extension.api.loader.parser.ParameterModelParser;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Java based implementation of {@link ParameterGroupModelParser} for non-default parameter groups explicitly defined in the
 * extension's code.
 *
 * @since 4.5.0
 */
public class JavaDeclaredParameterGroupModelParser extends AbstractJavaParameterGroupModelParser
    implements HasExtensionParameter {

  private final List<ExtensionParameter> parameters;
  private final ExtensionParameter groupParameter;
  private final Type type;
  private final String groupName;

  private Optional<ExclusiveOptionalDescriptor> exclusiveOptionalDescriptor;

  public JavaDeclaredParameterGroupModelParser(ExtensionParameter groupParameter,
                                               ParameterDeclarationContext context,
                                               Function<ParameterModelParser, ParameterModelParser> parameterMutator) {
    super(context, parameterMutator);

    this.groupParameter = groupParameter;
    type = groupParameter.getType();
    groupName = fetchGroupName();
    parameters = fetchAnnotatedParameter();

    assureValid(groupParameter, context);
    parseStructure();
  }

  private void parseStructure() {
    parseExclusiveOptionalDescriptor();
  }

  @Override
  public String getName() {
    return groupName;
  }

  @Override
  public String getDescription() {
    return groupParameter.getDescription();
  }

  @Override
  protected Stream<ExtensionParameter> doGetParameters() {
    return parameters.stream();
  }

  @Override
  public Optional<DisplayModel> getDisplayModel() {
    return mapReduceAnnotation(groupParameter, DisplayName.class, org.mule.sdk.api.annotation.param.display.DisplayName.class,
                               legacyAnnotationValueFetcher -> buildDisplayModel(legacyAnnotationValueFetcher
                                   .getStringValue(DisplayName::value)),
                               sdkAnnotationValueFetcher -> buildDisplayModel(sdkAnnotationValueFetcher
                                   .getStringValue(org.mule.sdk.api.annotation.param.display.DisplayName::value)),
                               () -> new IllegalParameterModelDefinitionException(format("ParameterGroup '%s' is annotated with '@%s' and '@%s' at the same time",
                                                                                         getName(),
                                                                                         DisplayName.class.getName(),
                                                                                         org.mule.sdk.api.annotation.param.display.DisplayName.class
                                                                                             .getName())));
  }

  @Override
  public Optional<LayoutModel> getLayoutModel() {
    return parseLayoutAnnotations(groupParameter, LayoutModel.builder(), groupParameter.getName());
  }

  @Override
  public Optional<ExclusiveOptionalDescriptor> getExclusiveOptionals() {
    return exclusiveOptionalDescriptor;
  }

  @Override
  public boolean showsInDsl() {
    return mapReduceSingleAnnotation(groupParameter, "parameter group", "",
                                     ParameterGroup.class,
                                     org.mule.sdk.api.annotation.param.ParameterGroup.class,
                                     value -> value.getBooleanValue(ParameterGroup::showInDsl),
                                     value -> value
                                         .getBooleanValue(org.mule.sdk.api.annotation.param.ParameterGroup::showInDsl))
                                             .orElse(false);
  }

  @Override
  public List<ModelProperty> getAdditionalModelProperties() {
    List<ModelProperty> properties = new LinkedList<>();
    properties.add(new ParameterGroupModelProperty(
                                                   new ParameterGroupDescriptor(groupName, type,
                                                                                groupParameter.getType().asMetadataType(),
                                                                                // TODO: Eliminate dependency to Annotated
                                                                                // Elements
                                                                                groupParameter.getDeclaringElement().orElse(null),
                                                                                groupParameter)));
    properties.add(new ExtensionParameterDescriptorModelProperty(groupParameter));

    return properties;
  }

  private void assureValid(ExtensionParameter groupParameter, ParameterDeclarationContext context) {
    if (DEFAULT_GROUP_NAME.equals(groupName)) {
      throw new IllegalParameterModelDefinitionException(
                                                         format("%s '%s' defines parameter group of name '%s' which is the default one. "
                                                             + "@%s cannot be used with the default group name",
                                                                context.getComponentType(),
                                                                context.getComponentName(),
                                                                groupName,
                                                                ParameterGroup.class.getSimpleName()));
    }

    final List<FieldElement> nestedGroups =
        type.getAnnotatedFields(ParameterGroup.class, org.mule.sdk.api.annotation.param.ParameterGroup.class);
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

    if (groupParameter.isAnnotatedWith(org.mule.sdk.api.annotation.param.Optional.class)) {
      throw new IllegalParameterModelDefinitionException(format(
                                                                "@%s can not be applied alongside with @%s. Affected parameter is [%s].",
                                                                org.mule.sdk.api.annotation.param.Optional.class
                                                                    .getSimpleName(),
                                                                ParameterGroup.class.getSimpleName(),
                                                                groupParameter.getName()));
    }
  }

  private String fetchGroupName() {
    return mapReduceSingleAnnotation(groupParameter, "parameter group", "",
                                     ParameterGroup.class,
                                     org.mule.sdk.api.annotation.param.ParameterGroup.class,
                                     value -> value.getStringValue(ParameterGroup::name),
                                     value -> value.getStringValue(org.mule.sdk.api.annotation.param.ParameterGroup::name))
                                         .orElse(DEFAULT_GROUP_NAME);
  }

  private DisplayModel buildDisplayModel(String displayName) {
    return DisplayModel.builder().displayName(displayName).build();
  }

  private List<ExtensionParameter> fetchAnnotatedParameter() {
    List<? extends ExtensionParameter> parameters =
        type.getAnnotatedFields(Parameter.class, org.mule.sdk.api.annotation.param.Parameter.class);
    if (parameters.isEmpty()) {
      parameters = getFieldsWithGetters(type);
    }

    return (List<ExtensionParameter>) parameters;
  }

  private void parseExclusiveOptionalDescriptor() {
    Optional<Boolean> exclusiveOptionalsIsOneRequired =
        mapReduceSingleAnnotation(type, "parameter", "",
                                  ExclusiveOptionals.class,
                                  org.mule.sdk.api.annotation.param.ExclusiveOptionals.class,
                                  value -> value.getBooleanValue(ExclusiveOptionals::isOneRequired),
                                  value -> value
                                      .getBooleanValue(org.mule.sdk.api.annotation.param.ExclusiveOptionals::isOneRequired));

    exclusiveOptionalDescriptor =
        exclusiveOptionalsIsOneRequired.map(isOneRequired -> new ExclusiveOptionalDescriptor(parameters.stream()
            .filter(f -> !f.isRequired())
            .map(WithAlias::getAlias)
            .collect(toSet()), isOneRequired));
  }

  @Override
  public ExtensionParameter getExtensionParameter() {
    return groupParameter;
  }
}
