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
    return "reconnection-with-streaming-config.xml";
  }

  @Test
  public void cursorComingFromProviderIsResetOnReconnection() throws Exception {
    CursorStream cursorStream = createMockCursor();

    CursorStreamProvider provider = mock(CursorStreamProvider.class);
    when(provider.openCursor()).thenReturn(cursorStream);

    assertReconnection(cursorStream, provider);
  }

  @Test
  public void standaloneCursorIsResetOnReconnection() throws Exception {
    CursorStream cursorStream = createMockCursor();
    assertReconnection(cursorStream, cursorStream);
  }

  @Test
  public void cursorIsNotAffectedIfCloseIsCalled() throws Exception {
    shouldFailWithConnectionException = true;
    operationExecutionCounter.set(0);
    CoreEvent response = flowRunner("streamingReconnectWithClosedStream").withVariable("signature", "hn").run();
    assertThat(response.getMessage().getPayload().getValue(), is("SUCCESS"));
    assertThat(operationExecutionCounter.get(), greaterThanOrEqualTo(2));
  }

  private void assertReconnection(CursorStream cursor, Object container) throws Exception {
    CoreEvent response = flowRunner("streamingReconnect").withVariable("signature", container).run();
    verify(cursor).seek(ORIGINAL_POSITION);
    verify(cursor, times(3)).read(any(byte[].class), anyInt(), anyInt());

    final Object payload = response.getMessage().getPayload().getValue();
    assertThat(payload, is(instanceOf(List.class)));
    assertThat((List<String>) payload, hasSize(3));
  }

  private CursorStream createMockCursor() throws IOException {
    CursorStream cursorStream = mock(CursorStream.class);
    when(cursorStream.getPosition()).thenReturn(ORIGINAL_POSITION);
    when(cursorStream.read(any(byte[].class), anyInt(), anyInt()))
        .thenThrow(new RuntimeException(new ConnectionException("kaboom")))
        .thenAnswer(i -> {
          byte[] buffer = (byte[]) i.getArguments()[0];
          buffer[0] = 'h';
          buffer[1] = 'n';

          return 2;
        })
        .thenReturn(-1);

    return cursorStream;
  }
}
