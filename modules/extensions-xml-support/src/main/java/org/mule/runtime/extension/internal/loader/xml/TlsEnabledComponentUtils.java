/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.extension.internal.loader.xml;

import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.api.meta.model.parameter.ParameterRole.BEHAVIOUR;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.getType;
import static org.mule.runtime.extension.internal.loader.util.InfrastructureTypeMapping.getDslConfiguration;
import static org.mule.runtime.extension.internal.loader.util.InfrastructureTypeMapping.getQName;

import static java.lang.Boolean.parseBoolean;

import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.meta.model.declaration.fluent.OptionalParameterDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterGroupDeclarer;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentParameterAst;
import org.mule.runtime.extension.api.declaration.type.DefaultExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.property.InfrastructureParameterModelProperty;
import org.mule.runtime.extension.api.property.SyntheticModelModelProperty;
import org.mule.runtime.extension.internal.loader.util.InfrastructureTypeMapping;

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
   */
  public static void addTlsContextParameter(ParameterGroupDeclarer<?> parameterGroupDeclarer) {
    InfrastructureTypeMapping.InfrastructureType tlsContextInfrastructureType =
        InfrastructureTypeMapping.getMap().get(TlsContextFactory.class);
    MetadataType tlsContextType = new DefaultExtensionsTypeLoaderFactory()
        .createTypeLoader(TlsEnabledComponentUtils.class.getClassLoader())
        .load(TlsContextFactory.class);

    ParameterDeclarer<OptionalParameterDeclarer> parameterDeclarer = parameterGroupDeclarer
        .withOptionalParameter(tlsContextInfrastructureType.getName())
        .ofType(tlsContextType)
        .withRole(BEHAVIOUR)
        .withExpressionSupport(NOT_SUPPORTED)
        .withModelProperty(new SyntheticModelModelProperty())
        .withModelProperty(new InfrastructureParameterModelProperty(tlsContextInfrastructureType.getSequence()));

    getQName(tlsContextInfrastructureType.getName()).ifPresent(parameterDeclarer::withModelProperty);
    getDslConfiguration(tlsContextInfrastructureType.getName()).ifPresent(parameterDeclarer::withDsl);
  }

  private static boolean isTlsContextFactoryParameter(ParameterModel parameterModel) {
    return getType(parameterModel.getType())
        .map(TlsContextFactory.class::isAssignableFrom)
        .orElse(false);
  }

  private static boolean hasTlsContextFactoryParameter(ParameterizedModel model) {
    return model.getAllParameterModels().stream().anyMatch(TlsEnabledComponentUtils::isTlsContextFactoryParameter);
  }

  private TlsEnabledComponentUtils() {
    // Private constructor to prevent instantiation
  }
}
