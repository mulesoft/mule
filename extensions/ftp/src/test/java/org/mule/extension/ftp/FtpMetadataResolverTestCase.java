/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp;

import org.mule.extension.FtpTestHarness;
import org.mule.runtime.api.metadata.MetadataManager;
import org.mule.runtime.core.api.registry.RegistrationException;
import org.mule.runtime.module.extension.file.api.FileMetadataResolverTestCommon;

import org.junit.Before;
import org.junit.Test;

public class FtpMetadataResolverTestCase extends FtpConnectorTestCase {

  private MetadataManager manager;
  private FileMetadataResolverTestCommon testCommon;

  public FtpMetadataResolverTestCase(String name, FtpTestHarness testHarness) {
    super(name, testHarness);
  }

  @Override
  protected String getConfigFile() {
    return "ftp-metadata-config.xml";
  }

  @Before
  public void setupManager() throws RegistrationException {
    manager = muleContext.getRegistry().lookupObject(MetadataManager.class);
    testCommon = new FileMetadataResolverTestCommon();
  }

  @Test
  public void getReadAttributesMetadata() {
    testCommon.testReadAttributesMetadata(manager, testHarness.getAttributesType());
  }

  @Test
  public void getListOperationOutputMetadata() {
    testCommon.testTreeNodeType(manager, testHarness.getAttributesType());
  }
}
