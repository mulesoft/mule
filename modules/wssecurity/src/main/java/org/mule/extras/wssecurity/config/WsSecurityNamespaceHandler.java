/*
 * $Id: 
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extras.wssecurity.config;

import org.mule.config.spring.handlers.AbstractIgnorableNamespaceHandler;
import org.mule.config.spring.parsers.collection.ChildMapEntryDefinitionParser;
import org.mule.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.extras.wssecurity.filters.WsSecurityFilter;

/**
 * Registers a Bean Definition Parser for handling WsSecurity related elements.
 */
public class WsSecurityNamespaceHandler extends AbstractIgnorableNamespaceHandler
{
    public void init()
    {
        registerMuleDefinitionParser("security-filter", new ChildDefinitionParser("securityFilter", WsSecurityFilter.class)).addAlias("decryptionFile", "wsDecryptionFile").addAlias("signatureFile", "wsSignatureFile");
        registerBeanDefinitionParser("property", new ChildMapEntryDefinitionParser("addOutboundProperties", "key", "value"));
    }
}
