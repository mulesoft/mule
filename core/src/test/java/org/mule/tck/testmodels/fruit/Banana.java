/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.testmodels.fruit;

import java.util.EventObject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class Banana implements Fruit
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -1371515374040436874L;

    /**
     * logger used by this class
     */
    private static final Log logger = LogFactory.getLog(Banana.class);

    private boolean peeled = false;
    private boolean bitten = false;

    public void peel()
    {
        peeled = true;
    }

    public void peelEvent(EventObject e)
    {
        logger.debug("Banana got peel event in peelEvent(EventObject)! MuleEvent says: "
                        + e.getSource().toString());
        peel();
    }

    public boolean isPeeled()
    {
        return peeled;
    }

    @Override
    public void bite()
    {
        bitten = true;
    }

    @Override
    public boolean isBitten()
    {
        return bitten;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (!(o instanceof Banana))
        {
            return false;
        }

        Banana banana = (Banana) o;

        if (bitten != banana.bitten)
        {
            return false;
        }
        if (peeled != banana.peeled)
        {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = (peeled ? 1 : 0);
        result = 31 * result + (bitten ? 1 : 0);
        return result;
    }
}
