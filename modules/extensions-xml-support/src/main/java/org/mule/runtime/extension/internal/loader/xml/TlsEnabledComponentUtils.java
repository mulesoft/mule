/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.extension.internal.loader.xml;

import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.api.meta.model.parameter.ParameterRole.BEHAVIOUR;
import static org.mule.runtime.extension.api.loader.util.InfrastructureTypeUtils.getMetadataTypeBasedInfrastructureType;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.getType;

import static java.lang.Boolean.parseBoolean;

import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.functional.Either;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterGroupDeclarer;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentParameterAst;
import org.mule.runtime.extension.api.declaration.type.DefaultExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.loader.util.InfrastructureTypeUtils.MetadataTypeBasedInfrastructureType;
import org.mule.runtime.extension.api.property.InfrastructureParameterModelProperty;
import org.mule.runtime.extension.api.property.SyntheticModelModelProperty;

import javax.xml.namespace.QName;

/**
 * Utility class for the support of TLS configuration macro-expansion on selected components.
 *
 * @since 4.6
 */
public class TlsEnabledComponentUtils {

  /**
   * The {@link QName qualified name} used to mark components as target for TLS context macro-expansion.
   *
   * @see #isTlsEnabled(ComponentAst)
   */
  public static final QName MODULE_TLS_ENABLED_MARKER_ANNOTATION_QNAME =
      new QName("http://www.w3.org/2000/xmlns/", "tlsEnabled", "xmlns");

  /**
   * @param parameter The parameter to evaluate.
   * @return Whether the given parameter is a TLS context parameter (candidate for TLS context macro-expansion).
   */
  public static boolean isTlsContextFactoryParameter(ComponentParameterAst parameter) {
    return isTlsContextFactoryParameter(parameter.getModel());
  }

  /**
   * @param componentAst The component to evaluate.
   * @return Whether the given component is a candidate for becoming a target of TLS context macro-expansion.
   */
  public static boolean isTlsConfigurationSupported(ComponentAst componentAst) {
    return componentAst.getModel(ParameterizedModel.class)
        .map(TlsEnabledComponentUtils::hasTlsContextFactoryParameter)
        .orElse(false);
  }

  /**
   * @param componentAst The component to evaluate.
   * @return Whether the given component has been marked as a target for TLS context macro-expansion.
   * @see #MODULE_TLS_ENABLED_MARKER_ANNOTATION_QNAME
   */
  public static boolean isTlsEnabled(ComponentAst componentAst) {
    Object annotation = componentAst.getAnnotations().get(MODULE_TLS_ENABLED_MARKER_ANNOTATION_QNAME.toString());
    if (annotation == null) {
      return false;
    }

    return parseBoolean(annotation.toString());
  }

  /**
   * Adds a synthetic TLS context infrastructure parameter to the given {@link ParameterGroupDeclarer}.
   *
   * @param parameterGroupDeclarer The parameter group declarer in which the new parameter should be declared.
   * @param targetComponent        The component that has been marked as a target for the macro-expansion of this synthetic
   *                               parameter.
   */
  public static void addTlsContextParameter(ParameterGroupDeclarer<?> parameterGroupDeclarer, ComponentAst targetComponent) {
    MetadataTypeBasedInfrastructureType tlsContextInfrastructureType =
        getMetadataTypeBasedInfrastructureType(TlsContextFactory.class);
    MetadataType tlsContextType = new DefaultExtensionsTypeLoaderFactory()
        .createTypeLoader(TlsEnabledComponentUtils.class.getClassLoader())
        .load(TlsContextFactory.class);

    ParameterDeclarer<?> parameterDeclarer;

    if (requiresTlsContextFactoryParameter(targetComponent) && !isTlsContextFactoryProvided(targetComponent)) {
      parameterDeclarer = parameterGroupDeclarer.withRequiredParameter(tlsContextInfrastructureType.getName());
    } else {
      parameterDeclarer = parameterGroupDeclarer.withOptionalParameter(tlsContextInfrastructureType.getName());
    }

    parameterDeclarer = parameterDeclarer
        .ofType(tlsContextType)
        .withRole(BEHAVIOUR)
        .withExpressionSupport(NOT_SUPPORTED)
        .withModelProperty(new SyntheticModelModelProperty())
        .withModelProperty(new InfrastructureParameterModelProperty(tlsContextInfrastructureType.getSequence()));

    tlsContextInfrastructureType.getQNameModelProperty().ifPresent(parameterDeclarer::withModelProperty);
    tlsContextInfrastructureType.getDslConfiguration().ifPresent(parameterDeclarer::withDsl);
  }

  private static boolean isTlsContextFactoryParameter(ParameterModel parameterModel) {
    return getType(parameterModel.getType())
        .map(TlsContextFactory.class::isAssignableFrom)
        .orElse(false);
  }

  private static boolean hasTlsContextFactoryParameter(ParameterizedModel model) {
    return model.getAllParameterModels().stream().anyMatch(TlsEnabledComponentUtils::isTlsContextFactoryParameter);
  }

  private static boolean requiresTlsContextFactoryParameter(ParameterizedModel model) {
    return model.getAllParameterModels().stream()
        .filter(ParameterModel::isRequired)
        .anyMatch(TlsEnabledComponentUtils::isTlsContextFactoryParameter);
  }

  private static boolean requiresTlsContextFactoryParameter(ComponentAst componentAst) {
    return componentAst.getModel(ParameterizedModel.class)
        .map(TlsEnabledComponentUtils::requiresTlsContextFactoryParameter)
        .orElse(false);
  }

  private static boolean isTlsContextFactoryProvided(ComponentAst componentAst) {
    return componentAst.getParameters().stream()
        .filter(TlsEnabledComponentUtils::isTlsContextFactoryParameter)
        .anyMatch(TlsEnabledComponentUtils::hasValue);
  }

  private static boolean hasValue(ComponentParameterAst parameterAst) {
    Either<String, ?> value = parameterAst.getValue();
    return value.isLeft() || value.isRight();
  }

  private TlsEnabledComponentUtils() {
    // Private constructor to prevent instantiation
  }
}
