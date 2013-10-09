/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.spring.parsers.processors;

import org.mule.config.spring.parsers.AbstractMuleBeanDefinitionParser;

public class IdAttribute extends AddAttribute
{

    public IdAttribute(String id)
    {
        super(AbstractMuleBeanDefinitionParser.ATTRIBUTE_ID, id);
    }

}
