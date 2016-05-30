/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.dsl.api.xml;

public class StaticXmlNamespaceInfo implements XmlNamespaceInfo
{
    private final String namespaceUriPrefix;
    private final String namespace;

    public StaticXmlNamespaceInfo(String namespaceUriPrefix, String namespace)
    {
        this.namespaceUriPrefix = namespaceUriPrefix;
        this.namespace = namespace;
    }

    @Override
    public String getNamespaceUriPrefix()
    {
        return namespaceUriPrefix;
    }

    @Override
    public String getNamespace()
    {
        return namespace;
    }
}
