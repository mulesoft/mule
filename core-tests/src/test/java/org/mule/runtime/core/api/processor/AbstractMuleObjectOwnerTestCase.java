/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.processor;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.tck.size.SmallTest;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class AbstractMuleObjectOwnerTestCase {

  @Mock
  private TestClass mockObject1;
  @Mock
  private TestClass mockObject2;
  @Mock
  private MuleContext mockMuleContext;
  @Mock
  private FlowConstruct mockFlowConstruct;
  private AbstractMuleObjectOwner<TestClass> abstractMuleObjectOwner;

  @Before
  public void before() {
    abstractMuleObjectOwner = new AbstractMuleObjectOwner<TestClass>() {

      @Override
      protected List<TestClass> getOwnedObjects() {
        return Arrays.asList(mockObject1, mockObject2);
      }
    };
    abstractMuleObjectOwner.setMuleContext(mockMuleContext);
  }

  @Test
  public void testInitialise() throws Exception {
    abstractMuleObjectOwner.initialise();
    verify(mockObject1).initialise();
    verify(mockObject2).initialise();
    // TODO TMULE-10764 Injection should only happen once
    verify(mockObject1, times(2)).setMuleContext(mockMuleContext);
    verify(mockObject2, times(2)).setMuleContext(mockMuleContext);
  }

  @Test
  public void testDispose() throws Exception {
    abstractMuleObjectOwner.dispose();
    verify(mockObject1).dispose();
    verify(mockObject2).dispose();
  }

  @Test
  public void testStart() throws Exception {
    abstractMuleObjectOwner.start();
    verify(mockObject1).start();
    verify(mockObject2).start();
  }

  @Test
  public void testStop() throws Exception {
    abstractMuleObjectOwner.stop();
    verify(mockObject1).stop();
    verify(mockObject2).stop();
  }

  public class TestClass implements Lifecycle, MuleContextAware {

    @Override
    public void dispose() {}

    @Override
    public void initialise() throws InitialisationException {}

    @Override
    public void setMuleContext(MuleContext context) {}

    @Override
    public void start() throws MuleException {}

    @Override
    public void stop() throws MuleException {}
  }
}
