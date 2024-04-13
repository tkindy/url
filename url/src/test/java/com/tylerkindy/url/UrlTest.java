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
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import com.google.common.io.Resources;
import com.tylerkindy.url.testdata.TestCase.Failure;
import com.tylerkindy.url.testdata.TestCase.Success;
import com.tylerkindy.url.testdata.TestCaseReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.assertj.core.api.Fail;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

class UrlTest {
  @Test
  void itParsesUrls() {
    assertThat(Url.parseOrThrow("https://example.com/foo").toString()).isEqualTo("https://example.com/foo");
  }

  // Pulled from the URL test data set to disable it
  @Test
  @Disabled("Java's standard UTF-8 encoder doesn't like unmatched surrogate pairs. Need to look into this deeper.")
  void itHandlesBogusSurrogatePairs() {
    assertThat(
        Url.parseOrThrow(
                "http://example.com/\uD800\uD801\uDFFE\uDFFF\uFDD0\uFDCF\uFDEF\uFDF0\uFFFE\uFFFF?\uD800\uD801\uDFFE\uDFFF\uFDD0\uFDCF\uFDEF\uFDF0\uFFFE\uFFFF"
            )
            .toString()
    )
        .isEqualTo(
            "http://example.com/%EF%BF%BD%F0%90%9F%BE%EF%BF%BD%EF%B7%90%EF%B7%8F%EF%B7%AF%EF%B7%B0%EF%BF%BE%EF%BF%BF?%EF%BF%BD%F0%90%9F%BE%EF%BF%BD%EF%B7%90%EF%B7%8F%EF%B7%AF%EF%B7%B0%EF%BF%BE%EF%BF%BF");
  }

  @TestFactory
  Stream<DynamicTest> urlTestDataTests() {
    return TestCaseReader.testCases()
        .map(
            testCase -> {
              if (testCase instanceof Success success) {
                return dynamicTest(
                    success.name(),
                    () -> {
                      UrlParseResult result = success
                          .base()
                          .map(base -> Url.parse(success.input(), Url.parseOrThrow(base)))
                          .orElseGet(() -> Url.parse(success.input()));

                      assertThat(result)
                          .withFailMessage(() -> "Expected '%s', but got %s".formatted(
                              success.href(),
                              result
                          ))
                          .isInstanceOf(UrlParseResult.Success.class);

                      assertThat(((UrlParseResult.Success) result).url().toString())
                          .isEqualTo(success.href());
                    });
              }
              if (testCase instanceof Failure failure) {
                return dynamicTest(
                    failure.name(),
                    () -> {
                      Optional<Url> maybeBase = failure.base().map(Url::parseOrThrow);

                      Url parsed;
                      try {
                        parsed = maybeBase
                            .map(base -> Url.parseOrThrow(failure.input(), base))
                            .orElseGet(() -> Url.parseOrThrow(failure.input()));
                      } catch (ValidationException e) {
                        // successful test
                        return;
                      }

                      Fail.fail("Expected parse failure, but got %s", parsed);
                    });
              }

              throw new IllegalStateException("Unknown TestCase class: " + testCase);
            });
  }

  @TestFactory
  Stream<DynamicTest> top100UrlsTests() {
    List<String> urlStrings;
    try {
      urlStrings = Resources.readLines(
          Resources.getResource("top100.txt"),
          StandardCharsets.UTF_8
      );
    } catch (IOException e) {
      throw new RuntimeException("Error reading top100.txt", e);
    }

    return urlStrings
        .stream()
        .map(urlString -> dynamicTest(urlString, () -> {
          UrlParseResult result = Url.parse(urlString.replaceAll("\\\\n", "\n"));

          assertThat(result)
              .withFailMessage(() -> "Expected '%s' to parse, but got %s".formatted(
                  urlString,
                  result
              ))
              .isInstanceOf(UrlParseResult.Success.class);
        }));
  }
}
