/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.container.internal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import org.mule.runtime.container.api.MuleFoldersUtil;
import org.mule.runtime.core.api.config.MuleProperties;
import org.mule.tck.MuleTestUtils;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.io.File;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

@SmallTest
public class MuleFoldersUtilTestCase extends AbstractMuleTestCase {

  @Rule
  public TemporaryFolder muleHome = new TemporaryFolder();

  @Test
  public void getsMuleHome() throws Exception {
    MuleTestUtils.testWithSystemProperty(MuleProperties.MULE_HOME_DIRECTORY_PROPERTY, muleHome.getRoot().getAbsolutePath(),
                                         () -> {
                                           File folder = MuleFoldersUtil.getMuleHomeFolder();
                                           assertThat(folder.getAbsolutePath(),
                                                      equalTo(muleHome.getRoot().getAbsolutePath()));
                                         });
  }
}
