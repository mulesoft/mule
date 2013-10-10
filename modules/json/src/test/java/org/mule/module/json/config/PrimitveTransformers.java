/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.json.config;

import org.mule.api.annotations.ContainsTransformerMethods;
import org.mule.api.annotations.Transformer;

/**
 * Provides transformer for converting from primitive class types and Strings.
 */
@ContainsTransformerMethods
public class PrimitveTransformers
{

    @Transformer
    public Boolean convertStringToBoolean(String s)
    {
        return "TRUE".equalsIgnoreCase(s);
    }

    @Transformer
    public String convertBooleanToString(Boolean b)
    {
        return b.toString();
    }

    @Transformer
    public Integer convertDoubleToInteger(Double d)
    {
        return d.intValue();
    }

    @Transformer
    public Double convertIntegerToDouble(Integer i)
    {
        return i.doubleValue();
    }

    @Transformer
    public Long convertDoubleToLong(Double d)
    {
        return d.longValue();
    }

    @Transformer
    public Double convertLongToDouble(Long l)
    {
        return l.doubleValue();
    }

    @Transformer
    public Float convertDoubleToFloat(Double d)
    {
        return d.floatValue();
    }

    @Transformer
    public Double convertFloatToDouble(Float f)
    {
        return f.doubleValue();
    }


    @Transformer
    public Long convertFloatToLong(Float f)
    {
        return f.longValue();
    }

    @Transformer
    public Float convertLongToFloat(Long l)
    {
        return l.floatValue();
    }

    @Transformer
    public Integer convertFloatToInteger(Float f)
    {
        return f.intValue();
    }

    @Transformer
    public Float convertIntegerToFloat(Integer i)
    {
        return i.floatValue();
    }

    @Transformer
    public Integer convertStringToInteger(String s)
    {
        return Integer.valueOf(s);
    }

    @Transformer
    public Long convertStringToLong(String s)
    {
        return Long.valueOf(s);
    }

    @Transformer
    public Float convertStringToFloat(String s)
    {
        return Float.valueOf(s);
    }

    @Transformer
    public Double convertStringToDouble(String s)
    {
        return Double.valueOf(s);
    }
}
