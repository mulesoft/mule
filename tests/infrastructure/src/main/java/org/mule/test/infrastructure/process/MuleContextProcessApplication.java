/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.infrastructure.process;

import static org.apache.commons.io.FileUtils.copyFile;
import static org.mule.runtime.deployment.model.api.application.ApplicationDescriptor.DEFAULT_CONFIGURATION_RESOURCE;
import static org.mule.test.infrastructure.process.MuleContextProcessBuilder.MULE_CORE_EXTENSIONS_PROPERTY;
import static org.mule.test.infrastructure.process.MuleContextProcessBuilder.TIMEOUT_IN_SECONDS;

import org.mule.runtime.container.api.MuleCoreExtension;
import org.mule.runtime.core.api.config.MuleProperties;
import org.mule.test.infrastructure.deployment.FakeMuleServer;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class MuleContextProcessApplication {

  public static final String TEST_APPLICATION_NAME = "test-app";

  private static Socket logSocket;
  private static Socket commandSocket;

  public static void main(String args[]) throws IOException {
    try {
      long initialTime = System.currentTimeMillis();
      replaceSystemOutputWithSocketOutput();
      System.out.println("MuleContextProcessApplication attached to log port");
      System.out.println("Creating fake mule server");
      String muleHomeSystemProperty = System.getProperty(MuleProperties.MULE_HOME_DIRECTORY_PROPERTY);
      System.out.println("Using mule home: " + muleHomeSystemProperty);
      FakeMuleServer fakeMuleServer = new FakeMuleServer(muleHomeSystemProperty, retrieveConfiguredCoreExtensions());
      File appsDirectory = fakeMuleServer.getAppsDir();
      System.out.println("Creating test app directory");
      File applicationDirectory = new File(appsDirectory, TEST_APPLICATION_NAME);
      if (!applicationDirectory.exists() && !applicationDirectory.mkdir()) {
        throw new RuntimeException("Could not create test-app directory to create test application within fake mule serevr");
      }
      System.out.println("Test app directory created");
      System.out.println("Creating app config file");
      String applicationConfiguration = System.getProperty(MuleContextProcessBuilder.CONFIG_FILE_KEY);
      File applicationConfigurationFile =
          new File(MuleContextProcessApplication.class.getClassLoader().getResource(applicationConfiguration).toURI());
      if (!applicationConfigurationFile.exists()) {
        throw new RuntimeException("Could not find file for application configuration " + applicationConfiguration);
      }
      copyFile(applicationConfigurationFile, new File(applicationDirectory, DEFAULT_CONFIGURATION_RESOURCE));
      System.out.println("Test app config file created");

      ApplicationStartedDeploymentListener applicationStartedDeploymentListener = new ApplicationStartedDeploymentListener();
      fakeMuleServer.addDeploymentListener(applicationStartedDeploymentListener);
      System.out.println("Starting fake mule server");
      fakeMuleServer.start();

      System.out.println("Fake mule server started");
      applicationStartedDeploymentListener.waitUntilApplicationDeployed();
      notifyMuleContextStarted();

      while (true) {
        if (System.currentTimeMillis() - initialTime > (Integer.valueOf(System.getProperty(TIMEOUT_IN_SECONDS)) * 1000)) {
          System.exit(-1);
        }
        Thread.sleep(1000);
      }

    } catch (Throwable e) {
      System.out.println("Failure starting process: " + e.getMessage());
      e.printStackTrace();
      throw new RuntimeException(e);
    } finally {
      closeQuietly(logSocket);
    }
  }

  private static List<MuleCoreExtension> retrieveConfiguredCoreExtensions() {
    List<MuleCoreExtension> muleCoreExtensions = new ArrayList<>();
    String coreExtensionsProperty = System.getProperty(MULE_CORE_EXTENSIONS_PROPERTY);
    if (coreExtensionsProperty != null) {
      String[] coreExtensionsAsString = coreExtensionsProperty.split(",");
      for (String coreExtensionClassName : coreExtensionsAsString) {
        try {
          muleCoreExtensions
              .add((MuleCoreExtension) org.apache.commons.lang3.ClassUtils.getClass(coreExtensionClassName).newInstance());
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }
    }
    return muleCoreExtensions;
  }

  private static void notifyMuleContextStarted() throws IOException {
    System.out.println("About to send mule context started notification");
    String commandServerPort = System.getProperty(MuleContextProcessBuilder.COMMAND_PORT_PROPERTY);
    commandSocket = new Socket("localhost", Integer.valueOf(commandServerPort));
    System.out.println("Connecting command client to " + commandServerPort);
    OutputStreamWriter outputStreamWriter = new OutputStreamWriter(commandSocket.getOutputStream());
    outputStreamWriter.write(CommandServer.MULE_CONTEXT_STARTED_COMMAND + "\n");
    outputStreamWriter.flush();
    System.out.println("Mule context started notification sent");
  }

  private static void replaceSystemOutputWithSocketOutput() throws IOException {
    String tcpPortForLoggingPropertyValue = System.getProperty(MuleContextProcessBuilder.LOG_PORT_PROPERTY);
    logSocket = new Socket("localhost", Integer.valueOf(tcpPortForLoggingPropertyValue));
    System.setOut(new PrintStream(logSocket.getOutputStream(), true));
    System.setErr(new PrintStream(logSocket.getOutputStream(), true));
  }

  private static void closeQuietly(Socket socket) {
    try {
      socket.close();
    } catch (IOException e) {
      // nothing to do.
    }
  }

}
