/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.deployment.model.internal.nativelib;

import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;
import org.mule.runtime.core.api.config.MuleProperties;
import org.mule.runtime.deployment.model.internal.nativelib.NativeLibraryFinder;
import org.mule.runtime.deployment.model.internal.nativelib.DefaultNativeLibraryFinderFactory;
import org.mule.runtime.deployment.model.internal.nativelib.PerAppNativeLibraryFinder;
import org.mule.tck.MuleTestUtils;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

@SmallTest
public class DefaultNativeLibraryFinderFactoryTestCase extends AbstractMuleTestCase {

  @Rule
  public TemporaryFolder muleHomeFolder = new TemporaryFolder();

  private final DefaultNativeLibraryFinderFactory nativeLibraryFinderFactory = new DefaultNativeLibraryFinderFactory();

  @Test
  public void createsPerAppNativeLibraryFinderWhenPropertyIsFalse() throws Exception {
    doCreateNativeLibraryFinderTest(PerAppNativeLibraryFinder.class);
  }

  private void doCreateNativeLibraryFinderTest(final Class<? extends NativeLibraryFinder> expectedNativeLibraryFinderClass)
      throws Exception {
    MuleTestUtils.testWithSystemProperty(MuleProperties.MULE_HOME_DIRECTORY_PROPERTY, muleHomeFolder.getRoot().getAbsolutePath(),
                                         new MuleTestUtils.TestCallback() {

                                           @Override
                                           public void run() throws Exception {
                                             NativeLibraryFinder nativeLibraryFinder =
                                                 nativeLibraryFinderFactory.create("testApp");

                                             assertThat(nativeLibraryFinder,
                                                        instanceOf(expectedNativeLibraryFinderClass));
                                           }
                                         });
  }
}
