/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java;

import static java.lang.String.format;
import static java.util.Optional.of;
import static org.mule.runtime.module.extension.internal.loader.parser.java.notification.NotificationModelParserUtils.declareEmittedNotifications;
import static org.mule.metadata.api.model.MetadataFormat.JAVA;
import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.api.meta.model.parameter.ParameterGroupModel.DEFAULT_GROUP_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.BACK_PRESSURE_STRATEGY_PARAMETER_DESCRIPTION;
import static org.mule.runtime.extension.api.ExtensionConstants.BACK_PRESSURE_STRATEGY_PARAMETER_NAME;
import static org.mule.runtime.extension.api.annotation.param.display.Placement.ADVANCED_TAB;
import static org.mule.runtime.module.extension.internal.loader.utils.ModelLoaderUtils.addSemanticTerms;

import org.mule.metadata.api.annotation.EnumAnnotation;
import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.java.api.annotation.ClassInformationAnnotation;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.HasSourceDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.OptionalParameterDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterizedDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.SourceDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.SourceDeclarer;
import org.mule.runtime.api.meta.model.display.LayoutModel;
import org.mule.runtime.extension.api.exception.IllegalSourceModelDefinitionException;
import org.mule.runtime.extension.api.runtime.source.BackPressureMode;
import org.mule.runtime.module.extension.internal.loader.java.property.BackPressureStrategyModelProperty;
import org.mule.runtime.module.extension.internal.loader.parser.SourceModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.SourceModelParser.SourceCallbackModelParser;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Helper class for declaring sources through a {@link DefaultJavaModelLoaderDelegate}
 *
 * @since 4.0
 */
final class SourceModelLoaderDelegate extends AbstractModelLoaderDelegate {

  private final Map<SourceModelParser, SourceDeclarer> sourceDeclarers = new HashMap<>();

  SourceModelLoaderDelegate(DefaultJavaModelLoaderDelegate delegate) {
    super(delegate);
  }

  // TODO: MULE-9220: Add a Syntax validator which checks that a Source class doesn't try to declare operations, configs, etc
  void declareMessageSources(ExtensionDeclarer extensionDeclarer,
                             HasSourceDeclarer ownerDeclarer,
                             List<SourceModelParser> parsers) {

    for (SourceModelParser parser : parsers) {

      if (parser.isIgnored()) {
        continue;
      }

      final boolean requiresConfig = parser.hasConfig() || parser.isConnected();
      HasSourceDeclarer actualDeclarer = requiresConfig
          ? ownerDeclarer
          : extensionDeclarer;

      if (actualDeclarer == extensionDeclarer && requiresConfig) {
        throw new IllegalSourceModelDefinitionException(
                                                        format("Source '%s' is defined at the extension level but it requires a config parameter. "
                                                            + "Remove such parameter or move the source to the proper config",
                                                               parser.getName()));
      }


      SourceDeclarer existingDeclarer = sourceDeclarers.get(parser);
      if (existingDeclarer != null) {
        actualDeclarer.withMessageSource(existingDeclarer);
        continue;
      }

      SourceDeclarer sourceDeclarer = actualDeclarer.withMessageSource(parser.getName())
          .describedAs(parser.getDescription())
          .hasResponse(parser.emitsResponse())
          .requiresConnection(parser.isConnected())
          .transactional(parser.isTransactional())
          .supportsStreaming(parser.supportsStreaming())
          .runsOnPrimaryNodeOnly(parser.runsOnPrimaryNodeOnly());

      parser.getDeprecationModel().ifPresent(sourceDeclarer::withDeprecation);
      parser.getDisplayModel().ifPresent(d -> sourceDeclarer.getDeclaration().setDisplayModel(d));
      parser.getSourceFactoryModelProperty().ifPresent(sourceDeclarer::withModelProperty);

      parser.getOutputType().applyOn(sourceDeclarer.withOutput());
      parser.getAttributesOutputType().applyOn(sourceDeclarer.withOutputAttributes());

      loader.getParameterModelsLoaderDelegate().declare(sourceDeclarer, parser.getParameterGroupModelParsers());

      parser.getMediaTypeModelProperty().ifPresent(sourceDeclarer::withModelProperty);
      parser.getExceptionHandlerModelProperty().ifPresent(sourceDeclarer::withModelProperty);
      loader.registerOutputTypes(sourceDeclarer.getDeclaration());

      addSemanticTerms(sourceDeclarer.getDeclaration(), parser);
      declareEmittedNotifications(parser, sourceDeclarer, loader::getNotificationModel);
      getStereotypeModelLoaderDelegate().addStereotypes(
                                                        parser,
                                                        sourceDeclarer,
                                                        of(() -> getStereotypeModelLoaderDelegate()
                                                            .getDefaultSourceStereotype(parser.getName())));
      parser.getAdditionalModelProperties().forEach(sourceDeclarer::withModelProperty);

      // TODO: MULE-9220 add syntax validator to check that none of these use @UseConfig or @Connection
      declareSourceCallbackParameters(parser.getOnSuccessCallbackParser(), sourceDeclarer::onSuccess);
      declareSourceCallbackParameters(parser.getOnErrorCallbackParser(), sourceDeclarer::onError);
      declareSourceCallbackParameters(parser.getOnTerminateCallbackParser(), sourceDeclarer::onTerminate);
      declareSourceCallbackParameters(parser.getOnBackPressureCallbackParser(), sourceDeclarer::onBackPressure);

      declareBackPressureSupport(extensionDeclarer, sourceDeclarer, parser.getBackPressureStrategyModelProperty());

      sourceDeclarers.put(parser, sourceDeclarer);
    }
  }

  private void declareBackPressureSupport(ExtensionDeclarer extensionDeclarer, SourceDeclarer sourceDeclarer,
                                          Optional<BackPressureStrategyModelProperty> configuredBackPressureStrategyModelProperty) {
    BackPressureStrategyModelProperty backPressureStrategyModelProperty =
        configuredBackPressureStrategyModelProperty.orElseGet(BackPressureStrategyModelProperty::getDefault);
    sourceDeclarer.withModelProperty(backPressureStrategyModelProperty);
    if (backPressureStrategyModelProperty.getSupportedModes().size() > 1) {
      addBackPressureParameter(extensionDeclarer, sourceDeclarer, backPressureStrategyModelProperty);
    }
  }

  private void addBackPressureParameter(ExtensionDeclarer extensionDeclarer,
                                        SourceDeclarer sourceDeclarer,
                                        BackPressureStrategyModelProperty property) {

    OptionalParameterDeclarer parameter =
        sourceDeclarer.onParameterGroup(DEFAULT_GROUP_NAME).withOptionalParameter(BACK_PRESSURE_STRATEGY_PARAMETER_NAME);
    parameter.describedAs(BACK_PRESSURE_STRATEGY_PARAMETER_DESCRIPTION);
    parameter.defaultingTo(property.getDefaultMode());
    parameter.withExpressionSupport(NOT_SUPPORTED);
    parameter.withLayout(LayoutModel.builder().tabName(ADVANCED_TAB).build());

    MetadataType type = BaseTypeBuilder.create(JAVA).stringType()
        .id(format("%s-%s-backPressureStrategy", extensionDeclarer.getDeclaration().getName(),
                   sourceDeclarer.getDeclaration().getName()))
        .with(new EnumAnnotation<>(property.getSupportedModes().stream().map(BackPressureMode::name).toArray(String[]::new)))
        .with(new ClassInformationAnnotation(BackPressureMode.class))
        .build();
    parameter.ofDynamicType(type);
  }

  private void declareSourceCallbackParameters(Optional<SourceCallbackModelParser> parser,
                                               Supplier<ParameterizedDeclarer> declarer) {
    parser.ifPresent(callback -> loader.getParameterModelsLoaderDelegate().declare(declarer.get(),
                                                                                   callback.getParameterGroupModelParsers()));
  }
}
