/* 
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 * 
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 * 
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file. 
 *
 */

package org.mule.tck.testmodels.fruit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.umo.UMOEventContext;
import org.mule.umo.UMOException;
import org.mule.umo.lifecycle.Callable;


public class Apple implements Fruit, Callable
{
    /**
     * logger used by this class
     */
    private static transient Log logger = LogFactory.getLog(Apple.class);

    private boolean bitten = false;
    private boolean washed = false;

    public void wash()
    {
        washed = true;
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

    public Object onCall(UMOEventContext context) throws UMOException
    {
        logger.debug("Apple received an event in UMOCallable.onEvent! Event says: " + context.getMessageAsString());
        wash();
        return null;
    }
}