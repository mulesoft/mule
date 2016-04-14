/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformer.simple;

import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractTransformer;
import org.mule.transformer.types.DataTypeFactory;

import org.apache.commons.beanutils.PropertyUtils;

/**
 * Looks up a property from a JavaBean using PropertyUtils.getProperty().
 * Nested properties are valid, assuming they follow JavaBean conventions.
 * 
 *   <transformer name="ExtractCustomer" className="org.mule.transformer.simple.GetBeanProperty">
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
        registerSourceType(DataTypeFactory.OBJECT);
        setReturnDataType(DataTypeFactory.OBJECT);
    }

    @Override
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
