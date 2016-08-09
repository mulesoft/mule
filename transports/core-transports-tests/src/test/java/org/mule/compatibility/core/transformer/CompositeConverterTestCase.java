/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core.transformer;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.withSettings;

import org.mule.compatibility.core.api.endpoint.ImmutableEndpoint;
import org.mule.compatibility.core.api.transformer.EndpointAwareTransformer;
import org.mule.compatibility.core.endpoint.EndpointAware;
import org.mule.compatibility.core.transport.service.DefaultEndpointAwareTransformer;
import org.mule.runtime.core.api.transformer.Converter;
import org.mule.runtime.core.transformer.CompositeConverter;
import org.mule.tck.size.SmallTest;

import org.junit.Test;

@SmallTest
public class CompositeConverterTestCase {

  private Converter mockConverterA = mock(Converter.class, withSettings().extraInterfaces(EndpointAware.class));
  private Converter mockConverterB = mock(Converter.class, withSettings().extraInterfaces(EndpointAware.class));

  @Test
  public void getEndpoint() {
    ImmutableEndpoint mockImmutableEndpoint = mock(ImmutableEndpoint.class);
    doReturn(mockImmutableEndpoint).when((EndpointAware) mockConverterA).getEndpoint();
    doReturn(mockImmutableEndpoint).when((EndpointAware) mockConverterB).getEndpoint();
    EndpointAwareTransformer compositeConverter =
        new DefaultEndpointAwareTransformer(new CompositeConverter(mockConverterA, mockConverterB), null);

    assertEquals(mockImmutableEndpoint, ((EndpointAware) compositeConverter).getEndpoint());
  }

  @Test
  public void setEndpoint() {
    ImmutableEndpoint mockImmutableEndpoint = mock(ImmutableEndpoint.class);
    EndpointAwareTransformer compositeConverter =
        new DefaultEndpointAwareTransformer(new CompositeConverter(mockConverterA, mockConverterB), null);

    ((EndpointAware) compositeConverter).setEndpoint(mockImmutableEndpoint);

    verify((EndpointAware) mockConverterA, atLeastOnce()).setEndpoint(mockImmutableEndpoint);
    verify((EndpointAware) mockConverterB, atLeastOnce()).setEndpoint(mockImmutableEndpoint);
  }

}
