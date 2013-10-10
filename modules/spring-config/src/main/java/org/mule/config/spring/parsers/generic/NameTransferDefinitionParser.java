/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers.generic;

import org.mule.config.spring.parsers.assembly.BeanAssembler;
import org.mule.config.spring.parsers.assembly.BeanAssemblerFactory;
import org.mule.config.spring.parsers.assembly.DefaultBeanAssembler;
import org.mule.config.spring.parsers.assembly.configuration.PropertyConfiguration;
import org.mule.config.spring.parsers.assembly.configuration.SingleProperty;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * This extends {@link ParentDefinitionParser} so that the "name" attribute is set locally,
 * not on the parent.
 *
 * <p>It's easier to understand with an example. Consider a custom security provider, set with the
 * following XML:</p>
 * <pre>
 &lt;mule:security-manager&gt;
     &lt;mule:custom-security-provider name="dummySecurityProvider"
                                    provider-ref="dummySecurityProvider"/&gt;
 &lt;/mule:security-manager&gt;</pre>
 * <p>What is happening here?  First, the custom-security-provider is being handled by this class.
 * Since this class extends ParentDefinitionParser, the provider value is set on the parent (the
 * security manager).  But we want the name attribute to be set on the provider (the referenced
 * bean).  So the "name" is set on the provider, not on the manager.  Then the provider is set on
 * the manager.</p> 
 */
public class NameTransferDefinitionParser extends ParentDefinitionParser
{

    private String name;
    private String componentAttributeValue;
    private String componentAttributeName;

    /**
     * @param componentAttributeName The attribute name (after processing, which will strip "-ref",
     * add plurals, etc) that identifies the service which will receive the "name".
     */
    public NameTransferDefinitionParser(String componentAttributeName)
    {
        this.componentAttributeName = componentAttributeName;
        setBeanAssemblerFactory(new LocalBeanAssemblerFactory());
    }

    // this is a bit of a hack - we transfer the name to the provider

    // reset for each use
    protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext)
    {
        name = null;
        componentAttributeValue = null;
        AbstractBeanDefinition bd = super.parseInternal(element, parserContext);
        element.removeAttribute(ATTRIBUTE_NAME);
        return bd; 
    }


    //  only set name if not already given
    private void setName()
    {
        BeanDefinition beanDef = getParserContext().getRegistry().getBeanDefinition(componentAttributeValue);
        MutablePropertyValues propertyValues = beanDef.getPropertyValues();
        if (!propertyValues.contains(ATTRIBUTE_NAME))
        {
            logger.debug("Setting " + ATTRIBUTE_NAME + " on " + componentAttributeValue + " to " + name);
            propertyValues.addPropertyValue(ATTRIBUTE_NAME, name);
        }
        else
        {
            logger.debug("Not setting " + ATTRIBUTE_NAME + " on " + componentAttributeValue +
                    " as already " + propertyValues.getPropertyValue(ATTRIBUTE_NAME));
        }
    }

    private class LocalBeanAssembler extends DefaultBeanAssembler
    {

        public LocalBeanAssembler(PropertyConfiguration beanConfig, BeanDefinitionBuilder bean,
                                      PropertyConfiguration targetConfig, BeanDefinition target)
        {
            super(beanConfig, bean, targetConfig, target);
        }

        protected void addPropertyWithReference(MutablePropertyValues properties, SingleProperty config, String name, Object value)
        {
            // intercept setting of name
            if (ATTRIBUTE_NAME.equals(name) && value instanceof String)
            {
                NameTransferDefinitionParser.this.name = (String) value;
                // name is set after service
                if (null != componentAttributeValue)
                {
                    setName();
                }
            }
            else
            {
                super.addPropertyWithReference(properties, config, name, value);

                // intercept setting of service
                if (componentAttributeName.equals(name) && value instanceof String)
                {
                    componentAttributeValue = (String) value;
                    // name was set before service
                    if (null != NameTransferDefinitionParser.this.name)
                    {
                        setName();
                    }
                }
            }
        }
    }

    private class LocalBeanAssemblerFactory implements BeanAssemblerFactory
    {

        public BeanAssembler newBeanAssembler(PropertyConfiguration beanConfig, BeanDefinitionBuilder bean,
                                                      PropertyConfiguration targetConfig, BeanDefinition target)
        {
            return new LocalBeanAssembler(beanConfig, bean, targetConfig, target);
        }

    }

}
