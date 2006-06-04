/*
 * $Id$
 * ------------------------------------------------------------------------------------------------------
 * 
 * Copyright (c) SymphonySoft Limited. All rights reserved. http://www.symphonysoft.com
 * 
 * The software in this package is published under the terms of the BSD style
 * license a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 *  
 */

package org.mule.test.util;

import org.mule.util.StringUtils;

import java.util.Arrays;

import junit.framework.TestCase;

/**
 * <p/> <code>StringUtilsTestCase</code> TODO (document class)
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class StringUtilsTestCase extends TestCase
{

    public void testSplitWithTrimming()
    {
        String[] inputValues = new String[]{"foo", "bar", "baz", "kaboom"};

        String inputString = new StringBuffer(40).append(inputValues[0]).append(" ,").append(
                        ",  ").append(inputValues[1]).append(" ,").append(inputValues[2])
                        .append("  ,  ").append(inputValues[3]).append(" ").toString();

        assertTrue(Arrays.equals(inputValues, StringUtils.split(inputString, ",")));
    }

}
