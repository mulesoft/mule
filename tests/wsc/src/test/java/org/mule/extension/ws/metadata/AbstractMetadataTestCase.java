/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.metadata;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.metadata.MetadataKeyBuilder.newKey;
import org.mule.extension.ws.AbstractSoapServiceTestCase;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.runtime.api.metadata.MetadataService;
import org.mule.runtime.api.metadata.ProcessorId;
import org.mule.runtime.api.metadata.descriptor.ComponentMetadataDescriptor;
import org.mule.runtime.api.metadata.resolving.MetadataResult;
import org.mule.runtime.core.internal.metadata.MuleMetadataService;

import org.junit.Before;
import ru.yandex.qatools.allure.annotations.Step;

public abstract class AbstractMetadataTestCase extends AbstractSoapServiceTestCase {

  protected static final String MESSAGE_PARAM = "message";

  protected static final String ECHO_ACCOUNT_FLOW = "getEchoAccountMetadata";
  protected static final String ECHO_FLOW = "getEchoMetadata";
  protected static final String NO_PARAMS_FLOW = "getNoParams";
  protected static final String ECHO_HEADERS_FLOW = "getEchoHeadersMetadata";

  protected MetadataService service;

  @Before
  public void init() throws Exception {
    service = muleContext.getRegistry().lookupObject(MuleMetadataService.class);
  }

  @Override
  protected String getConfigurationFile() {
    return "config/metadata.xml";
  }

  @Step("Retrieve Dynamic Metadata")
  protected MetadataResult<ComponentMetadataDescriptor> getMetadata(String flow, String key) {
    MetadataResult<ComponentMetadataDescriptor> result = service.getMetadata(id(flow), newKey(key).build());
    assertThat(result.isSuccess(), is(true));
    return result;
  }

  @Step("Retrieve Dynamic Metadata for the Message Builder parameter")
  protected ObjectType getMessageBuilderType(String flow, String key) {
    MetadataResult<ComponentMetadataDescriptor> metadata = getMetadata(flow, key);
    return toObjectType(metadata.get().getInputMetadata().get().getParameterMetadata(MESSAGE_PARAM).get().getType());
  }

  protected ProcessorId id(String flow) {
    return new ProcessorId(flow, "0");
  }

  protected ObjectType toObjectType(MetadataType type) {
    assertThat(type, is(instanceOf(ObjectType.class)));
    return (ObjectType) type;
  }

  protected MetadataType getMessageBuilderFieldType(MetadataType messageResult, String name) {
    ObjectType objectType = toObjectType(messageResult);
    return objectType.getFields().stream()
        .filter(f -> f.getKey().getName().getLocalPart().equals(name)).findAny().get().getValue();
  }
}
