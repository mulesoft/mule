/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.transformer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import com.google.common.base.Charsets;

import java.nio.charset.Charset;

import org.junit.Before;
import org.junit.Test;

@SmallTest
public class TransformerSourceTypeEnforcementTestCase extends AbstractMuleTestCase {

  private MuleContext muleContext = mock(MuleContext.class);
  private MuleConfiguration muleConfiguration = mock(MuleConfiguration.class);

  @Before
  public void setUp() throws Exception {
    when(muleConfiguration.getDefaultEncoding()).thenReturn(Charsets.UTF_8.name());
    when(muleContext.getConfiguration()).thenReturn(muleConfiguration);
  }

  @Test
  public void rejectsBadInputIfEnforcementOn() throws TransformerException {
    AbstractTransformer transformer = createDummyTransformer(true);

    try {
      transformer.transform("TEST");
      fail("Transformation should fail because source type is not supported");
    } catch (TransformerException expected) {
    }
  }

  @Test
  public void rejectsBadInputUsingDefaultEnforcement() throws TransformerException {
    AbstractTransformer transformer = createDummyTransformer(true);

    try {
      transformer.transform("TEST");
      fail("Transformation should fail because source type is not supported");
    } catch (TransformerException expected) {
    }
  }

  @Test
  public void transformsValidSourceTypeWithNoCheckForEnforcement() throws TransformerException {
    AbstractTransformer transformer = createDummyTransformer(true);
    transformer.sourceTypes.add(DataType.STRING);
    transformer.setReturnDataType(DataType.STRING);

    when(muleContext.getConfiguration()).thenReturn(muleConfiguration);

    Object result = transformer.transform("TEST");
    assertEquals("TRANSFORMED", result);
  }

  private AbstractTransformer createDummyTransformer(boolean ignoreBadInput) {
    AbstractTransformer result = new AbstractTransformer() {

      @Override
      protected Object doTransform(Object src, Charset enc) throws TransformerException {
        return "TRANSFORMED";
      }
    };

    result.sourceTypes.add(DataType.BYTE_ARRAY);
    result.setMuleContext(muleContext);
    result.setIgnoreBadInput(ignoreBadInput);

    return result;
  }
}
