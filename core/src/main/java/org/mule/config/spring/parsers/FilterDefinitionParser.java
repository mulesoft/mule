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

import org.mule.routing.filters.EqualsFilter;
import org.mule.routing.filters.ExceptionTypeFilter;
import org.mule.routing.filters.MessagePropertyFilter;
import org.mule.routing.filters.PayloadTypeFilter;
import org.mule.routing.filters.RegExFilter;
import org.mule.routing.filters.WildcardFilter;
import org.mule.routing.filters.logic.AndFilter;
import org.mule.routing.filters.logic.NotFilter;
import org.mule.routing.filters.logic.OrFilter;

import org.w3c.dom.Element;

/**
 * TODO
 */
public class FilterDefinitionParser extends AbstractChildBeanDefinitionParser
{
    protected Class getBeanClass(Element element)
    {
        if (element.getLocalName().equals("and-filter"))
        {
            return AndFilter.class;
        }
        else if (element.getLocalName().equals("or-filter"))
        {
            return NotFilter.class;
        }
        else if (element.getLocalName().equals("not-filter"))
        {
            return OrFilter.class;
        }
        else if (element.getLocalName().equals("regex-filter"))
        {
            return RegExFilter.class;
        }
        else if (element.getLocalName().equals("exception-type-filter"))
        {
            return ExceptionTypeFilter.class;
        }
        else if (element.getLocalName().equals("message-property-filter"))
        {
            return MessagePropertyFilter.class;
        }
        else if (element.getLocalName().equals("payload-type-filter"))
        {
            return PayloadTypeFilter.class;
        }
        else if (element.getLocalName().equals("exception-type-filter"))
        {
            return ExceptionTypeFilter.class;
        }
        else if (element.getLocalName().equals("wildcard-filter"))
        {
            return WildcardFilter.class;
        }
        else if (element.getLocalName().equals("equals-filter"))
        {
            return EqualsFilter.class;
        }
        return null;
    }

    public String getPropertyName(Element e)
    {
        return "filter";
    }


    public boolean isCollection(Element element)
    {
        String parent = element.getParentNode().getLocalName();
        if(parent==null) return false;

        if (parent.equals("and-filter"))
        {
            return true;
        }
        else if (parent.equals("or-filter"))
        {
            return true;
        }
        else
        {
            return false;
        }
    }
}