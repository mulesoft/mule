/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.spring.parsers.delegate;

import org.mule.config.spring.parsers.MuleChildDefinitionParser;
import org.mule.config.spring.parsers.MuleDefinitionParser;
import org.mule.config.spring.parsers.MuleDefinitionParserConfiguration;
import org.mule.config.spring.parsers.assembly.configuration.PropertyConfiguration;

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
