/*
 * $Id: ObjectFactoryDefinitionParser.java 10209 2008-01-03 18:11:14Z tcarlson $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.parsers.specific;

import org.mule.config.spring.parsers.AbstractMuleBeanDefinitionParser;
import org.mule.util.object.SpringFactoryBean;

public class SpringFactoryBeanDefinitionParser extends ObjectFactoryDefinitionParser
{
    public SpringFactoryBeanDefinitionParser()
    {
        super(SpringFactoryBean.class);
        addAlias(AbstractMuleBeanDefinitionParser.ATTRIBUTE_REF, "factoryBean");
    }                                                             
}
