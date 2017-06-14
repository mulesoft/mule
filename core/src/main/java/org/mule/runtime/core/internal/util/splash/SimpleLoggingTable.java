/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.splash;

import static org.apache.commons.lang3.StringUtils.center;
import static org.apache.commons.lang3.StringUtils.rightPad;
import static org.apache.commons.lang3.StringUtils.substring;

import org.mule.runtime.core.api.util.StringUtils;

import java.util.LinkedList;
import java.util.List;

/**
 * Provides a simple table implementation useful to log information in a tabular form.
 */
public class SimpleLoggingTable {

  protected static final String NEW_LINE = String.format("%n");
  private static final char SEPARATOR_CHAR = '*';

  private List<TableColumn> columns = new LinkedList<TableColumn>();
  private final List<String[]> data = new LinkedList<String[]>();
  private int width;

  /**
   * Adds a new column to the table.
   *
   * @param title column title that will be displayed in the table header
   * @param size the size of the column
   */
  public void addColumn(String title, int size) {
    columns.add(new TableColumn(title, size));
    width = calculateTableWidth();
  }

  /**
   * Adds a new row of data into the table.
   *
   * @param dataRow the data to be added. DataRow must contain a value for each declared column.
   */
  public void addDataRow(String[] dataRow) {
    if (dataRow.length != columns.size()) {
      throw new IllegalArgumentException("Data does not contain enough elements");
    }

    data.add(dataRow.clone());
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();

    addHeaders(builder);

    addRows(builder);

    return builder.toString();
  }

  private void addHeaders(StringBuilder builder) {
    addSeparatorLine(builder);

    for (TableColumn column : columns) {
      builder.append(String.format("%c %s ", SEPARATOR_CHAR, formatHeaderValue(column.name, column.width)));
    }
    builder.append(SEPARATOR_CHAR).append(NEW_LINE);

    addSeparatorLine(builder);
  }

  private void addRows(StringBuilder builder) {
    for (String[] row : data) {
      for (int i = 0; i < row.length; i++) {
        builder.append(String.format("%c %s ", SEPARATOR_CHAR, formatValue(row[i], columns.get(i).width)));
      }
      builder.append(SEPARATOR_CHAR).append(NEW_LINE);
    }

    addSeparatorLine(builder);
  }

  private void addSeparatorLine(StringBuilder builder) {
    builder.append(StringUtils.repeat(SEPARATOR_CHAR, width));
    builder.append(NEW_LINE);
  }

  /**
   * Calculates the real table width based on the column sizes and the extra characters used to separate them.
   *
   * @return the real table width.
   */
  private int calculateTableWidth() {
    int result = 0;

    for (TableColumn column : columns) {
      // Count three chars more per column: a separator char + 2 white spaces
      result = result + column.width + 3;
    }

    // Counts another separator at the right end
    result++;

    return result;
  }

  private String formatValue(String value, int size) {
    String result = substring(value, 0, size);
    result = rightPad(result, size, ' ');

    return result;
  }

  private String formatHeaderValue(String value, int size) {
    String result = substring(value, 0, size);
    result = center(String.format("- - + %s + - -", result), size, ' ');

    return result;
  }

  private class TableColumn {

    protected final String name;
    protected final int width;

    public TableColumn(String name, int width) {
      this.name = name;
      this.width = width;
    }
  }
}
