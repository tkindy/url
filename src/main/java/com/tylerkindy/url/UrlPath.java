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

import java.util.ArrayList;
import java.util.List;

public sealed interface UrlPath {
  default UrlPath copy() {
    return switch (this) {
      case Opaque o -> o;
      case NonOpaque(var segments) -> new NonOpaque(new ArrayList<>(segments));
    };
  }

  default UrlPath shorten() {
    return switch (this) {
      case Opaque o -> throw new AssertionError("Cannot shorten an opaque path");
      case NonOpaque(var segments) -> segments.isEmpty() ? this : new NonOpaque(segments.subList(0, segments.size() - 1));
    };
  }

  record Opaque(String segment) implements UrlPath {
    @Override
    public String toString() {
      return segment;
    }
  }
  record NonOpaque(List<String> segments) implements UrlPath {
    @Override
    public String toString() {
      StringBuilder output = new StringBuilder();

      for (String segment : segments) {
        output.append('/').append(segment);
      }

      return output.toString();
    }
  }
}
