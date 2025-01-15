/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.transformer;

import static java.nio.charset.Charset.defaultCharset;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.fail;

import org.mule.runtime.api.metadata.DataType;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.nio.charset.Charset;

import org.junit.Test;

@SmallTest
public class TransformerSourceTypeEnforcementTestCase extends AbstractMuleTestCase {

  @Test
  public void rejectsBadInputIfEnforcementOn() throws TransformerException {
    AbstractTransformer transformer = createDummyTransformer(true);

    assertThrows(TransformerException.class, () -> transformer.transform("TEST"));
  }

  @Test
  public void rejectsBadInputUsingDefaultEnforcement() throws TransformerException {
    AbstractTransformer transformer = createDummyTransformer(true);

    assertThrows(TransformerException.class, () -> transformer.transform("TEST"));
  }

  @Test
  public void transformsValidSourceTypeWithNoCheckForEnforcement() throws TransformerException {
    AbstractTransformer transformer = createDummyTransformer(true);
    transformer.sourceTypes.add(DataType.STRING);
    transformer.setReturnDataType(DataType.STRING);

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
    result.setArtifactEncoding(() -> defaultCharset());
    result.setIgnoreBadInput(ignoreBadInput);

    return result;
  }
}
