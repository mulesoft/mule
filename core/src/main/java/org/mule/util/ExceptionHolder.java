/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.util;

import java.beans.ExceptionListener;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/** TODO */
public class ExceptionHolder implements ExceptionListener
{
    protected final Log logger = LogFactory.getLog(getClass());
    private List<Exception> exceptions = new ArrayList<Exception>(2);

    public void exceptionThrown(Exception e)
    {
        exceptions.add(e);
    }

    public List getExceptions()
    {
        return exceptions;
    }

    public boolean isExceptionThrown()
    {
        return exceptions.size() > 0;
    }

    public void clear()
    {
        exceptions.clear();
    }

    public void print()
    {
        for (Exception exception : exceptions)
        {
            logger.error(exception);
        }
    }
}
