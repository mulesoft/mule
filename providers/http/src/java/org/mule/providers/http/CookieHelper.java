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
    protected static transient final Log logger = LogFactory.getLog(CookieHelper.class);

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
        List cookies = new ArrayList();
        CookieSpec cookieSpec = getCookieSpec(spec);
        HeaderElement[] headerElements = header.getElements();

        for (int j = 0; j < headerElements.length; j++) {
            HeaderElement headerElement = headerElements[j];
            NameValuePair[] headerElementParameters = headerElement.getParameters();
            Cookie cookie = new Cookie();

            for (int k = 0; k < headerElementParameters.length; k++) {
                NameValuePair nameValuePair = headerElementParameters[k];
                cookieSpec.parseAttribute(nameValuePair, cookie);
            }

            if (cookie.isExpired()) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Cookie: " + cookie.toString() + " has expired, not adding it.");
                }
            }
            else {
                cookies.add(cookie);
            }
        }

        return (Cookie[])cookies.toArray(new Cookie[cookies.size()]);
    }

}
