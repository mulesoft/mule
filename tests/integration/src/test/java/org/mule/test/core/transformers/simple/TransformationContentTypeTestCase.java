/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.core.transformers.simple;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.message.Message.of;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.test.AbstractIntegrationTestCase;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class TransformationContentTypeTestCase extends AbstractIntegrationTestCase {

  @Override
  protected String getConfigFile() {
    return "content-type-setting-transformer-configs.xml";
  }

  @Test
  public void testReturnType() throws Exception {
    Transformer trans = muleContext.getRegistry().lookupTransformer("testTransformer");
    assertNotNull(trans);
    String inputMessage = "ABCDEF";

    Message message = of(inputMessage);
    List<Transformer> transformers = Arrays.asList(new Transformer[] {trans});
    message = muleContext.getTransformationService().applyTransformers(message, null, transformers);
    assertThat(message.getPayload().getDataType().getMediaType().getPrimaryType(), is("text"));
    assertThat(message.getPayload().getDataType().getMediaType().getSubType(), is("plain"));
    assertThat(message.getPayload().getDataType().getMediaType().getCharset().get(), is(ISO_8859_1));
  }
}
