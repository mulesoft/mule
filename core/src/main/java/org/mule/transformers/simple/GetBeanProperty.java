/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transformers.simple;

import org.mule.transformers.AbstractTransformer;
import org.mule.umo.transformer.TransformerException;

import org.apache.commons.beanutils.PropertyUtils;

/**
 * Looks up a property from a JavaBean using PropertyUtils.getProperty().
 * Nested properties are valid, assuming they follow JavaBean conventions.
 * 
 *   <transformer name="ExtractCustomer" className="org.mule.transformers.simple.GetBeanProperty">
 *       <properties>
 *           <property name="propertyName" value="customerRequest.customer" />
 *       </properties>
 *   </transformer>
 */
public class GetBeanProperty extends AbstractTransformer
{
    private String propertyName;
    
    public GetBeanProperty()
    {
        super();
        registerSourceType(Object.class);
        setReturnClass(Object.class);
    }

    public Object doTransform(Object src, String encoding) throws TransformerException
    {
        try
        {
            return PropertyUtils.getProperty(src, this.propertyName);
        }
        catch (Exception e)
        {
            throw new TransformerException(this, e);
        }
    }

    public String getPropertyName()
    {
        return propertyName;
    }

    public void setPropertyName(String propertyName)
    {
        this.propertyName = propertyName;
    }

}
