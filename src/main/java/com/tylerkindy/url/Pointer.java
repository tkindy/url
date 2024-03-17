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

import com.tylerkindy.url.Pointer.PrefixPattern.AsciiDigit;
import com.tylerkindy.url.Pointer.PrefixPattern.Literal;
import java.util.function.IntSupplier;
import java.util.function.Predicate;

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

  public void increase() {
    increase(1);
  }

  public void increase(int numCodePoints) {
    move(numCodePoints);
  }

  public void decrease() {
    decrease(1);
  }

  public void decrease(int numCodePoints) {
    move(-1 * numCodePoints);
  }

  private void move(int numCodePoints) {
    if (numCodePoints == 0) {
      return;
    }
    
    final IntSupplier nextCodePoint;
    final IndexUpdater indexUpdater;
    final Predicate<Pointer> isAtEnd;
    if (numCodePoints > 0) {
      nextCodePoint = this::getCurrentCodePoint;
      indexUpdater = (index, amount) -> index + amount;
      isAtEnd = Pointer::isEof;
    } else {
      nextCodePoint = () -> s.codePointBefore(codeUnitIndex);
      indexUpdater = (index, amount) -> index - amount;
      isAtEnd = p -> p.codePointIndex == -1;
      numCodePoints *= -1;
    }

    for (int i = 0; i < numCodePoints; i++) {
      if (isAtEnd.test(this)) {
        return;
      }
      if (indexUpdater.update(codePointIndex, 1) == -1) {
        codePointIndex = -1;
        codeUnitIndex = -1;
        continue;
      }
      if (indexUpdater.update(codePointIndex, 1) == 0) {
        codePointIndex = 0;
        codeUnitIndex = 0;
        continue;
      }

      int codePoint = nextCodePoint.getAsInt();

      codePointIndex = indexUpdater.update(codePointIndex, 1);
      if (codePoint <= 0xFFFF) {
        // Basic Multilingual Plane, made of one char
        codeUnitIndex = indexUpdater.update(codeUnitIndex, 1);
      } else {
        // supplementary character, made of two chars
        codeUnitIndex = indexUpdater.update(codeUnitIndex, 2);
      }
    }
  }

  public boolean isEof() {
    return codePointIndex >= codePointLength;
  }

  public boolean doesRemainingStartWith(String prefix) {
    long prefixLength = prefix
        .chars()
        .filter(c -> c != '%')
        .count();

    int remainingLength = s.length() - codeUnitIndex - 1;
    if (remainingLength < prefixLength) {
      return false;
    }

    for (int i = 0; i < prefix.length(); i++) {
      char sChar = s.charAt(codeUnitIndex + i + 1);
      char prefixChar = prefix.charAt(i);

      final PrefixPattern prefixPattern;
      if (prefixChar == '%') {
        char patternChar = prefix.charAt(i + 1);
        prefixPattern = switch (patternChar) {
          case 'd' -> new AsciiDigit();
          default -> throw new IllegalArgumentException("Unexpected prefix pattern char: " + patternChar);
        };
      } else {
        prefixPattern = new Literal(prefixChar);
      }

      if (!prefixPattern.matches(sChar)) {
        return false;
      }
    }

    return true;
  }

  public void reset() {
    codePointIndex = 0;
    codeUnitIndex = 0;
  }
  
  @FunctionalInterface
  private interface IndexUpdater {
    int update(int index, int amount);
  }

  sealed interface PrefixPattern {
    default boolean matches(char c) {
      return switch (this) {
        case AsciiDigit ad -> c >= '0' && c <= '9';
        case Literal(var l) -> c == l;
      };
    }

    record AsciiDigit() implements PrefixPattern {}
    record Literal(char c) implements PrefixPattern {}
  }
}
