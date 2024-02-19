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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Range;
import java.util.Set;
import java.util.function.Predicate;

final class PercentEncodeSet implements Predicate<Integer> {
  private final Set<Range<Integer>> ranges;
  private final Set<Integer> extras;

  private PercentEncodeSet(Set<Range<Integer>> ranges, Set<Integer> extras) {
    this.ranges = ranges;
    this.extras = extras;
  }

  public static Builder builder() {
    return new Builder();
  }

  @Override
  public boolean test(Integer codePoint) {
    return contains(codePoint);
  }

  public boolean contains(int codePoint) {
    return extras.contains(codePoint) || ranges.stream().anyMatch(range -> range.contains(codePoint));
  }

  static final class Builder {
    private final ImmutableSet.Builder<Range<Integer>> ranges;
    private final ImmutableSet.Builder<Integer> extras;

    Builder() {
      this.ranges = ImmutableSet.builder();
      this.extras = ImmutableSet.builder();
    }

    public Builder addAll(PercentEncodeSet other) {
      ranges.addAll(other.ranges);
      extras.addAll(other.extras);
      return this;
    }

    public Builder addRange(Range<Integer> range) {
      ranges.add(range);
      return this;
    }

    public Builder addCodePoint(char c) {
      return addCodePoint((int) c);
    }

    public Builder addCodePoint(int c) {
      extras.add(c);
      return this;
    }

    public PercentEncodeSet build() {
      return new PercentEncodeSet(ranges.build(), extras.build());
    }
  }
}
