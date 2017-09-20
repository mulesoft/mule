/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.transformer;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.core.api.util.SystemUtils.getDefaultEncoding;
import static org.mule.tck.util.MuleContextUtils.eventBuilder;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.privileged.transformer.ExtendedTransformationService;
import org.mule.runtime.core.privileged.transformer.TransformerChain;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Before;
import org.junit.Test;

import java.nio.charset.Charset;

public class TransformerChainingTestCase extends AbstractMuleContextTestCase {

  private ExtendedTransformationService transformationService;

  @Before
  public void setUp() throws Exception {
    transformationService = new ExtendedTransformationService(muleContext);
  }

  @Test
  public void testSingleChainedTransformer() throws Exception {
    AbstractTransformer validTransformer = (AbstractTransformer) this.getIncreaseByOneTransformer();
    assertNotNull(validTransformer);

    Message message = of(new Integer(0));
    Transformer messageTransformer = new TransformerChain(validTransformer);
    message = transformationService.applyTransformers(message, eventBuilder(muleContext).message(of(0)).build(),
                                                      messageTransformer);

    Object transformedMessage = message.getPayload().getValue();
    assertNotNull(transformedMessage);
    assertEquals(new Integer(1), transformedMessage);
  }

  @Test
  public void testTwoChainedTransformers() throws Exception {
    AbstractTransformer validTransformer = (AbstractTransformer) this.getIncreaseByOneTransformer();
    assertNotNull(validTransformer);

    Message message = of(new Integer(0));
    Transformer messageTransformer = new TransformerChain(validTransformer, validTransformer);
    message = transformationService.applyTransformers(message, eventBuilder(muleContext).message(of(0)).build(),
                                                      singletonList(messageTransformer));

    Object transformedMessage = message.getPayload().getValue();
    assertNotNull(transformedMessage);
    assertEquals(new Integer(2), transformedMessage);
  }

  @Test
  public void testThreeChainedTransformers() throws Exception {
    AbstractTransformer validTransformer = (AbstractTransformer) this.getIncreaseByOneTransformer();
    assertNotNull(validTransformer);

    Message message = of(new Integer(0));
    Transformer messageTransformer = new TransformerChain(validTransformer, validTransformer, validTransformer);
    message = transformationService.applyTransformers(message, eventBuilder(muleContext).message(of(0)).build(),
                                                      messageTransformer);

    Object transformedMessage = message.getPayload().getValue();
    assertNotNull(transformedMessage);
    assertEquals(new Integer(3), transformedMessage);
  }

  @Test(expected = MessageTransformerException.class)
  public void testIgnoreBadInputBreaksWithTransformationOrderInvalidValidWhenEnforcedOn() throws Exception {
    AbstractTransformer invalidTransformer = (AbstractTransformer) this.getInvalidTransformer();
    assertNotNull(invalidTransformer);
    invalidTransformer.setIgnoreBadInput(true);

    AbstractTransformer validTransformer = (AbstractTransformer) this.getIncreaseByOneTransformer();
    assertNotNull(validTransformer);

    Message message = of(new Integer(0));
    Transformer messageTransformer = new TransformerChain(invalidTransformer, validTransformer);
    transformationService.applyTransformers(message, eventBuilder(muleContext).message(of(0)).build(),
                                            messageTransformer);
  }

  @Test
  public void testIgnoreBadInputBreaksChainWithTransformationOrderInvalidValid() throws Exception {
    AbstractTransformer invalidTransformer = (AbstractTransformer) this.getInvalidTransformer();
    assertNotNull(invalidTransformer);
    invalidTransformer.setIgnoreBadInput(false);

    AbstractTransformer validTransformer = (AbstractTransformer) this.getIncreaseByOneTransformer();
    assertNotNull(validTransformer);

    Message message = of(new Integer(0));
    Transformer messageTransformer = new TransformerChain(invalidTransformer, validTransformer);

    try {
      transformationService.applyTransformers(message, eventBuilder(muleContext).message(of(0)).build(),
                                              messageTransformer);
      fail("Transformer chain is expected to fail because of invalid transformer within chain.");
    } catch (MuleException tfe) {
      // ignore
    }
  }

  @Test
  public void testIgnoreBadInputBreaksChainWithTransformationOrderValidInvalid() throws Exception {
    AbstractTransformer invalidTransformer = (AbstractTransformer) this.getInvalidTransformer();
    assertNotNull(invalidTransformer);
    invalidTransformer.setIgnoreBadInput(false);

    AbstractTransformer validTransformer = (AbstractTransformer) this.getIncreaseByOneTransformer();
    assertNotNull(validTransformer);

    Message message = of(new Integer(0));
    Transformer messageTransformer = new TransformerChain(validTransformer, invalidTransformer);

    try {
      transformationService.applyTransformers(message, eventBuilder(muleContext).message(of(0)).build(),
                                              messageTransformer);
      fail("Transformer chain is expected to fail because of invalid transformer within chain.");
    } catch (MuleException tfe) {
      assertNotNull(tfe);
    }
  }

  private Transformer getInvalidTransformer() throws Exception {
    AbstractTransformer transformer = new AbstractTransformer() {

      @Override
      protected Object doTransform(final Object src, final Charset encoding) throws TransformerException {
        throw new RuntimeException("This transformer must not perform any transformations.");
      }
    };

    // Use this class as a bogus source type to enforce a simple invalid transformer
    transformer.registerSourceType(DataType.fromType(this.getClass()));

    return transformer;
  }

  private Transformer getIncreaseByOneTransformer() throws Exception {
    AbstractTransformer transformer = new AbstractTransformer() {

      @Override
      protected Object doTransform(Object src, Charset encoding) throws TransformerException {
        return new Integer(((Integer) src).intValue() + 1);
      }
    };

    DataType integerDataType = DataType.fromType(Integer.class);
    transformer.registerSourceType(integerDataType);
    transformer.setReturnDataType(DataType.builder(integerDataType).charset(getDefaultEncoding(muleContext)).build());

    return transformer;
  }

}
