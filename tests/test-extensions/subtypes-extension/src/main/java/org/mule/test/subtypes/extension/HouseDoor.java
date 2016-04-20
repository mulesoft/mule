/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.subtypes.extension;

public class HouseDoor implements Door
{

    private boolean isLocked;

    @Override
    public void open()
    {
    }

    public boolean isLocked()
    {
        return isLocked;
    }

    public void setLocked(boolean locked)
    {
        isLocked = locked;
    }
}
