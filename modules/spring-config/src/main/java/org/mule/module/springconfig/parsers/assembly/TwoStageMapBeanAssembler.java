/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.springconfig.parsers.assembly;

import org.mule.module.springconfig.MuleHierarchicalBeanDefinitionParserDelegate;
import org.mule.module.springconfig.parsers.assembly.configuration.PropertyConfiguration;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;

/**
 * This is used by {@link org.mule.module.springconfig.parsers.delegate.MapDefinitionParserMutator} - it takes
 * a normal bean definition and re-packages it as a map (rather than individual values).  The difference
 * between this and {@link org.mule.module.springconfig.parsers.assembly.AttributeMapBeanAssemblerFactory} is
 * that this allows child elements to generate the properties (it's an ugly consequence of the fact that
 * BDPs are called before nested children - this is a hack that gets "re-called" after the children to
 * complete the work).
 */
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
     * driver loop ({@link org.mule.module.springconfig.MuleHierarchicalBeanDefinitionParserDelegate})
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
