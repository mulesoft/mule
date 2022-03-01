package org.mule.runtime.module.deployment.logging.rule;

import static java.lang.Class.forName;

import java.lang.reflect.Field;
import java.util.Map;

import org.junit.rules.ExternalResource;
import org.slf4j.impl.StaticMDCBinder;
import org.slf4j.spi.MDCAdapter;
import uk.org.lidalia.lang.ThreadLocal;
import uk.org.lidalia.slf4jtest.TestLoggerFactory;
import uk.org.lidalia.slf4jtest.TestMDCAdapter;

public class LogCleanup extends ExternalResource {

  @Override
  protected void before() throws Throwable {
    clearAllLogs();
  }

  @Override
  protected void after() {
    clearLogsAndMDCThreadReferences();
  }

  public void clearAllLogs() {
    // Logs that are stored for later assert need to be cleared before every test
    // clearAll will reset state across all threads
    try {
      forName("uk.org.lidalia.slf4jtest.TestLoggerFactory", false, getClass().getClassLoader());
      TestLoggerFactory.clearAll();
    } catch (ClassNotFoundException e) {
    } 
  }

  public void clearLogsAndMDCThreadReferences() {
    // TestMDCAdapter contains its own implementation of ThreadLocal variables which hold strong references to Threads that should
    // be released to prevent possible leakages
    try {
      clearAllLogs();
      forName("uk.org.lidalia.slf4jtest.TestMDCAdapter", false, getClass().getClassLoader());
      forName("uk.org.lidalia.lang.ThreadLocal", false, getClass().getClassLoader());
      MDCAdapter testMDCAdapter = StaticMDCBinder.SINGLETON.getMDCA();
      Field valueField = TestMDCAdapter.class.getDeclaredField("value");
      valueField.setAccessible(true);
      ThreadLocal<Map<String, String>> threadLocal = (ThreadLocal<Map<String, String>>) valueField.get(testMDCAdapter);
      threadLocal.reset();
    } catch (ClassNotFoundException e) {
    } catch (NoSuchFieldException | IllegalAccessException e) {
      throw new RuntimeException("Could not resolve field", e);
    }
  }
  
  
}
