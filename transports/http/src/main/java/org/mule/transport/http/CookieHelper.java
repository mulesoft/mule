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

import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.expression.ExpressionManager;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.cookie.CookieSpec;
import org.apache.commons.httpclient.cookie.MalformedCookieException;
import org.apache.commons.httpclient.cookie.NetscapeDraftSpec;
import org.apache.commons.httpclient.cookie.RFC2109Spec;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tomcat.util.http.Cookies;
import org.apache.tomcat.util.http.MimeHeaders;
import org.apache.tomcat.util.http.ServerCookie;

/**
 * Helper functions for parsing, formatting, storing and retrieving cookie headers.
 * <p>
 * It is important that all access to Cookie data is done using this class. This will
 * help to prevent ClassCastExceptions and data corruption.
 * <p>
 * The reasons for such a very complex {@link CookieHelper} class are historical and
 * are related to the fact that cookies are a multivalued property and we store them
 * as a single message property under the name
 * {@link HttpConnector#HTTP_COOKIES_PROPERTY "cookies"}.
 * <p>
 * In an HTTP message going from client to server the cookies come on their own
 * {@link HttpConstants#HEADER_COOKIE "Cookie"} header. The HTTP message can have
 * several of these Cookie headers and each of them can store 1 or more cookies. One
 * problem with this is that in Mule we use {@link Map Maps} to store the HTTP
 * headers and this means that we can only have one object with the key
 * {@link HttpConnector#HTTP_COOKIES_PROPERTY "cookies"} (yes, we use that constant
 * instead of {@link HttpConstants#HEADER_COOKIE "Cookie"} when we store the cookies
 * inside a {@link MuleMessage}).
 * <p>
 * In an HTTP message going from server to client the Cookies go in their own
 * {@link HttpConstants#HEADER_COOKIE_SET "Set-Cookie"} header. But, again,
 * internally we store all the HTTP headers inside a {@link Map} that maps each HTTP
 * header with a single value. For Cookies it is a special case so have to be able to
 * store many cookies in the value from that map.
 * <p>
 * With all these layed out one could say that we could just have a
 * {@link Collection} of {@link Cookie Cookies}. But this is not that simple. In some
 * parts of the code the cookies are stored as an array of Cookies ({@link Cookie
 * Cookie[]}) and in some others it is stored as a {@link Map} where each entry
 * corresponds to a cookie's name/value pair (which is not strictly a cookie).
 * Specifically, when parsing cookies from the client (ie, acting as a server), the
 * code stores it as an array of cookies. When the cookies are specified as a
 * property in the endpoint (like <a href=
 * "http://www.mulesoft.org/documentation/display/MULE2USER/HTTP+Transport#HTTPTransport-Cookies"
 * >explained in the docs</a>), they are stored as a {@link Map}.
 * <p>
 * This class has helper methods that helps making code that is independent of the
 * way the cookies are stored and still keep backward compatibility. It is very
 * hacky, but I think it is better than what we had before.
 * <p>
 * <b>Know Limitation:</b> because of how cookies are handled in Mule, we don't
 * handle well the host, port and path of a Cookie. We just handle Cookies as if they
 * were only name/value pairs. This means that, for example, if a message with
 * cookies is received on an endpoint called http://localhost:4020/hello (1) and that
 * message goes to http://www.mulesoft.org/jira/ (2), then service (2) will receive
 * all the cookies that were sent to service (1) as if they were their own.
 * Furthermore, the same thing will happend on the response: all the returned cookies
 * from service (2) will reach service (1) and then the client will receive them as
 * if they were from service (1).
 */
public class CookieHelper
{

    /**
     * This is used as the default {@link URI} for
     * {@link #parseCookiesAsAClient(String, String, URI)} and overloading methods
     * for when the {@link URI} supplied is null.
     */
    private static final String DEFAULT_URI_STRING = "http://localhost:80/";
    /**
     * logger used by this class
     */
    protected static final Log logger = LogFactory.getLog(CookieHelper.class);

    /**
     * Do not instantiate.
     */
    private CookieHelper()
    {
        // no op
    }

    /**
     * @param spec
     * @return the {@link CookieSpec} (defaults to {@link RFC2109Spec} when spec is
     *         null)
     */
    public static CookieSpec getCookieSpec(String spec)
    {
        if (spec != null && spec.equalsIgnoreCase(HttpConnector.COOKIE_SPEC_NETSCAPE))
        {
            return new NetscapeDraftSpec();
        }
        else
        {
            return new RFC2109Spec();
        }
    }

    /**
     * @param spec
     * @return the cookie policy (defaults to {@link CookiePolicy#RFC_2109} when spec
     *         is null).
     */
    public static String getCookiePolicy(String spec)
    {
        if (spec != null && spec.equalsIgnoreCase(HttpConnector.COOKIE_SPEC_NETSCAPE))
        {
            return CookiePolicy.NETSCAPE;
        }
        else
        {
            return CookiePolicy.RFC_2109;
        }
    }

    /**
     * @see #parseCookiesAsAClient(String, String, URI)
     */
    public static Cookie[] parseCookiesAsAClient(Header cookieHeader, String spec)
        throws MalformedCookieException
    {
        return parseCookiesAsAClient(cookieHeader.getValue(), spec, null);
    }

    /**
     * @see #parseCookiesAsAClient(String, String, URI)
     */
    public static Cookie[] parseCookiesAsAClient(String cookieHeaderValue, String spec)
        throws MalformedCookieException
    {
        return parseCookiesAsAClient(cookieHeaderValue, spec, null);
    }

    /**
     * @see #parseCookiesAsAClient(String, String, URI)
     */
    public static Cookie[] parseCookiesAsAClient(Header cookieHeader, String spec, URI uri)
        throws MalformedCookieException
    {
        return parseCookiesAsAClient(cookieHeader.getValue(), spec, uri);
    }

    /**
     * This method parses the value of {@link HttpConstants#HEADER_COOKIE_SET
     * "Set-Cookie"} HTTP header, returning an array with all the {@link Cookie}
     * found. This method is intended to be used from the client side of the HTTP
     * connection.
     * 
     * @param cookieHeaderValue the value with the cookie/s to parse.
     * @param spec the spec according to {@link #getCookieSpec(String)} (can be null)
     * @param uri the uri information that will be use to complete Cookie information
     *            (host, port and path). If null then the default
     *            {@value #DEFAULT_URI_STRING} will be used.
     * @return
     * @throws MalformedCookieException
     */
    public static Cookie[] parseCookiesAsAClient(String cookieHeaderValue, String spec, URI uri)
        throws MalformedCookieException
    {
        if (uri == null)
        {
            try
            {
                uri = new URI(DEFAULT_URI_STRING);
            }
            catch (URISyntaxException e)
            {
                throw new RuntimeException("This should have not happened", e);
            }
        }
        CookieSpec cookieSpec = getCookieSpec(spec);
        boolean secure = uri.getScheme() != null && uri.getScheme().equalsIgnoreCase("https");
        String host = uri.getHost();
        int port = getPortFromURI(uri);
        String path = uri.getPath();

        return cookieSpec.parse(host, port, path, secure, cookieHeaderValue);
    }

    private static int getPortFromURI(URI uri) throws MalformedCookieException
    {
        int port = uri.getPort();
        if (port < 0)
        {
            String scheme = uri.getScheme();
            if (scheme.equalsIgnoreCase("https"))
            {
                port = 443;
            }
            else if (scheme.equalsIgnoreCase("http"))
            {
                port = 80;
            }
            else
            {
                throw new MalformedCookieException(
                    "The uri ("
                                    + uri
                                    + ") does not specify a port and no default is available for its scheme ("
                                    + scheme + ").");
            }
        }
        return port;
    }

    /**
     * This method parses the value of an HTTP {@link HttpConstants#HEADER_COOKIE
     * "Cookie"} header that comes from a client to a server. It returns all the
     * Cookies present in the header.
     * 
     * @param header the header from which the cookie will be parsed. Please not that
     *            only the {@link Header#getValue() value} of this header will be
     *            used. No validation will be made to make sure that the
     *            {@link Header#getName() headerName} is actually a
     *            {@link HttpConstants#HEADER_COOKIE}.
     * @return
     */
    public static Cookie[] parseCookiesAsAServer(Header header, URI uri)
    {
        return parseCookiesAsAServer(header.getValue(), uri);
    }

    /**
     * This method parses the value of an HTTP {@link HttpConstants#HEADER_COOKIE
     * "Cookie"} header that comes from a client to a server. It returns all the
     * Cookies present in the header.
     * 
     * @param headerValue the value of the header from which the cookie will be
     *            parsed.
     * @param uri
     * @return
     */
    public static Cookie[] parseCookiesAsAServer(String headerValue, URI uri)
    {
        MimeHeaders mimeHeaders = new MimeHeaders();
        mimeHeaders.addValue(HttpConstants.HEADER_COOKIE).setBytes(headerValue.getBytes(), 0,
            headerValue.length());

        Cookies cs = new Cookies(mimeHeaders);
        Cookie[] cookies = new Cookie[cs.getCookieCount()];
        for (int i = 0; i < cs.getCookieCount(); i++)
        {
            ServerCookie serverCookie = cs.getCookie(i);
            cookies[i] = transformServerCookieToClientCookie(serverCookie);
            if (uri != null)
            {
                cookies[i].setSecure(uri.getScheme() != null && uri.getScheme().equalsIgnoreCase("https"));
                cookies[i].setDomain(uri.getHost());
                cookies[i].setPath(uri.getPath());
            }
        }
        return cookies;
    }

    /**
     * Transforms a {@link ServerCookie} (from apache tomcat) into a {@link Cookie}
     * (from commons httpclient). Both types of Cookie hold the same data but the
     * {@link ServerCookie} is the type that you get when parsing cookies as a
     * Server.
     * 
     * @param serverCookie
     * @return
     */
    protected static Cookie transformServerCookieToClientCookie(ServerCookie serverCookie)
    {
        Cookie clientCookie = new Cookie(serverCookie.getDomain().toString(), serverCookie.getName()
            .toString(), serverCookie.getValue().toString(), serverCookie.getPath().toString(),
            serverCookie.getMaxAge(), serverCookie.getSecure());
        clientCookie.setComment(serverCookie.getComment().toString());
        clientCookie.setVersion(serverCookie.getVersion());
        return clientCookie;
    }

    /**
     * This method formats the cookie so it can be send from server to client in a
     * {@link HttpConstants#HEADER_COOKIE_SET "Set-Cookie"} header.
     * 
     * @param cookie
     * @return
     */
    public static String formatCookieForASetCookieHeader(Cookie cookie)
    {
        StringBuffer sb = new StringBuffer();
        ServerCookie.appendCookieValue(sb, cookie.getVersion(), cookie.getName(), cookie.getValue(),
            cookie.getPath(), cookie.getDomain(), cookie.getComment(), -1, cookie.getSecure());
        String cookieForASetCookieHeader = sb.toString();
        return cookieForASetCookieHeader;
    }

    /**
     * Adds to the client all the cookies present in the cookiesObject.
     * 
     * @param client
     * @param cookiesObject this must be either a {@link Map Map&lt;String,
     *            String&gt;} or a {@link Cookie Cookie[]}. It can be null.
     * @param policy
     * @param event this one is used only if the cookies are stored in a {@link Map}
     *            in order to resolve expressions with the {@link ExpressionManager}.
     * @param destinationUri the host, port and path of this {@link URI} will be used
     *            as the data of the cookies that are added.
     */
    public static void addCookiesToClient(HttpClient client,
                                          Object cookiesObject,
                                             String policy,
                                             MuleEvent event,
                                             URI destinationUri)
    {
        CookieStorageType.resolveCookieStorageType(cookiesObject).addCookiesToClient(client, cookiesObject,
            policy, event, destinationUri);
    }

    /**
     * This method merges a new Cookie (or override the previous one if it exists) to
     * the preExistentCookies. The result (the old cookies with the new one added) is
     * returned. If a cookie with the same name already exists, then it will be
     * overridden.
     * <p>
     * It is <b>important</b> that you use the returned value of this method because
     * for some implementations of preExistentCookies it is not possible to add new
     * Cookies (for example, on {@link Cookie Cookie[]}).
     * 
     * @param preExistentCookies this must be either a {@link Map Map&lt;String,
     *            String&gt;} or a {@link Cookie Cookie[]}. It can be null.
     * @param cookieName the new cookie name to be added.
     * @param cookieValue the new cookie value to be added.
     * @return
     */
    public static Object putAndMergeCookie(Object preExistentCookies, String cookieName, String cookieValue)
    {
        return CookieStorageType.resolveCookieStorageType(preExistentCookies).putAndMergeCookie(
            preExistentCookies, cookieName, cookieValue);
    }

    /**
     * Merges all the Cookies in newCookiesArray with the preExistentCookies, adding
     * the new ones and overwriting the existing ones (existing means same cookie
     * name).
     * <p>
     * It is <b>important</b> that you use the returned value of this method because
     * for some implementations of preExistentCookies it is not possible to add new
     * Cookies (for example, on {@link Cookie Cookie[]}).
     * 
     * @param preExistentCookies
     * @param newCookiesArray
     * @return
     */
    public static Object putAndMergeCookie(Object preExistentCookies, Cookie[] newCookiesArray)
    {
        return CookieStorageType.resolveCookieStorageType(preExistentCookies).putAndMergeCookie(
            preExistentCookies, newCookiesArray);
    }

    /**
     * Merges all the Cookies in newCookiesMap with the preExistentCookies, adding
     * the new ones and overwriting the existing ones (existing means same cookie
     * name).
     * <p>
     * It is <b>important</b> that you use the returned value of this method because
     * for some implementations of preExistentCookies it is not possible to add new
     * Cookies (for example, on {@link Cookie Cookie[]}).
     * 
     * @param preExistentCookies
     * @param newCookiesMap
     * @return
     */
    public static Object putAndMergeCookie(Object preExistentCookies, Map<String, String> newCookiesMap)
    {
        return CookieStorageType.resolveCookieStorageType(preExistentCookies).putAndMergeCookie(
            preExistentCookies, newCookiesMap);
    }

    /**
     * Searches and return the cookie with the cookieName in the cookiesObject. It
     * returns null if the cookie is not present.
     * 
     * @param cookiesObject
     * @param cookieName
     * @return
     */
    public static String getCookieValueFromCookies(Object cookiesObject, String cookieName)
    {
        return CookieStorageType.resolveCookieStorageType(cookiesObject).getCookieValueFromCookies(
            cookiesObject, cookieName);
    }

    /**
     * Returns an array view of the cookiesObject.
     * 
     * @param cookiesObject
     * @return
     */
    public static Cookie[] asArrayOfCookies(Object cookiesObject)
    {
        return CookieStorageType.resolveCookieStorageType(cookiesObject).asArrayOfCookies(cookiesObject);
    }

}

/**
 * This enum type is here to distinguish and handle the two type of cookie storage
 * that we have. The method
 * {@link CookieStorageType#resolveCookieStorageType(Object)} allows you to select
 * the appropriate {@link CookieStorageType} for the cookiesObject that you have.
 */
enum CookieStorageType
{
    /**
     * This corresponds to the storage of cookies as a {@link Cookie Cookie[]}.
     * <p>
     * All the parameters of type {@link Object} in the method of this object are
     * assumed to be of type {@link Cookie Cookie[]} and won't be checked. They will
     * be cast to {@link Cookie Cookie[]}.
     */
    ARRAY_OF_COOKIES
    {
        @Override
        public Object putAndMergeCookie(Object preExistentCookies, String cookieName, String cookieValue)
        {
            final Cookie[] preExistentCookiesArray = (Cookie[]) preExistentCookies;

            final int sessionIndex = getCookieIndexFromCookiesArray(cookieName, preExistentCookiesArray);

            // domain, path, secure (https) and expiry are handled in method
            // CookieHelper.addCookiesToClient()
            final Cookie newSessionCookie = new Cookie(null, cookieName, cookieValue);
            final Cookie[] mergedCookiesArray;
            if (sessionIndex >= 0)
            {
                preExistentCookiesArray[sessionIndex] = newSessionCookie;
                mergedCookiesArray = preExistentCookiesArray;
            }
            else
            {
                Cookie[] newSessionCookieArray = new Cookie[]{newSessionCookie};
                mergedCookiesArray = concatenateCookies(preExistentCookiesArray, newSessionCookieArray);
            }
            return mergedCookiesArray;
        }

        protected Cookie[] concatenateCookies(Cookie[] cookies1, Cookie[] cookies2)
        {
            if (cookies1 == null)
            {
                return cookies2;
            }
            else if (cookies2 == null)
            {
                return null;
            }
            else
            {
                Cookie[] mergedCookies = new Cookie[cookies1.length + cookies2.length];
                System.arraycopy(cookies1, 0, mergedCookies, 0, cookies1.length);
                System.arraycopy(cookies2, 0, mergedCookies, cookies1.length, cookies2.length);
                return mergedCookies;
            }
        }

        protected int getCookieIndexFromCookiesArray(String cookieName, Cookie[] preExistentCookies)
        {
            if (preExistentCookies != null && cookieName != null)
            {
                for (int i = 0; i < preExistentCookies.length; i++)
                {
                    if (cookieName.equals(preExistentCookies[i].getName()))
                    {
                        return i;
                    }
                }
            }
            return -1;
        }

        @Override
        public String getCookieValueFromCookies(Object cookiesObject, String cookieName)
        {
            Cookie[] cookies = (Cookie[]) cookiesObject;

            int sessionIndex = getCookieIndexFromCookiesArray(cookieName, cookies);
            if (sessionIndex >= 0)
            {
                return cookies[sessionIndex].getValue();
            }
            else
            {
                return null;
            }
        }

        @Override
        public void addCookiesToClient(HttpClient client,
                                       Object cookiesObject,
                                       String policy,
                                       MuleEvent event,
                                       URI destinationUri)
        {
            Cookie[] cookies = (Cookie[]) cookiesObject;

            if (cookies != null && cookies.length > 0)
            {
                String host = destinationUri.getHost();
                String path = destinationUri.getPath();
                for (Cookie cookie : cookies)
                {
                    client.getState().addCookie(
                        new Cookie(host, cookie.getName(), cookie.getValue(), path, cookie.getExpiryDate(),
                            cookie.getSecure()));
                }
                client.getParams().setCookiePolicy(CookieHelper.getCookiePolicy(policy));
            }
        }

        @Override
        public Object putAndMergeCookie(Object preExistentCookies, Cookie[] newCookiesArray)
        {
            if (newCookiesArray == null)
            {
                return preExistentCookies;
            }
            final List<Cookie> cookiesThatAreReallyNew = new ArrayList<Cookie>(newCookiesArray.length);
            final Cookie[] preExistentCookiesArray = (Cookie[]) preExistentCookies;
            for (Cookie newCookie : newCookiesArray)
            {
                int newCookieInPreExistentArrayIndex = getCookieIndexFromCookiesArray(newCookie.getName(),
                    preExistentCookiesArray);
                if (newCookieInPreExistentArrayIndex >= 0)
                {
                    // overwrite the old one
                    preExistentCookiesArray[newCookieInPreExistentArrayIndex] = newCookie;
                }
                else
                {
                    // needs to add it at the end
                    cookiesThatAreReallyNew.add(newCookie);
                }
            }

            return concatenateCookies(preExistentCookiesArray,
                cookiesThatAreReallyNew.toArray(new Cookie[cookiesThatAreReallyNew.size()]));
        }

        @Override
        public Object putAndMergeCookie(Object preExistentCookies, Map<String, String> newCookiesMap)
        {
            if (newCookiesMap == null)
            {
                return putAndMergeCookie(preExistentCookies, (Cookie[]) null);
            }
            else
            {
                Cookie[] cookiesArray = new Cookie[newCookiesMap.size()];
                int i = 0;
                for (Entry<String, String> cookieEntry : newCookiesMap.entrySet())
                {
                    Cookie cookie = new Cookie();
                    cookie.setName(cookieEntry.getKey());
                    cookie.setValue(cookieEntry.getValue());
                    cookiesArray[i++] = cookie;
                }
                return putAndMergeCookie(preExistentCookies, cookiesArray);
            }
        }

        @Override
        public Cookie[] asArrayOfCookies(Object cookiesObject)
        {
            if (cookiesObject == null)
            {
                return ZERO_COOKIES;
            }
            else
            {
                return (Cookie[]) cookiesObject;
            }
        }

    },
    /**
     * This corresponds to the storage of cookies as {@link Map Map<String,String>},
     * where the keys are the cookie names and the values are the cookie values.
     * <p>
     * All the parameters of type {@link Object} in the method of this object are
     * assumed to be of type {@link Map Map&lt;String, String&gt;} and won't be
     * checked. They will be cast to {@link Map} and used as if all the keys and
     * values are of type {@link String}.
     */
    MAP_STRING_STRING
    {
        @Override
        public Object putAndMergeCookie(Object preExistentCookies, String cookieName, String cookieValue)
        {
            final Map<String, String> cookieMap = (Map<String, String>) preExistentCookies;

            cookieMap.put(cookieName, cookieValue);
            return cookieMap;
        }

        @Override
        public String getCookieValueFromCookies(Object cookiesObject, String cookieName)
        {
            return ((Map<String, String>) cookiesObject).get(cookieName);
        }

        @Override
        public void addCookiesToClient(HttpClient client,
                                       Object cookiesObject,
                                       String policy,
                                       MuleEvent event,
                                       URI destinationUri)
        {
            Map<String, String> cookieMap = (Map<String, String>) cookiesObject;

            client.getParams().setCookiePolicy(CookieHelper.getCookiePolicy(policy));

            String host = destinationUri.getHost();
            String path = destinationUri.getPath();
            Iterator<String> keyIter = cookieMap.keySet().iterator();
            while (keyIter.hasNext())
            {
                String key = keyIter.next();
                String cookieValue = cookieMap.get(key);

                final String value;
                if (event != null)
                {
                    value = event.getMuleContext()
                        .getExpressionManager()
                        .parse(cookieValue, event.getMessage());
                }
                else
                {
                    value = cookieValue;
                }

                Cookie cookie = new Cookie(host, key, value, path, null, false);
                client.getState().addCookie(cookie);
            }

        }

        @Override
        public Object putAndMergeCookie(Object preExistentCookies, Cookie[] newCookiesArray)
        {
            if (newCookiesArray == null)
            {
                return preExistentCookies;
            }
            for (Cookie cookie : newCookiesArray)
            {
                preExistentCookies = putAndMergeCookie(preExistentCookies, cookie.getName(),
                    cookie.getValue());
            }
            return preExistentCookies;
        }

        @Override
        public Object putAndMergeCookie(Object preExistentCookies, Map<String, String> newCookiesMap)
        {
            if (newCookiesMap == null)
            {
                return preExistentCookies;
            }
            for (Entry<String, String> cookieEntry : newCookiesMap.entrySet())
            {
                preExistentCookies = putAndMergeCookie(preExistentCookies, cookieEntry.getKey(),
                    cookieEntry.getValue());
            }
            return preExistentCookies;
        }

        @Override
        public Cookie[] asArrayOfCookies(Object cookiesObject)
        {
            Map<String, String> cookieMap = (Map<String, String>) cookiesObject;
            Cookie[] arrayOfCookies = new Cookie[cookieMap.size()];
            int i = 0;
            for (Entry<String, String> cookieEntry : cookieMap.entrySet())
            {
                Cookie cookie = new Cookie();
                cookie.setName(cookieEntry.getKey());
                cookie.setValue(cookieEntry.getValue());
                arrayOfCookies[i++] = cookie;
            }
            return arrayOfCookies;
        }

    };

    private static final Cookie[] ZERO_COOKIES = new Cookie[0];

    /**
     * Resolves the cookiesObject to the appropriate {@link CookieStorageType}.
     * 
     * @param cookiesObject
     * @return
     */
    public static CookieStorageType resolveCookieStorageType(Object cookiesObject)
    {
        if (cookiesObject == null || cookiesObject instanceof Cookie[])
        {
            return CookieStorageType.ARRAY_OF_COOKIES;
        }
        else if (cookiesObject instanceof Map)
        {
            return CookieStorageType.MAP_STRING_STRING;
        }
        else
        {
            throw new IllegalArgumentException("Invalid cookiesObject. Only " + Cookie.class + "[] and "
                                               + Map.class + " are supported: " + cookiesObject);
        }
    }

    /**
     * @see CookieHelper#putAndMergeCookie(Object, String, String)
     */
    public abstract Object putAndMergeCookie(Object preExistentCookies, String cookieName, String cookieValue);

    /**
     * @see CookieHelper#putAndMergeCookie(Object, Cookie[])
     */
    public abstract Object putAndMergeCookie(Object preExistentCookies, Cookie[] newCookiesArray);

    /**
     * @see CookieHelper#putAndMergeCookie(Object, Map)
     */
    public abstract Object putAndMergeCookie(Object preExistentCookies, Map<String, String> newCookiesMap);

    /**
     * @see CookieHelper#getCookieValueFromCookies(Object, String)
     */
    public abstract String getCookieValueFromCookies(Object cookiesObject, String cookieName);

    /**
     * @see CookieHelper#addCookiesToClient(HttpClient, Object, String, MuleEvent,
     *      URI)
     */
    public abstract void addCookiesToClient(HttpClient client,
                                            Object cookiesObject,
                                            String policy,
                                            MuleEvent event,
                                            URI destinationUri);

    /**
     * @see CookieHelper#asArrayOfCookies(Object)
     */
    public abstract Cookie[] asArrayOfCookies(Object cookiesObject);
}
