/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.xml.el;

import org.mule.el.mvel.DataConversion;
import org.mule.util.BeanUtils;

import org.mvel2.PropertyAccessException;
import org.mvel2.integration.PropertyHandler;
import org.mvel2.integration.VariableResolverFactory;
import org.w3c.dom.Element;

class W3CElementPropertyHandler extends DataConversion implements PropertyHandler
{
    @Override
    public Object getProperty(String name, Object contextObj, VariableResolverFactory variableFactory)
    {
        if (name.equals("attributes"))
        {
            return new W3CElementAttributesWrapperMap((Element) contextObj);
        }
        else
        {
            try
            {
                return BeanUtils.getProperty(contextObj, name);
            }
            catch (Exception e)
            {
                throw new PropertyAccessException(e);
            }
        }
    }

    @Override
    public Object setProperty(String name,
                              Object contextObj,
                              VariableResolverFactory variableFactory,
                              Object value)
    {
        if (name.equals("text"))
        {
            ((org.w3c.dom.Node) contextObj).setTextContent(handleTypeCoercion(String.class, value));
        }
        else
        {
            throw new UnsupportedOperationException();
        }
        return value;
    }

}
