/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.transformer.datatype;

import static java.nio.charset.Charset.defaultCharset;
import static java.nio.charset.StandardCharsets.US_ASCII;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;

import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.nio.charset.UnsupportedCharsetException;

import org.junit.Test;

@SmallTest
public class DataTypeTransformersTestCase extends AbstractMuleTestCase {

  @Test
  public void validCharset() throws TransformerException {
    final StringToCharsetTransformer transformer = new StringToCharsetTransformer();
    transformer.setArtifactEncoding(() -> defaultCharset());

    assertThat(transformer.transform(US_ASCII.name()), is(US_ASCII));
  }

  @Test
  public void invalidCharset() throws TransformerException {
    final StringToCharsetTransformer transformer = new StringToCharsetTransformer();
    transformer.setArtifactEncoding(() -> defaultCharset());

    var thrown = assertThrows(TransformerException.class, () -> transformer.transform("invalid"));
    assertThat(thrown.getCause(), instanceOf(UnsupportedCharsetException.class));
  }

  @Test
  public void validMediaType() throws TransformerException {
    final StringToMediaTypeTransformer transformer = new StringToMediaTypeTransformer();
    transformer.setArtifactEncoding(() -> defaultCharset());

    final MediaType transformed = (MediaType) transformer.transform("text/plain");
    assertThat(transformed.getPrimaryType(), is("text"));
    assertThat(transformed.getSubType(), is("plain"));
    assertThat(transformed.getCharset().isPresent(), is(false));
  }

  @Test
  public void validMediaTypeWithCharset() throws TransformerException {
    final StringToMediaTypeTransformer transformer = new StringToMediaTypeTransformer();
    transformer.setArtifactEncoding(() -> defaultCharset());

    final MediaType transformed = (MediaType) transformer.transform("text/plain;charset=" + US_ASCII.name());
    assertThat(transformed.getPrimaryType(), is("text"));
    assertThat(transformed.getSubType(), is("plain"));
    assertThat(transformed.getCharset().get(), is(US_ASCII));
  }

  @Test
  public void invalidMediaType() throws TransformerException {
    final StringToMediaTypeTransformer transformer = new StringToMediaTypeTransformer();
    transformer.setArtifactEncoding(() -> defaultCharset());

    var thrown = assertThrows(TransformerException.class, () -> transformer.transform("invalid"));
    assertThat(thrown.getCause(), instanceOf(IllegalArgumentException.class));
  }
}
