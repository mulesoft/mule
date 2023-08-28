/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.config.internal.dsl.processor.xml.provider;

import static java.lang.String.format;
import static java.util.Arrays.asList;

import static org.mule.runtime.internal.dsl.DslConstants.DEFAULT_NAMESPACE_URI_MASK;
import static org.mule.runtime.internal.dsl.DslConstants.OPERATION_PREFIX;

import org.mule.runtime.dsl.api.xml.XmlNamespaceInfo;
import org.mule.runtime.dsl.api.xml.XmlNamespaceInfoProvider;

import java.util.Collection;

/**
 * {@link XmlNamespaceInfoProvider} for the {@code operation} namespace
 *
 * @since 4.5.0
 */
public class OperationDslNamespaceInfoProvider implements XmlNamespaceInfoProvider {

  public static final String OPERATION_DSL_NAMESPACE = OPERATION_PREFIX;
  public static final String OPERATION_DSL_NAMESPACE_URI = format(DEFAULT_NAMESPACE_URI_MASK, OPERATION_DSL_NAMESPACE);
  public static final String OPERATION_DSL_XSD_FILE_NAME = "mule-operation.xsd";
  public static final String OPERATION_DSL_SCHEMA_LOCATION = OPERATION_DSL_NAMESPACE + "/current/" + OPERATION_DSL_XSD_FILE_NAME;

  @Override
  public Collection<XmlNamespaceInfo> getXmlNamespacesInfo() {

    return asList(new XmlNamespaceInfo() {

      @Override
      public String getNamespaceUriPrefix() {
        return OPERATION_DSL_NAMESPACE_URI;
      }

      @Override
      public String getNamespace() {
        return OPERATION_DSL_NAMESPACE;
      }
    });
  }
}
