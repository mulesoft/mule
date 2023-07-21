/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
