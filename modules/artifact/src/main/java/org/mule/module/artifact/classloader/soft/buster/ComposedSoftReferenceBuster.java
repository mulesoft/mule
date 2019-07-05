/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.artifact.classloader.soft.buster;

import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;

public class ComposedSoftReferenceBuster implements SoftReferenceBuster {

  private static WeakHashMap<ClassLoader, List<SoftReferenceBuster>> busters =
      new WeakHashMap<ClassLoader, List<SoftReferenceBuster>>();
  private static ComposedSoftReferenceBuster INSTANCE;

  @Override
  public void bustSoftReferences(ClassLoader loader) {
    busters.values().stream().forEach(busterList -> busterList.stream().forEach(buster -> buster.bustSoftReferences(loader)));
    busters.remove(loader);
  }

  public static void registerBuster(ClassLoader classLoader, SoftReferenceBuster buster) {
    List<SoftReferenceBuster> bustersList;
    if (!busters.containsKey(classLoader)) {
      bustersList = new ArrayList<SoftReferenceBuster>();
      busters.put(classLoader, bustersList);
    } else {
      bustersList = busters.get(classLoader);
    }
    bustersList.add(buster);
  }

  public static ComposedSoftReferenceBuster getInstance() {
    if (INSTANCE == null) {
      INSTANCE = new ComposedSoftReferenceBuster();
    }

    return INSTANCE;
  }

}
