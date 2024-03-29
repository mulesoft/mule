/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.xml;

import org.mule.api.annotation.NoExtend;
import org.mule.api.annotation.NoInstantiate;
import org.mule.runtime.dsl.api.xml.XmlNamespaceInfo;

/**
 * A {@link XmlNamespaceInfo} which returns fix values obtained through the constructor
 *
 * @since 4.0
 */
@NoExtend
@NoInstantiate
public final class StaticXmlNamespaceInfo implements XmlNamespaceInfo {

  private final String namespaceUriPrefix;
  private final String namespace;

  /**
   * Creates a new instance
   *
   * @param namespaceUriPrefix the value to be returned by {@link #getNamespaceUriPrefix()}
   * @param namespace          the value to be returned by {@link #getNamespace()}
   */
  public StaticXmlNamespaceInfo(String namespaceUriPrefix, String namespace) {
    this.namespaceUriPrefix = namespaceUriPrefix;
    this.namespace = namespace;
  }

  @Override
  public String getNamespaceUriPrefix() {
    return namespaceUriPrefix;
  }

  @Override
  public String getNamespace() {
    return namespace;
  }
}
