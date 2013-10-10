/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.spring.parsers.specific;

import org.mule.config.spring.parsers.AbstractMuleBeanDefinitionParser;
import org.mule.config.spring.parsers.generic.MuleOrphanDefinitionParser;

public class TransactionManagerDefinitionParser extends MuleOrphanDefinitionParser
{

    public TransactionManagerDefinitionParser()
    {
        super(true);
        setConstraints();
    }

    public TransactionManagerDefinitionParser(Class clazz)
    {
        super(clazz, true);
        setConstraints();
    }

    protected void setConstraints()
    {
        addIgnored(AbstractMuleBeanDefinitionParser.ATTRIBUTE_NAME);
    }

}
