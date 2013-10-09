/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.processor;

import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.construct.FlowConstructAware;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Lifecycle;
import org.mule.tck.size.SmallTest;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class AbstractMuleObjectOwnerTest {

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
    public void before()
    {
        abstractMuleObjectOwner = new AbstractMuleObjectOwner<TestClass>()
        {
            @Override
            protected List<TestClass> getOwnedObjects() {
                return Arrays.asList(mockObject1,mockObject2);
            }
        };
        abstractMuleObjectOwner.setFlowConstruct(mockFlowConstruct);
        abstractMuleObjectOwner.setMuleContext(mockMuleContext);
    }

    @Test
    public void testInitialise() throws Exception {
        abstractMuleObjectOwner.initialise();
        verify(mockObject1).initialise();
        verify(mockObject2).initialise();
        verify(mockObject1).setMuleContext(mockMuleContext);
        verify(mockObject2).setMuleContext(mockMuleContext);
        verify(mockObject1).setFlowConstruct(mockFlowConstruct);
        verify(mockObject2).setFlowConstruct(mockFlowConstruct);
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

    public class TestClass implements Lifecycle, MuleContextAware, FlowConstructAware
    {
        @Override
        public void dispose() {
        }

        @Override
        public void setFlowConstruct(FlowConstruct flowConstruct) {
        }

        @Override
        public void initialise() throws InitialisationException {
        }

        @Override
        public void setMuleContext(MuleContext context) {
        }

        @Override
        public void start() throws MuleException {
        }

        @Override
        public void stop() throws MuleException {
        }
    }
}
