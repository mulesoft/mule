package org.mule.test.util;

import java.util.Calendar;

import junit.framework.TestCase;

import org.mule.util.NumberUtils;

/**
 * @author <a href="mailto:carlson@hotpop.com">Travis Carlson</a>
 */
public class NumberUtilsTestCase extends TestCase {

    static final long l = 1000000000;

    public void testStringToLong() {
        assertEquals(l, NumberUtils.toLong("1000000000"));
    }

    public void testLongToLong() {
        assertEquals(l, NumberUtils.toLong(new Long(l)));
    }

    public void testIntegerToLong() {
        assertEquals(l, NumberUtils.toLong(new Integer(1000000000)));
    }

    public void testIncompatible() {
        try {
            NumberUtils.toLong(Calendar.getInstance().getTime());
            fail();
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    // This test doesn't work as expected because it calls
    // org.apache.commons.lang.math.toLong(String str) instead of org.mule.util.toLong(Object obj)
//    public void testNull() {
//        try {
//            NumberUtils.toLong(null);
//            fail();
//        } catch (IllegalArgumentException e) {
//            // expected
//        }
//    }
}
