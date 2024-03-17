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
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import com.tylerkindy.url.testdata.TestCase.Failure;
import com.tylerkindy.url.testdata.TestCase.Success;
import com.tylerkindy.url.testdata.TestCaseReader;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

class UrlTest {
  @Test
  void itParsesUrls() {
    assertThat(Url.parseOrThrow("https://example.com/foo").toString()).isEqualTo("https://example.com/foo");
  }

  @TestFactory
  Stream<DynamicTest> urlTestDataTests() {
    return TestCaseReader.testCases()
        .map(
            testCase -> {
              switch (testCase) {
                case Success success -> {
                  return dynamicTest(
                      success.name(),
                      () -> {
                        Url parsed =
                            success
                                .base()
                                .map(base -> Url.parseOrThrow(success.input(), Url.parseOrThrow(base)))
                                .orElseGet(() -> Url.parseOrThrow(success.input()));
                        assertThat(parsed.toString()).isEqualTo(success.href());
                      });
                }
                case Failure failure -> {
                  return dynamicTest(
                      failure.name(),
                      () -> {
                        Optional<Url> maybeBase = failure.base().map(Url::parseOrThrow);
                        assertThatExceptionOfType(ValidationException.class)
                            .isThrownBy(
                                () ->
                                    maybeBase
                                        .map(base -> Url.parseOrThrow(failure.input(), base))
                                        .orElseGet(() -> Url.parseOrThrow(failure.input())));
                      });
                }
              }
            });
  }
}
