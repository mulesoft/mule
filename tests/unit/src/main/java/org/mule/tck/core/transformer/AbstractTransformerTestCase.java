/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.core.transformer;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.fail;
import static org.mule.runtime.api.message.Message.of;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.api.util.IOUtils;
import org.mule.runtime.core.privileged.transformer.ExtendedTransformationService;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.testmodels.fruit.InvalidSatsuma;

import java.io.InputStream;
import java.util.Arrays;

import org.junit.Test;

public abstract class AbstractTransformerTestCase extends AbstractMuleContextTestCase {

  // Remove tabs and line breaks in the passed String; this makes comparison of XML
  // fragments easier
  protected String normalizeString(String rawString) {
    rawString = rawString.replaceAll("\r", "");
    rawString = rawString.replaceAll("\n", "");
    return rawString.replaceAll("\t", "");
  }

  @Test
  public void testTransform() throws Exception {
    Transformer trans = this.getTransformer();
    Object result = trans.transform(getTestData());

    Object expectedResult = this.getResultData();

    final boolean match = this.compareResults(expectedResult, result);
    if (!match) {
      fail(format("Transformation result does not match expected result. Expected '%s', but got '%s'",
                  expectedResult, result));
    }
  }

  @Test
  public void testRoundtripTransform() throws Exception {
    Transformer roundTripTransformer = this.getRoundTripTransformer();
    // If null this is just a one way test
    if (roundTripTransformer != null) {
      Object result = roundTripTransformer.transform(this.getResultData());
      assertNotNull("The result of the roundtrip transform shouldn't be null", result);

      final boolean match = this.compareRoundtripResults(this.getTestData(), result);

      if (!match) {
        fail(format("The result of the roundtrip transform does not match expected result. Expected '%s', but got '%s'",
                    this.getTestData(), result));
      }
    }
  }

  @Test
  public void testBadReturnType() throws Exception {
    this.doTestBadReturnType(this.getTransformer(), this.getTestData());
  }

  @Test
  public void testRoundtripBadReturnType() throws Exception {
    if (this.getRoundTripTransformer() != null) {
      this.doTestBadReturnType(this.getRoundTripTransformer(), this.getResultData());
    }
  }

  @Test
  public void testRoundTrip() throws Exception {
    if (this.getRoundTripTransformer() != null) {
      Transformer trans = this.getTransformer();
      Transformer trans2 = this.getRoundTripTransformer();
      Message message = of(getTestData());
      message = ((ExtendedTransformationService) muleContext.getTransformationService()).applyTransformers(message, null,
                                                                                                           asList(trans, trans2));
      Object result = message.getPayload().getValue();
      this.compareRoundtripResults(this.getTestData(), result);
    }
  }

  public void doTestBadReturnType(Transformer tran, Object src) throws Exception {
    tran.setReturnDataType(DataType.fromType(InvalidSatsuma.class));
    try {
      tran.transform(src);
      fail("Should throw exception for bad return type");
    } catch (TransformerException e) {
      // expected
    }
  }

  protected void doTestClone(Transformer original, Transformer clone) throws Exception {
    assertNotSame(original, clone);
  }

  public abstract Transformer getTransformer() throws Exception;

  public abstract Transformer getRoundTripTransformer() throws Exception;

  public abstract Object getTestData();

  public abstract Object getResultData();

  public boolean compareResults(Object expected, Object result) {
    if (expected == null && result == null) {
      return true;
    }

    if (expected == null || result == null) {
      return false;
    }

    if (expected instanceof Object[] && result instanceof Object[]) {
      return Arrays.equals((Object[]) expected, (Object[]) result);
    } else if (expected instanceof byte[] && result instanceof byte[]) {
      return Arrays.equals((byte[]) expected, (byte[]) result);
    }

    if (expected instanceof InputStream && result instanceof InputStream) {
      return IOUtils.toString((InputStream) expected).equals(IOUtils.toString((InputStream) result));
    } else if (expected instanceof InputStream) {
      expected = IOUtils.toString((InputStream) expected);
    }

    // Special case for Strings: normalize comparison arguments
    if (expected instanceof String && result instanceof String) {
      expected = this.normalizeString((String) expected);
      result = this.normalizeString((String) result);
    }

    return expected.equals(result);
  }

  public boolean compareRoundtripResults(Object expected, Object result) {
    return compareResults(expected, result);
  }

}
