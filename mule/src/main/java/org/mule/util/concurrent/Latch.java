/* 
 * $Id$
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
package org.mule.util.concurrent;

import edu.emory.mathcs.backport.java.util.concurrent.CountDownLatch;

/**
 * @author Holger Hoffstaette
 */
// @ThreadSafe
public class Latch extends CountDownLatch
{

    public Latch()
    {
        super(1);
    }

}
