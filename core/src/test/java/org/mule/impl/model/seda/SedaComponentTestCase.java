/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.impl.model.seda;

import org.mule.MuleRuntimeException;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.util.object.SimpleObjectFactory;

import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkEvent;
import javax.resource.spi.work.WorkException;

public class SedaComponentTestCase extends AbstractMuleTestCase
{

    public void testSedaModelEventTimeoutDefault() throws Exception
    {
        SedaComponent component = new SedaComponent();
        component.setName("test");
        component.setServiceFactory(new SimpleObjectFactory(Object.class));
        component.setModel(new SedaModel());
        component.setManagementContext(managementContext);
        component.getModel().setManagementContext(managementContext);

        component.getModel().initialise();
        component.initialise();

        assertNotNull(component.getQueueTimeout());
        assertTrue(component.getQueueTimeout().intValue() != 0);
    }
    
    public void testSpiWorkThrowableHandling() throws Exception
    {
        try
        {
            // getTestComponent() currently already returns a SedaComponent, but
            // here we are safe-guarding for any future changes
            SedaComponent component = new SedaComponent();
            component.setName("test");
            component.setServiceFactory(new SimpleObjectFactory(Object.class));
            component.setModel(new SedaModel());

            component.handleWorkException(getTestWorkEvent(), "workRejected");
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
