/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.jpms.api;

import static java.lang.StackWalker.Option.RETAIN_CLASS_REFERENCE;

import java.util.List;

public class JpmsUtils {

  /**
   * 
   * @param layer          the layer containing the module to open the {@code packages} to.
   * @param moduleName     the name of the module within {@code layer} to open the {@code packages} to.
   * @param bootModuleName the name of the module in the boot layer to open the {@code packages} from.
   * @param packages       the packages to open from {@code bootModuleName} to {@code moduleName}.
   */
  public static void openToModule(ModuleLayer layer, String moduleName, String bootModuleName, List<String> packages) {
    // Make sure only allowed users within the Mule Runtime use this
    if (!StackWalker.getInstance(RETAIN_CLASS_REFERENCE).getCallerClass().getName()
        .equals("org.mule.runtime.module.service.api.artifact.ServiceModuleLayerFactory")) {
      throw new UnsupportedOperationException("This is for internal use only.");
    }

    layer.findModule(moduleName)
        .ifPresent(module -> {
          Module bootModule = ModuleLayer.boot()
              .findModule(bootModuleName).get();

          for (String pkg : packages) {
            bootModule.addOpens(pkg, module);
          }
        });

  }
}
