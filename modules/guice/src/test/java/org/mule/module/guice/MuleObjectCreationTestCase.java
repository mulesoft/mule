/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.guice;

import org.mule.api.transformer.Transformer;
import org.mule.api.transport.Connector;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.testmodels.fruit.RedApple;
import org.mule.tck.testmodels.mule.TestConnector;
import org.mule.transformer.types.DataTypeFactory;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.name.Names;

public class MuleObjectCreationTestCase extends AbstractMuleTestCase
{
    @Override
    protected void doSetUp() throws Exception
    {
        GuiceConfigurationBuilder cb = new GuiceConfigurationBuilder(new MuleObjectsModule());
        cb.configure(muleContext);
    }

    public void testObjectCreation() throws Exception
    {
        TestConnector c = (TestConnector) muleContext.getRegistry().lookupConnector("testConnector");
        assertNotNull(c);
        assertEquals("testConnector", c.getName());
        assertEquals("foo", c.getSomeProperty());

        c = (TestConnector) muleContext.getRegistry().lookupConnector("testConnector2");
        assertNotNull(c);
        assertEquals("testConnector2", c.getName());
        assertEquals("boundProperty", c.getSomeProperty());

        OrangetoAppleTransformer t = (OrangetoAppleTransformer)muleContext.getRegistry().lookupTransformer("testTransformer");
        assertNotNull(t);
        assertEquals(DataTypeFactory.create(RedApple.class), t.getReturnDataType());

        assertEquals(2, muleContext.getRegistry().getConnectors().size());
    }

    public void testObjectLifecycle() throws Exception
    {
        TestConnector c = (TestConnector) muleContext.getRegistry().lookupConnector("testConnector");
        assertNotNull(c);
        assertTrue(c.isInitialised());

        TestConnector c2 = (TestConnector) muleContext.getRegistry().lookupConnector("testConnector2");
        assertNotNull(c2);
        //AbstractMuleTestCase does not start the muleContext, just initialises it
        assertTrue(c2.isInitialised());

        assertFalse(c.isStarted());
        assertFalse(c2.isStarted());

        muleContext.start();

        assertTrue(c.isStarted());
        assertTrue(c2.isStarted());

        muleContext.stop();

        assertTrue(c.isStopped());
        assertTrue(c2.isStopped());

        muleContext.dispose();

        assertTrue(c.isDisposed());
        assertTrue(c2.isDisposed());
    }

    public class MuleObjectsModule extends AbstractMuleGuiceModule
    {
        @Override
        protected void doConfigure() throws Exception
        {
            //lets test injection of properties into providers
            bindConstant().annotatedWith(Names.named("connectorProperty")).to("boundProperty");
        }


        //TODO Annotated Service
//        @Provides @AnnotatedService
//        TestAnnotatedService createSerivce()
//        {
//            OrangetoAppleTransformer transformer = new OrangetoAppleTransformer();
//            transformer.setReturnDataType(new DataTypeFactory().create(RedApple.class));
//            return transformer;
//        }

        @Provides
        @Named("testTransformer")
        Transformer createNamedTransformer()
        {
            OrangetoAppleTransformer transformer = new OrangetoAppleTransformer();
            transformer.setReturnDataType(DataTypeFactory.create(RedApple.class));
            transformer.setMuleContext(muleContext);
            return transformer;
        }

        @Provides
        @Named("testConnector")
        @Singleton
        Connector createNamedConnector()
        {
            TestConnector connector = new TestConnector(muleContext);
            connector.setSomeProperty("foo");
            return connector;
        }

        @Provides
        @Named("testConnector2")
        @Singleton
        Connector createNamedConnector2(@Named("connectorProperty") String property)
        {
            TestConnector connector = new TestConnector(muleContext);
            connector.setSomeProperty(property);
            return connector;
        }
    }
}
