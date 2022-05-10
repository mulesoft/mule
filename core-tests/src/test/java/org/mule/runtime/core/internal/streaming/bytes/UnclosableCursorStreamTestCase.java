/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming.bytes;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.mule.runtime.api.streaming.CursorProvider;
import org.mule.runtime.api.streaming.bytes.CursorStream;
import org.mule.runtime.core.internal.util.message.stream.UnclosableCursorStream;

import java.io.IOException;

import io.qameta.allure.Issue;
import org.junit.Test;

@Issue("W-11073049")
public class UnclosableCursorStreamTestCase {

  private static final String TO_STRING_MESSAGE = "toString Message";

  @Test
  public void delegatesToString() {
    UnclosableCursorStream cursorStream = new UnclosableCursorStream(new TestCursorStream());
    assertThat(cursorStream.toString(), is(TO_STRING_MESSAGE));
  }

  private static class TestCursorStream extends CursorStream {

    @Override
    public int read() throws IOException {
      return 0;
    }

    @Override
    public long getPosition() {
      return 0;
    }

    @Override
    public void seek(long position) throws IOException {

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
      return TO_STRING_MESSAGE;
    }
  }

}
