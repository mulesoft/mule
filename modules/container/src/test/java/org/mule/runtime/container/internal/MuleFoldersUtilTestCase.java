/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
