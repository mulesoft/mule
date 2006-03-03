/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */

package org.mule.providers.http;

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HeaderElement;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.cookie.CookieSpec;
import org.apache.commons.httpclient.cookie.MalformedCookieException;
import org.apache.commons.httpclient.cookie.NetscapeDraftSpec;
import org.apache.commons.httpclient.cookie.RFC2109Spec;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper functions for parsing cookie headers
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class CookieHelper
{

    /**
     * logger used by this class
     */
    protected static transient Log logger = LogFactory.getLog(CookieHelper.class);

    public static CookieSpec getCookieSpec(String spec)
    {
        if (spec != null && spec.equalsIgnoreCase(HttpConnector.COOKIE_SPEC_NETSCAPE)) {
            return new NetscapeDraftSpec();
        }
        else {
            return new RFC2109Spec();
        }
    }

    public static String getCookiePolicy(String spec)
    {
        if (spec != null && spec.equalsIgnoreCase(HttpConnector.COOKIE_SPEC_NETSCAPE)) {
            return CookiePolicy.NETSCAPE;
        }
        else {
            return CookiePolicy.RFC_2109;
        }
    }

    public static Cookie[] parseCookies(Header header, String spec) throws MalformedCookieException
    {
        CookieSpec cookieSpec = getCookieSpec(spec);
        List cookies = new ArrayList();
        Cookie cookie = null;
        HeaderElement headerElement = null;
        NameValuePair nameValuePair = null;
        for (int j = 0; j < header.getElements().length; j++) {
            cookie = new Cookie();
            headerElement = header.getElements()[j];
            for (int k = 0; k < headerElement.getParameters().length; k++) {
                nameValuePair = headerElement.getParameters()[k];
                cookieSpec.parseAttribute(nameValuePair, cookie);
            }
            if (cookie.isExpired()) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Cookie: " + cookie.toString() + " has expired removing it");
                }
            }
            else {
                cookies.add(cookie);
            }
        }
        return (Cookie[])cookies.toArray(new Cookie[cookies.size()]);
    }
}
