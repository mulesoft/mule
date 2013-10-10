/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.http;


import org.mule.api.MuleMessage;
import org.mule.api.expression.ExpressionManager;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.NameValuePair;

public class CookieWrapper extends NameValuePair
{
    private String domain;
    private String path;
    private Object expiryDate;
    private String maxAge;
    private String secure;
    private String version;

    public void parse(MuleMessage message, ExpressionManager expressionManager)
    {
        setName(parse(getName(), message, expressionManager));
        setValue(parse(getValue(), message, expressionManager));
        this.domain = parse(domain, message, expressionManager);
        this.path = parse(path, message, expressionManager);
        if(expiryDate != null)
        {
            this.expiryDate = evaluateDate(expiryDate, message, expressionManager);
        }
        this.maxAge = parse(maxAge, message, expressionManager);
        this.secure = parse(secure, message, expressionManager);
        this.version = parse(version, message, expressionManager);
    }

    private String parse(String value, MuleMessage message, ExpressionManager expressionManager)
    {
        if(value != null)
        {
            return expressionManager.parse(value, message);
        }
        return value;
    }

    private Object evaluateDate(Object date, MuleMessage message, ExpressionManager expressionManager)
    {

        if(date != null && date instanceof String && expressionManager.isExpression(date.toString()))
        {
            return expressionManager.evaluate(date.toString(), message);
        }
        return date;
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
            cookie.setExpiryDate(formatExpiryDate(expiryDate));
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

    private Date formatExpiryDate(Object expiryDate) throws ParseException
    {
        if(expiryDate instanceof String)
        {
            SimpleDateFormat format = new SimpleDateFormat(HttpConstants.DATE_FORMAT, Locale.US);
            format.setTimeZone(TimeZone.getTimeZone("GMT"));
            return format.parse((String) expiryDate);
        }
        return (Date) expiryDate;
    }


    public void setDomain(String domain)
    {
        this.domain = domain;
    }

    public void setPath(String path)
    {
        this.path = path;
    }

    public void setExpiryDate(Object expiryDate)
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
