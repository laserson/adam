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
package org.bdgenomics.adam.apis.java

import htsjdk.samtools.{ ValidationStringency, SAMFileHeader }
import org.apache.spark.api.java.JavaRDD
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.rdd.RDD
import org.apache.spark.storage.StorageLevel
import org.bdgenomics.adam.algorithms.consensus.{ ConsensusGeneratorFromReads, ConsensusGenerator }
import org.bdgenomics.adam.models._
import org.bdgenomics.adam.rdd.ADAMContext._
import org.bdgenomics.adam.rdd.ADAMSaveAnyArgs
import org.bdgenomics.adam.rdd.read.FlagStatMetrics
import org.bdgenomics.formats.avro._
import org.seqdoop.hadoop_bam.SAMRecordWritable

class JavaAlignmentRecordRDD(val rdd: RDD[AlignmentRecord]) extends Serializable {

  import JavaAlignmentRecordRDD._

  def filterByOverlappingRegion(query: ReferenceRegion): JavaAlignmentRecordRDD = {
    fromRDD(rdd.filterByOverlappingRegion(query))
  }

  def maybeSaveBam(args: ADAMSaveAnyArgs, isSorted: Boolean): Boolean = {
    rdd.maybeSaveBam(args, isSorted)
  }

  def maybeSaveBam(args: ADAMSaveAnyArgs): Boolean = {
    rdd.maybeSaveBam(args)
  }

  def adamAlignedRecordSave(args: ADAMSaveAnyArgs): Boolean = {
    rdd.adamAlignedRecordSave(args)
  }

  def adamSave(args: ADAMSaveAnyArgs, isSorted: Boolean): Boolean = {
    rdd.adamSave(args, isSorted)
  }

  def adamSave(args: ADAMSaveAnyArgs): Boolean = {
    rdd.adamSave(args)
  }

  def adamSAMString: String = {
    rdd.adamSAMString
  }

  def adamSAMSave(filePath: String, asSam: Boolean, asSingleFile: Boolean, isSorted: Boolean) = {
    rdd.adamSAMSave(filePath, asSam, asSingleFile, isSorted)
  }

  def adamSAMSave(filePath: String) = {
    rdd.adamSAMSave(filePath)
  }

  def getSequenceRecordsFromElement(elem: AlignmentRecord): Set[SequenceRecord] = {
    rdd.getSequenceRecordsFromElement(elem)
  }

  def adamGetReadGroupDictionary(): RecordGroupDictionary = {
    rdd.adamGetReadGroupDictionary()
  }

  def adamConvertToSAM(isSorted: Boolean): (JavaRDD[SAMRecordWritable], SAMFileHeader) = {
    val p = rdd.adamConvertToSAM(isSorted)
    new JavaRDD(p._1) -> p._2
  }

  def adamConvertToSAM(): (JavaRDD[SAMRecordWritable], SAMFileHeader) = {
    val p = rdd.adamConvertToSAM()
    new JavaRDD(p._1) -> p._2
  }

  def adamCountKmers(kmerLength: Int): JavaRDD[(String, Long)] = {
    rdd.adamCountKmers(kmerLength)
  }

  def adamSortReadsByReferencePosition(): JavaAlignmentRecordRDD = {
    fromRDD(rdd.adamSortReadsByReferencePosition())
  }

  def adamMarkDuplicates(): JavaAlignmentRecordRDD = {
    fromRDD(rdd.adamMarkDuplicates())
  }

  def adamBQSR(knownSnps: Broadcast[SnpTable], observationDumpFile: Option[String],
               validationStringency: ValidationStringency): JavaAlignmentRecordRDD = {
    fromRDD(rdd.adamBQSR(knownSnps, observationDumpFile, validationStringency))
  }

  def adamBQSR(knownSnps: Broadcast[SnpTable]): JavaAlignmentRecordRDD = {
    fromRDD(rdd.adamBQSR(knownSnps))
  }

  def adamRealignIndels(consensusModel: ConsensusGenerator = new ConsensusGeneratorFromReads,
                        isSorted: Boolean = false,
                        maxIndelSize: Int = 500,
                        maxConsensusNumber: Int = 30,
                        lodThreshold: Double = 5.0,
                        maxTargetSize: Int = 3000): JavaAlignmentRecordRDD = {
    fromRDD(rdd.adamRealignIndels(consensusModel, isSorted, maxIndelSize, maxConsensusNumber,
      lodThreshold, maxTargetSize))
  }

  def adamRealignIndels(): JavaAlignmentRecordRDD = {
    fromRDD(rdd.adamRealignIndels())
  }

  def adamFlagStat(): (FlagStatMetrics, FlagStatMetrics) = {
    rdd.adamFlagStat()
  }

  def adamSingleReadBuckets(): JavaRDD[SingleReadBucket] = {
    rdd.adamSingleReadBuckets()
  }

  def adamCharacterizeTags(): JavaRDD[(String, Long)] = {
    rdd.adamCharacterizeTags()
  }

  def adamCharacterizeTagValues(tag: String): Map[Any, Long] = {
    rdd.adamCharacterizeTagValues(tag)
  }

  def adamFilterRecordsWithTag(tagName: String): JavaAlignmentRecordRDD = {
    fromRDD(rdd.adamFilterRecordsWithTag(tagName))
  }

  def adamSaveAsPairedFastq(fileName1: String,
                            fileName2: String,
                            outputOriginalBaseQualities: Boolean,
                            validationStringency: ValidationStringency,
                            persistLevel: Option[StorageLevel]): Unit = {
    rdd.adamSaveAsPairedFastq(
      fileName1, fileName2, outputOriginalBaseQualities, validationStringency, persistLevel)
  }

  def adamSaveAsPairedFastq(fileName1: String, fileName2: String): Unit = {
    rdd.adamSaveAsPairedFastq(fileName1, fileName2)
  }

  def adamSaveAsFastq(fileName: String,
                      fileName2Opt: Option[String],
                      outputOriginalBaseQualities: Boolean,
                      sort: Boolean,
                      validationStringency: ValidationStringency,
                      persistLevel: Option[StorageLevel]): Unit = {
    rdd.adamSaveAsFastq(
      fileName, fileName2Opt, outputOriginalBaseQualities, sort, validationStringency, persistLevel)
  }

  def adamSaveAsFastq(fileName: String): Unit = {
    rdd.adamSaveAsFastq(fileName)
  }

  def adamRePairReads(secondPairRdd: JavaAlignmentRecordRDD,
                      validationStringency: ValidationStringency): JavaAlignmentRecordRDD = {
    fromRDD(rdd.adamRePairReads(secondPairRdd, validationStringency))
  }

  def adamRePairReads(secondPairRdd: JavaAlignmentRecordRDD): JavaAlignmentRecordRDD = {
    fromRDD(rdd.adamRePairReads(secondPairRdd))
  }

  def toFragments: JavaFragmentRDD = {
    new JavaFragmentRDD(rdd.toFragments)
  }

  def adamGetSequenceDictionary(performLexSort: Boolean): SequenceDictionary = {
    rdd.adamGetSequenceDictionary(performLexSort)
  }

  def adamGetSequenceDictionary(): SequenceDictionary = {
    rdd.adamGetSequenceDictionary()
  }
}

object JavaAlignmentRecordRDD {
  def fromRDD(rdd: RDD[AlignmentRecord]): JavaAlignmentRecordRDD = new JavaAlignmentRecordRDD(rdd)

  implicit def toRDD(rdd: JavaAlignmentRecordRDD): RDD[AlignmentRecord] = rdd.rdd
}
