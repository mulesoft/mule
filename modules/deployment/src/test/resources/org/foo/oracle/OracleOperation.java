/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.foo.oracle;

import static java.lang.Thread.sleep;
import static org.mockito.Mockito.mock;
import static org.slf4j.LoggerFactory.getLogger;
import static org.mule.runtime.extension.api.annotation.param.MediaType.TEXT_PLAIN;
import static java.lang.ClassLoader.getSystemClassLoader;
import static java.sql.DriverManager.getConnection;

import java.sql.Driver;
import java.sql.DriverManager;
import java.util.Timer;
import java.util.TimerTask;
import java.lang.Class;
import java.lang.Thread;
import java.sql.Connection;

import oracle.jdbc.OracleDriver;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.test.runner.ArtifactClassLoaderRunnerConfig;

public class OracleOperation {

    public OracleOperation() {}

    @MediaType(value = TEXT_PLAIN, strict = false)
    public String printMessage(@Config OracleExtension config) {
        return "Test plugin extension says hello!";
    }

    @MediaType(value = TEXT_PLAIN, strict = false)
    public String connect(@Config OracleExtension config) {
        try {
            //Register test oracle driver to trick JdbcResourceReleaser
            //into releasing resources.
            Class.forName("oracle.jdbc.OracleDriver");
            DriverManager.registerDriver(new oracle.jdbc.OracleDriver());
            //Mimic oracle driver behaviour,
            //spawn timer threads.
            Timer timerThread = new Timer();
            TimerTask task = new TimerTask() {
                @Override
                public void run() {
                    getLogger(OracleOperation.class).info("This is timer task reporting for duty.");
                }
            };

            timerThread.schedule(task, 10, 1000);

            return "Connection success!";

        } catch (Exception e) {
            return "Exception ocurred while attempting to load test driver: " + e.getMessage();
        }
    }
}