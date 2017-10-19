/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.capability.xml.schema.doc;

import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.mule.runtime.module.extension.internal.capability.xml.schema.doc.JavaDocReader.ParsingState.ON_BODY;
import static org.mule.runtime.module.extension.internal.capability.xml.schema.doc.JavaDocReader.ParsingState.ON_PARAM;
import static org.mule.runtime.module.extension.internal.capability.xml.schema.doc.JavaDocReader.ParsingState.UNSUPPORTED;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;

import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.commons.lang3.StringUtils;

/**
 * JavaDoc Reader which encapsulates the logic of parsing JavaDocs.
 *
 * @since 4.0
 */
public class JavaDocReader {

  private static final String PARAM = "@param";
  private static final String EMPTY = "";
  private static final char AT_CHAR = '@';
  private static final char NEW_LINE_CHAR = '\n';
  private static final char SPACE_CHAR = ' ';
  private static final char CLOSING_BRACKET_CHAR = '}';
  private static final char OPENING_BRACKET_CHAR = '{';

  /**
   * Extracts the JavaDoc of an element and parses it returning a easy to consume {@link JavaDocModel}
   *
   * @param processingEnv The current {@link ProcessEnvironment}
   * @param element       Element to introspect
   * @return A {@link JavaDocModel} representing the {@link Element} JavaDoc.
   */
  public static JavaDocModel parseJavaDoc(ProcessingEnvironment processingEnv, Element element) {
    String comment = extractJavadoc(processingEnv, element);
    ParsingState readingState = ON_BODY;

    StringBuilder body = new StringBuilder();
    Map<String, StringBuilder> params = new HashMap<>();

    String paramName = EMPTY;

    StringTokenizer st = new StringTokenizer(comment, "\n\r");

    while (st.hasMoreTokens()) {
      String token = st.nextToken().trim();
      if (token.isEmpty()) {
        switch (readingState) {
          case ON_BODY:
            body.append(NEW_LINE_CHAR);
            break;
          case ON_PARAM:
            params.get(paramName).append(NEW_LINE_CHAR);
            break;
          case UNSUPPORTED:
            break;
        }
      } else if (token.startsWith(PARAM)) {
        readingState = ON_PARAM;
        paramName = parseParameter(token, params);
      } else if (!(token.charAt(0) == AT_CHAR)) {
        switch (readingState) {
          case ON_BODY:
            body.append(SPACE_CHAR).append(token);
            break;
          case ON_PARAM:
            params.get(paramName).append(SPACE_CHAR).append(token);
            break;
          case UNSUPPORTED:
            break;
        }
      } else if (token.charAt(0) == AT_CHAR) {
        readingState = UNSUPPORTED;
      }
    }

    return new JavaDocModel(stripTags(body.toString()), params.entrySet().stream()
        .collect(toMap(Map.Entry::getKey, entry -> stripTags(entry.getValue().toString()))));
  }

  private static String parseParameter(String token, Map<String, StringBuilder> params) {
    StringBuilder paramBuilder = new StringBuilder();
    String currentParamName;
    String param = token.replaceFirst(PARAM, StringUtils.EMPTY).trim();
    int descriptionIndex = param.indexOf(" ");
    String description;
    if (descriptionIndex != -1) {
      currentParamName = param.substring(0, descriptionIndex).trim();
      description = param.substring(descriptionIndex).trim();
    } else {
      currentParamName = param;
      description = "";
    }
    paramBuilder.append(description);
    params.put(currentParamName, paramBuilder);

    return currentParamName;
  }

  private static String stripTags(String comment) {
    if (isEmpty(comment)) {
      return comment;
    } else {
      StringBuilder builder = new StringBuilder();
      boolean insideTag = false;
      comment = comment.trim();

      final int length = comment.length();
      for (int i = 0; i < length; i++) {
        if (comment.charAt(i) == OPENING_BRACKET_CHAR) {
          int nextCharIndex = i + 1;
          if (nextCharIndex < length && comment.charAt(nextCharIndex) == AT_CHAR) {
            while (comment.charAt(nextCharIndex) != SPACE_CHAR && comment.charAt(nextCharIndex) != CLOSING_BRACKET_CHAR) {
              nextCharIndex = i++;
            }
            insideTag = true;
            i = nextCharIndex;
            continue;
          }
        } else if (comment.charAt(i) == CLOSING_BRACKET_CHAR && insideTag) {
          insideTag = false;
          continue;
        }

        builder.append(comment.charAt(i));
      }

      String strippedComments = builder.toString().trim();
      while (strippedComments.length() > 0 && strippedComments.charAt(strippedComments.length() - 1) == NEW_LINE_CHAR) {
        strippedComments = StringUtils.chomp(strippedComments);
      }
      return strippedComments;
    }
  }

  private static String extractJavadoc(ProcessingEnvironment processingEnv, Element element) {
    String comment = processingEnv.getElementUtils().getDocComment(element);

    if (StringUtils.isBlank(comment)) {
      return StringUtils.EMPTY;
    }

    return comment.trim();
  }

  public enum ParsingState {
    ON_PARAM, ON_BODY, UNSUPPORTED
  }
}
