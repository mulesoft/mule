/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.core.internal.serialization;

import static org.mule.runtime.api.message.Message.of;
import static org.mule.tck.util.MuleContextUtils.eventBuilder;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.core.Is.is;

import org.mule.runtime.api.serialization.SerializationProtocol;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.el.datetime.DateTime;
import org.mule.runtime.core.internal.message.InternalMessage;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Locale;

import org.junit.Test;

public abstract class AbstractSerializerProtocolContractTestCase extends AbstractMuleContextTestCase {

  private static final String STRING_MESSAGE = "Hello World";

  protected SerializationProtocol serializationProtocol;

  @Test(expected = IllegalArgumentException.class)
  public final void nullBytes() throws Exception {
    serializationProtocol.deserialize((byte[]) null);
  }

  @Test(expected = IllegalArgumentException.class)
  public final void nullStream() throws Exception {
    serializationProtocol.deserialize((InputStream) null);
  }

  @Test
  public final void nullObject() throws Exception {
    byte[] bytes = serializationProtocol.serialize(null);
    Object object = serializationProtocol.deserialize(bytes);
    assertNull(object);
  }

  @Test
  public final void inputStreamClosed() throws Exception {
    final byte[] bytes = serializationProtocol.serialize(STRING_MESSAGE);
    CustomByteArrayInputStream inputStream = new CustomByteArrayInputStream(new ByteArrayInputStream(bytes));
    String output = serializationProtocol.deserialize(inputStream);

    assertThat(inputStream.getCloseCount(), greaterThanOrEqualTo(1));
    assertThat(output, equalTo(STRING_MESSAGE));
  }

  @Test
  public final void serializeWithoutDefaultConstructor() throws Exception {
    Calendar calendar = Calendar.getInstance();
    Locale locale = Locale.ITALIAN;

    DateTime dateTime = new DateTime(calendar, locale);
    dateTime.changeTimeZone("Pacific/Midway");

    CoreEvent event = eventBuilder(muleContext).message(of(dateTime)).build();
    byte[] bytes = serializationProtocol.serialize(event.getMessage());

    InternalMessage message = serializationProtocol.deserialize(bytes);
    DateTime deserealized = (DateTime) message.getPayload().getValue();

    assertEquals(calendar, deserealized.toCalendar());
    assertEquals(dateTime.format(), deserealized.format());
  }
}
