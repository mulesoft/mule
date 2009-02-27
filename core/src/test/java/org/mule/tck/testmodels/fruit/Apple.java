/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tck.testmodels.fruit;

import org.mule.api.MuleEventContext;
import org.mule.api.MuleException;
import org.mule.api.lifecycle.Callable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class Apple implements Fruit, Callable
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -7631993371500076921L;

    /**
     * logger used by this class
     */
    private static final Log logger = LogFactory.getLog(Apple.class);

    private boolean bitten = false;
    private boolean washed = false;

    private FruitCleaner cleaner;

    public void wash()
    {
        if (cleaner != null)
        {
            cleaner.wash(this);
        }
        washed = true;
    }

    public void polish()
    {
        cleaner.polish(this);
    }

    public boolean isWashed()
    {
        return washed;
    }

    public void bite()
    {
        bitten = true;
    }

    public boolean isBitten()
    {
        return bitten;
    }

    public Object onCall(MuleEventContext context) throws MuleException
    {
        logger.debug("Apple received an event in Callable.onEvent! MuleEvent says: "
                        + context.getMessageAsString());
        wash();
        return null;
    }


    public FruitCleaner getAppleCleaner()
    {
        return cleaner;
    }

    public void setAppleCleaner(FruitCleaner cleaner)
    {
        this.cleaner = cleaner;
    }

    public Object methodReturningNull()
    {
        return null;
    }

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

        final Apple apple = (Apple) o;

        if (bitten != apple.bitten)
        {
            return false;
        }
        if (washed != apple.washed)
        {
            return false;
        }

        return true;
    }

    public int hashCode()
    {
        int result;
        result = (bitten ? 1 : 0);
        result = 29 * result + (washed ? 1 : 0);
        return result;
    }

    public String toString()
    {
        return "Just an apple.";
    }

    public void setBitten(boolean bitten)
    {
        this.bitten = bitten;
    }

    public void setWashed(boolean washed)
    {
        this.washed = washed;
    }
}
