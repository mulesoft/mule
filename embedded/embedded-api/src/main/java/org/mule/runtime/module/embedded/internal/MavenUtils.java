/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.embedded.internal;

import java.io.File;
import java.io.FileReader;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;

// TODO MULE-11878 - consolidate with other aether usages in mule.
public class MavenUtils {

  private MavenUtils() {}

  public static Model createModelFromPom(File pomLocation) {
    try (FileReader fileReader = new FileReader(pomLocation)) {
      MavenXpp3Reader reader = new MavenXpp3Reader();
      return reader.read(fileReader);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

  }

}
