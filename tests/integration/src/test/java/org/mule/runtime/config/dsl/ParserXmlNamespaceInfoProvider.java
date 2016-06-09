/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.dsl;

import org.mule.runtime.config.spring.dsl.api.xml.XmlNamespaceInfo;
import org.mule.runtime.config.spring.dsl.api.xml.XmlNamespaceInfoProvider;

import java.util.Arrays;
import java.util.Collection;

public class ParserXmlNamespaceInfoProvider implements XmlNamespaceInfoProvider
{

    public static final String PARSERS_TEST_NAMESACE = "parsers-test";

    @Override
    public Collection<XmlNamespaceInfo> getXmlNamespacesInfo()
    {
        return Arrays.asList(new XmlNamespaceInfo()
        {
            @Override
            public String getNamespaceUriPrefix()
            {
                return "http://www.mulesoft.org/schema/mule/parsers-test";
            }

            @Override
            public String getNamespace()
            {
                return PARSERS_TEST_NAMESACE;
            }
        });
    }
}
