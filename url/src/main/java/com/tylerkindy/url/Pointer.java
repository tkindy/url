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

import static com.tylerkindy.url.CharacterUtils.isAsciiAlpha;

import com.tylerkindy.url.Pointer.PointedAt.CodePoint;
import com.tylerkindy.url.Pointer.PointedAt.Eof;
import com.tylerkindy.url.Pointer.PointedAt.Nowhere;
import com.tylerkindy.url.Pointer.PrefixPattern.AsciiHexDigit;
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

  public PointedAt pointedAt() {
    if (isEof()) {
      return new Eof();
    }
    if (codePointIndex == -1) {
      return new Nowhere();
    }
    return new CodePoint(getCurrentCodePoint());
  }

  @Deprecated
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

      int newCodePointIndex = indexUpdater.update(codePointIndex, 1);
      if (newCodePointIndex == -1) {
        codePointIndex = -1;
        codeUnitIndex = -1;
        continue;
      }
      if (newCodePointIndex == 0) {
        codePointIndex = 0;
        codeUnitIndex = 0;
        continue;
      }

      int codePoint = nextCodePoint.getAsInt();

      codePointIndex = newCodePointIndex;
      if (codePoint <= 0xFFFF) {
        // Basic Multilingual Plane, made of one char
        codeUnitIndex = indexUpdater.update(codeUnitIndex, 1);
      } else {
        // supplementary character, made of two chars
        codeUnitIndex = indexUpdater.update(codeUnitIndex, 2);
      }
    }
  }

  @Deprecated
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
          case 'd' -> new AsciiHexDigit();
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

  public boolean doesRemainingStartWithWindowsDriveLetter() {
    int remainingLength = s.length() - codeUnitIndex;
    if (remainingLength < 2) {
      return false;
    }
    if (!isAsciiAlpha(s.codePointAt(codeUnitIndex))) {
      return false;
    }

    int secondCodePoint = s.codePointAt(codeUnitIndex + 1);
    if (secondCodePoint != ':' && secondCodePoint != '|') {
      return false;
    }

    if (remainingLength == 2) {
      return true;
    }

    int thirdCodePoint = s.codePointAt(codeUnitIndex + 2);
    return thirdCodePoint == '/' ||
        thirdCodePoint == '\\' ||
        thirdCodePoint == '?' ||
        thirdCodePoint == '#';
  }

  public void reset() {
    codePointIndex = 0;
    codeUnitIndex = 0;
  }

  @Override
  public String toString() {
    return switch (pointedAt()) {
      case CodePoint(var c) -> "'" + Character.toString(c) + "'";
      case Eof eof -> "EOF";
      case Nowhere nowhere -> "Nowhere";
    };
  }

  @FunctionalInterface
  private interface IndexUpdater {
    int update(int index, int amount);
  }

  sealed interface PrefixPattern {
    default boolean matches(char c) {
      return switch (this) {
        case AsciiHexDigit ahd -> CharacterUtils.isAsciiHexDigit(c);
        case Literal(var l) -> c == l;
      };
    }

    record AsciiHexDigit() implements PrefixPattern {}
    record Literal(char c) implements PrefixPattern {}
  }

  public sealed interface PointedAt {
    record Nowhere() implements PointedAt {}
    record Eof() implements PointedAt {}
    record CodePoint(int codePoint) implements PointedAt {}
  }
}
