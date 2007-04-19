/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.impl;

import junit.framework.TestCase;

public class DefaultExceptionStrategyTestCase extends TestCase
{

    // MULE-1404
    public void testExceptions() throws Exception
    {
        Instrumented instrumented = new Instrumented();
        instrumented.exceptionThrown(new NullPointerException("boom"));
        assertEquals(1, instrumented.getCount());
    }

    private class Instrumented extends DefaultExceptionStrategy
    {

        private int count = 0;

        // @Override
        protected void defaultHandler(Throwable t)
        {
            count++;
            super.defaultHandler(t);
        }

        public int getCount()
        {
            return count;
        }

    }

}
