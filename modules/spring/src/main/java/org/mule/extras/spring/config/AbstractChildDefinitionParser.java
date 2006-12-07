/*
 * $Header: /opt/cvsroot/mule2/mule-core/src/main/java/org/mule/spring/config/AbstractChildDefinitionParser.java,v 1.1 2006/02/01 19:42:11 rossmason Exp $
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.extras.spring.config;

import org.mule.util.StringUtils;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.w3c.dom.Element;

import java.util.List;

/**
 * todo document
 *
 */
public abstract class AbstractChildDefinitionParser extends AbstractMuleSingleBeanDefinitionParser
{

    protected boolean asList = false;


    protected void postProcess(BeanDefinitionBuilder beanDefinition, Element element)
    {
        String parentBean = ((Element) element.getParentNode()).getAttribute("id");
        if (StringUtils.isBlank(parentBean)) {
            parentBean = StringUtils.EMPTY + beanDefinition.hashCode();
        }

        String name = parentBean + "-" + element.getNodeName();
        element.setAttribute("id", name);
        BeanDefinition parent = registry.getBeanDefinition(parentBean);

        String propertyName = getPropertyName(element);
        PropertyValue pv;
        pv = parent.getPropertyValues().getPropertyValue(propertyName);
        if(pv==null) {
            propertyName += "s";
            pv = parent.getPropertyValues().getPropertyValue(propertyName);            
        }
        //If the property has already been registered under the same name, we assume we're dealing with a list property
        if (pv != null) {
            if(!(pv.getValue() instanceof List)) {
                Object o = pv.getValue();
                ManagedList l = new ManagedList();
                l.add(o);
                parent.getPropertyValues().removePropertyValue(propertyName);
                pv = new PropertyValue(propertyName + "s", l);
                parent.getPropertyValues().addPropertyValue(pv);
            }
            ((List) pv.getValue()).add(beanDefinition);
        } else {
            pv = new PropertyValue(getPropertyName(element), beanDefinition);
        }
        parent.getPropertyValues().addPropertyValue(pv);
    }

    protected abstract String getPropertyName(Element e);
}
