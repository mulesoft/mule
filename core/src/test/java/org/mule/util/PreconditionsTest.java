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
