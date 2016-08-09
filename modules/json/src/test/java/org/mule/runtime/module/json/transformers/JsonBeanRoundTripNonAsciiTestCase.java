/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.json.transformers;

import static org.junit.Assert.fail;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.config.i18n.LocaleMessageHandler;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Orange;

import java.io.UnsupportedEncodingException;
import java.util.Locale;

import org.junit.Ignore;

@Ignore("See MULE-9307")
public class JsonBeanRoundTripNonAsciiTestCase extends JsonBeanRoundTripTestCase {

  private static final String ENCODING = "Windows-31J";

  private final String jsonString;
  private final FruitCollection jsonObject;

  public JsonBeanRoundTripNonAsciiTestCase() {
    jsonString = "{\"apple\":{\"bitten\":true,\"washed\":false},\"orange\":{\"brand\":\"" + getBrandOfOrange(Locale.JAPAN)
        + "\",\"segments\":8,\"radius\":3.45,\"listProperties\":null,\"mapProperties\":null,\"arrayProperties\":null}}";

    jsonObject = new FruitCollection(new Apple(true), null, new Orange(8, 3.45, getBrandOfOrange(Locale.JAPAN)));
  }

  @Override
  public void testTransform() throws Exception {
    // This test fails under Java 1.6 on Windows, because the Java fields are serialized in a different order.
    String javaVersion = System.getProperty("java.specification.version", "<None>");
    String osName = System.getProperty("os.name", "<None>");
    if (javaVersion.equals("1.6") && osName.startsWith("Windows")) {
      return;
    }
    super.testTransform();
  }

  @Override
  public Transformer getRoundTripTransformer() throws Exception {
    Transformer trans = super.getRoundTripTransformer();
    return trans;
  }

  @Override
  public Transformer getTransformer() throws Exception {
    Transformer trans = super.getTransformer();
    trans.setReturnDataType(DataType.BYTE_ARRAY);
    return trans;
  }

  @Override
  public Object getTestData() {
    return jsonObject;
  }

  @Override
  public Object getResultData() {
    try {
      return jsonString.getBytes(ENCODING);
    } catch (UnsupportedEncodingException e) {
      fail();
      return null;
    }
  }

  private String getBrandOfOrange(Locale locale) {
    return LocaleMessageHandler.getString("test-data", locale, "JsonBeanRoundTripNonAsciiTestCase.getBrandOfOrange",
                                          new Object[] {});
  }
}
