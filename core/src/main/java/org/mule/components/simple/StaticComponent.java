/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.components.simple;

import org.mule.umo.UMOEventContext;
import org.mule.umo.lifecycle.Callable;
import org.mule.umo.lifecycle.Initialisable;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.util.IOUtils;

import java.io.IOException;

/**
 * A component that will return a static data object as a result. This is useful for
 * testing with expected results. The data returned can be read from a file or set as
 * a property on this component.
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

    public Object onCall(UMOEventContext eventContext) throws Exception
    {
        if (data != null)
        {
            return data;
        }

        String eventData = eventContext.getTransformedMessageAsString();

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
