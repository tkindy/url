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

package com.tylerkindy.url.common;

public sealed interface CodePoints {
  record Single(int codePoint) implements CodePoints {
    @Override
    public String toString() {
      return Integer.toHexString(codePoint);
    }
  }

  record Range(int lowCodePoint, int highCodePoint) implements CodePoints {
    @Override
    public String toString() {
      return Integer.toHexString(lowCodePoint) + ".." + Integer.toHexString(highCodePoint);
    }
  }

  static CodePoints fromInput(String codePoints) {
    String[] parts = codePoints.split("\\.\\.");

    return switch (parts.length) {
      case 1 -> new Single(Integer.parseInt(parts[0], 16));
      case 2 -> new Range(Integer.parseInt(parts[0], 16), Integer.parseInt(parts[1], 16));
      default -> throw new IllegalArgumentException("Unexpected code points: " + codePoints);
    };
  }
}
