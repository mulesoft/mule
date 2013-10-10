/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
