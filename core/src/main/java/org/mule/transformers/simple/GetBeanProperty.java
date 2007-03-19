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
 * Looks up a property/field from a JavaBean using PropertyUtils.getProperty()
 * 
 *   <transformer name="ExtractCustomer" className="org.mule.transformers.simple.GetBeanProperty">
 *       <properties>
 *           <property name="field" value="customerRequest.customer" />
 *       </properties>
 *   </transformer>
 */
public class GetBeanProperty extends AbstractTransformer
{
    private String field;
    
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
            return PropertyUtils.getProperty(src, this.field);
        }
        catch (Exception e)
        {
            throw new TransformerException(this, e);
        }
    }

    public String getField()
    {
        return field;
    }

    public void setField(String field)
    {
        this.field = field;
    }
}
