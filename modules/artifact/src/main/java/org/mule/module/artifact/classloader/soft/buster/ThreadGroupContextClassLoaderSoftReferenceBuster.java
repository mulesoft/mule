/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.artifact.classloader.soft.buster;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class ThreadGroupContextClassLoaderSoftReferenceBuster implements SoftReferenceBuster {

  private ClassLoader classLoader;

  public ThreadGroupContextClassLoaderSoftReferenceBuster(ClassLoader classLoader) {
    this.classLoader = classLoader;
  }

  @Override
  public void bustSoftReferences(ClassLoader classloader) {
    final Object /* WeakIdentityMap<ThreadGroupContext> */ contexts =
        SoftReferenceBusterReflectionUtils.getStaticFieldValue("java.beans.ThreadGroupContext", "contexts");
    if (contexts != null) {
      final Field tableField = SoftReferenceBusterReflectionUtils
          .findField(SoftReferenceBusterReflectionUtils.findClass("java.beans.WeakIdentityMap"), "table");
      if (tableField != null) {
        final WeakReference/* java.beans.WeakIdentityMap.Entry */[] table =
            SoftReferenceBusterReflectionUtils.getFieldValue(tableField, contexts);
        if (table != null) {
          Method clearBeanInfoCache = null;
          for (WeakReference entry : table) {
            if (entry != null) {
              Object /* ThreadGroupContext */ context = SoftReferenceBusterReflectionUtils.getFieldValue(entry, "value");
              if (context != null) {
                if (clearBeanInfoCache == null) { // FirstThreadGroupContext
                  clearBeanInfoCache = SoftReferenceBusterReflectionUtils.findMethod(context.getClass(), "clearBeanInfoCache");
                }

                try {
                  clearBeanInfoCache.invoke(context);
                } catch (Exception e) {

                }
              }
            }
          }
        }
      }
    }
  }

}
