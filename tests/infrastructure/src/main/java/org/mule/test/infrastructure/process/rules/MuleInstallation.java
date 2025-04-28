/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.infrastructure.process.rules;

import static java.lang.Boolean.parseBoolean;
import static java.lang.System.getProperty;

import static org.apache.commons.io.FileUtils.deleteQuietly;
import static org.apache.commons.io.FileUtils.moveDirectory;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.junit.rules.ExternalResource;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * This is a JUnit rule to install Mule Runtime during tests. Usage:
 * <p>
 *
 * <pre>
 * public class MuleRuntimeInstallationTest {
 *
 *   &#064;Rule
 *   public MuleInstallation installation = new MuleInstallation(&quot;/path/to/packed/distribution.zip&quot;);
 *
 *   &#064;Test
 *   public void usingMuleRuntime() throws IOException {
 *     String muleHomePath = installation.getMuleHome();
 *     MuleProcessController mule = new MuleProcessController(muleHomePath);
 *     mule.start();
 *   }
 * }
 * </pre>
 */
public class MuleInstallation extends ExternalResource {

  private static final String DISTRIBUTION_PROPERTY = "mule.distribution";
  private static final String DISTRIBUTIONS_DIR = "servers";
  private static final Path WORKING_DIRECTORY = Paths.get(getProperty("user.dir")).resolve(DISTRIBUTIONS_DIR);
  private static final String DELETE_ON_EXIT = getProperty("mule.test.deleteOnExit");
  private static final String zippedDistributionFromProperty = getProperty(DISTRIBUTION_PROPERTY);
  private static Logger logger = LoggerFactory.getLogger(MuleInstallation.class);
  private final String locationSuffix;
  protected String location;
  private final File distribution;
  private File muleHome;

  public MuleInstallation() {
    this(zippedDistributionFromProperty);
  }

  public MuleInstallation(String distribution) {
    this(zippedDistributionFromProperty, "");
  }

  public MuleInstallation(String distribution, String locationSuffix) {
    if (StringUtils.isEmpty(distribution)) {
      logger.error("You must configure the location for Mule distribution in the system property: " + DISTRIBUTION_PROPERTY);
    }
    this.distribution = new File(distribution);
    if (!this.distribution.exists()) {
      throw new IllegalArgumentException("Distribution not found: " + this.distribution);
    }

    this.locationSuffix = locationSuffix;
  }

  public String getMuleHome() {
    return muleHome.getAbsolutePath();
  }

  @Override
  public Statement apply(final Statement base, final Description description) {
    location = description.getTestClass().getSimpleName() + locationSuffix;
    return super.apply(base, description);
  }

  @Override
  protected void before() throws Throwable {
    if (distribution.isDirectory()) {
      installDistributionFromDirectory();
    } else {
      logger.info("Unpacking Mule Distribution: " + distribution);
      muleHome = new DistroUnzipper(distribution, WORKING_DIRECTORY.resolve(location).toFile()).unzip().muleHome();
    }
  }

  private void installDistributionFromDirectory() {
    muleHome = WORKING_DIRECTORY.resolve(location).toFile();
    if (muleHome.exists()) {
      return;
    }
    muleHome.mkdirs();
    Arrays.stream(distribution.listFiles())
        .forEach(file -> {
          try {
            if (file.isDirectory()) {
              FileUtils.copyDirectory(file, new File(muleHome, file.getName()));
            } else {
              FileUtils.copyFile(file, new File(muleHome, file.getName()));
            }
          } catch (IOException e) {
            e.printStackTrace();
          }

        });
    FileUtils.listFilesAndDirs(muleHome, TrueFileFilter.TRUE, TrueFileFilter.TRUE)
        .forEach(file -> DistroUnzipper.chmodRwx(file));
  }

  @Override
  protected void after() {
    File dest = new File(new File("logs"), location);
    deleteQuietly(dest);
    if (isEmpty(DELETE_ON_EXIT) || parseBoolean(DELETE_ON_EXIT)) {
      try {
        logger.info("Deleting Mule Installation");
        File logs = new File(muleHome, "logs");
        moveDirectory(logs, dest);
        deleteQuietly(muleHome);
      } catch (IOException e) {
        logger.warn("Couldn't delete directory [" + muleHome + "], delete it manually. Root exception: " + e.getMessage());
      }
    }
  }

}
