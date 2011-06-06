/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.jdbc.config;

import org.mule.config.spring.parsers.generic.MuleOrphanDefinitionParser;

// TODO pool: destroy-method="shutdown"
public class PoolDefinitionParser extends MuleOrphanDefinitionParser
{
    public PoolDefinitionParser(Class<? extends AbstractPoolFactoryBean> poolFactoryClass)
    {
        super(poolFactoryClass, true);
        addIgnored("name");
    }
}
