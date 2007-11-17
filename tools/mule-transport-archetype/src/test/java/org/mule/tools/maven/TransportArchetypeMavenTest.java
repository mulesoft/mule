/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tools.maven;

import org.mule.util.FileUtils;

import java.io.File;
import java.util.Collections;

import org.apache.maven.cli.ConsoleDownloadMonitor;
import org.apache.maven.embedder.MavenEmbedder;
import org.apache.maven.embedder.MavenEmbedderConsoleLogger;
import org.apache.maven.embedder.PlexusLoggerAdapter;
import org.apache.maven.monitor.event.DefaultEventMonitor;
import org.apache.maven.monitor.event.EventMonitor;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.PlexusTestCase;

/**
 * This is the documented way to embed maven for integration testing... but its not
 * working on the maven side.
 * The idea is that within the test,  we try to create
 * a new project from the archetype and compile it (the most we can probably do is
 * compile to make sure it builds with the correct version of Mule)
 * <p/>
 */
public class TransportArchetypeMavenTest extends PlexusTestCase
{
    /** @throws Exception  */
    public void testDefaultProject()
            throws Exception
    {
        System.out.println("Base Dir is: " + getBasedir());

        MavenEmbedder maven = new MavenEmbedder();
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        maven.setClassLoader(classLoader);
        maven.setLogger(new MavenEmbedderConsoleLogger());
        maven.setOffline(true);
        maven.setLocalRepositoryDirectory(getTestFile("target/local-repo"));

        maven.start();


        File itbasedir = new File(getBasedir(), "tools/mule-transport-archetype");
        MavenProject pom =
                maven.readProjectWithDependencies(new File(itbasedir, "pom.xml"));

        EventMonitor eventMonitor =
                new DefaultEventMonitor(
                        new PlexusLoggerAdapter(
                                new MavenEmbedderConsoleLogger()));

        //mvn mule-transport-archetype:create -DtransportId=test2 -DmuleVersion=2.0-M2-SNAPSHOT -Dinteractive=false

        System.setProperty("transportId", "xxx");
        System.setProperty("muleVersion", pom.getVersion());
        System.setProperty("interactive", "false");

        //
        maven.execute(pom,
                Collections.singletonList(
                        "org.mule.tools:mule-transport-archetype:" + pom.getVersion() + ":create"),
                eventMonitor,
                new ConsoleDownloadMonitor(),
                null,
                itbasedir);

        maven.stop();
    }

    //@java.lang.Override
    protected void tearDown() throws Exception
    {
        FileUtils.deleteTree(new File("xxx"));
        super.tearDown();
    }
}
