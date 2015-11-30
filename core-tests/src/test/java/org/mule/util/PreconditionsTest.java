/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util;


import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;

import org.junit.Test;

public class PreconditionsTest
{

    @Test
    public void validateCheckArgumentThrowsAnExceptionWhenConditionIsFalse()
    {
        try
        {
            Preconditions.checkArgument(false, "MyMessage");
            fail("IllegalArgumentException must be thrown");
        }
        catch (IllegalArgumentException e)
        {
            assertEquals("MyMessage", e.getMessage());
        }
    }

    @Test
    public void validateCheckArgument()
    {
        try
        {
            Preconditions.checkArgument(true, "MyMessage");
        }
        catch (IllegalArgumentException e)
        {
            fail("IllegalArgumentException must not be thrown when condition is true");
        }
    }
}
