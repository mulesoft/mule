/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.handlers;

/**
 * This is the domain namespace handler for Mule domains and configures all domains configuration elements under the
 * <code>http://www.mulesoft.org/schema/mule/domain/${version}</code> Namespace.
 */
public class MuleDomainNamespaceHandler extends MuleNamespaceHandler
{

    @Override
    public void init()
    {
        super.init();
        registerIgnoredElement("mule-domain");
    }

}
