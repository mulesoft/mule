/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util;

import java.beans.ExceptionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;

/** TODO */
public class ExceptionHolder implements ExceptionListener
{
    private List exceptions = new ArrayList(2);

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
        for (Iterator iterator = exceptions.iterator(); iterator.hasNext();)
        {
            Exception exception = (Exception) iterator.next();
            exception.printStackTrace();
        }
    }
}
