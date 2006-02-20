package org.mule.extras.spring.remoting;

import java.io.Serializable;

public class ComplexData implements Serializable
{
    private String someString = "Default String";
    private Integer someInteger = new Integer(13);
    
    public ComplexData()
    {
        super();
    }

    public ComplexData(String someString, Integer someInteger)
    {
        super();
        setSomeString(someString);
        setSomeInteger(someInteger);
    }

    public String toString()
    {
        try
        {
            String foo = new String(someString);
            if(someString == null) foo = "NULL";
            return "[ComplexData: [someString=" + foo + "][someInteger=" + someInteger + "]]";
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
    public Integer getSomeInteger()
    {
        return someInteger;
    }

    public void setSomeInteger(Integer someInteger)
    {
        this.someInteger = someInteger;
    }

    public String getSomeString()
    {
        return someString;
    }

    public void setSomeString(String someString)
    {
        this.someString = someString;
    }

    private static final long serialVersionUID = -886414019167115007L;
}
