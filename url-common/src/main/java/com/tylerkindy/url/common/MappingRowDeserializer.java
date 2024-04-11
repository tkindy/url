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

package com.tylerkindy.url.common;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.tylerkindy.url.common.MappingRow.WithMapping;
import com.tylerkindy.url.common.MappingRow.WithoutMapping;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class MappingRowDeserializer extends StdDeserializer<MappingRow> {

  public MappingRowDeserializer() {
    super(MappingRow.class);
  }

  @Override
  public MappingRow deserialize(JsonParser p, DeserializationContext ctxt)
      throws IOException, JacksonException {
    JsonNode row = p.getCodec().readTree(p);

    CodePoints codePoints = p.getCodec().treeToValue(row.get("codePoints"), CodePoints.class);
    Status status = p.getCodec().treeToValue(row.get("status"), Status.class);

    if (row.has("mapping") && !row.get("mapping").textValue().isEmpty()) {
      return new WithMapping(
          codePoints,
          status,
          parseMapping(row.get("mapping").textValue())
      );
    }

    return new WithoutMapping(codePoints, status);
  }

  private List<Integer> parseMapping(String mapping) {
    return Arrays.stream(mapping.split(" "))
        .map(codePoint -> Integer.parseInt(codePoint, 16))
        .toList();
  }
}
