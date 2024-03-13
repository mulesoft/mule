/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util;

import static org.mule.runtime.core.internal.util.FilenameUtils.normalizeDecodedPath;

import static java.io.File.separator;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Test;

@SmallTest
public class FilenameUtilsTestCase extends AbstractMuleTestCase {

  @Test
  public void doesNotAddSlashAtBeginningInWindows() {
    String path = "file:/C:/ProgramFiles/zaraza";
    assertThat(normalizeDecodedPath(path, true), is("C:/ProgramFiles/zaraza"));
  }

  @Test
  public void addsSlashAtBeginningInUnix() {
    String path = "file:/etc/zaraza";
    assertThat(normalizeDecodedPath(path, false),
               is(separator + "etc/zaraza"));
  }

  @Test
  public void addsSlashAtBeginningInWindowsWhenSharedPath() {
    String path = "file://SERVER/zaraza";
    assertThat(normalizeDecodedPath(path, true), is(separator + "/SERVER/zaraza"));
  }

}
