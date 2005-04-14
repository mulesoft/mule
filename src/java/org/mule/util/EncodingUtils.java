/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.util;

/**
 * <code>EncodingUtils</code> a collections of methods to help with SGML encoding.
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class EncodingUtils
{
    public static String encode(String string) {
        String result = decode(string);
        result = result.replaceAll("&", "&amp;");
        return result;
    }

    public static String decode(String string) {
        String result = string.replaceAll("&amp;", "&");
        result = string.replaceAll("&amp;", "&");
        return result;
    }

    /**
   * Converts a character to its SGML numeric encoding
   *
   * @param c the character
   * @return a string with the representation of c as
   * "Numeric Character Reference" in SGMLese
   *
   * <br><br><b>Example</b>:
   * <li><code>toSgmlEncoding('\n')</code>
   * returns "&amp;#10;".</li>
   */
  protected static String toSgmlEncoding(char c) {
    return c > 0x20 || c == 0x9 || c == 0xa || c == 0xd ? "&#" + (int)c + ";" : "?";
  }

  /**
   * Encodes a character using its SGML sequence
   * It can be a hex representation
   * @param c the character
   * @return the string with either Predefined Entity, Numeric Character Reference,
   * or null if no entity could be found
   *
   */
  public static String toSgmlEntity(char c) {
    return (c == '<') ? "&lt;" :
           (c == '>') ? "&gt;" :
           (c == '\'') ? "&apos;" :
           (c == '\"') ? "&quot;" :
           (c == '&') ? "&amp;" :
           (c == ']') ? "&#93;" :
           (c < '\u0020' && c != '\n' && c != '\r' && c != '\t') || c > '\u0080' ?
              toSgmlEncoding(c) :
           null;
  }

    public static String sgmlEncode(String string)
    {
        char[] chars = string.toCharArray();
        char c;
        StringBuffer buffer = new StringBuffer(string.length());
        String converted;
        for (int i = 0; i < chars.length; i++)
        {
            c = chars[i];
            converted = toSgmlEntity(c);
            if(converted!=null) {
                buffer.append(converted);
            } else {
                buffer.append(c);
            }
        }
        return buffer.toString();
    }
}
