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

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.Range;
import org.junit.jupiter.api.Test;

class PercentEncodeSetTest {
  @Test
  void itContainsIndividualCodePoint() {
    PercentEncodeSet set = PercentEncodeSet.builder().addCodePoint('a').build();
    assertThat(set.contains('a')).isTrue();
  }

  @Test
  void itContainsRangeOfCodePoints() {
    PercentEncodeSet set =
        PercentEncodeSet.builder().addRange(Range.closed((int) 'b', (int) 'c')).build();

    assertThat(set.contains('a')).isFalse();
    assertThat(set.contains('b')).isTrue();
    assertThat(set.contains('c')).isTrue();
    assertThat(set.contains('d')).isFalse();
  }

  @Test
  void itDoesNotContainMissingCodePoint() {
    PercentEncodeSet set = PercentEncodeSet.builder().addCodePoint('a').build();
    assertThat(set.contains('b')).isFalse();
  }

  @Test
  void itUnionsRangesAndIndividualCodePoints() {
    PercentEncodeSet set =
        PercentEncodeSet.builder()
            .addRange(Range.closed((int) 'b', (int) 'c'))
            .addCodePoint('e')
            .build();

    assertThat(set.contains('a')).isFalse();
    assertThat(set.contains('b')).isTrue();
    assertThat(set.contains('c')).isTrue();
    assertThat(set.contains('d')).isFalse();
    assertThat(set.contains('e')).isTrue();
    assertThat(set.contains('f')).isFalse();
  }
}
