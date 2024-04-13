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

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;

public sealed interface UrlPath {
  UrlPath copy();
  UrlPath shorten(String scheme);
  UrlPath append(String segment);
  boolean isEmpty();

  record Opaque(String segment) implements UrlPath {
    @Override
    public UrlPath copy() {
      return this;
    }

    @Override
    public UrlPath shorten(String scheme) {
      throw new AssertionError("Cannot shorten an opaque path");
    }

    @Override
    public UrlPath append(String segment) {
      return new Opaque(this.segment + segment);
    }

    @Override
    public boolean isEmpty() {
      throw new AssertionError("Opaque paths have no concept of empty");
    }

    @Override
    public String toString() {
      return segment;
    }
  }

  record NonOpaque(List<String> segments) implements UrlPath {

    @Override
    public UrlPath copy() {
      return new NonOpaque(new ArrayList<>(segments));
    }

    @Override
    public UrlPath shorten(String scheme) {
      if (
          scheme.equals("file") &&
              segments.size() == 1 &&
              CharacterUtils.isNormalizedWindowsDriveLetter(segments.get(0))
      ) {
        return this;
      }
      return segments.isEmpty() ? this : new NonOpaque(segments.subList(0, segments.size() - 1));
    }

    @Override
    public UrlPath append(String segment) {
      return new NonOpaque(ImmutableList.<String>builder().addAll(segments).add(segment).build());
    }

    @Override
    public boolean isEmpty() {
      return segments.isEmpty();
    }

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
