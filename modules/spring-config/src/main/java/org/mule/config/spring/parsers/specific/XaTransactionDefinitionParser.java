/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.parsers.specific;

public class XaTransactionDefinitionParser extends TransactionDefinitionParser
{

    private static String[] ignoredAttributes = new String[] {FACTORY_REF, ACTION, INTERACT_WTH_EXTERNAL};

    public XaTransactionDefinitionParser(Class factoryClass)
    {
        super(factoryClass);
    }

    @Override
    protected String[] getIgnoredAttributes()
    {
        return ignoredAttributes;
    }
}
