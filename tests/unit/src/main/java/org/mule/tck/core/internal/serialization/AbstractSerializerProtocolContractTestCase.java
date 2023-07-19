/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.tck.core.internal.serialization;

import static org.mule.runtime.api.message.Message.of;
import static org.mule.tck.util.MuleContextUtils.eventBuilder;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

import org.mule.runtime.api.serialization.SerializationProtocol;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.el.datetime.DateTime;
import org.mule.runtime.core.internal.message.InternalMessage;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

public abstract class AbstractSerializerProtocolContractTestCase extends AbstractMuleContextTestCase {

  /**
   * This class is added to get rid of mockito spy, tried replacing it with mockito-inline, that still does not work in Java 17
   * and it still requires us to open java.base/java.io so that protected byte[] java.io.ByteArrayInputStream.buf can be accessed,
   * which we are trying to avoid at all costs"
   */
  public class CloseCounterInputStreamWrapper extends InputStream {

    private final InputStream inputStream;

    private AtomicInteger closeCount = new AtomicInteger();

    public CloseCounterInputStreamWrapper(InputStream inputStream) {
      this.inputStream = inputStream;
    }

    @Override
    public int read() throws IOException {
      return inputStream.read();
    }

    public void close() throws IOException {
      closeCount.incrementAndGet();
      inputStream.close();
    }

    public int getCloseCount() {
      return closeCount.intValue();
    }
  }

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

    CloseCounterInputStreamWrapper inputStream = new CloseCounterInputStreamWrapper(new ByteArrayInputStream(bytes));
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
