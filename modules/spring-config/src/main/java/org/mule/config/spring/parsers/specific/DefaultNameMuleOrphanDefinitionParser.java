/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.spring.parsers.specific;

import org.mule.config.spring.parsers.generic.MuleOrphanDefinitionParser;
import org.mule.config.spring.parsers.processors.ProvideDefaultNameFromElement;

public class DefaultNameMuleOrphanDefinitionParser extends MuleOrphanDefinitionParser
{

    public DefaultNameMuleOrphanDefinitionParser()
    {
        this(false);
    }

    public DefaultNameMuleOrphanDefinitionParser(boolean ignoreName)
    {
        super(true);
        registerPreProcessor(new ProvideDefaultNameFromElement());
        if (ignoreName)
        {
            addIgnored(ATTRIBUTE_NAME);
        }
    }

    public DefaultNameMuleOrphanDefinitionParser(Class beanClass)
    {
        super(beanClass, true);
        registerPreProcessor(new ProvideDefaultNameFromElement());
    }

}
