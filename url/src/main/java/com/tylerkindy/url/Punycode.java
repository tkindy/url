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

/**
 * @see <a href="https://www.rfc-editor.org/rfc/rfc3492.html">RFC 3492</a>
 */
final class Punycode {
  private static final int BASE = 36;
  private static final int T_MIN = 1;
  private static final int T_MAX = 26;
  private static final int SKEW = 38;
  private static final int DAMP = 700;
  private static final int INITIAL_BIAS = 72;
  private static final int INITIAL_N = 0x80;
  private static final int DELIMITER = '-';

  private Punycode() {
    throw new RuntimeException();
  }

  /**
   * @see <a href="https://www.rfc-editor.org/rfc/rfc3492.html#section-6.3">Punycode encoding procedure</a>
   */
  public static String encode(String label) {
    int n = INITIAL_N;
    int delta = 0;
    int bias = INITIAL_BIAS;
    StringBuilder output = new StringBuilder(label.length());

    int b = 0;
    for (int codePoint : new CodePointIterable(label)) {
      if (isBasic(codePoint)) {
        output.appendCodePoint(codePoint);
        b += 1;
      }
    }

    if (b > 0) {
      output.appendCodePoint(DELIMITER);
    }

    int h = b;
    while (h < label.length()) {
      final int finalN = n;
      int m = label.codePoints()
          .filter(codePoint -> Integer.compareUnsigned(codePoint, finalN) >= 0)
          .min()
          .orElseThrow();
      delta = Math.addExact(delta, Math.multiplyExact(m - n, h + 1));
      n = m;

      for (int c : new CodePointIterable(label)) {
        if (c < n) {
          delta = Math.addExact(delta, 1);
        }
        if (c == n) {
          int q = delta;
          int k = BASE;

          while (true) {
            int t = Math.clamp(k - bias, T_MIN, T_MAX);
            if (q < t) {
              break;
            }

            int digit = t + ((q - t) % (BASE - t));
            output.appendCodePoint(getCodePointForDigitValue(digit));
            q = (q - t) / (BASE - t);

            k += BASE;
          }

          output.appendCodePoint(getCodePointForDigitValue(q));
          bias = adapt(delta, h + 1, h == b);
          delta = 0;
          h += 1;
        }
      }
      delta += 1;
      n += 1;
    }

    return output.toString();
  }

  private static boolean isBasic(int codePoint) {
    return Integer.compareUnsigned(codePoint, 0x7F) <= 0;
  }

  private static int getCodePointForDigitValue(int digitValue) {
    if (0 <= digitValue && digitValue <= 25) {
      return 'a' + digitValue;
    }
    if (26 <= digitValue && digitValue <= 35) {
      return '0' + digitValue - 26;
    }
    throw new IllegalArgumentException("No code point for digit value " + digitValue);
  }

  private static int adapt(int delta, int numPoints, boolean firstTime) {
    if (firstTime) {
      delta = delta / DAMP;
    } else {
      delta = delta / 2;
    }
    delta += delta / numPoints;
    int k = 0;
    while (delta > ((BASE - T_MIN) * T_MAX) / 2) {
      delta = delta / (BASE - T_MIN);
      k += BASE;
    }
    return k + (((BASE - T_MIN + 1) * delta) / (delta + SKEW));
  }
}
