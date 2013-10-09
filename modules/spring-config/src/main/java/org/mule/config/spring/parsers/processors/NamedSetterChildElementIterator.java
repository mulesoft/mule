/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.spring.parsers.processors;

import org.mule.config.spring.parsers.assembly.BeanAssembler;
import org.mule.config.spring.parsers.assembly.BeanAssemblerFactory;
import org.mule.config.spring.parsers.assembly.configuration.PropertyConfiguration;

import org.w3c.dom.Element;

/**
 * This iterates over the child elements, generates beans, and sets them on the current bean via the
 * setter given.  So presumably there's either a single child or the destination is a collection.
 *
 * <p>Since this handles the iteration over children explicitly you need to set the flag
 * {@link org.mule.config.spring.MuleHierarchicalBeanDefinitionParserDelegate#MULE_NO_RECURSE}
 * on the parser.
 *
 * @see org.mule.config.spring.parsers.processors.AbstractChildElementIterator - please read the
 * documentation for that processor
 */
public class NamedSetterChildElementIterator extends AbstractChildElementIterator
{

    private String setter;

    public NamedSetterChildElementIterator(String setter, BeanAssemblerFactory beanAssemblerFactory, PropertyConfiguration configuration)
    {
        super(beanAssemblerFactory, configuration);
        this.setter = setter;
    }

    protected void insertBean(BeanAssembler targetAssembler, Object childBean, Element parent, Element child)
    {
        targetAssembler.extendTarget(setter, setter, childBean);
    }

}
