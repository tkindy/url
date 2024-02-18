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

package com.tylerkindy.url.testdata;

final class StringEscaper {
  public static String escape(String input) {
    StringBuilder sb = new StringBuilder(input.length());

    for (int i = 0; i < input.length(); i++) {
      char c = input.charAt(i);
      switch (c) {
        case ' ' -> sb.append("\\s");
        case '\t' -> sb.append("\\t");
        case '\n' -> sb.append("\\n");
        case '\f' -> sb.append("\\f");
        case '\r' -> sb.append("\\r");
        case '\\' -> sb.append("\\\\");
        default -> sb.append(c);
      }
    }

    return sb.toString();
  }
}
