package org.mule.functional.api.component;

import static java.util.Arrays.asList;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class SummaryLogCheckerTestCase extends AbstractMuleTestCase {

  private SummaryLogChecker summaryLogChecker = new SummaryLogChecker();

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void successIfInfoFound() throws Exception {
    summaryLogChecker.setExclusiveContent(false);
    SummaryLogChecker.SummaryInfo summaryInfo = new SummaryLogChecker.SummaryInfo("key", "value");
    summaryLogChecker.setExpectedInfo(asList(summaryInfo));
    summaryLogChecker.check("key: value");
  }

  @Test
  public void failureIfNoInfoFound() throws Exception {
    SummaryLogChecker.SummaryInfo summaryInfo = new SummaryLogChecker.SummaryInfo("key", "value");
    summaryLogChecker.setExpectedInfo(asList(summaryInfo));
    expectedException.expect(AssertionError.class);
    summaryLogChecker.check("other stuff");
  }

  @Test
  public void keyMatchSucceedsIfNoValueSpecified() throws Exception {
    summaryLogChecker.setExclusiveContent(false);
    SummaryLogChecker.SummaryInfo summaryInfo = new SummaryLogChecker.SummaryInfo("key");
    summaryLogChecker.setExpectedInfo(asList(summaryInfo));
    summaryLogChecker.check("key: some other stuff");
  }

  @Test
  public void extraInformationSucceedsIfExclusiveContentIsFalse() throws Exception {
    summaryLogChecker.setExclusiveContent(false);
    SummaryLogChecker.SummaryInfo summaryInfo = new SummaryLogChecker.SummaryInfo("key", "value");
    summaryLogChecker.setExpectedInfo(asList(summaryInfo));
    summaryLogChecker.check("key: value\nkey2: value2");
  }

  @Test
  public void extraInformationFailsIfExclusiveContentIsTrue() throws Exception {
    summaryLogChecker.setExclusiveContent(true);
    SummaryLogChecker.SummaryInfo summaryInfo = new SummaryLogChecker.SummaryInfo("key", "value");
    summaryLogChecker.setExpectedInfo(asList(summaryInfo));
    expectedException.expect(AssertionError.class);
    summaryLogChecker.check("key: value\nkey2: value2");
  }

  @Test
  public void successWithStacktraceNoiseAndExclusiveContentAsTrue() throws Exception {
    StringWriter stringWriter = new StringWriter();
    PrintWriter printWriter = new PrintWriter(stringWriter);
    new Exception().printStackTrace(printWriter);
    summaryLogChecker.setExclusiveContent(true);
    SummaryLogChecker.SummaryInfo summaryInfo = new SummaryLogChecker.SummaryInfo("key", "value");
    summaryLogChecker.setExpectedInfo(asList(summaryInfo));
    summaryLogChecker.check("key: value\n" + stringWriter.toString());
  }



}
