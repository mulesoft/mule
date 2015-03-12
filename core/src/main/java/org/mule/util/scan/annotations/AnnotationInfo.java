/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util.scan.annotations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @deprecated: As ASM 3.3.1 is not fully compliant with Java 8, this class has been deprecated, however you can still use it under Java 7.
 */
@Deprecated
public class AnnotationInfo
{
    private String className;
    private List<NameValue> params = new ArrayList<NameValue>();

    public List<NameValue> getParams()
    {
        return params;
    }

    public Map<String, Object> getParamsAsMap()
    {
        Map m = new HashMap(params.size());
        for (NameValue param : params)
        {
            m.put(param.name, param.value);
        }
        return m;
    }

    public void setParams(List<NameValue> params)
    {
        this.params = params;
    }

    public String getClassName()
    {
        return className;
    }

    public void setClassName(String className)
    {
        this.className = className;
    }

    public boolean equals(final Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }

        final AnnotationInfo that = (AnnotationInfo) o;

        if (!className.equals(that.className))
        {
            return false;
        }
        if (params != null ? !params.equals(that.params) : that.params != null)
        {
            return false;
        }

        return true;
    }

    public int hashCode()
    {
        int result;
        result = className.hashCode();
        result = 31 * result + (params != null ? params.hashCode() : 0);
        return result;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder(params.size() * 20);
        sb.append(className).append('(');
        for (int i = 0; i < params.size(); i++)
        {
            NameValue param = params.get(i);
            sb.append(param.name).append('=').append(param.value);
            if (i < params.size() - 1)
            {
                sb.append(',');
            } else
            {
                sb.append(')');
            }
        }
        return sb.toString();
    }

    public static class NameValue
    {
        public String name;
        public Object value;

        NameValue(final String name, final Object value)
        {
            this.name = name;
            this.value = value;
        }

        public boolean equals(final Object o)
        {
            if (this == o)
            {
                return true;
            }
            if (o == null || getClass() != o.getClass())
            {
                return false;
            }

            final NameValue nameValue = (NameValue) o;

            if (!name.equals(nameValue.name))
            {
                return false;
            }
            if (!value.equals(nameValue.value))
            {
                return false;
            }

            return true;
        }

        public int hashCode()
        {
            int result;
            result = name.hashCode();
            result = 31 * result + value.hashCode();
            return result;
        }

        @Override
        public String toString()
        {
            return String.format("%s=%s", name, value);
        }
    }
}
