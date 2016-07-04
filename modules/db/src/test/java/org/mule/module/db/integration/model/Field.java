/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.integration.model;

import static org.mule.module.db.integration.model.FieldUtils.getValueAsString;

public class Field
{

    private final String name;
    private final Object value;

    public Field(String name, Object value)
    {
        this.name = name.toUpperCase();
        this.value = value;
    }

    public String getName()
    {
        return name;
    }

    public Object getValue()
    {
        return value;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }

        Field field = (Field) o;

        return name.equals(field.name) && !(value != null ? !checkEqualValues(field) : field.value != null);
    }

    /**
     * Lets subclasses to determine when another field has the same value
     *
     * @param field field to compare against to
     * @return true if current field has the same value than the given one, false otherwise
     */
    protected boolean checkEqualValues(Field field)
    {
        if (value instanceof Object[] && field.value instanceof Object[])
        {
            final Object[] arrayValue = (Object[]) this.value;
            final Object[] arrayFieldValue = (Object[]) field.value;
            if (arrayValue.length == arrayFieldValue.length)
            {
                final String s1 = getValueAsString(arrayValue);
                final String s2 = getValueAsString(arrayFieldValue);
                return s1.equals(s2);
            }
            return false;
        }
        else
        {
            return value.toString().equals(field.value.toString());
        }
    }

    @Override
    public int hashCode()
    {
        return name.hashCode();
    }

    @Override
    public String toString()
    {
        return "{" + name + ", " + getValueAsString(value) + "}";
    }
}
