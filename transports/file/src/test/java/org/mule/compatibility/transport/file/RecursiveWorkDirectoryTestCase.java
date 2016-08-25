/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.compatibility.transport.file;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.tck.junit4.rule.SystemPropertyTemporaryFolder;

import org.hamcrest.core.Is;
import org.junit.Rule;
import org.junit.Test;

public class RecursiveWorkDirectoryTestCase extends FunctionalTestCase {

  @Rule
  public SystemPropertyTemporaryFolder temporaryFolder = new SystemPropertyTemporaryFolder("temp");


  @Override
  protected String getConfigFile() {
    return "recursive-work-directory-config.xml";
  }

  @Test
  public void ignoresWorkDirectoryOnRequest() throws Exception {
    MuleClient client = muleContext.getClient();
    assertThat(client.request("file://" + temporaryFolder.getRoot(), RECEIVE_TIMEOUT).getRight().isPresent(), is(false));
  }
}
