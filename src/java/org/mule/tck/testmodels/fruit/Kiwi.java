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

import org.mule.umo.UMOEventContext;

/**
 * A test object not implementing Callable, but having
 * a matching method accepting UMOEventContext.
 */
public class Kiwi implements Fruit {

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
