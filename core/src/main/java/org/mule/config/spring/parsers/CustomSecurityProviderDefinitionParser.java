/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.parsers;

import org.mule.config.MuleProperties;
import org.mule.umo.security.UMOSecurityManager;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

public class CustomSecurityProviderDefinitionParser extends ParentDefinitionParser
{

    public static final String NAME = "name";
    public static final String PROVIDER = "provider";
    public static final String PROVIDERS = "providers";

    private String name;
    private BeanDefinition provider;

    public CustomSecurityProviderDefinitionParser()
    {
        super(MuleProperties.OBJECT_SECURITY_MANAGER, UMOSecurityManager.class);
        registerAttributeMapping(PROVIDER, PROVIDERS);
        registerList(PROVIDERS);
    }

    // this is a bit of a hack - we transfer the name to the provider

    // reset for each use
    protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext)
    {
        name = null;
        provider = null;
        return super.parseInternal(element, parserContext);    //To change body of overridden methods use File | Settings | File Templates.
    }

    // we don't know which will be set first and want to avoid setting name on the
    // manager itself...
    protected void setProperty(BeanDefinition target, String name, Object value)
    {
        if (NAME.equals(name))
        {
            this.name = (String) value;
            if (null != provider)
            {
                setName();
            }
        }
        else {
            super.setProperty(target, name, value);

            if (PROVIDERS.equals(name))
            {
                provider = (BeanDefinition) value;
                if (null != this.name)
                {
                    setName();
                }
            }
        }
    }

    //  only set name if not already given
    private void setName()
    {
        MutablePropertyValues propertyValues = provider.getPropertyValues();
        if (!propertyValues.contains(NAME))
        {
            propertyValues.addPropertyValue(NAME, name);
        }
    }

}
