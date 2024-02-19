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

public sealed interface ValidationError {
  record InvalidUrlUnit(String message) implements ValidationError {}
  record SpecialSchemeMissingFollowingSolidus() implements ValidationError {}
  record MissingSchemeNonRelativeUrl() implements ValidationError {}
  record InvalidReverseSolidus() implements ValidationError {}
  record InvalidCredentials() implements ValidationError {}
  record HostMissing() implements ValidationError {}
}
