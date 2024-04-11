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
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.tylerkindy.url.common.CodePoints.Range;
import com.tylerkindy.url.common.CodePoints.Single;
import java.io.IOException;

public class CodePointsDeserializer extends StdDeserializer<CodePoints> {

public CodePointsDeserializer() {
  super(CodePoints.class);
}

  @Override
  public CodePoints deserialize(JsonParser p, DeserializationContext ctxt)
      throws IOException, JacksonException {
    String str = p.getValueAsString();
    String[] parts = str.split("\\.\\.");

    if (parts.length == 1) {
      return new Single(Integer.parseInt(parts[0], 16));
    }

    return new Range(Integer.parseInt(parts[0], 16), Integer.parseInt(parts[1], 16));
  }
}
