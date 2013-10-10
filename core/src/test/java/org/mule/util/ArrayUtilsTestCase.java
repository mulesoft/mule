/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.util;

import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@SmallTest
public class ArrayUtilsTestCase extends AbstractMuleTestCase
{

    @Test
    public void testToArrayOfComponentType()
    {
        // null array
        assertNull(ArrayUtils.toArrayOfComponentType(null, String.class));

        // empty array, same result
        String[] a = new String[]{};
        String[] a2 = (String[])ArrayUtils.toArrayOfComponentType(a, String.class);
        assertSame(a2, a);

        // null service type is not allowed
        try
        {
            ArrayUtils.toArrayOfComponentType(a, null);
            fail();
        }
        catch (IllegalArgumentException iex)
        {
            // ok
        }

        // single element
        a = new String[]{":-)"};
        CharSequence[] cs = (CharSequence[])ArrayUtils.toArrayOfComponentType(a, CharSequence.class);
        assertEquals(a.length, cs.length);
        assertSame(a[0], cs[0]);

        // incompatible element types are not a good idea either
        try
        {
            ArrayUtils.toArrayOfComponentType(a, List.class);
            fail();
        }
        catch (ArrayStoreException asx)
        {
            // ok
        }

    }

    @Test
    public void testToStringMaxLength()
    {
        Object test = new byte[100];
        for (int i = 0; i < ((byte[])test).length; i++)
        {
            ((byte[])test)[i] = (byte)i;
        }

        // the String will contain not more than exactly MAX_ARRAY_LENGTH elements
        String result = ArrayUtils.toString(test, 10);
        assertTrue(result.endsWith("[..]}"));
        assertEquals(9, StringUtils.countMatches(result, ","));
    }

}
