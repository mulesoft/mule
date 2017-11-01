/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.drools;

public class Data
{
    public static final int NON_CATEGORIZED = -1;
    
    private Integer value;
    private Integer category = NON_CATEGORIZED;


    public Data(Integer value)
    {
        this.value = value;
    }
    
    public Integer getValue()
    {
        return value;
    }

    public void setValue(Integer value)
    {
        this.value = value;
    }

    public Integer getCategory()
    {
        return category;
    }

    public void setCategory(Integer category)
    {
        this.category = category;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((value == null) ? 0 : value.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Data other = (Data) obj;
        if (value == null)
        {
            if (other.value != null)
                return false;
        }
        else if (!value.equals(other.value))
            return false;
        return true;
    }
}
