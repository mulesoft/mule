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
import org.mule.api.service.Service;
import org.mule.model.seda.SedaService;
import org.mule.tck.functional.EventCallback;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.util.concurrent.Latch;
import org.mule.util.object.SingletonObjectFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;

public class FileInboundEndpointMoveDeleteTestCase extends AbstractFileFunctionalTestCase
{

    public void testMoveAndDeleteStreaming() throws Exception
    {
        File inFile = initForRequest();

        File moveToDir = configureConnector(inFile, true, true, true);

        assertRecevied(configureService(inFile));

        assertFiles(inFile, moveToDir, true, true);
    }

    public void testMoveOnlyStreaming() throws Exception
    {
        File inFile = initForRequest();

        File moveToDir = configureConnector(inFile, true, true, false);

        assertRecevied(configureService(inFile));

        assertFiles(inFile, moveToDir, true, false);
    }

    public void testDeleteOnlyStreaming() throws Exception
    {
        File inFile = initForRequest();

        File moveToDir = configureConnector(inFile, true, false, true);

        assertRecevied(configureService(inFile));

        assertFiles(inFile, moveToDir, false, true);
    }

    public void testNoMoveNoDeleteStreaming() throws Exception
    {
        File inFile = initForRequest();

        File moveToDir = configureConnector(inFile, true, false, false);

        assertRecevied(configureService(inFile));

        assertFiles(inFile, moveToDir, false, false);
    }

    public void testMoveAndDelete() throws Exception
    {
        File inFile = initForRequest();

        File moveToDir = configureConnector(inFile, false, true, true);
        assertRecevied(configureService(inFile));

        assertFiles(inFile, moveToDir, true, true);
    }

    public void testMoveOnly() throws Exception
    {
        File inFile = initForRequest();

        File moveToDir = configureConnector(inFile, false, true, false);

        assertRecevied(configureService(inFile));

        assertFiles(inFile, moveToDir, true, false);
    }

    public void testDeleteOnly() throws Exception
    {
        File inFile = initForRequest();

        File moveToDir = configureConnector(inFile, false, false, true);

        assertRecevied(configureService(inFile));

        assertFiles(inFile, moveToDir, false, true);
    }

    public void testNoMoveNoDelete() throws Exception
    {
        File inFile = initForRequest();

        File moveToDir = configureConnector(inFile, false, false, false);

        assertRecevied(configureService(inFile));

        assertFiles(inFile, moveToDir, false, false);
    }

    protected File configureConnector(File inFile, boolean stream, boolean move, boolean delete)
        throws Exception
    {
        FileConnector fc = new FileConnector();
        fc.setName("moveDeleteConnector");
        File moveToDir = new File(inFile.getParent() + "/moveto/");
        moveToDir.mkdir();
        muleContext.getRegistry().registerConnector(fc);
        if (move)
        {
            fc.setMoveToDirectory(moveToDir.getPath());
        }
        fc.setAutoDelete(delete);
        fc.setStreaming(stream);
        return moveToDir;
    }

    protected Latch configureService(File inFile) throws Exception
    {

        Service s = new SedaService();
        s.setName("moveDeleteBridgeService");
        String url = fileToUrl(inFile.getParentFile()) + "?connector=moveDeleteConnector";
        s.getInboundRouter().addEndpoint(
            muleContext.getRegistry().lookupEndpointFactory().getInboundEndpoint(url));
        final Latch latch = new Latch();
        FunctionalTestComponent component = new FunctionalTestComponent();
        component.setEventCallback(new EventCallback()
        {
            public void eventReceived(final MuleEventContext context, final Object message) throws Exception
            {
                System.out.println("COUTNTING DOWN LATCH: " + latch);
                System.out.println(latch.getCount() + "-> ");
                assertEquals(1, latch.getCount());
                latch.countDown();
                System.out.println(latch.getCount());
                assertEquals(TEST_MESSAGE, context.transformMessageToString());
            }
        });

        component.initialise();
        s.setServiceFactory(new SingletonObjectFactory(component));
        s.setModel(muleContext.getRegistry().lookupSystemModel());
        muleContext.getRegistry().registerService(s);
        s.start();
        return latch;
    }

    protected void assertRecevied(Latch latch) throws Exception
    {
        assertTrue(latch != null && latch.await(2000, TimeUnit.MILLISECONDS));
    }

    protected void assertFiles(File inFile, File moveToDir, boolean move, boolean delete) throws Exception
    {
        waitForFileSystem();
        assertTrue(inFile.exists() == !delete);

        if (!delete)
        {
            assertEquals(TEST_MESSAGE, new BufferedReader(new FileReader(inFile)).readLine());
        }

        File movedFile = new File(moveToDir.getPath() + inFile.getName());
        assertTrue(movedFile.exists() == move);

        if (move)
        {
            assertEquals(TEST_MESSAGE, new BufferedReader(new FileReader(movedFile)).readLine());
        }
    }

}
