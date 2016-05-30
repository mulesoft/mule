/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.dsl.api.xml;

import static com.google.common.collect.ImmutableList.copyOf;

import java.util.Collection;

public class StaticXmlNamespaceInfoProvider implements XmlNamespaceInfoProvider
{
    private final Collection<XmlNamespaceInfo> namespaceInfos;

    public StaticXmlNamespaceInfoProvider(Collection<XmlNamespaceInfo> namespaceInfos)
    {
        this.namespaceInfos = copyOf(namespaceInfos);
    }

    @Override
    public Collection<XmlNamespaceInfo> getXmlNamespacesInfo()
    {
        return namespaceInfos;
    }
}
