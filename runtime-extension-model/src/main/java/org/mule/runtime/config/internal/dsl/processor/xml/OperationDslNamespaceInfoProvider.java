/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.processor.xml;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.mule.runtime.internal.dsl.DslConstants.DEFAULT_NAMESPACE_URI_MASK;

import org.mule.runtime.dsl.api.xml.XmlNamespaceInfo;
import org.mule.runtime.dsl.api.xml.XmlNamespaceInfoProvider;

import java.util.Collection;

public class OperationDslNamespaceInfoProvider implements XmlNamespaceInfoProvider {

  public static final String OPERATION_DSL_NAMESPACE = "operation";
  public static final String OPERATION_DSL_NAMESPACE_URI = format(DEFAULT_NAMESPACE_URI_MASK, "operation");
  public static final String OPERATION_DSL_XSD_FILE_NAME = "mule-operation.xsd";
  public static final String OPERATION_DSL_SCHEMA_LOCATION = OPERATION_DSL_NAMESPACE + "/current/" + OPERATION_DSL_XSD_FILE_NAME;

  @Override
  public Collection<XmlNamespaceInfo> getXmlNamespacesInfo() {

    return asList(new XmlNamespaceInfo() {

      @Override
      public String getNamespaceUriPrefix() {
        return OPERATION_DSL_NAMESPACE;
      }

      @Override
      public String getNamespace() {
        return OPERATION_DSL_NAMESPACE;
      }
    });
  }
}
