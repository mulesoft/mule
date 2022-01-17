/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.streaming;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.test.petstore.extension.PetStoreOperations.operationExecutionCounter;
import static org.mule.test.petstore.extension.PetStoreOperations.shouldFailWithConnectionException;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.streaming.bytes.CursorStream;
import org.mule.runtime.api.streaming.bytes.CursorStreamProvider;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;

import java.io.IOException;
import java.util.List;

import org.junit.Test;

public class ReconnectionWithStreamingTestCase extends AbstractExtensionFunctionalTestCase {

  private static final long ORIGINAL_POSITION = 10;

  @Override
  protected String getConfigFile() {
    return "streaming/reconnection-with-streaming-config.xml";
  }

  @Test
  public void cursorComingFromProviderIsResetOnReconnection() throws Exception {
    CursorStream cursorStream = createMockCursor(ORIGINAL_POSITION, "hn");

    CursorStreamProvider provider = mock(CursorStreamProvider.class);
    when(provider.openCursor()).thenReturn(cursorStream);

    assertReconnection("streamingReconnect", cursorStream, provider);
  }

  @Test
  public void standaloneCursorIsResetOnReconnection() throws Exception {
    CursorStream cursorStream = createMockCursor(ORIGINAL_POSITION, "hn");
    assertReconnection("streamingReconnect", cursorStream, cursorStream);
  }

  @Test
  public void cursorIsNotAffectedIfCloseIsCalled() throws Exception {
    shouldFailWithConnectionException = true;
    operationExecutionCounter.set(0);
    CoreEvent response = flowRunner("streamingReconnectWithClosedStream").withVariable("signature", "hn").run();
    assertThat(response.getMessage().getPayload().getValue(), is("SUCCESS"));
    assertThat(operationExecutionCounter.get(), greaterThanOrEqualTo(2));
  }

  @Test
  public void cursorWrappedInTypedValueIsNotAffectedIfCloseIsCalled() throws Exception {
    shouldFailWithConnectionException = true;
    operationExecutionCounter.set(0);
    CoreEvent response = flowRunner("streamingTypedValueReconnectWithClosedStream").withVariable("signature", "hn").run();
    assertThat(response.getMessage().getPayload().getValue(), is("SUCCESS"));
    assertThat(operationExecutionCounter.get(), greaterThanOrEqualTo(2));
  }

  @Test
  public void cursorInParameterGroupIsResetOnReconnection() throws Exception {
    CursorStream cursor = createMockCursor(ORIGINAL_POSITION, "hn");
    assertReconnection("streamingReconnectWithParameterGroup", cursor, cursor);
  }

  @Test
  public void cursorInParameterGroupWithShowDslIsResetOnReconnection() throws Exception {
    CursorStream cursor = createMockCursor(ORIGINAL_POSITION, "hn");
    assertReconnection("streamingReconnectWithParameterGroupShowDsl", cursor, cursor);
  }

  @Test
  public void cursorWithTypedValueInParameterGroupWithShowDslIsResetOnReconnection() throws Exception {
    CursorStream signatureCursor = createMockCursor(ORIGINAL_POSITION, "hn");
    CursorStream addressCursor = createMockCursorNotThrowingError(ORIGINAL_POSITION, "Juana Manso 999");

    CoreEvent response = flowRunner("streamingReconnectWithParameterGroupShowDslWithTypedParameter")
        .withVariable("signature", signatureCursor)
        .withVariable("address", addressCursor)
        .run();

    assertCursor(signatureCursor, ORIGINAL_POSITION, 3);
    assertCursor(addressCursor, ORIGINAL_POSITION, 3);
    assertResponse(response);
  }

  @Test
  public void cursorWithAliasInParameterGroupWithShowDslIsResetOnReconnection() throws Exception {
    CursorStream signatureCursor = createMockCursor(ORIGINAL_POSITION, "hn");
    CursorStream certificateCursor = createMockCursorNotThrowingError(ORIGINAL_POSITION, "ownership certificate");

    CoreEvent response = flowRunner("streamingReconnectWithParameterGroupShowDslWithParameterWithAlias")
        .withVariable("signature", signatureCursor)
        .withVariable("certificate", certificateCursor)
        .run();

    assertCursor(signatureCursor, ORIGINAL_POSITION, 3);
    assertCursor(certificateCursor, ORIGINAL_POSITION, 3);
    assertResponse(response);
  }

  @Test
  public void cursorWithTypedValueIsResetOnReconnection() throws Exception {
    CursorStream cursor = createMockCursor(ORIGINAL_POSITION, "hn");
    assertReconnection("streamingReconnectWithTypedParameter", cursor, cursor);
  }

  private void assertReconnection(String flowName, CursorStream cursor, Object container) throws Exception {
    CoreEvent response = flowRunner(flowName).withVariable("signature", container).run();
    assertCursor(cursor, ORIGINAL_POSITION, 3);
    assertResponse(response);
  }

  private void assertCursor(CursorStream cursor, long position, int numberOfReads) throws IOException {
    verify(cursor).seek(position);
    verify(cursor, times(numberOfReads)).read(any(byte[].class), anyInt(), anyInt());
  }

  private void assertResponse(CoreEvent response) {
    final Object payload = response.getMessage().getPayload().getValue();
    assertThat(payload, is(instanceOf(List.class)));
    assertThat((List<String>) payload, hasSize(3));
  }

  private CursorStream createMockCursor(long originalPosition, String data) throws IOException {
    CursorStream cursorStream = mock(CursorStream.class);
    when(cursorStream.getPosition()).thenReturn(originalPosition);
    when(cursorStream.read(any(byte[].class), anyInt(), anyInt()))
        .thenThrow(new RuntimeException(new ConnectionException("kaboom")))
        .thenAnswer(i -> copyDataBytes(data, (byte[]) i.getArguments()[0]))
        .thenReturn(-1);
    return cursorStream;
  }

  private CursorStream createMockCursorNotThrowingError(long originalPosition, String data) throws IOException {
    CursorStream cursorStream = mock(CursorStream.class);
    when(cursorStream.getPosition()).thenReturn(originalPosition);
    when(cursorStream.read(any(byte[].class), anyInt(), anyInt()))
        .thenAnswer(i -> copyDataBytes(data, (byte[]) i.getArguments()[0]))
        .thenReturn(-1);
    return cursorStream;
  }

  private int copyDataBytes(String data, byte[] buffer) {
    byte[] dataBuffer = data.getBytes();
    System.arraycopy(dataBuffer, 0, buffer, 0, dataBuffer.length);
    return dataBuffer.length;
  }
}
