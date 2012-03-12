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


import org.mule.api.MuleMessage;
import org.mule.api.expression.ExpressionManager;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.NameValuePair;

public class CookieWrapper extends NameValuePair
{
    private static final String COOKIE_EXPIRY_DATE_FORMAT = "EEE, dd MMM yyyy hh:mm:ss zzz";

    private String domain;
    private String path;
    private String expiryDate;
    private String maxAge;
    private String secure;
    private String version;

    public void evaluate(MuleMessage message, ExpressionManager expressionManager)
    {
        setName((String) evaluate(getName(), message, expressionManager));
        setValue((String) evaluate(getValue(), message, expressionManager));
        this.domain = (String) evaluate(domain, message, expressionManager);
        this.path = (String) evaluate(path, message, expressionManager);
        this.expiryDate = (String) evaluate(expiryDate, message, expressionManager);
        if(maxAge != null)
        {
            this.maxAge = String.valueOf(evaluate(maxAge, message, expressionManager));
        }
        if(secure != null)
        {
            this.secure = String.valueOf(evaluate(secure, message, expressionManager));
        }
        if(version != null)
        {
            this.version = String.valueOf(evaluate(version, message, expressionManager));
        }
    }

    private Object evaluate(String value, MuleMessage message, ExpressionManager expressionManager)
    {
        if(value != null && expressionManager.isExpression(value))
        {
            return expressionManager.evaluate(value, message);
        }
        return value;
    }

    public Cookie createCookie() throws ParseException
    {
        Cookie cookie = new Cookie();
        cookie.setName(getName());
        cookie.setValue(getValue());
        cookie.setDomain(domain);
        cookie.setPath(path);

        if(expiryDate != null)
        {
            cookie.setExpiryDate(getExpiryDateFromString(expiryDate));
        }

        if(maxAge != null && expiryDate == null)
        {
            cookie.setExpiryDate(new Date(System.currentTimeMillis() + Integer.valueOf(maxAge) * 1000L));
        }

        if(secure != null)
        {
            cookie.setSecure(Boolean.valueOf(secure));
        }
        if(version != null)
        {
            cookie.setVersion(Integer.valueOf(version));
        }
        return cookie;
    }

    private Date getExpiryDateFromString(String expiryDate) throws ParseException
    {
        SimpleDateFormat format = new SimpleDateFormat(COOKIE_EXPIRY_DATE_FORMAT);
        return format.parse(expiryDate);
    }


    public void setDomain(String domain)
    {
        this.domain = domain;
    }

    public void setPath(String path)
    {
        this.path = path;
    }

    public void setExpiryDate(String expiryDate)
    {
        this.expiryDate = expiryDate;
    }

    public void setMaxAge(String maxAge)
    {
        this.maxAge = maxAge;
    }

    public void setSecure(String secure)
    {
        this.secure = secure;
    }

    public void setVersion(String version)
    {
        this.version = version;
    }

}
