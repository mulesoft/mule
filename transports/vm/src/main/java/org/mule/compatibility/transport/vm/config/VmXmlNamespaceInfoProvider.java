/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.vm.config;

import org.mule.runtime.config.spring.dsl.api.xml.XmlNamespaceInfoProvider;

/**
 * {@link XmlNamespaceInfoProvider} for VM transport.
 *
 * @since 4.0
 */
public class VmXmlNamespaceInfoProvider implements XmlNamespaceInfoProvider
{

    public static final String VM_TRANSPORT_NAMESPACE = "vm";

    @Override
    public String getNamespaceUriPrefix()
    {
        return "http://www.mulesoft.org/schema/mule/transport/vm";
    }

    @Override
    public String getNamespace()
    {
        return VM_TRANSPORT_NAMESPACE;
    }
}
