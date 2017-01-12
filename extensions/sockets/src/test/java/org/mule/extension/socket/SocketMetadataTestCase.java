/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.socket;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.mule.tck.junit4.matcher.MetadataKeyMatcher.metadataKeyWithId;
import org.mule.metadata.api.model.AnyType;
import org.mule.metadata.api.model.BinaryType;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.metadata.ConfigurationId;
import org.mule.runtime.api.metadata.MetadataKey;
import org.mule.runtime.api.metadata.MetadataKeysContainer;
import org.mule.runtime.api.metadata.MetadataService;
import org.mule.runtime.api.metadata.ProcessorId;
import org.mule.runtime.api.metadata.descriptor.ComponentMetadataDescriptor;
import org.mule.runtime.api.metadata.resolving.MetadataResult;
import org.mule.runtime.core.api.registry.RegistrationException;
import org.mule.tck.junit4.rule.DynamicPort;

import java.util.Set;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features("Sockets Connector")
@Stories("Metadata")
public class SocketMetadataTestCase extends SocketExtensionTestCase {

  private MetadataService service;

  @Rule
  public DynamicPort dynamicPort = new DynamicPort("port");

  @Before
  public void setupManager() throws RegistrationException {
    service = muleContext.getRegistry().lookupObject(MetadataService.class);
  }

  @Override
  protected String getConfigFile() {
    return "metadata-config.xml";
  }

  @Test
  @Description("Resolves the MetadataKeys from an implicit Keys resolver based in a boolean parameter")
  public void retrieveSendMetadataKeys() {
    MetadataResult<MetadataKeysContainer> keysResult = service.getMetadataKeys(new ConfigurationId("tcp-requester"));
    String socketsCategory = "SocketCategory";
    assertThat(keysResult.get().getCategories(), contains(socketsCategory));
    Set<MetadataKey> keys = keysResult.get().getKeys(socketsCategory).get();
    assertThat(keys, hasItems(metadataKeyWithId("TRUE"), metadataKeyWithId("FALSE")));
    assertThat(keys, hasSize(2));
  }

  @Test
  @Description("Resolves the Metadata for the send operation, the metadata changes whether the operation waits" +
      "for response or not")
  public void resolveMetadata() {
    ComponentMetadataDescriptor<OperationModel> metadataWithOutResponse =
        service.getOperationMetadata(new ProcessorId("tcp-send-without-response", "0")).get();
    assertThat(metadataWithOutResponse.getModel().getOutput().getType(),
               is(instanceOf(AnyType.class)));

    ComponentMetadataDescriptor<OperationModel> metadataWithResponse =
        service.getOperationMetadata(new ProcessorId("tcp-send-with-response", "0")).get();
    assertThat(metadataWithResponse.getModel().getOutput().getType(),
               is(instanceOf(BinaryType.class)));
  }
}
