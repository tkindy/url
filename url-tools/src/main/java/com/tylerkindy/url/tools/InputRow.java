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

package com.tylerkindy.url.tools;

import com.tylerkindy.url.common.CodePoints;
import com.tylerkindy.url.common.Status;
import java.util.List;

sealed interface InputRow {
  CodePoints codePoints();
  Status status();

  record WithoutMapping(CodePoints codePoints, Status status) implements InputRow {}
  record WithMapping(
      CodePoints codePoints,
      Status status,
      List<Integer> mapping
  ) implements InputRow {}
}