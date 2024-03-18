/*
 * Copyright 2024 Tyler Kindy.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tylerkindy.url;

import com.google.common.collect.Range;

final class CharacterUtils {
  private static final CharacterSet URL_CODE_POINTS = CharacterSet.builder()
      .addRange(Range.closed((int) '0', (int) '9'))
      .addRange(Range.closed((int) 'A', (int) 'Z'))
      .addRange(Range.closed((int) 'a', (int) 'z'))
      .addCodePoints(
          '!', '$', '&', '\'', '(', ')',
          '*', '+', ',', '-', '.',
          '/', ':', ';', '=', '?',
          '@', '_', '~'
      )
      // TODO: exclude surrogated and non-chacters from following set
      .addRange(Range.closed(0x00a0, 0x10fffd))
      .build();

  private CharacterUtils() {
    throw new RuntimeException();
  }

  public static boolean isC0ControlOrSpace(int codePoint) {
    return isC0Control(codePoint) || codePoint == ' ';
  }

  private static boolean isC0Control(int codePoint) {
    return codePoint >= 0x0 && codePoint <= 0x1F;
  }

  public static boolean isAsciiAlpha(int codePoint) {
    return isAsciiLowerAlpha(codePoint) || isAsciiUpperAlpha(codePoint);
  }

  private static boolean isAsciiLowerAlpha(int codePoint) {
    return codePoint >= 'a' && codePoint <= 'z';
  }

  private static boolean isAsciiUpperAlpha(int codePoint) {
    return codePoint >= 'A' && codePoint <= 'Z';
  }

  public static boolean isAsciiAlphanumeric(int codePoint) {
    return isAsciiAlpha(codePoint) || isAsciiDigit(codePoint);
  }

  public static boolean isAsciiHexDigit(int codePoint) {
    return isAsciiUpperHexDigit(codePoint) || isAsciiLowerHexDigit(codePoint);
  }

  private static boolean isAsciiUpperHexDigit(int codePoint) {
    return isAsciiDigit(codePoint) || (codePoint >= 'A' && codePoint <= 'F');
  }

  private static boolean isAsciiLowerHexDigit(int codePoint) {
    return isAsciiDigit(codePoint) || (codePoint >= 'a' && codePoint <= 'f');
  }

  public static boolean isAsciiDigit(int codePoint) {
    return codePoint >= '0' && codePoint <= '9';
  }

  public static boolean isAsciiTabOrNewline(int c) {
    return c == '\t' || c == '\f' || c == '\r' || c == '\n';
  }

  public static boolean isWindowsDriveLetter(String s) {
    if (s.length() != 2 && s.codePointCount(0, 2) != 2) {
      return false;
    }

    return isAsciiAlpha(s.codePointAt(0)) &&
        (s.codePointAt(1) == ':' || s.codePointAt(1) == '|');
  }

  public static boolean isNormalizedWindowsDriveLetter(String s) {
    return isWindowsDriveLetter(s) && s.codePointAt(1) == ':';
  }

  public static boolean isUrlCodePoint(int codePoint) {
    return URL_CODE_POINTS.contains(codePoint);
  }
}
