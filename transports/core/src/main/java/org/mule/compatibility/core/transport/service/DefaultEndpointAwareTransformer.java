/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core.transport.service;

import org.mule.compatibility.core.api.endpoint.ImmutableEndpoint;
import org.mule.compatibility.core.api.transformer.EndpointAwareTransformer;
import org.mule.compatibility.core.endpoint.EndpointAware;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.transformer.Converter;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.transformer.CompositeConverter;
import org.mule.runtime.core.transformer.TransformerChain;

import java.nio.charset.Charset;
import java.util.List;

public class DefaultEndpointAwareTransformer implements EndpointAwareTransformer {

  protected final Transformer transformer;
  private ImmutableEndpoint endpoint;
  private Charset defaultEncoding;

  public DefaultEndpointAwareTransformer(Transformer transformer, Charset defaultEncoding) {
    this.transformer = transformer;
    this.defaultEncoding = defaultEncoding;
  }

  @Override
  public void setEndpoint(ImmutableEndpoint ep) {
    this.endpoint = ep;
    if (transformer instanceof CompositeConverter) {
      for (Converter converter : ((CompositeConverter) transformer).getConverters()) {
        if (converter instanceof EndpointAware) {
          ((EndpointAware) converter).setEndpoint(ep);
        }
      }
    } else if (transformer instanceof TransformerChain) {
      for (Transformer innerTransformer : ((TransformerChain) transformer).getTransformers()) {
        if (innerTransformer instanceof EndpointAware) {
          ((EndpointAware) innerTransformer).setEndpoint(ep);
        }
      }
    } else if (transformer instanceof EndpointAware) {
      ((EndpointAware) transformer).setEndpoint(ep);
    }
  }

  @Override
  public ImmutableEndpoint getEndpoint() {
    if (transformer instanceof CompositeConverter) {
      if (!((CompositeConverter) transformer).getConverters().isEmpty()) {
        return ((EndpointAware) ((CompositeConverter) transformer).getConverters().iterator().next()).getEndpoint();
      } else {
        return null;
      }
    } else {
      return this.endpoint;
    }
  }

  @Override
  public boolean isSourceDataTypeSupported(DataType dataType) {
    return transformer.isSourceDataTypeSupported(dataType);
  }

  @Override
  public List<DataType> getSourceDataTypes() {
    return transformer.getSourceDataTypes();
  }

  @Override
  public boolean isAcceptNull() {
    return transformer.isAcceptNull();
  }

  @Override
  public boolean isIgnoreBadInput() {
    return transformer.isIgnoreBadInput();
  }

  @Override
  public Object transform(Object src) throws TransformerException {
    return transformer.transform(src, resolveEncoding(src));
  }

  protected Charset resolveEncoding(Object src) {
    if (src instanceof MuleMessage) {
      return ((MuleMessage) src).getDataType().getMediaType().getCharset().orElse(getDefaultEncoding());
    } else {
      return getDefaultEncoding();
    }
  }

  private Charset getDefaultEncoding() {
    Charset enc = null;
    if (endpoint != null) {
      enc = endpoint.getEncoding();
    } else if (enc == null) {
      enc = defaultEncoding;
    }
    return enc;
  }

  @Override
  public Object transform(Object src, Charset encoding) throws TransformerException {
    return transformer.transform(src, encoding);
  }

  @Override
  public void setReturnDataType(DataType type) {
    transformer.setReturnDataType(type);
  }

  @Override
  public DataType getReturnDataType() {
    return transformer.getReturnDataType();
  }

  @Override
  public MuleEvent process(MuleEvent event) throws MuleException {
    return transformer.process(event);
  }

  @Override
  public void initialise() throws InitialisationException {
    transformer.initialise();
  }

  @Override
  public void dispose() {
    transformer.dispose();
  }

  @Override
  public void setName(String name) {
    transformer.setName(name);
  }

  @Override
  public String getName() {
    return transformer.getName();
  }

  @Override
  public void setMuleContext(MuleContext context) {
    transformer.setMuleContext(context);
  }

  public Transformer getTransformer() {
    return transformer;
  }
}
