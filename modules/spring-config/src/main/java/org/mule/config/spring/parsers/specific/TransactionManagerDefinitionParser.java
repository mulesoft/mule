/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.parsers.specific;

import org.mule.config.spring.parsers.generic.MuleOrphanDefinitionParser;
import org.mule.config.spring.parsers.AbstractMuleBeanDefinitionParser;

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
