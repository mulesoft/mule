/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.http;


import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.expression.ExpressionManager;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.NameValuePair;

public class CookieWrapper extends NameValuePair {

  private String domain;
  private String path;
  private Object expiryDate;
  private String maxAge;
  private String secure;
  private String version;

  public void parse(MuleEvent event, ExpressionManager expressionManager) {
    setName(parse(getName(), event, expressionManager));
    setValue(parse(getValue(), event, expressionManager));
    this.domain = parse(domain, event, expressionManager);
    this.path = parse(path, event, expressionManager);
    if (expiryDate != null) {
      this.expiryDate = evaluateDate(expiryDate, event, expressionManager);
    }
    this.maxAge = parse(maxAge, event, expressionManager);
    this.secure = parse(secure, event, expressionManager);
    this.version = parse(version, event, expressionManager);
  }

  private String parse(String value, MuleEvent event, ExpressionManager expressionManager) {
    if (value != null) {
      return expressionManager.parse(value, event);
    }
    return value;
  }

  private Object evaluateDate(Object date, MuleEvent event, ExpressionManager expressionManager) {

    if (date != null && date instanceof String && expressionManager.isExpression(date.toString())) {
      return expressionManager.evaluate(date.toString(), event);
    }
    return date;
  }

  public Cookie createCookie() throws ParseException {
    Cookie cookie = new Cookie();
    cookie.setName(getName());
    cookie.setValue(getValue());
    cookie.setDomain(domain);
    cookie.setPath(path);

    if (expiryDate != null) {
      cookie.setExpiryDate(formatExpiryDate(expiryDate));
    }

    if (maxAge != null && expiryDate == null) {
      cookie.setExpiryDate(new Date(System.currentTimeMillis() + Integer.valueOf(maxAge) * 1000L));
    }

    if (secure != null) {
      cookie.setSecure(Boolean.valueOf(secure));
    }
    if (version != null) {
      cookie.setVersion(Integer.valueOf(version));
    }

    return cookie;
  }

  private Date formatExpiryDate(Object expiryDate) throws ParseException {
    if (expiryDate instanceof String) {
      SimpleDateFormat format = new SimpleDateFormat(HttpConstants.DATE_FORMAT_RFC822, Locale.US);
      format.setTimeZone(TimeZone.getTimeZone("GMT"));
      return format.parse((String) expiryDate);
    }
    return (Date) expiryDate;
  }


  public void setDomain(String domain) {
    this.domain = domain;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public void setExpiryDate(Object expiryDate) {
    this.expiryDate = expiryDate;
  }

  public void setMaxAge(String maxAge) {
    this.maxAge = maxAge;
  }

  public void setSecure(String secure) {
    this.secure = secure;
  }

  public void setVersion(String version) {
    this.version = version;
  }

}
