/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.mule.interceptors;

import org.mule.interceptors.InterceptorStack;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.umo.Invocation;
import org.mule.umo.UMODescriptor;
import org.mule.umo.UMOException;
import org.mule.umo.UMOInterceptor;
import org.mule.umo.UMOMessage;

import com.mockobjects.dynamic.Mock;

import java.util.ArrayList;
import java.util.List;

import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicInteger;

/**
 * TODO: document this class
 * 
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 * @version $Revision$
 */
public class InterceptorStackTestCase extends AbstractMuleTestCase
{

    public static class DummyInvocation extends Invocation
    {
        public DummyInvocation(UMODescriptor d, UMOMessage msg)
        {
            super(d, msg, null);
        }

        public UMOMessage execute() throws UMOException
        {
            return getMessage();
        }
    }

    public void testStack() throws Exception
    {
        final AtomicInteger c = new AtomicInteger(0);
        final UMOMessage m1 = (UMOMessage)new Mock(UMOMessage.class).proxy();
        final UMOMessage m2 = (UMOMessage)new Mock(UMOMessage.class).proxy();
        final UMOMessage m3 = (UMOMessage)new Mock(UMOMessage.class).proxy();
        final UMOMessage m4 = (UMOMessage)new Mock(UMOMessage.class).proxy();
        final UMOMessage m5 = (UMOMessage)new Mock(UMOMessage.class).proxy();
        final UMODescriptor d = (UMODescriptor)new Mock(UMODescriptor.class).proxy();

        InterceptorStack s = new InterceptorStack();
        List interceptors = new ArrayList();
        interceptors.add(new UMOInterceptor()
        {
            public UMOMessage intercept(Invocation invocation) throws UMOException
            {
                assertEquals(0, c.get());
                c.incrementAndGet();
                assertTrue(m1 == invocation.getMessage());
                invocation.setMessage(m2);
                UMOMessage msg = invocation.execute();
                assertEquals(3, c.get());
                c.incrementAndGet();
                assertTrue(m4 == msg);
                assertTrue(d == invocation.getDescriptor());
                return m5;
            }
        });
        interceptors.add(new UMOInterceptor()
        {
            public UMOMessage intercept(Invocation invocation) throws UMOException
            {
                assertEquals(1, c.get());
                c.incrementAndGet();
                assertTrue(m2 == invocation.getMessage());
                invocation.setMessage(m3);
                UMOMessage msg = invocation.execute();
                assertEquals(2, c.get());
                c.incrementAndGet();
                assertTrue(m3 == msg);
                assertTrue(d == invocation.getDescriptor());
                return m4;
            }
        });
        s.setInterceptors(interceptors);

        UMOMessage r = s.intercept(new DummyInvocation(d, m1));
        assertTrue(r == m5);
        assertEquals(4, c.get());
    }

}
