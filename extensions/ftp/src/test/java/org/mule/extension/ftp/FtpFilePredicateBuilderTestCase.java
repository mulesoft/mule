/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp;

import static org.mockito.Mockito.when;
import org.mule.extension.ftp.api.FtpFileAttributes;
import org.mule.extension.ftp.api.FtpFilePredicateBuilder;
import org.mule.runtime.module.extension.file.FilePredicateBuilderContractTestCase;

import java.time.LocalDateTime;

import org.junit.Before;
import org.junit.Test;

public class FtpFilePredicateBuilderTestCase
    extends FilePredicateBuilderContractTestCase<FtpFilePredicateBuilder, FtpFileAttributes> {

  private static final LocalDateTime TIMESTAMP = LocalDateTime.of(1983, 4, 20, 21, 15);

  @Override
  protected FtpFilePredicateBuilder createPredicateBuilder() {
    return new FtpFilePredicateBuilder();
  }

  @Override
  protected Class<FtpFileAttributes> getFileAttributesClass() {
    return FtpFileAttributes.class;
  }

  @Before
  @Override
  public void before() {
    super.before();
    when(attributes.getTimestamp()).thenReturn(TIMESTAMP);
  }

  @Test
  public void matchesAll() {
    builder.setFilenamePattern("glob:*.{java, js}").setPathPattern("glob:**.{java, js}")
        .setTimestampSince(LocalDateTime.of(1980, 1, 1, 0, 0)).setTimestampUntil(LocalDateTime.of(1990, 1, 1, 0, 0))
        .setRegularFile(true).setDirectory(false).setSymbolicLink(false).setMinSize(1L).setMaxSize(1024L);

    assertMatch();
  }

  @Test
  public void timestampSince() {
    builder.setTimestampSince(LocalDateTime.of(1980, 1, 1, 0, 0));
    assertMatch();
  }

  @Test
  public void timestampUntil() {
    builder.setTimestampUntil(LocalDateTime.of(1990, 1, 1, 0, 0));
    assertMatch();
  }

  @Test
  public void rejectTimestampSince() {
    builder.setTimestampSince(LocalDateTime.of(1984, 1, 1, 0, 0));
    assertReject();
  }

  @Test
  public void rejectTimestampUntil() {
    builder.setTimestampUntil(LocalDateTime.of(1982, 4, 2, 0, 0));
    assertReject();
  }
}
