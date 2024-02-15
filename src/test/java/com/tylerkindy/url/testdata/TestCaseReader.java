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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import java.io.IOException;
import java.util.List;
import java.util.stream.StreamSupport;

public class TestCaseReader {
  public static void main(String[] args) {
    ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new Jdk8Module());

    List<TestCase> testCases;
    try (var urlTestData = TestCaseReader.class.getResourceAsStream("/urltestdata.json")) {
      ArrayNode node = objectMapper.readValue(urlTestData, new TypeReference<>() {});
      testCases = StreamSupport.stream(node.spliterator(), false)
          .filter(ObjectNode.class::isInstance)
          .map(ObjectNode.class::cast)
          .map(o -> {
            o.remove("comment");
            o.remove("searchParams"); // TODO: test these?
            return objectMapper.convertValue(o, TestCase.class);
          })
          .toList();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    testCases.forEach(System.out::println);
  }
}
