/**
 * Licensed to Big Data Genomics (BDG) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The BDG licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.bdgenomics.adam.projections

import org.bdgenomics.formats.avro.Feature

/**
 * This enumeration exist in order to reduce typo errors in the code. It needs to be kept
 * in sync with any changes to Feature.
 *
 * This enumeration is necessary because Parquet needs the field string names
 * for predicates and projections.
 */
object FeatureField extends FieldEnumeration(Feature.SCHEMA$) {

  val featureId, featureType, source, contig, start, end, strand, value, dbxrefs, parentIds, attributes = SchemaValue
}
