/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.http;

import org.mule.tck.AbstractMuleTestCase;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.httpclient.Cookie;

public class CookieHelperTestCase extends AbstractMuleTestCase
{
    private static final String COOKIE_1_NAME = "cookie1";
    private static final String COOKIE_1_ORIGINAL_VALUE = "value1";
    private static final String COOKIE_2_NAME = "cookie2";
    private static final String COOKIE_2_VALUE = "value2";
    private static final String COOKIE_1_NEW_VALUE = "newValue1 That Overrides Previous One";

    public void testPutAndMergeCookieObjectMapOfStringString_CookiesInMap_NewCookiesInMap()
    {
        Map<String, String> cookiesObject = new HashMap<String, String>();
        cookiesObject.put(COOKIE_1_NAME, COOKIE_1_ORIGINAL_VALUE);

        assertEquals(1, cookiesObject.size());

        Map<String, String> newCookiesMap = new HashMap<String, String>();
        newCookiesMap.put(COOKIE_1_NAME, COOKIE_1_NEW_VALUE);
        newCookiesMap.put(COOKIE_2_NAME, COOKIE_2_VALUE);
        cookiesObject = (Map<String, String>) CookieHelper.putAndMergeCookie(cookiesObject, newCookiesMap);

        assertEquals(2, cookiesObject.size());
        assertEquals(COOKIE_1_NEW_VALUE, cookiesObject.get(COOKIE_1_NAME));
        assertEquals(COOKIE_2_VALUE, cookiesObject.get(COOKIE_2_NAME));

        Map<String, String> unModifiedCookiesObject = (Map<String, String>) CookieHelper.putAndMergeCookie(
            cookiesObject, (Map<String, String>) null);
        assertSame(cookiesObject, unModifiedCookiesObject);
        assertEquals(2, cookiesObject.size());
    }

    public void testPutAndMergeCookieObjectMapOfStringString_CookiesInArray_NewCookiesInMap()
    {
        Cookie[] cookiesObject = new Cookie[]{new Cookie(null, COOKIE_1_NAME, COOKIE_1_ORIGINAL_VALUE)};

        Map<String, String> newCookiesMap = new HashMap<String, String>();
        newCookiesMap.put(COOKIE_1_NAME, COOKIE_1_NEW_VALUE);
        newCookiesMap.put(COOKIE_2_NAME, COOKIE_2_VALUE);

        cookiesObject = (Cookie[]) CookieHelper.putAndMergeCookie(cookiesObject, newCookiesMap);

        assertEquals(2, cookiesObject.length);

        assertEquals(COOKIE_1_NAME, cookiesObject[0].getName());
        assertEquals(COOKIE_1_NEW_VALUE, cookiesObject[0].getValue());

        assertEquals(COOKIE_2_NAME, cookiesObject[1].getName());
        assertEquals(COOKIE_2_VALUE, cookiesObject[1].getValue());

        Cookie[] unModifiedCookiesObject = (Cookie[]) CookieHelper.putAndMergeCookie(cookiesObject,
            (Map<String, String>) null);
        assertSame(cookiesObject, unModifiedCookiesObject);
        assertEquals(2, cookiesObject.length);
    }

    @SuppressWarnings("unchecked")
    public void testPutAndMergeCookieObjectCookieArray_CookiesInMap_NewCookiesInArray()
    {
        Map<String, String> cookiesObject = new HashMap<String, String>();
        cookiesObject.put(COOKIE_1_NAME, COOKIE_1_ORIGINAL_VALUE);

        assertEquals(1, cookiesObject.size());

        Cookie[] newCookiesArray = new Cookie[]{new Cookie(null, COOKIE_1_NAME, COOKIE_1_NEW_VALUE),
            new Cookie(null, COOKIE_2_NAME, COOKIE_2_VALUE)};

        cookiesObject = (Map<String, String>) CookieHelper.putAndMergeCookie(cookiesObject, newCookiesArray);

        assertEquals(2, cookiesObject.size());
        assertEquals(COOKIE_1_NEW_VALUE, cookiesObject.get(COOKIE_1_NAME));
        assertEquals(COOKIE_2_VALUE, cookiesObject.get(COOKIE_2_NAME));

        Map<String, String> unModifiedCookiesObject = (Map<String, String>) CookieHelper.putAndMergeCookie(
            cookiesObject, (Cookie[]) null);
        assertSame(cookiesObject, unModifiedCookiesObject);
        assertEquals(2, cookiesObject.size());
    }

    public void testPutAndMergeCookieObjectCookieArray_CookiesInArray_NewCookiesInArray()
    {
        Cookie[] cookiesObject = new Cookie[]{new Cookie(null, COOKIE_1_NAME, COOKIE_1_ORIGINAL_VALUE)};

        assertEquals(1, cookiesObject.length);

        Cookie[] newCookiesArray = new Cookie[]{new Cookie(null, COOKIE_1_NAME, COOKIE_1_NEW_VALUE),
            new Cookie(null, COOKIE_2_NAME, COOKIE_2_VALUE)};

        cookiesObject = (Cookie[]) CookieHelper.putAndMergeCookie(cookiesObject, newCookiesArray);

        assertEquals(2, cookiesObject.length);

        assertEquals(COOKIE_1_NAME, cookiesObject[0].getName());
        assertEquals(COOKIE_1_NEW_VALUE, cookiesObject[0].getValue());

        assertEquals(COOKIE_2_NAME, cookiesObject[1].getName());
        assertEquals(COOKIE_2_VALUE, cookiesObject[1].getValue());

        Cookie[] unModifiedCookiesObject = (Cookie[]) CookieHelper.putAndMergeCookie(cookiesObject,
            (Cookie[]) null);
        assertSame(cookiesObject, unModifiedCookiesObject);
        assertEquals(2, cookiesObject.length);
    }

    public void testAsArrayOfCookies_CookiesInArray() throws Exception
    {
        Cookie[] cookiesObject = new Cookie[]{new Cookie()};
        assertSame(cookiesObject, CookieHelper.asArrayOfCookies(cookiesObject));

        Cookie[] emptyArray = CookieHelper.asArrayOfCookies(null);
        assertNotNull("A null cookiesObject should return a non null array", emptyArray);
        assertEquals(0, emptyArray.length);
    }

    public void testAsArrayOfCookies_CookiesInMap() throws Exception
    {
        Map<String, String> cookiesObject = new LinkedHashMap<String, String>();
        cookiesObject.put(COOKIE_1_NAME, COOKIE_1_ORIGINAL_VALUE);
        cookiesObject.put(COOKIE_2_NAME, COOKIE_2_VALUE);

        Cookie[] cookiesAsArray = CookieHelper.asArrayOfCookies(cookiesObject);
        assertNotNull("Array of cookies should not be null", cookiesAsArray);

        assertEquals(2, cookiesAsArray.length);

        assertEquals(COOKIE_1_NAME, cookiesAsArray[0].getName());
        assertEquals(COOKIE_1_ORIGINAL_VALUE, cookiesAsArray[0].getValue());

        assertEquals(COOKIE_2_NAME, cookiesAsArray[1].getName());
        assertEquals(COOKIE_2_VALUE, cookiesAsArray[1].getValue());

    }

    public void testResolveCookieStorageType() throws Exception
    {
        assertSame(CookieStorageType.MAP_STRING_STRING,
            CookieStorageType.resolveCookieStorageType(new HashMap<String, String>()));

        assertSame(CookieStorageType.ARRAY_OF_COOKIES, CookieStorageType.resolveCookieStorageType(null));

        assertSame(CookieStorageType.ARRAY_OF_COOKIES,
            CookieStorageType.resolveCookieStorageType(new Cookie[2]));

        try
        {
            CookieStorageType.resolveCookieStorageType(new Object());
            fail("It should have thrown an exception since Object it is not a valid type");
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(e.getMessage().contains("Invalid cookiesObject"));
        }
    }
}
