/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.parsers.specific.security;

public class CustomEncryptionStrategyDefinitionParser extends CustomSecurityDefinitionParser
{

    public static final String STRATEGY = "encryptionStrategy";
    public static final String STRATEGIES = "encryptionStrategies";

    public CustomEncryptionStrategyDefinitionParser()
    {
        super(STRATEGIES);
        withAlias(STRATEGY, STRATEGIES);
        withCollection(STRATEGIES);
    }

}
