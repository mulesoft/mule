/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.metadata;

import static org.mule.extension.ws.WscTestUtils.DOWNLOAD_ATTACHMENT;
import static org.mule.extension.ws.WscTestUtils.ECHO;
import static org.mule.extension.ws.WscTestUtils.ECHO_ACCOUNT;
import static org.mule.extension.ws.WscTestUtils.ECHO_HEADERS;
import static org.mule.extension.ws.WscTestUtils.FAIL;
import static org.mule.extension.ws.WscTestUtils.NO_PARAMS;
import static org.mule.extension.ws.WscTestUtils.NO_PARAMS_HEADER;
import static org.mule.extension.ws.WscTestUtils.UPLOAD_ATTACHMENT;
import static org.mule.test.allure.AllureConstants.WscFeature.WSC_EXTENSION;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.collection.IsIn.isIn;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.mule.runtime.api.metadata.MetadataKey;
import org.mule.runtime.api.metadata.MetadataKeysContainer;
import org.mule.runtime.api.metadata.resolving.MetadataResult;

import java.util.Set;

import org.junit.Test;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features(WSC_EXTENSION)
@Stories("Metadata")
public class KeysMetadataTestCase extends AbstractMetadataTestCase {

  public static final String[] OPERATIONS =
      {ECHO, ECHO_ACCOUNT, ECHO_HEADERS, FAIL, NO_PARAMS_HEADER, NO_PARAMS, UPLOAD_ATTACHMENT, DOWNLOAD_ATTACHMENT};

  @Test
  @Description("Checks the MetadataKeys for the WSC")
  public void getOperationKeys() {
    MetadataResult<MetadataKeysContainer> result = service.getMetadataKeys(location(ECHO_ACCOUNT_FLOW));
    assertThat(result.isSuccess(), is(true));
    Set<MetadataKey> keys = result.get().getKeys("WebServiceConsumerCategory").get();
    assertThat(keys, hasSize(OPERATIONS.length));
    keys.forEach(key -> assertThat(key.getId(), isIn(OPERATIONS)));
  }
}
