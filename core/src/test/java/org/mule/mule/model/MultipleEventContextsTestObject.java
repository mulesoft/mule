/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.mule.model;

import org.mule.umo.UMOEventContext;

/**
 * A test object with multiple matching methods accepting UMOEventContext for the
 * discovery to fail.
 */
public class MultipleEventContextsTestObject
{

    public void onCall(UMOEventContext context)
    {
        // nothing to do
    }

    public void doCall(UMOEventContext context)
    {
        // nothing to do
    }

}
