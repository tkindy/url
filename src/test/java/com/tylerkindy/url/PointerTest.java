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

import org.junit.jupiter.api.Test;

class PointerTest {
  @Test
  void itAdvancesOverAscii() {
    Pointer p = new Pointer("abcdef");

    p.increase();
    assertThat(p.getCurrentCodePoint()).isEqualTo('b');
  }

  @Test
  void itAdvancesOverSupplmentaryCharacters() {
    Pointer p = new Pointer("\uD800\uDC02\uD800\uDC14");

    p.increase();
    assertThat(p.getCurrentCodePoint())
        .isEqualTo(Character.toCodePoint('\uD800', '\uDC14'));
  }

  @Test
  void itIsEofFromStartOnEmptyString() {
    Pointer p = new Pointer("");
    assertThat(p.isEof()).isTrue();
  }

  @Test
  void itIsEofAfterAdvancingPastEndOfString() {
    Pointer p = new Pointer("ab");

    assertThat(p.isEof()).isFalse();

    p.increase();
    assertThat(p.isEof()).isFalse();

    p.increase();
    assertThat(p.isEof()).isTrue();
  }

  @Test
  void itExcludesCurrentCodePointFromRemaining() {
    assertThat(new Pointer("abc").doesRemainingStartWith("a"))
        .isFalse();
  }

  @Test
  void itFindsSingleCharacterPrefixOfRemaining() {
    assertThat(new Pointer("abc").doesRemainingStartWith("b"))
        .isTrue();
  }

  @Test
  void itFindsMultipleCharacterPrefixOfRemaining() {
    assertThat(new Pointer("abcd").doesRemainingStartWith("bc"))
        .isTrue();
  }

  @Test
  void itMatchesEntireRemaining() {
    assertThat(new Pointer("abc").doesRemainingStartWith("bc"))
        .isTrue();
  }

  @Test
  void itHandlesTooManyCharactersInRemainingPrefix() {
    assertThat(new Pointer("abc").doesRemainingStartWith("bcd"))
        .isFalse();
  }

  @Test
  void itResets() {
    Pointer p = new Pointer("abc");
    p.increase();
    p.reset();

    assertThat(p.getCurrentCodePoint()).isEqualTo('a');
  }

  @Test
  void itDecreases() {
    Pointer p = new Pointer("abc");
    p.increase();
    p.decrease();

    assertThat(p.getCurrentCodePoint()).isEqualTo('a');
  }
}
