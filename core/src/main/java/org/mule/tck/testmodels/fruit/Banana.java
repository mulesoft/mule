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
        logger.debug("Banana got peel event in peelEvent(EventObject)! Event says: "
                        + e.getSource().toString());
        peel();
    }

    public boolean isPeeled()
    {
        return peeled;
    }

    public void bite()
    {
        bitten = true;
    }

    public boolean isBitten()
    {
        return bitten;
    }
}
