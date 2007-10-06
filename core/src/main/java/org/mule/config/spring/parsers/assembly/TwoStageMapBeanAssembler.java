/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.parsers.assembly;

import org.mule.config.spring.MuleHierarchicalBeanDefinitionParserDelegate;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;

public class TwoStageMapBeanAssembler extends AbstractMapBeanAssembler
{

    private TwoStageMapBeanAssemblerFactory.BeanAssemblerStore store;

    public TwoStageMapBeanAssembler(TwoStageMapBeanAssemblerFactory.BeanAssemblerStore store,
            PropertyConfiguration beanConfig, BeanDefinitionBuilder bean,
            PropertyConfiguration targetConfig, BeanDefinition target)
    {
        super(beanConfig, bean, targetConfig, target);
        this.store = store;
    }

    /**
     * We overwrite this method to populate a map instead of inserting the definition.
     * However, the bean definition is not complete until all child elements have been
     * parsed - and that parsing happens after this routine is called.  So on first
     * pass we set a flag in the definition.  This is picked up by the main
     * driver loop ({@link org.mule.config.spring.MuleHierarchicalBeanDefinitionParserDelegate})
     * and our enclosing bean definition parser is called again.  At the same time, to
     * avoid complicating otherwise "normal" BDPs, we pass this assembler to a callback,
     * so that it can be called the second time in a more direct way.
     */
    public void insertBeanInTarget(String oldName)
    {
        assertTargetPresent();

        if (MuleHierarchicalBeanDefinitionParserDelegate.testFlag(getBean().getBeanDefinition(),
                MuleHierarchicalBeanDefinitionParserDelegate.MULE_POST_CHILDREN))
        {
            insertDefinitionAsMap(oldName);
        }
        else
        {
            // called for the first time, so set the flag and store this assembler for
            // later processing
            MuleHierarchicalBeanDefinitionParserDelegate.setFlag(getBean().getBeanDefinition(),
                    MuleHierarchicalBeanDefinitionParserDelegate.MULE_POST_CHILDREN);
            store.saveBeanAssembler(this);
        }
    }

}
