/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tck.testmodels.fruit;

import org.mule.umo.UMOEventContext;

/**
 * A test object not implementing Callable, but having
 * a matching method accepting UMOEventContext.
 */
public class Kiwi implements Fruit
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -1468423665948468954L;

    private boolean bitten;

    public void handle(UMOEventContext eventContext) throws Exception {
        final Object payload = eventContext.getTransformedMessage();
        if (payload instanceof FruitLover) {
            this.bite();
        }
    }

    public void bite () {
        this.bitten = true;
    }

    public boolean isBitten () {
        return this.bitten;
    }
}
