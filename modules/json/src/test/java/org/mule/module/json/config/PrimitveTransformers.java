/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
