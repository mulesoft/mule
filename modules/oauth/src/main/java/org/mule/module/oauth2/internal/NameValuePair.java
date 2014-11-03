package org.mule.module.oauth2.internal;

/**
 *
 */
public class NameValuePair
{

    private String name;
    private Object value;

    public NameValuePair(String name, Object value)
    {
        this.name = name;
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
}
