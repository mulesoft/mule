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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.junit.Test;

public class ReconnectionWithStreamingTestCase extends AbstractExtensionFunctionalTestCase {

  private static final long ORIGINAL_POSITION = 10;

  @Override
  protected String getConfigFile() {
    return "reconnection-with-streaming-config.xml";
  }

  @Test
  public void cursorComingFromProviderIsResetOnReconnection() throws Exception {
    CursorStream cursorStream = createFailingMockCursor("hn".getBytes());
    CursorStreamProvider provider = mock(CursorStreamProvider.class);
    when(provider.openCursor()).thenReturn(cursorStream);

    CoreEvent response = flowRunner("streamingReconnect").withVariable("signature", provider).run();
    assertReconnection(response, cursorStream);
  }

  @Test
  public void standaloneCursorIsResetOnReconnection() throws Exception {
    CursorStream cursorStream = createFailingMockCursor("hn".getBytes());

    CoreEvent response = flowRunner("streamingReconnect").withVariable("signature", cursorStream).run();
    assertReconnection(response, cursorStream);
  }

  @Test
  public void typedValueCursorIsResetOnReconnection() throws Exception {
    CursorStream ownerSignature = createFailingMockCursor("hn".getBytes());

    CoreEvent response = flowRunner("streamingTypedValueReconnect").withVariable("signature", ownerSignature).run();
    assertReconnection(response, ownerSignature);
  }

  @Test
  public void literalIsRepeatableOnReconnection() throws Exception {
    shouldFailWithConnectionException = true;

    CoreEvent response = flowRunner("literalReconnect").run();
    assertThat(response.getMessage().getPayload().getValue(), is("SUCCESS"));
  }

  @Test
  public void typedValueCursorInParameterGroupIsResetOnReconnection() throws Exception {
    CursorStream ownerSignature = createFailingMockCursor("hn".getBytes());
    CursorStream ownerName = createMockCursor("jo".getBytes());
    CoreEvent response =
        flowRunner("streamingParameterGroupReconnect").withVariable("signature", ownerSignature).withPayload(ownerName).run();

    assertReconnection(response, ownerSignature);
  }

  @Test
  public void cursorInParameterGroupIsResetOnReconnection() throws Exception {
    CursorStream ownerSignature = createMockCursor("hn".getBytes());
    CursorStream ownerName = createFailingMockCursor("jo".getBytes());
    CoreEvent response =
        flowRunner("streamingParameterGroupReconnect").withVariable("signature", ownerSignature).withPayload(ownerName).run();

    assertReconnection(response, ownerName);
  }

  @Test
  public void cursorInMapIsResetOnReconnection() throws Exception {
    CursorStream ownerSignature = createMockCursor("hn".getBytes());
    CursorStream ownerName = createFailingMockCursor("jo".getBytes());
    HashMap<String, InputStream> ownerInformation = new HashMap<>();
    ownerInformation.put("ownerName", ownerName);
    ownerInformation.put("ownerSignature", ownerSignature);

    CoreEvent response = flowRunner("streamingMapReconnect").withPayload(ownerInformation).run();

    assertReconnection(response, ownerName);
  }

  @Test
  public void typedValueCursorInMapIsResetOnReconnection() throws Exception {
    CursorStream ownerSignature = createMockCursor("hn".getBytes());
    CursorStream ownerName = createFailingMockCursor("jo".getBytes());
    HashMap<String, InputStream> ownerInformation = new HashMap<>();
    ownerInformation.put("ownerName", ownerName);
    ownerInformation.put("ownerSignature", ownerSignature);

    CoreEvent response = flowRunner("streamingMapReconnect").withPayload(ownerInformation).run();

    assertReconnection(response, ownerName);
  }

  @Test
  public void cursorInCollectionIsResetOnReconnection() throws Exception {
    CursorStream ownerSignature = createMockCursor("hn".getBytes());
    CursorStream ownerName = createFailingMockCursor("jo".getBytes());
    List<InputStream> ownerInformation = new ArrayList<>();
    ownerInformation.add(ownerName);
    ownerInformation.add(ownerSignature);

    CoreEvent response = flowRunner("streamingCollectionReconnect").withPayload(ownerInformation).run();

    assertReconnection(response, ownerName);
  }

  @Test
  public void typedValueCursorInCollectionIsResetOnReconnection() throws Exception {
    CursorStream ownerSignature = createMockCursor("hn".getBytes());
    CursorStream ownerName = createFailingMockCursor("jo".getBytes());
    List<InputStream> ownerInformation = new ArrayList<>();
    ownerInformation.add(ownerName);
    ownerInformation.add(ownerSignature);

    CoreEvent response = flowRunner("streamingCollectionReconnect").withPayload(ownerInformation).run();

    assertReconnection(response, ownerName);
  }

  @Test
  public void cursorIsNotAffectedIfCloseIsCalled() throws Exception {
    shouldFailWithConnectionException = true;
    operationExecutionCounter.set(0);
    CoreEvent response = flowRunner("streamingReconnectWithClosedStream").withVariable("signature", "hn").run();
    assertThat(response.getMessage().getPayload().getValue(), is("SUCCESS"));
    assertThat(operationExecutionCounter.get(), greaterThanOrEqualTo(2));
  }

  private void assertReconnection(CoreEvent response, CursorStream cursor) throws Exception {
    verify(cursor).seek(ORIGINAL_POSITION);
    verify(cursor, times(3)).read(any(byte[].class), anyInt(), anyInt());

    final Object payload = response.getMessage().getPayload().getValue();
    assertThat(payload, is(instanceOf(List.class)));
    assertThat((List<String>) payload, hasSize(3));
  }

  private CursorStream createFailingMockCursor(byte[] bytes) throws IOException {
    CursorStream cursorStream = mock(CursorStream.class);
    when(cursorStream.getPosition()).thenReturn(ORIGINAL_POSITION);
    when(cursorStream.read(any(byte[].class), anyInt(), anyInt()))
        .thenThrow(new RuntimeException(new ConnectionException("kaboom")))
        .thenAnswer(i -> {
          byte[] buffer = (byte[]) i.getArguments()[0];
          buffer[0] = bytes[0];
          buffer[1] = bytes[1];

          return 2;
        })
        .thenReturn(-1);

    return cursorStream;
  }

  private CursorStream createMockCursor(byte[] bytes) throws IOException {
    CursorStream cursorStream = mock(CursorStream.class);
    when(cursorStream.getPosition()).thenReturn(ORIGINAL_POSITION);
    when(cursorStream.read(any(byte[].class), anyInt(), anyInt()))
        .thenAnswer(i -> {
          byte[] buffer = (byte[]) i.getArguments()[0];
          buffer[0] = bytes[0];
          buffer[1] = bytes[1];

          return 2;
        })
        .thenAnswer(i -> -1)
        .thenAnswer(i -> {
          byte[] buffer = (byte[]) i.getArguments()[0];
          buffer[0] = bytes[0];
          buffer[1] = bytes[1];

          return 2;
        })
        .thenReturn(-1);

    return cursorStream;
  }
}
