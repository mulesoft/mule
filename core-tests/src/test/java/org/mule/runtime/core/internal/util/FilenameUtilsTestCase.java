/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util;

import static org.apache.commons.lang3.SystemUtils.getUserDir;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.mule.runtime.core.api.util.FileUtils;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.io.File;

import org.junit.Test;

@SmallTest
public class FilenameUtilsTestCase extends AbstractMuleTestCase {

  @Test
  public void testFileWithPathComponentsNullParameter() {
    File result = FilenameUtils.fileWithPathComponents(null);
    assertNull(result);
  }

  @Test
  public void testFileWithNullElements() {
    File tempDir = getBuidDirectory();
    File result = FilenameUtils.fileWithPathComponents(new String[] {tempDir.getAbsolutePath(), "tmp", null, "bar"});

    // make sure that we can validate the test result on all platforms.
    String resultNormalized = result.getAbsolutePath().replace(File.separatorChar, '|');
    String excpected = tempDir.getAbsolutePath().replace(File.separatorChar, '|') + "|tmp|bar";
    assertEquals(excpected, resultNormalized);
  }

  @Test
  public void testFileWithPathComponents() {
    String tempDirPath = getBuidDirectory().getAbsolutePath();

    File result = FilenameUtils.fileWithPathComponents(new String[] {tempDirPath, "tmp", "foo", "bar"});

    // make sure that we can validate the test result on all platforms.
    String resultNormalized = result.getAbsolutePath().replace(File.separatorChar, '|');
    String expected = tempDirPath.replace(File.separatorChar, '|') + "|tmp|foo|bar";
    assertEquals(expected, resultNormalized);
  }

  /**
   * Used to obtain base directory used in tests. Uses the build directory; "target" in the current working directory.
   */
  private File getBuidDirectory() {
    return FileUtils.newFile(getUserDir(), "target");
  }

}
