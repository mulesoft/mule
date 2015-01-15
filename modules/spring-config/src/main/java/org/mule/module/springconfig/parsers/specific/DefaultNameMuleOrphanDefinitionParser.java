/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.springconfig.parsers.specific;

import org.mule.module.springconfig.parsers.generic.MuleOrphanDefinitionParser;
import org.mule.module.springconfig.parsers.processors.ProvideDefaultNameFromElement;

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
