/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.el.datetime;

import java.util.Calendar;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.XMLGregorianCalendar;

public interface Instant {

  boolean isBefore(Instant date);

  boolean isAfter(Instant date);

  String format();

  String format(String pattern);

  String getTimeZone();

  Instant withTimeZone(String newTimezone);

  Instant changeTimeZone(String newTimezone);

  Instant withLocale(String locale);

  Calendar toCalendar();

  java.util.Date toDate();

  XMLGregorianCalendar toXMLCalendar() throws DatatypeConfigurationException;

}
