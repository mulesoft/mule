/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
