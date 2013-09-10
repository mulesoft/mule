/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers.specific.endpoint;

import org.mule.config.spring.parsers.generic.ParentDefinitionParser;
import org.mule.config.spring.parsers.processors.CheckExclusiveAttributes;
import org.mule.util.StringUtils;

import org.w3c.dom.Element;

/**
 * Configures a reference to an endpoint on a parent bean.  This is typically used in configuration
 * where a reference to the actual endpoint is not wanted (i.e. Reply-To endpoints should be set as a string
 * on a message).
 *
 * Note that endpoint Reference elements should always have an 'address' and 'ref' attributes available.  These
 * are mutually exclusive.
 *
 * Any other attributes on the element processed by this parser will also be set on the parent object.
 */
public class EndpointRefParser extends ParentDefinitionParser
{
    public EndpointRefParser(String propertyName)
    {
        addAlias("address", propertyName);
        addAlias("ref", propertyName);
        addAlias("reference", propertyName);
        registerPreProcessor(new CheckExclusiveAttributes(new String[][]{new String[]{"ref"}, new String[]{"address"}}));
    }



    @Override
    protected void preProcess(Element element)
    {
        //This causes the Bean framework to process the "ref" as a string rather than a ref to another object
        if(StringUtils.isNotEmpty(element.getAttribute("ref")))
        {
            element.setAttribute("reference", element.getAttribute("ref"));
            element.removeAttribute("ref");
        }
        super.preProcess(element);

    }
}
