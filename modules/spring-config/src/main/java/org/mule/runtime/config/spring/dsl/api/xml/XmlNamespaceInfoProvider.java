/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.dsl.api.xml;

/**
 * Mule XML extensions needs to define a {@code} XmlNamespaceProvider in which they define
 * the extensions namespace name and the extensions xml namespace uri prefix.
 *
 * The extensions namespace must much the namespace provided at the {@link org.mule.runtime.config.spring.dsl.api.ComponentBuildingDefinitionProvider}.
 *
 * @since 4.0
 */
public interface XmlNamespaceInfoProvider
{

    /**
     * A mule extension may support different xml versions. Each version must always have the same prefix
     * and no other extension can have that prefix. Mule core has http://www.mulesoft.org/schema/mule/core/
     * as prefix for all the possible versions of the namespace.
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
