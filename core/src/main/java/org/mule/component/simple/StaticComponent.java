/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.component.simple;

import org.mule.api.MuleEventContext;
import org.mule.api.lifecycle.Callable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.util.IOUtils;

import java.io.IOException;

/**
 * A service that will return a static data object as a result. This is useful for
 * testing with expected results. The data returned can be read from a file or set as
 * a property on this service.
 */
public class StaticComponent implements Callable, Initialisable
{

    private Object data;
    private String dataFile;
    private String prefix;
    private String postfix;

    public void initialise() throws InitialisationException
    {
        if (dataFile != null)
        {
            try
            {
                data = IOUtils.getResourceAsString(dataFile, getClass());
            }
            catch (IOException e)
            {
                throw new InitialisationException(e, this);
            }
        }
    }

    public Object getData()
    {
        return data;
    }

    public void setData(Object data)
    {
        this.data = data;
    }

    public String getDataFile()
    {
        return dataFile;
    }

    public void setDataFile(String dataFile)
    {
        this.dataFile = dataFile;
    }

    public String getPrefix()
    {
        return prefix;
    }

    public void setPrefix(String prefix)
    {
        this.prefix = prefix;
    }

    public String getPostfix()
    {
        return postfix;
    }

    public void setPostfix(String postfix)
    {
        this.postfix = postfix;
    }

    public Object onCall(MuleEventContext eventContext) throws Exception
    {
        if (data != null)
        {
            return data;
        }

        String eventData = eventContext.transformMessageToString();

        if (prefix != null)
        {
            eventData = prefix + eventData;
        }

        if (postfix != null)
        {
            eventData += postfix;
        }

        return eventData;
    }
}
