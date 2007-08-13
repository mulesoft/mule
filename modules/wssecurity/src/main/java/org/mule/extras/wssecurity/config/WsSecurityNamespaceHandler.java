/*
 * $Id: 
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extras.wssecurity.config;

import org.mule.config.spring.parsers.collection.ChildMapEntryDefinitionParser;
import org.mule.config.spring.parsers.generic.GrandchildDefinitionParser;
import org.mule.config.spring.parsers.generic.ParentDefinitionParser;
import org.mule.extras.wssecurity.filters.WsSecurityFilter;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

/**
 * Registers a Bean Definition Parser for handling WsSecurity related elements.
 */
public class WsSecurityNamespaceHandler extends NamespaceHandlerSupport
{
    public void init()
    {
        registerBeanDefinitionParser("security-filters", new ParentDefinitionParser());
        registerBeanDefinitionParser("security-filter", new GrandchildDefinitionParser("securityFilter", WsSecurityFilter.class).addAlias("decryptionFile", "wsDecryptionFile").addAlias("signatureFile", "wsSignatureFile"));
        registerBeanDefinitionParser("property", new ChildMapEntryDefinitionParser("addOutboundProperties", "key", "value"));
    }
}
