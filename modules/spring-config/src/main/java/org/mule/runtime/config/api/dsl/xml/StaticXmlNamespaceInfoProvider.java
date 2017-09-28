/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.api.dsl.xml;

import static com.google.common.collect.ImmutableList.copyOf;

import org.mule.runtime.dsl.api.xml.XmlNamespaceInfo;
import org.mule.runtime.dsl.api.xml.XmlNamespaceInfoProvider;

import java.util.Collection;

/**
 * A {@link XmlNamespaceInfoProvider} which provides a fixed set of {@link XmlNamespaceInfo} instances obtained through the
 * constructor
 *
 * @since 4.0
 */
public class StaticXmlNamespaceInfoProvider implements XmlNamespaceInfoProvider {

  private final Collection<XmlNamespaceInfo> namespaceInfos;

  /**
   * Creates a new instance
   *
   * @param namespaceInfos the {@link Collection} to be returned by {@link #getXmlNamespacesInfo()}
   */
  public StaticXmlNamespaceInfoProvider(Collection<XmlNamespaceInfo> namespaceInfos) {
    this.namespaceInfos = copyOf(namespaceInfos);
  }

  @Override
  public Collection<XmlNamespaceInfo> getXmlNamespacesInfo() {
    return namespaceInfos;
  }
}
