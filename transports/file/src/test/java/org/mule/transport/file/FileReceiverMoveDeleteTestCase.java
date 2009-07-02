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
import org.mule.transformer.NoActionTransformer;
import org.mule.util.concurrent.Latch;

import java.io.File;

import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;

public class FileReceiverMoveDeleteTestCase extends AbstractFileMoveDeleteTestCase
{

    public void testMoveAndDeleteStreaming() throws Exception
    {
        File inFile = initForRequest();

        File moveToDir = configureConnector(inFile, true, true, true, null);

        assertRecevied(configureService(inFile, true, false));

        assertFiles(inFile, moveToDir, true, true);
    }

    public void testMoveOnlyStreaming() throws Exception
    {
        File inFile = initForRequest();

        File moveToDir = configureConnector(inFile, true, true, false, null);

        assertRecevied(configureService(inFile, true, false));

        assertFiles(inFile, moveToDir, true, false);
    }

    public void testDeleteOnlyStreaming() throws Exception
    {
        File inFile = initForRequest();

        File moveToDir = configureConnector(inFile, true, false, true, null);

        assertRecevied(configureService(inFile, true, false));

        assertFiles(inFile, moveToDir, false, true);
    }

    public void testNoMoveNoDeleteStreaming() throws Exception
    {
        File inFile = initForRequest();

        File moveToDir = configureConnector(inFile, true, false, false, null);

        assertRecevied(configureService(inFile, true, false));

        assertFiles(inFile, moveToDir, false, false);
    }

    public void testMoveAndDelete() throws Exception
    {
        File inFile = initForRequest();

        File moveToDir = configureConnector(inFile, false, true, true, null);
        assertRecevied(configureService(inFile, false, false));

        assertFiles(inFile, moveToDir, true, true);
    }

    public void testMoveOnly() throws Exception
    {
        File inFile = initForRequest();

        File moveToDir = configureConnector(inFile, false, true, false, null);

        assertRecevied(configureService(inFile, false, false));

        assertFiles(inFile, moveToDir, true, false);
    }

    public void testDeleteOnly() throws Exception
    {
        File inFile = initForRequest();

        File moveToDir = configureConnector(inFile, false, false, true, null);

        assertRecevied(configureService(inFile, false, false));

        assertFiles(inFile, moveToDir, false, true);
    }

    public void testNoMoveNoDelete() throws Exception
    {
        File inFile = initForRequest();

        File moveToDir = configureConnector(inFile, false, false, false, null);

        assertRecevied(configureService(inFile, false, false));

        assertFiles(inFile, moveToDir, false, false);
    }

    public void testMoveAndDeleteFilePayload() throws Exception
    {
        File inFile = initForRequest();

        File moveToDir = configureConnector(inFile, false, true, false, FileMessageAdapter.class);

        // TODO MULE-3198
        // assertRecevied(configureService(inFile, false, true));
        // assertFiles(inFile, moveToDir, true, true);
    }

    public void testMoveOnlyFilePayload() throws Exception
    {
        File inFile = initForRequest();

        File moveToDir = configureConnector(inFile, false, true, false, FileMessageAdapter.class);

        // TODO MULE-3198
        // assertRecevied(configureService(inFile, false, true));
        // assertFiles(inFile, moveToDir, true, false);
    }

    public void testDeleteOnlyFilePayload() throws Exception
    {
        File inFile = initForRequest();

        File moveToDir = configureConnector(inFile, false, false, true, FileMessageAdapter.class);

        // TODO MULE-3198
        // assertRecevied(configureService(inFile, false, true));
        // assertFiles(inFile, moveToDir, false, true);
    }

    public void testNoMoveNoDeleteFilePayload() throws Exception
    {
        File inFile = initForRequest();

        File moveToDir = configureConnector(inFile, false, false, false, FileMessageAdapter.class);

        // TODO MULE-3198
        // assertRecevied(configureService(inFile, false, true));

        assertFiles(inFile, moveToDir, false, false);
    }

    protected Latch configureService(File inFile, boolean streaming, boolean filePayload) throws Exception
    {

        Service service = new SedaService();
        service.setMuleContext(muleContext);
        service.setName("moveDeleteBridgeService");
        String url = fileToUrl(inFile.getParentFile()) + "?connector=moveDeleteConnector";
        org.mule.api.transformer.Transformer transformer = null;
        if (streaming)
        {
            if (filePayload)
            {
                fail("Inconsistant test case: streaming and file payload are not compatible");
            }
            else
            {
                transformer = new FileMessageAdaptorAssertingTransformer(FileMessageAdapter.class,
                    ReceiverFileInputStream.class);
            }
        }
        else
        {
            if (filePayload)
            {
                transformer = new FileMessageAdaptorAssertingTransformer(FileMessageAdapter.class, File.class);
            }
            else
            {
                transformer = new FileMessageAdaptorAssertingTransformer(FileContentsMessageAdapter.class, byte[].class);
            }
        }
        EndpointBuilder endpointBuilder = new EndpointURIEndpointBuilder(new URIBuilder(url, muleContext), muleContext);
        endpointBuilder.addTransformer(transformer);
        if (filePayload)
        {
            endpointBuilder.addTransformer(new NoActionTransformer());
        }
        endpointBuilder.setSynchronous(true);
        service.getInboundRouter().addEndpoint(
            muleContext.getRegistry().lookupEndpointFactory().getInboundEndpoint(endpointBuilder));
        final Latch latch = new Latch();
        FunctionalTestComponent testComponent = new FunctionalTestComponent();
        testComponent.setEventCallback(new EventCallback()
        {
            public void eventReceived(final MuleEventContext context, final Object message) throws Exception
            {
                assertEquals(1, latch.getCount());
                latch.countDown();
                assertEquals(TEST_MESSAGE, context.transformMessageToString());
            }
        });

        testComponent.initialise();
        final DefaultJavaComponent component = new DefaultJavaComponent(new SingletonObjectFactory(testComponent));
        component.setMuleContext(muleContext);
        service.setComponent(component);
        service.setMuleContext(muleContext);
        service.setModel(muleContext.getRegistry().lookupSystemModel());
        muleContext.getRegistry().registerService(service);
        service.start();
        return latch;
    }

    protected void assertRecevied(Latch latch) throws Exception
    {
        assertTrue(latch != null && latch.await(2000, TimeUnit.MILLISECONDS));
    }

    private class FileMessageAdaptorAssertingTransformer extends AbstractMessageAwareTransformer
    {
        private Class expectedMessageAdaptor;
        private Class expectedPayload;

        public FileMessageAdaptorAssertingTransformer(Class expectedMessageAdaptor, Class expectedPayload)
        {
            this.expectedMessageAdaptor = expectedMessageAdaptor;
            this.expectedPayload = expectedPayload;
        }

        public Object transform(MuleMessage message, String outputEncoding) throws TransformerException
        {
            assertEquals(expectedMessageAdaptor, message.getAdapter().getClass());
            assertEquals(expectedPayload, message.getAdapter().getPayload().getClass());

            // If we are streaming, copy/delete shouldn't have happened yet
            if (expectedMessageAdaptor.equals(FileMessageAdapter.class))
            {
                assertFilesUntouched(((FileMessageAdapter) message.getAdapter()).file);
            }
            return message;
        }
    }

}
