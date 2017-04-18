/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.config.spring.parsers;

import static org.slf4j.LoggerFactory.getLogger;

import org.mule.test.config.spring.parsers.beans.OrphanBean;

import org.junit.Test;
import org.slf4j.Logger;

public class MapCombinerTestCase extends AbstractNamespaceTestCase {

  private static final Logger LOGGER = getLogger(MapCombinerTestCase.class);

  @Override
  protected String getConfigFile() {
    return "org/mule/config/spring/parsers/map-combiner-test.xml";
  }

  @Test
  public void testProperties() {
    OrphanBean bean = (OrphanBean) assertBeanExists("checkProps", OrphanBean.class);
    LOGGER.info("Map size: " + bean.getMap().size());
    assertMapEntryExists(bean.getMap(), "0", 0);
  }

  @Test
  public void testCombinedMap() {
    OrphanBean bean = (OrphanBean) assertBeanExists("orphan", OrphanBean.class);
    LOGGER.info("Map size: " + bean.getMap().size());
    for (int i = 0; i < 6; ++i) {
      assertMapEntryExists(bean.getMap(), Integer.toString(i + 1), i + 1);
    }
  }

  @Test
  public void testReverersedOrder() {
    OrphanBean bean = (OrphanBean) assertBeanExists("orphan2", OrphanBean.class);
    LOGGER.info("Map size: " + bean.getMap().size());
    for (int i = 0; i < 2; ++i) {
      assertMapEntryExists(bean.getMap(), Integer.toString(i + 1), i + 1);
    }
  }
}
