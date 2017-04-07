/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.file;

import static org.mule.test.allure.AllureConstants.FileFeature.FILE_EXTENSION;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;

import ru.yandex.qatools.allure.annotations.Features;

@Features(FILE_EXTENSION)
public class FileMoveTestCase extends FileCopyTestCase {

  @Override
  protected String getConfigFile() {
    return "file-move-config.xml";
  }

  @Override
  protected String getFlowName() {
    return "move";
  }

  @Override
  protected void assertCopy(String target) throws Exception {
    super.assertCopy(target);
    assertThat(new File(sourcePath).exists(), is(false));
  }
}
