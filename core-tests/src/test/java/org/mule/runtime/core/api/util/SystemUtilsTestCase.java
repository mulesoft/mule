/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.util;

import static org.apache.commons.lang3.SystemUtils.IS_OS_WINDOWS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import org.mule.runtime.core.privileged.util.MapUtils;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

@SmallTest
public class SystemUtilsTestCase extends AbstractMuleTestCase {

  @Test
  public void testEnvironment() throws Exception {
    Map env = SystemUtils.getenv();
    assertNotNull(env);
    assertFalse(env.isEmpty());
    assertSame(env, SystemUtils.getenv());

    String envVarToTest = "PATH";
    if (IS_OS_WINDOWS) {
      // Depending on the presence of Cygwin, it might be one or the other.
      if (env.get(envVarToTest) == null) {
        envVarToTest = "Path";
      }
    }

    assertNotNull(env.get(envVarToTest));
  }

  @Test
  public void testParsePropertyDefinitions() {
    Map expected = Collections.EMPTY_MAP;
    String input;

    assertEquals(expected, SystemUtils.parsePropertyDefinitions(null));
    assertEquals(expected, SystemUtils.parsePropertyDefinitions(""));
    assertEquals(expected, SystemUtils.parsePropertyDefinitions(" "));
    assertEquals(expected, SystemUtils.parsePropertyDefinitions("foo"));
    assertEquals(expected, SystemUtils.parsePropertyDefinitions("-D"));
    assertEquals(expected, SystemUtils.parsePropertyDefinitions("-D="));

    expected = Collections.singletonMap("-D", "true");
    assertEquals(expected, SystemUtils.parsePropertyDefinitions("-D-D"));

    expected = Collections.singletonMap("-D-D", "true");
    assertEquals(expected, SystemUtils.parsePropertyDefinitions("-D-D-D"));

    expected = Collections.singletonMap("-D-D-D", "true");
    assertEquals(expected, SystemUtils.parsePropertyDefinitions("-D-D-D-D"));

    assertEquals(Collections.EMPTY_MAP, SystemUtils.parsePropertyDefinitions("-D=noKey"));
    assertEquals(Collections.EMPTY_MAP, SystemUtils.parsePropertyDefinitions("=-D"));
    assertEquals(Collections.EMPTY_MAP, SystemUtils.parsePropertyDefinitions("foo =foo foo"));

    expected = Collections.singletonMap("k", "true");
    assertEquals(expected, SystemUtils.parsePropertyDefinitions(" -Dk "));

    expected = Collections.singletonMap("key", "true");
    assertEquals(expected, SystemUtils.parsePropertyDefinitions("-Dkey"));

    expected = Collections.singletonMap("k", "v");
    assertEquals(expected, SystemUtils.parsePropertyDefinitions(" -Dk=v "));

    expected = Collections.singletonMap("key", "value");
    assertEquals(expected, SystemUtils.parsePropertyDefinitions("-Dkey=value"));

    expected = Collections.singletonMap("key", "quoted");
    assertEquals(expected, SystemUtils.parsePropertyDefinitions("-Dkey=\"quoted\""));

    expected = MapUtils.mapWithKeysAndValues(HashMap.class, new String[] {"key", "foo"}, new String[] {"-Dvalue", "bar"});
    assertEquals(expected, SystemUtils.parsePropertyDefinitions("-Dkey=-Dvalue -Dfoo=bar"));

    assertEquals(Collections.EMPTY_MAP, SystemUtils.parsePropertyDefinitions("-D=-Dfoo-D== =foo"));

    expected = Collections.singletonMap("key", "split value");
    assertEquals(expected, SystemUtils.parsePropertyDefinitions("-Dkey=\"split value\""));

    expected =
        MapUtils.mapWithKeysAndValues(HashMap.class, new String[] {"key1", "key2"}, new String[] {"split one", "split two"});
    input = "-Dkey1=\"split one\" -Dkey2=\"split two\" ";
    assertEquals(expected, SystemUtils.parsePropertyDefinitions(input));

    expected = Collections.singletonMap("key", "open end");
    input = "-Dkey=\"open end";
    assertEquals(expected, SystemUtils.parsePropertyDefinitions(input));

    expected = MapUtils.mapWithKeysAndValues(HashMap.class, new String[] {"keyOnly", "mule.foo", "mule.bar"},
                                             new String[] {"true", "xfoo", "xbar"});
    input = "  standalone key=value -D -D= -DkeyOnly -D=noKey -Dmule.foo=xfoo -Dmule.bar=xbar ";
    assertEquals(expected, SystemUtils.parsePropertyDefinitions(input));
  }

}
