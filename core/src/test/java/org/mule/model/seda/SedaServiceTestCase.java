/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.model.seda;

import org.mule.api.MuleRuntimeException;
import org.mule.api.config.MuleProperties;
import org.mule.api.registry.RegistrationException;
import org.mule.component.DefaultJavaComponent;
import org.mule.config.QueueProfile;
import org.mule.object.PrototypeObjectFactory;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.util.queue.QueueConfiguration;
import org.mule.util.queue.QueueManager;

import com.mockobjects.dynamic.C;
import com.mockobjects.dynamic.FullConstraintMatcher;
import com.mockobjects.dynamic.Mock;

import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkEvent;
import javax.resource.spi.work.WorkException;

import junit.framework.AssertionFailedError;

public class SedaServiceTestCase extends AbstractMuleTestCase // AbstractServiceTestCase
{
    // Cannot extend AbstractServiceTestCase because of inconsistent behaviour. See
    // MULE-2843

    // protected void doSetUp() throws Exception
    // {
    // service = new SedaService();
    // service.setName("seda");
    // service.setServiceFactory(new PrototypeObjectFactory(Object.class));
    // service.setMuleContext(muleContext);
    // service.setModel(new SedaModel());
    // service.getModel().setMuleContext(muleContext);
    // service.getModel().initialise();
    // }
    //
    // protected void doTearDown() throws Exception
    // {
    // service = null;
    // }

    /**
     * ENSURE THAT: 
     * 1) The queueProfile set on the SedaService is used to configure
     * the queue that is used.
     * 2) The queue used by the SedaService has the correct
     * name.
     */
    public void testQueueConfiguration() throws Exception
    {
        boolean persistent = true;
        int capacity = 345;
        String queueName = "test.service";

        QueueManager queueManager = muleContext.getQueueManager();

        Mock mockTransactionalQueueManager = new Mock(QueueManager.class);
        mockTransactionalQueueManager.expect("toString");
        mockTransactionalQueueManager.expect("setQueueConfiguration", new FullConstraintMatcher(
            C.eq(queueName), C.eq(new QueueConfiguration(capacity, persistent))));
        mockTransactionalQueueManager.expectAndReturn("getQueueSession", queueManager.getQueueSession());

        // Replace queueManager instance with mock via registry as it cannot be set
        // once muleContext is initialized.
        muleContext.getRegistry().registerObject(MuleProperties.OBJECT_QUEUE_MANAGER,
            (QueueManager) mockTransactionalQueueManager.proxy());

        SedaService service = new SedaService();
        service.setName("test");
        service.setModel(new SedaModel());
        service.setQueueProfile(new QueueProfile(capacity, persistent));

        try
        {
            muleContext.getRegistry().registerService(service);
        }
        catch (RegistrationException e)
        {
            if (e.getCause().getCause().getCause() instanceof AssertionFailedError)
            {
                fail("Queue configuration does not match service queue profile");
            }
            else
            {
                throw e;
            }
        }

        assertEquals(queueName, service.queue.getName());

    }

    public void testSedaModelEventTimeoutDefault() throws Exception
    {
        SedaService service = new SedaService();
        service.setName("test");
        service.setComponent(new DefaultJavaComponent(new PrototypeObjectFactory(Object.class)));
        service.setModel(new SedaModel());
        service.setMuleContext(muleContext);
        service.getModel().setMuleContext(muleContext);

        service.getModel().initialise();
        service.initialise();

        assertNotNull(service.getQueueTimeout());
        assertTrue(service.getQueueTimeout().intValue() != 0);
    }

    public void testSpiWorkThrowableHandling() throws Exception
    {
        try
        {
            // getTestComponent() currently already returns a SedaService, but
            // here we are safe-guarding for any future changes
            SedaService service = new SedaService();
            service.setName("test");
            service.setComponent(new DefaultJavaComponent(new PrototypeObjectFactory(Object.class)));
            service.setModel(new SedaModel());

            service.handleWorkException(getTestWorkEvent(), "workRejected");
        }
        catch (MuleRuntimeException mrex)
        {
            assertNotNull(mrex);
            assertTrue(mrex.getCause().getClass() == Throwable.class);
            assertEquals("testThrowable", mrex.getCause().getMessage());
        }
    }

    private WorkEvent getTestWorkEvent()
    {
        return new WorkEvent(this, // source
            WorkEvent.WORK_REJECTED, getTestWork(), new WorkException(new Throwable("testThrowable")));
    }

    private Work getTestWork()
    {
        return new Work()
        {
            public void release()
            {
                // noop
            }

            public void run()
            {
                // noop
            }
        };
    }
}
