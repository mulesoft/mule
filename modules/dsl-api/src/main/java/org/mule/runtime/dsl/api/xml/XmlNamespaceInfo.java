/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.dsl.api.xml;

/**
 * Relates a namespace identifier to a XML namespace URI prefix.
 *
 * The namespace identifier must be unique.
 *
 * @since 4.0
 */
public interface XmlNamespaceInfo {

  /**
   * A mule extension may support different xml versions. Each version must always have the same prefix and no other extension can
   * have that prefix. Mule core has http://www.mulesoft.org/schema/mule/core/ as prefix for all the possible versions of the
   * namespace.
   *
   *
   * @return the xml namespace uri prefix for the xml extensions configuration.
   */
  String getNamespaceUriPrefix();

  /**
   * @return the namespace of the extension. This namespace must be unique across every extension.
   */
  String getNamespace();

}
