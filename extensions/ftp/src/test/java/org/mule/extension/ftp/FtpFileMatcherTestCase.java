/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp;

import static org.mockito.Mockito.when;
import static org.mule.extension.file.common.api.matcher.MatchPolicy.ACCEPTS;
import static org.mule.extension.file.common.api.matcher.MatchPolicy.ONLY;
import static org.mule.test.allure.AllureConstants.FtpFeature.FTP_EXTENSION;
import org.mule.extension.ftp.api.FtpFileAttributes;
import org.mule.extension.ftp.api.FtpFileMatcher;
import org.mule.test.extension.file.common.FileMatcherContractTestCase;

import java.time.LocalDateTime;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.qatools.allure.annotations.Features;

@Features(FTP_EXTENSION)
public class FtpFileMatcherTestCase
    extends FileMatcherContractTestCase<FtpFileMatcher, FtpFileAttributes> {

  private static final LocalDateTime TIMESTAMP = LocalDateTime.of(1983, 4, 20, 21, 15);

  @Override
  protected FtpFileMatcher createPredicateBuilder() {
    return new FtpFileMatcher();
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
        .setTimestampSince(LocalDateTime.of(1980, 1, 1, 0, 0))
        .setTimestampUntil(LocalDateTime.of(1990, 1, 1, 0, 0))
        .setRegularFiles(ONLY)
        .setDirectories(ACCEPTS)
        .setSymLinks(ACCEPTS)
        .setMinSize(1L)
        .setMaxSize(1024L);

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
