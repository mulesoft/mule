package org.mule.runtime.core.util;

import org.junit.Test;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.weave.v2.el.ByteArrayBasedCursorStreamProvider;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Created by matiasbargas on 5/15/17.
 */
public class ExpressionEvaluatorTestCase extends AbstractMuleContextTestCase {

  @Test
  public void handleNullEvent() throws MuleException {
    TypedValue evaluate = muleContext.getExpressionManager().evaluate("%dw 2.0\n%output application/json\n---\n{a: 1}");
    ByteArrayBasedCursorStreamProvider value = (ByteArrayBasedCursorStreamProvider) evaluate.getValue();
    String expected = "{\n" +
        "  \"a\": 1\n" +
        "}";
    assertThat(IOUtils.toString(value), is(expected));
  }
}
