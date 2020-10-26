/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling.internal.artifact.params;

import static java.lang.String.format;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.KeyDeserializer;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.function.Function;

import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;

public class DateTimeModule extends SimpleModule {

  private static final String NAME = "SessionTimeModule";
  private static final Version VERSION = Version.unknownVersion();

  DateTimeModule() {
    //This is copied from the behavior in Runtime to avoid inconsistencies.
    addDeserializer(LocalDate.class,
                    new DateTimeDeserializer<>(LocalDate.class,
                                               d -> LocalDate.of(d.getYear(), d.getMonthOfYear(), d.getDayOfMonth())));
    addDeserializer(Date.class, new DateTimeDeserializer<>(Date.class, d -> d.toDate()));
    addDeserializer(LocalDateTime.class, new DateTimeDeserializer<>(LocalDateTime.class, d -> LocalDateTime
        .ofInstant(Instant.ofEpochMilli(d.getMillis()), ZoneId.of(d.getZone().getID()))));
    addDeserializer(Calendar.class, new DateTimeDeserializer<>(Calendar.class, d -> {
      Calendar calendar = Calendar.getInstance();
      calendar.setTime(d.toDate());
      return calendar;
    }));
  }

  @Override
  public String getModuleName() {
    return NAME;
  }

  @Override
  public Version version() {
    return VERSION;
  }

  private static class DateTimeDeserializer<T> extends StdScalarDeserializer<T> {

    private Function<DateTime, T> mapper;

    private DateTimeDeserializer(Class<T> clazz, Function<DateTime, T> mapper) {
      super(clazz);
      this.mapper = mapper;
    }

    @Override
    public T deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
      if (p.hasToken(JsonToken.VALUE_STRING)) {
        String string = p.getText().trim();
        //This is copied from the behavior in Runtime to avoid inconsistencies.
        try {
          return mapper.apply(ISODateTimeFormat.dateTimeParser().withOffsetParsed().parseDateTime(string));
        } catch (DateTimeParseException e) {
          throw new IllegalArgumentException(format("Could not parse value '%s' according to ISO 8601", string));
        }
      }
      throw new IllegalArgumentException("Expected DATETIME value");
    }
  }

}

