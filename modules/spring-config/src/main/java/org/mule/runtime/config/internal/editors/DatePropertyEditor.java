/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.editors;

import java.beans.PropertyEditorSupport;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;

import org.springframework.util.StringUtils;

/**
 * Handles the conversion of date strings in {@link java.util.Date} objects.
 */
public class DatePropertyEditor extends PropertyEditorSupport {

  private DateFormat dateFormat;

  private DateFormat shortDateFormat;

  private final boolean allowEmpty;

  private final int exactDateLength;


  /**
   * Create a new CustomDateEditor instance, using the given DateFormat for parsing and rendering.
   * <p>
   * The "allowEmpty" parameter states if an empty String should be allowed for parsing, i.e. get interpreted as null value.
   * Otherwise, an IllegalArgumentException gets thrown in that case.
   * 
   * @param longDateFormat DateFormat to use for parsing and rendering
   * @param shortDateFormat a short form of DateFormat to use for parsing and rendering
   * @param allowEmpty if empty strings should be allowed
   */
  public DatePropertyEditor(DateFormat longDateFormat, DateFormat shortDateFormat, boolean allowEmpty) {
    this.dateFormat = longDateFormat;
    this.shortDateFormat = shortDateFormat;
    this.allowEmpty = allowEmpty;
    this.exactDateLength = -1;
  }

  /**
   * Create a new CustomDateEditor instance, using the given DateFormat for parsing and rendering.
   * <p>
   * The "allowEmpty" parameter states if an empty String should be allowed for parsing, i.e. get interpreted as null value.
   * Otherwise, an IllegalArgumentException gets thrown in that case.
   * <p>
   * The "exactDateLength" parameter states that IllegalArgumentException gets thrown if the String does not exactly match the
   * length specified. This is useful because SimpleDateFormat does not enforce strict parsing of the year part, not even with
   * <code>setLenient(false)</code>. Without an "exactDateLength" specified, the "01/01/05" would get parsed to "01/01/0005".
   * 
   * @param longDateFormat DateFormat to use for parsing and rendering
   * @param allowEmpty if empty strings should be allowed
   * @param exactDateLength the exact expected length of the date String
   */
  public DatePropertyEditor(DateFormat longDateFormat, boolean allowEmpty, int exactDateLength) {
    this.dateFormat = longDateFormat;
    this.allowEmpty = allowEmpty;
    this.exactDateLength = exactDateLength;
  }


  /**
   * Parse the Date from the given text, using the specified DateFormat.
   */
  @Override
  public void setAsText(String text) throws IllegalArgumentException {
    if (this.allowEmpty && !StringUtils.hasText(text)) {
      // Treat empty String as null value.
      setValue(null);
    } else if (text.equals("now")) {
      setValue(new Date());
    } else if (this.exactDateLength >= 0 && text.length() != this.exactDateLength) {
      throw new IllegalArgumentException("Could not parse date: it is not exactly" + this.exactDateLength + "characters long");
    } else {
      try {
        if (shortDateFormat != null && text.length() <= 10) {
          setValue(this.shortDateFormat.parse(text));
        } else {
          setValue(this.dateFormat.parse(text));
        }
      } catch (ParseException ex) {
        throw new IllegalArgumentException("Could not parse date: " + ex.getMessage(), ex);
      }
    }
  }

  /**
   * Format the Date as String, using the specified DateFormat.
   */
  @Override
  public String getAsText() {
    Date value = (Date) getValue();
    return (value != null ? this.dateFormat.format(value) : "");
  }
}
