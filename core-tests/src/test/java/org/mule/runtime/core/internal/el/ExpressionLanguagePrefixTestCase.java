/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.el;

import static java.lang.System.lineSeparator;
import static java.util.Arrays.asList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_MEL_AS_DEFAULT;
import static org.mule.test.allure.AllureConstants.ExpressionLanguageFeature.EXPRESSION_LANGUAGE;

import org.mule.runtime.core.internal.el.dataweave.DataWeaveExpressionLanguageAdaptor;
import org.mule.runtime.core.internal.el.mvel.MVELExpressionLanguage;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.tck.size.SmallTest;

import java.util.Collection;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.verification.VerificationMode;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(EXPRESSION_LANGUAGE)
@Story("Support Mixing DW and MEL in a same application")
@RunWith(Parameterized.class)
@SmallTest
public class ExpressionLanguagePrefixTestCase extends AbstractMuleTestCase {

  private ExpressionLanguageAdaptorHandler elAdapter;
  private DataWeaveExpressionLanguageAdaptor dwLanguage;
  private MVELExpressionLanguage melLanguage;

  @Rule
  public SystemProperty melDefault;
  private VerificationMode melLangMode;
  private VerificationMode dwLangMode;

  @Parameters(name = "{0}")
  public static Collection<Boolean> params() {
    return asList(new Boolean[] {
        true, false
    });
  }

  public ExpressionLanguagePrefixTestCase(Boolean melDefault) {
    this.melDefault = new SystemProperty(MULE_MEL_AS_DEFAULT, melDefault.toString());
    if (melDefault) {
      dwLangMode = never();
      melLangMode = times(1);
    } else {
      dwLangMode = times(1);
      melLangMode = never();
    }
  }

  @Before
  public void before() {
    dwLanguage = mock(DataWeaveExpressionLanguageAdaptor.class);
    melLanguage = mock(MVELExpressionLanguage.class);
    elAdapter = new ExpressionLanguageAdaptorHandler(dwLanguage, melLanguage);
  }

  @Test
  public void singleLineNoPrefixNoMarker() {
    elAdapter.validate("expr");
    doVerify();
  }

  @Test
  public void singleLineNoPrefixMarker() {
    elAdapter.validate("#[expr]");
    doVerify();
  }

  @Test
  public void singleLineMelPrefixNoMarker() {
    elAdapter.validate("mel:expr");
    verify(dwLanguage, never()).validate(anyString());
    verify(melLanguage, times(1)).validate(anyString());
  }

  @Test
  public void singleLineMelPrefixMarker() {
    elAdapter.validate("#[mel:expr]");
    verify(dwLanguage, never()).validate(anyString());
    verify(melLanguage, times(1)).validate(anyString());
  }

  @Test
  public void singleLineWeavePrefixNoMarker() {
    elAdapter.validate("dw:expr");
    verify(dwLanguage, times(1)).validate(anyString());
    verify(melLanguage, never()).validate(anyString());
  }

  @Test
  public void singleLineWeavePrefixMarker() {
    elAdapter.validate("#[dw:expr]");
    verify(dwLanguage, times(1)).validate(anyString());
    verify(melLanguage, never()).validate(anyString());
  }

  @Test
  public void singleLineInvalidPrefixNoMarker() {
    elAdapter.validate("nolang:expr");
    doVerify();
  }

  @Test
  public void singleLineInvalidPrefixMarker() {
    elAdapter.validate("#[nolang:expr]");
    doVerify();
  }

  @Test
  public void multiLineNoPrefixNoMarker() {
    elAdapter.validate("expr" + lineSeparator() + "a:b");
    doVerify();
  }

  @Test
  public void multiLineNoPrefixMarker() {
    elAdapter.validate("#[expr" + lineSeparator() + "a:b]");
    doVerify();
  }

  @Test
  public void multiLineMelPrefixNoMarker() {
    elAdapter.validate("mel:expr" + lineSeparator() + "a:b");
    verify(dwLanguage, never()).validate(anyString());
    verify(melLanguage, times(1)).validate(anyString());
  }

  @Test
  public void multiLineMelPrefixMarker() {
    elAdapter.validate("#[mel:expr" + lineSeparator() + "a:b]");
    verify(dwLanguage, never()).validate(anyString());
    verify(melLanguage, times(1)).validate(anyString());
  }

  @Test
  public void multiLineWeavePrefixNoMarker() {
    elAdapter.validate("dw:expr" + lineSeparator() + "a:b");
    verify(dwLanguage, times(1)).validate(anyString());
    verify(melLanguage, never()).validate(anyString());
  }

  @Test
  public void multiLineWeavePrefixMarker() {
    elAdapter.validate("#[dw:expr" + lineSeparator() + "a:b]");
    verify(dwLanguage, times(1)).validate(anyString());
    verify(melLanguage, never()).validate(anyString());
  }

  @Test
  public void multiLineInvalidPrefixNoMarker() {
    elAdapter.validate("nolang:expr" + lineSeparator() + "a:b");
    doVerify();
  }

  @Test
  public void multiLineInvalidPrefixMarker() {
    elAdapter.validate("#[nolang:expr" + lineSeparator() + "a:b]");
    doVerify();
  }

  @Test
  public void paddedNoPrefixNoMarker() {
    elAdapter.validate("    expr a:b");
    doVerify();
  }

  @Test
  public void paddedNoPrefixMarker() {
    elAdapter.validate("#[    expr a:b]");
    doVerify();
  }

  @Test
  public void paddedMelPrefixNoMarker() {
    elAdapter.validate("    mel:expr a:b");
    verify(dwLanguage, never()).validate(anyString());
    verify(melLanguage, times(1)).validate(anyString());
  }

  @Test
  public void paddedMelPrefixMarker() {
    elAdapter.validate("#[    mel:expr a:b]");
    verify(dwLanguage, never()).validate(anyString());
    verify(melLanguage, times(1)).validate(anyString());
  }

  @Test
  public void paddedInvalidPrefixNoMarker() {
    elAdapter.validate("    nolang:expr a:b");
    doVerify();
  }

  @Test
  public void paddedInvalidPrefixMarker() {
    elAdapter.validate("#[    nolang:expr a:b]");
    doVerify();
  }

  private void doVerify() {
    verify(dwLanguage, dwLangMode).validate(anyString());
    verify(melLanguage, melLangMode).validate(anyString());
  }

  @Test
  public void fullWeaveScript() {
    elAdapter.validate("%dw 2.0 --- expr]");
    verify(dwLanguage).validate(anyString());
    verify(melLanguage, never()).validate(anyString());
  }

  @Test
  public void fullWeaveMultilineScript() {
    elAdapter.validate(lineSeparator() + "%dw 2.0" + lineSeparator() + " ---" + lineSeparator() + "expr]");
    verify(dwLanguage).validate(anyString());
    verify(melLanguage, never()).validate(anyString());
  }
}
