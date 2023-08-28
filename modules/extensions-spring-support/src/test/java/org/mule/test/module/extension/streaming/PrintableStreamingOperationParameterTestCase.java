/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.module.extension.streaming;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.mule.runtime.api.streaming.CursorProvider;
import org.mule.runtime.api.streaming.bytes.CursorStream;

import java.io.IOException;

import io.qameta.allure.Issue;
import org.junit.Test;

@Issue("W-11073049")
public class PrintableStreamingOperationParameterTestCase extends AbstractStreamingExtensionTestCase {

  private static final String STREAM_CONTENT = "This is the stream content.";

  @Override
  protected String getConfigFile() {
    return "streaming/streaming-parameter-config.xml";
  }

  @Test
  public void printableCursorStream() throws Exception {
    String result = (String) flowRunner("streamingParameter").withPayload(new ByteBasedCursorStream(STREAM_CONTENT.getBytes()))
        .run().getMessage().getPayload().getValue();
    assertThat(result, is(STREAM_CONTENT));
  }

  private static class ByteBasedCursorStream extends CursorStream {

    private byte[] content;
    private int position;

    public ByteBasedCursorStream(byte[] content) {
      this.content = content;
      this.position = 0;
    }

    @Override
    public int read() throws IOException {
      if (position >= content.length) {
        return 0;
      }
      return content[position++];
    }

    @Override
    public long getPosition() {
      return position;
    }

    @Override
    public void seek(long position) throws IOException {
      this.position = (int) position;
    }

    @Override
    public void release() {

    }

    @Override
    public boolean isReleased() {
      return false;
    }

    @Override
    public CursorProvider getProvider() {
      return null;
    }

    @Override
    public String toString() {
      return new String(content);
    }
  }
}
