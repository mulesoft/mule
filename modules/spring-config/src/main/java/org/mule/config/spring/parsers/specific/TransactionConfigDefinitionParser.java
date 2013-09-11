/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers.specific;

import org.mule.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.transaction.MuleTransactionConfig;

public class TransactionConfigDefinitionParser extends ChildDefinitionParser
{

    public TransactionConfigDefinitionParser()
    {
        super("transactionConfig", MuleTransactionConfig.class);
        addMapping("action", "NONE=0,ALWAYS_BEGIN=1,BEGIN_OR_JOIN=2,ALWAYS_JOIN=3,JOIN_IF_POSSIBLE=4,NOT_SUPPORTED=7");
    }

}
