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

final class Pointer {
  private final String s;
  private final int codePointLength;

  /** The index of the current code point. */
  private int codePointIndex;
  /** The direct index into the string's char array. */
  private int codeUnitIndex;

  Pointer(String s) {
    this.s = s;
    this.codePointLength = s.codePointCount(0, s.length());

    codePointIndex = 0;
    codeUnitIndex = 0;
  }

  public int getCurrentCodePoint() {
    return s.codePointAt(codeUnitIndex);
  }

  public void advance() {
    advance(1);
  }

  public void advance(int numCodePoints) {
    if (numCodePoints < 0) {
      throw new IllegalArgumentException("Can't advance by a negative number: " + numCodePoints);
    }

    for (int i = 0; i < numCodePoints; i++) {
      if (isEof()) {
        return;
      }

      int codePoint = getCurrentCodePoint();

      codePointIndex += 1;
      if (codePoint <= 0xFFFF) {
        // Basic Multilingual Plane, made of one char
        codeUnitIndex += 1;
      } else {
        // supplementary character, made of two chars
        codeUnitIndex += 2;
      }
    }
  }

  public boolean isEof() {
    return codePointIndex >= codePointLength;
  }
}
