/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.transformer.simple;

import static java.lang.String.format;
import static java.nio.charset.Charset.defaultCharset;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;

import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Before;
import org.junit.Test;

@SmallTest
public class StringToEnumTestCase extends AbstractMuleTestCase {

  enum TestEnum {
    A, B
  }

  private StringToEnum transformer;

  @Before
  public void setUp() {
    transformer = new StringToEnum(TestEnum.class);
    transformer.setArtifactEncoding(() -> defaultCharset());
  }

  @Test
  public void transform() throws Exception {
    for (TestEnum value : TestEnum.values()) {
      assertThat(transformer.transform(value.name()), is(value));
    }
  }

  @Test
  public void illegalValue() throws Exception {
    var thrown = assertThrows(TransformerException.class, () -> transformer.transform("NOT ENUM VALUE"));
    assertThat(thrown.getCause(), instanceOf(IllegalArgumentException.class));
  }

  @Test
  public void nullClass() {
    assertThrows(IllegalArgumentException.class, () -> new StringToEnum(null));
  }

  @Test
  public void name() {
    String name = format("StringTo%sTransformer", TestEnum.class.getName());
    assertThat(transformer.getName(), is(name));
  }

}
