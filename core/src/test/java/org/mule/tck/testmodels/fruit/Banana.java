/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
