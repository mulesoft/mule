/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
