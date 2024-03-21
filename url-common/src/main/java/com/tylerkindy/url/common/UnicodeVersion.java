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

public record UnicodeVersion(int major, int minor, int update) {
  public static UnicodeVersion from(String version) {
    String[] parts = version.split("\\.");
    if (parts.length != 3) {
      throw new IllegalArgumentException("Not a Unicode version: " + version);
    }

    return new UnicodeVersion(
        parsePart(parts[0], "major"),
        parsePart(parts[1], "minor"),
        parsePart(parts[2], "update")
    );
  }

  private static int parsePart(String part, String partName) {
    try {
      return Integer.parseInt(part);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Illegal " + partName + " version: " + part);
    }
  }
}
