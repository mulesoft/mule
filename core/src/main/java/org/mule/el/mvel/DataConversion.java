/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.el.mvel;

public class DataConversion extends org.mvel2.DataConversion
{

    @SuppressWarnings("unchecked")
    protected static <T> T handleTypeCoercion(Class<T> type, Object value)
    {
        if (type != null && value != null && value.getClass() != type)
        {
            if (!canConvert(type, value.getClass()))
            {
                throw new RuntimeException("cannot assign " + value.getClass().getName() + " to type: "
                                           + type.getName());
            }
            try
            {
                return convert(value, type);
            }
            catch (Exception e)
            {
                throw new RuntimeException("cannot convert value of " + value.getClass().getName() + " to: "
                                           + type.getName());
            }
        }
        return (T) value;
    }

}
