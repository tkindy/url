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

class PunycodeTest {

  @Test
  void itEncodes() {
    assertThat(Punycode.encode("\u0644\u064A\u0647\u0645\u0627\u0628\u062A\u0643\u0644\u0645\u0648\u0634\u0639\u0631\u0628\u064a\u061f"))
        .isEqualTo("egbpdaj6bu4bxfgehfvwxn");
    assertThat(Punycode.encode("\u4ed6\u4eec\u4e3a\u4ec0\u4e48\u4e0d\u8bf4\u4e2d\u6587"))
        .isEqualTo("ihqwcrb4cv8a8dqg056pqjye");
    assertThat(Punycode.encode("Pročprostěnemluvíčesky"))
        .isEqualTo("Proprostnemluvesky-uyb24dma41a");
    assertThat(Punycode.encode("PorquénopuedensimplementehablarenEspañol"))
        .isEqualTo("PorqunopuedensimplementehablarenEspaol-fmd56a");
  }

  @Test
  void itDecodes() {
    assertThat(Punycode.decode("egbpdaj6bu4bxfgehfvwxn"))
        .isEqualTo("\u0644\u064A\u0647\u0645\u0627\u0628\u062A\u0643\u0644\u0645\u0648\u0634\u0639\u0631\u0628\u064a\u061f");
    assertThat(Punycode.decode("ihqwcrb4cv8a8dqg056pqjye"))
        .isEqualTo("\u4ed6\u4eec\u4e3a\u4ec0\u4e48\u4e0d\u8bf4\u4e2d\u6587");
    assertThat(Punycode.decode("Proprostnemluvesky-uyb24dma41a"))
        .isEqualTo("Pročprostěnemluvíčesky");
    assertThat(Punycode.decode("PorqunopuedensimplementehablarenEspaol-fmd56a"))
        .isEqualTo("PorquénopuedensimplementehablarenEspañol");
  }
}
