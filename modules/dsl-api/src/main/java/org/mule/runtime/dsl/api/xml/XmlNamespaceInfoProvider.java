/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.dsl.api.xml;

import org.mule.runtime.dsl.api.component.ComponentBuildingDefinitionProvider;

import java.util.Collection;

/**
 * Mule XML extensions needs to define a {@code} XmlNamespaceProvider in which they define the extensions namespace name and the
 * extensions xml namespace uri prefix.
 * <p>
 * The extensions namespace must much the namespace provided at the {@link ComponentBuildingDefinitionProvider}.
 *
 * @since 4.0
 */
public interface XmlNamespaceInfoProvider {

  /**
   * Most likely, hand made extensions will return a single value since they only provide support for a namespace but for other
   * scenarios, like extensions build with the SDK, it may provide several values.
   *
   * @return a collection of {@code XmlNamespaceInfo} with the relation between a prefix and it's namespace URI in XML.
   */
  Collection<XmlNamespaceInfo> getXmlNamespacesInfo();

}
