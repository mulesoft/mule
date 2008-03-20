/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.file;

import org.mule.api.MuleEventContext;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.service.Service;
import org.mule.api.transformer.TransformerException;
import org.mule.component.DefaultJavaComponent;
import org.mule.endpoint.EndpointURIEndpointBuilder;
import org.mule.endpoint.URIBuilder;
import org.mule.model.seda.SedaService;
import org.mule.object.SingletonObjectFactory;
import org.mule.tck.functional.EventCallback;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.transformer.AbstractMessageAwareTransformer;
import org.mule.util.concurrent.Latch;

import java.io.File;

import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;

public class FileReceiverMoveDeleteTestCase extends AbstractFileMoveDeleteTestCase
{

    public void testMoveAndDeleteStreaming() throws Exception
    {
        File inFile = initForRequest();

        File moveToDir = configureConnector(inFile, true, true, true);

        assertRecevied(configureService(inFile, true));

        assertFiles(inFile, moveToDir, true, true);
    }

    public void testMoveOnlyStreaming() throws Exception
    {
        File inFile = initForRequest();

        File moveToDir = configureConnector(inFile, true, true, false);

        assertRecevied(configureService(inFile, true));

        assertFiles(inFile, moveToDir, true, false);
    }

    public void testDeleteOnlyStreaming() throws Exception
    {
        File inFile = initForRequest();

        File moveToDir = configureConnector(inFile, true, false, true);

        assertRecevied(configureService(inFile, true));

        assertFiles(inFile, moveToDir, false, true);
    }

    public void testNoMoveNoDeleteStreaming() throws Exception
    {
        File inFile = initForRequest();

        File moveToDir = configureConnector(inFile, true, false, false);

        assertRecevied(configureService(inFile, true));

        assertFiles(inFile, moveToDir, false, false);
    }

    public void testMoveAndDelete() throws Exception
    {
        File inFile = initForRequest();

        File moveToDir = configureConnector(inFile, false, true, true);
        assertRecevied(configureService(inFile, false));

        assertFiles(inFile, moveToDir, true, true);
    }

    public void testMoveOnly() throws Exception
    {
        File inFile = initForRequest();

        File moveToDir = configureConnector(inFile, false, true, false);

        assertRecevied(configureService(inFile, false));

        assertFiles(inFile, moveToDir, true, false);
    }

    public void testDeleteOnly() throws Exception
    {
        File inFile = initForRequest();

        File moveToDir = configureConnector(inFile, false, false, true);

        assertRecevied(configureService(inFile, false));

        assertFiles(inFile, moveToDir, false, true);
    }

    public void testNoMoveNoDelete() throws Exception
    {
        File inFile = initForRequest();

        File moveToDir = configureConnector(inFile, false, false, false);

        assertRecevied(configureService(inFile, false));

        assertFiles(inFile, moveToDir, false, false);
    }

    protected Latch configureService(File inFile, boolean streaming) throws Exception
    {

        Service s = new SedaService();
        s.setName("moveDeleteBridgeService");
        String url = fileToUrl(inFile.getParentFile()) + "?connector=moveDeleteConnector";
        org.mule.api.transformer.Transformer transformer;
        if (streaming)
        {
            transformer = new FileMessageAdaptorAssertingTransformer(FileMessageAdapter.class);
        }
        else
        {
            transformer = new FileMessageAdaptorAssertingTransformer(FileContentsMessageAdapter.class);

        }
        EndpointBuilder endpointBuilder = new EndpointURIEndpointBuilder(new URIBuilder(url), muleContext);
        endpointBuilder.addTransformer(transformer);
        endpointBuilder.setSynchronous(true);
        s.getInboundRouter().addEndpoint(
            muleContext.getRegistry().lookupEndpointFactory().getInboundEndpoint(endpointBuilder));
        final Latch latch = new Latch();
        FunctionalTestComponent component = new FunctionalTestComponent();
        component.setEventCallback(new EventCallback()
        {
            public void eventReceived(final MuleEventContext context, final Object message) throws Exception
            {
                assertEquals(1, latch.getCount());
                latch.countDown();
                assertEquals(TEST_MESSAGE, context.transformMessageToString());
            }
        });

        component.initialise();
        s.setComponent(new DefaultJavaComponent(new SingletonObjectFactory(component)));
        s.setModel(muleContext.getRegistry().lookupSystemModel());
        muleContext.getRegistry().registerService(s);
        s.start();
        return latch;
    }

    protected void assertRecevied(Latch latch) throws Exception
    {
        assertTrue(latch != null && latch.await(2000, TimeUnit.MILLISECONDS));
    }

    private class FileMessageAdaptorAssertingTransformer extends AbstractMessageAwareTransformer
    {
        private Class expectedMessageAdaptor;

        public FileMessageAdaptorAssertingTransformer(Class expectedMessageAdaptor)
        {
            this.expectedMessageAdaptor = expectedMessageAdaptor;
        }

        public Object transform(MuleMessage message, String outputEncoding) throws TransformerException
        {
            assertEquals(expectedMessageAdaptor, message.getAdapter().getClass());

            // If we are streaming, copy/delete shouldn't have happened yet
            if (expectedMessageAdaptor.equals(FileMessageAdapter.class))
            {
                assertFilesUntouched(((FileMessageAdapter) message.getAdapter()).file);
            }
            return message;
        }
    }

}
