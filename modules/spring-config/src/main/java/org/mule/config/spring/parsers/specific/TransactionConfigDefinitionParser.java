/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
