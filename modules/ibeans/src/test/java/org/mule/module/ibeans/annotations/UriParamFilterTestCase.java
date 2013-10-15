/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.ibeans.annotations;

import org.ibeans.impl.support.util.UriParamFilter;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class UriParamFilterTestCase extends AbstractIBeansTestCase
{
    private UriParamFilter filter = new UriParamFilter();

    @Test
    public void testOptionalRemoveOneParam() throws Exception
    {
        String test = "http://foo.com?param=null.param";
        test = filter.filterParamsByValue(test, "null.param");
        assertEquals("http://foo.com", test);
    }

    @Test
    public void testOptionalRemoveTwoParam() throws Exception
    {
        String test = "http://foo.com?param=null.param&param2=foo";
        test = filter.filterParamsByValue(test, "null.param");
        assertEquals("http://foo.com?param2=foo", test);
    }

    @Test
    public void testOptionalRemoveThrteeParamsMiddle() throws Exception
    {
        String test = "http://foo.com?param0=foo&param1=null.param&param2=bar";
        test = filter.filterParamsByValue(test, "null.param");
        assertEquals("http://foo.com?param0=foo&param2=bar", test);
    }

    @Test
    public void testOptionalRemoveThreeParamsEnd() throws Exception
    {
        String test = "http://foo.com?param0=foo&param1=bar&param2=null.param";
        test = filter.filterParamsByValue(test, "null.param");
        assertEquals("http://foo.com?param0=foo&param1=bar", test);
    }

    @Test
    public void testOptionalRemoveAllButOne() throws Exception
    {
        String test = "http://foo.com?param0=foo&param1=null.param&param2=null.param&param3=null.param";
        test = filter.filterParamsByValue(test, "null.param");
        assertEquals("http://foo.com?param0=foo", test);
    }
}
