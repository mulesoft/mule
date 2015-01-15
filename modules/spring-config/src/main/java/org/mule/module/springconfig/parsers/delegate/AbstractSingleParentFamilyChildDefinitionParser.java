/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.springconfig.parsers.delegate;

import org.mule.module.springconfig.parsers.MuleChildDefinitionParser;
import org.mule.module.springconfig.parsers.MuleDefinitionParser;
import org.mule.module.springconfig.parsers.MuleDefinitionParserConfiguration;
import org.mule.module.springconfig.parsers.assembly.configuration.PropertyConfiguration;

import org.springframework.beans.factory.config.BeanDefinition;


public class AbstractSingleParentFamilyChildDefinitionParser
    extends AbstractSingleParentFamilyDefinitionParser implements MuleChildDefinitionParser
{

    protected MuleDefinitionParserConfiguration addDelegate(MuleDefinitionParser delegate)
    {
        return addDelegateAsChild(delegate);
    }

    public void forceParent(BeanDefinition parent)
    {
        ((MuleChildDefinitionParser) getDelegate(0)).forceParent(parent);
    }

    public PropertyConfiguration getTargetPropertyConfiguration()
    {
        return ((MuleChildDefinitionParser) getDelegate(0)).getTargetPropertyConfiguration();
    }

}
