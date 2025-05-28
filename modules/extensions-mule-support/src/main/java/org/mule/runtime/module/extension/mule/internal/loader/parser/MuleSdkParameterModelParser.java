/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.loader.parser;

import static org.mule.metadata.api.model.MetadataFormat.JAVA;
import static org.mule.runtime.api.component.ComponentIdentifier.buildFromStringRepresentation;
import static org.mule.runtime.api.meta.model.parameter.ParameterRole.BEHAVIOUR;
import static org.mule.runtime.api.meta.model.parameter.ParameterRole.CONTENT;
import static org.mule.runtime.module.extension.internal.type.catalog.SpecialTypesTypeLoader.VOID;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableSet;
import static java.util.Optional.empty;
import static java.util.Optional.of;

import org.mule.metadata.api.TypeLoader;
import org.mule.metadata.api.annotation.TypeAnnotation;
import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.java.api.annotation.ClassInformationAnnotation;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.meta.ExpressionSupport;
import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.api.meta.model.ParameterDslConfiguration;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.deprecated.DeprecationModel;
import org.mule.runtime.api.meta.model.display.DisplayModel;
import org.mule.runtime.api.meta.model.display.LayoutModel;
import org.mule.runtime.api.meta.model.parameter.ParameterRole;
import org.mule.runtime.api.meta.model.stereotype.StereotypeModel;
import org.mule.runtime.api.meta.version.MuleMinorVersion;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.model.ExtensionModelHelper;
import org.mule.runtime.extension.api.connectivity.oauth.OAuthParameterModelProperty;
import org.mule.runtime.extension.api.declaration.type.annotation.TypedValueTypeAnnotation;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.runtime.config.ConfigurationProvider;
import org.mule.runtime.module.extension.internal.loader.java.enricher.MetadataTypeEnricher;
import org.mule.runtime.module.extension.internal.loader.parser.ParameterModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.StereotypeModelFactory;
import org.mule.runtime.module.extension.internal.loader.parser.java.utils.ResolvedMinMuleVersion;
import org.mule.runtime.module.extension.internal.loader.parser.metadata.InputResolverModelParser;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * {@link ParameterModelParser} implementation for Mule SDK (required parameters).
 *
 * @since 4.5.0
 */
public class MuleSdkParameterModelParser extends BaseMuleSdkExtensionModelParser implements ParameterModelParser {

  private static final MetadataTypeEnricher METADATA_TYPE_ENRICHER = new MetadataTypeEnricher();
  private static final Set<TypeAnnotation> METADATA_TYPE_ANNOTATIONS = singleton(new TypedValueTypeAnnotation());
  private static final String MIN_MULE_VERSION = "4.5";

  /**
   * {@link MetadataType} representing a {@link ConfigurationProvider}, suitable for config-ref parameters.
   */
  private static final MetadataType CONFIG_TYPE = BaseTypeBuilder.create(JAVA)
      .objectType()
      .id(ConfigurationProvider.class.getName())
      .with(new ClassInformationAnnotation(ConfigurationProvider.class))
      .build();

  protected final ComponentAst parameterAst;
  private final TypeLoader typeLoader;
  private final ExtensionModelHelper extensionModelHelper;

  private String name;
  private MetadataType type;
  private List<StereotypeModel> allowedStereotypes = emptyList();
  private Optional<ParameterDslConfiguration> dslConfiguration = empty();
  private ParameterLayoutParser parameterLayoutParser;

  public MuleSdkParameterModelParser(ComponentAst parameterAst, TypeLoader typeLoader,
                                     ExtensionModelHelper extensionModelHelper) {
    this.parameterAst = parameterAst;
    this.typeLoader = typeLoader;
    this.extensionModelHelper = extensionModelHelper;

    parseStructure();
  }

  private void parseStructure() {
    parseName();
    parseType();
    parseLayout();
  }

  private void parseLayout() {
    parameterLayoutParser = new ParameterLayoutParser(parameterAst);
  }

  private void parseName() {
    name = getParameter(parameterAst, "name");
  }

  private void parseType() {
    final String type = getParameter(parameterAst, "type");
    if (VOID.equals(type)) {
      throw new IllegalModelDefinitionException(voidParameterIsForbidden());
    }

    if (!parseTypeFromTypeLoader(type) && !parseTypeAsConfigurationName(type)) {
      throw new IllegalModelDefinitionException(unknownType(type));
    }
  }

  /**
   * Attempts to parse the parameter type and extract the {@link MetadataType} from it using the {@link #typeLoader}.
   *
   * @param type The parameter's declared type.
   * @return {@code true} on success, {@code false} otherwise.
   */
  private boolean parseTypeFromTypeLoader(String type) {
    Optional<MetadataType> metadataType = typeLoader.load(type);
    if (!metadataType.isPresent()) {
      return false;
    }

    // We want to enrich all types with the TypedValue annotation so that the DataType is preserved and available inside the body
    // of the operation.
    // While this makes most sense for String and Stream types, we want to keep it for all attributes so that delegation to
    // composed operations that expect TypedValue parameters works properly.
    this.type = METADATA_TYPE_ENRICHER.enrich(metadataType.get(), METADATA_TYPE_ANNOTATIONS);
    return true;
  }

  /**
   * Attempts to parse the parameter type as if it were a configuration type from an extension in context.
   * <p>
   * The parameter's declared type will be of the form "extension_namespace:config_type_name".
   * <p>
   * If a {@link ConfigurationModel} can be found, we will treat the parameter as a configuration name. This means the type will
   * be the string primitive and only the {@link ConfigurationModel}'s stereotype will be allowed.
   *
   * @param type The parameter's declared type.
   * @return {@code true} on success, {@code false} otherwise.
   */
  private boolean parseTypeAsConfigurationName(String type) {
    // Tries to find the configuration model from the extensions in context
    ComponentIdentifier componentIdentifier = buildFromStringRepresentation(type);
    Optional<ConfigurationModel> configurationModel = extensionModelHelper.findConfigurationModel(componentIdentifier);
    if (!configurationModel.isPresent()) {
      return false;
    }

    // Sets the type as the string primitive and the allowed stereotype to the one from the configuration model
    this.type = CONFIG_TYPE;
    this.dslConfiguration = of(ParameterDslConfiguration.builder().allowsReferences(true).build());
    this.allowedStereotypes = singletonList(configurationModel.get().getStereotype());
    return true;
  }

  @Override
  public List<StereotypeModel> getAllowedStereotypes(StereotypeModelFactory factory) {
    return allowedStereotypes;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getDescription() {
    return this.<String>getOptionalParameter(parameterAst, "description").orElse("");
  }

  @Override
  public MetadataType getType() {
    return type;
  }

  private String unknownType(String type) {
    return format("Parameter '%s' references unknown type '%s'", getName(), type);
  }

  private String voidParameterIsForbidden() {
    return format("Parameter '%s' references type '%s', which is forbidden for parameters", getName(), VOID);
  }

  @Override
  public boolean isRequired() {
    return true;
  }

  @Override
  public Object getDefaultValue() {
    return null;
  }

  @Override
  public ParameterRole getRole() {
    if (getParameter(parameterAst, "allowInlineScript")) {
      return CONTENT;
    } else {
      return BEHAVIOUR;
    }
  }

  @Override
  public ExpressionSupport getExpressionSupport() {
    return ExpressionSupport.valueOf(getParameter(parameterAst, "expressionSupport"));
  }

  @Override
  public Optional<LayoutModel> getLayoutModel() {
    return parameterLayoutParser.getLayoutModel();
  }

  @Override
  public Optional<ParameterDslConfiguration> getDslConfiguration() {
    return dslConfiguration;
  }

  @Override
  public boolean isExcludedFromConnectivitySchema() {
    return false;
  }

  @Override
  public boolean isConfigOverride() {
    return getParameter(parameterAst, "configOverride");
  }

  @Override
  public boolean isComponentId() {
    return false;
  }

  @Override
  public List<ModelProperty> getAdditionalModelProperties() {
    return emptyList();
  }

  @Override
  public Optional<DeprecationModel> getDeprecationModel() {
    return getSingleChild(parameterAst, DEPRECATED_CONSTRUCT_NAME).map(this::buildDeprecationModel);
  }

  @Override
  public Optional<DisplayModel> getDisplayModel() {
    return parameterLayoutParser.getDisplayModel();
  }

  @Override
  public Optional<OAuthParameterModelProperty> getOAuthParameterModelProperty() {
    return empty();
  }

  @Override
  public Optional<ResolvedMinMuleVersion> getResolvedMinMuleVersion() {
    return of(new ResolvedMinMuleVersion(name, new MuleMinorVersion(MIN_MULE_VERSION),
                                         format("Parameter %s has min mule version %s because the Mule Sdk was introduced in that version.",
                                                name, MIN_MULE_VERSION)));
  }

  @Override
  public Set<String> getSemanticTerms() {
    return unmodifiableSet(parameterLayoutParser.getSemanticTerms());
  }

  @Override
  public Optional<InputResolverModelParser> getInputResolverModelParser() {
    return empty();
  }

  @Override
  public Optional<Pair<Integer, Boolean>> getMetadataKeyPart() {
    return empty();
  }
}
