/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extras.pgp.config;

import org.mule.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.config.spring.parsers.generic.NamedDefinitionParser;
import org.mule.config.MuleProperties;
import org.mule.extras.pgp.KeyBasedEncryptionStrategy;
import org.mule.extras.pgp.PGPSecurityProvider;
import org.mule.extras.pgp.filters.PGPSecurityFilter;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

public class PgpNamespaceHandler extends NamespaceHandlerSupport
{
    public void init()
    {
        registerBeanDefinitionParser("security-manager", new NamedDefinitionParser(MuleProperties.OBJECT_SECURITY_MANAGER));
        registerBeanDefinitionParser("security-provider", new ChildDefinitionParser("provider", PGPSecurityProvider.class));
        registerBeanDefinitionParser("security-filter", new ChildDefinitionParser("securityFilter", PGPSecurityFilter.class));
        registerBeanDefinitionParser("keybased-encryption-strategy", new ChildDefinitionParser("encryptionStrategy", KeyBasedEncryptionStrategy.class));
    }

}
