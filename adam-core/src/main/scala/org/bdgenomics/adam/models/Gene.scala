/*
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
package org.bdgenomics.adam.models

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import org.apache.spark.SparkContext._
import org.bdgenomics.adam.rich.ReferenceMappingContext.FeatureReferenceMapping
import org.bdgenomics.formats.avro.{ Strand, Feature }

import scala.collection.JavaConversions._
import GeneFeatureRDD._

/**
 * A 'gene model' is a small, hierarchical collection of objects: Genes, Transcripts, and Exons.
 * Each Gene contains a collection of Transcripts, and each Transcript contains a collection of
 * Exons, and together they describe how the genome is transcribed and translated into a family
 * of related proteins (or other RNA products that aren't translated at all).
 *
 * This review,
 * Gerstein et al. "What is a gene, post-ENCODE? History and updated definition" Genome Research (2007)
 * http://genome.cshlp.org/content/17/6/669.full
 *
 * is a reasonably good overview both of what the term 'gene' has meant in the past as well as
 * where it might be headed in the future.
 *
 * Here, we aren't trying to answer any of these questions about "what is a gene," but rather to
 * provide the routines necessary to _re-assemble_ hierarchical models of genes that have been
 * flattened into features (GFF, GTF, or BED)
 *
 * @param id A name, presumably unique within a gene dataset, of a Gene
 * @param names Common names for the gene, possibly shared with other genes (for historical or
 *              ad hoc reasons)
 * @param strand The strand of the Gene (this is from data, not derived from the Transcripts' strand(s), and
 *               we leave open the possibility that a single Gene will have Transcripts in _both_ directions,
 *               e.g. anti-sense transcripts)
 * @param transcripts The Transcripts that are part of this gene model
 */
case class Gene(id: String, names: Seq[String], strand: Boolean, transcripts: Iterable[Transcript]) {

  /**
   * Finds the union of all the locations of the transcripts for this gene,
   * across all the reference sequences indicates by the transcripts themselves.
   * @return A Seq of ReferenceRegions
   */
  lazy val regions =
    ReferenceUtils.unionReferenceSet(transcripts.map(_.region)).toSeq

}

/**
 * A transcript model (here represented as a value of the Transcript class) is a simple,
 * hierarchical model containing a collection of exon models as well as an associated
 * gene identifier, transcript identifier, and a set of common names (synonyms).
 *
 * @param id the (unique) identifier of the Transcript
 * @param names Common names for the transcript
 * @param geneId The (unique) identifier of the gene to which the transcript belongs
 * @param exons The set of exons in the transcript model; each of these contain a
 *              reference region whose coordinates are in genomic space.
 * @param cds the set of CDS regions (the subset of the exons that are coding) for this
 *            transcript
 * @param utrs
 */
case class Transcript(id: String,
                      names: Seq[String],
                      geneId: String,
                      strand: Boolean,
                      exons: Iterable[Exon],
                      cds: Iterable[CDS],
                      utrs: Iterable[UTR]) {

  lazy val region = exons.map(_.region).reduceLeft[ReferenceRegion]((acc, ex) => acc.hull(ex))

  def extractTranscribedRNASequence(refSequence: String)(implicit alphabet: Alphabet): String = {
    val minStart = exons.map(_.region.start).toSeq.sorted.head.toInt
    val maxEnd = -exons.map(-_.region.end).toSeq.sorted.head.toInt
    if (strand)
      refSequence.substring(minStart, maxEnd)
    else
      alphabet.reverseComplement(refSequence.substring(minStart, maxEnd))
  }

  def extractCodingSequence(refSequence: String): String = {
    val builder = new StringBuilder()
    val cdses =
      if (strand)
        cds.toSeq.sortBy(_.region.start)
      else
        cds.toSeq.sortBy(_.region.start).reverse

    cdses.foreach(cds => builder.append(cds.extractSequence(refSequence)))
    builder.toString()
  }

  def extractSplicedmRNASequence(referenceSequence: String): String = {
    val builder = new StringBuilder()
    val exs =
      if (strand)
        exons.toSeq.sortBy(_.region.start)
      else
        exons.toSeq.sortBy(_.region.start).reverse

    exs.foreach(exon => builder.append(exon.extractSequence(referenceSequence)))
    builder.toString()
  }

}

/**
 * A trait for values (usually regions or collections of regions) that can be
 * subsetted or extracted out of a larger region string -- for example, exons
 * or transcripts which have a sequence defined in terms of their coordinates
 * against a reference chromosome.  Passing the sequence of the reference
 * chromosome to a transcript's 'extractSequence' method will return the
 * sequence of the transcript.
 */
trait Extractable {

  /**
   * Returns the subset of the given referenceSequence that corresponds to this
   * Extractable (not necessarily a contiguous substring, and possibly reverse-
   * complemented or transformed in other ways).
   *
   * @param referenceSequence The reference sequence (e.g. chromosomal sequence) with
   *                          respect to which the Extractable's coordinates or locators
   *                          are given
   * @return the String representation of this Extractable
   */
  def extractSequence(referenceSequence: String): String
}

abstract class BlockExtractable(strand: Boolean, region: ReferenceRegion, alphabet: Alphabet = DNAAlphabet)
    extends Extractable {

  def extractSequence(referenceSequence: String): String =
    if (strand)
      referenceSequence.substring(region.start.toInt, region.end.toInt)
    else
      alphabet.reverseComplement(referenceSequence.substring(region.start.toInt, region.end.toInt))
}

/**
 * An exon model (here represented as a value of the Exon class) is a representation of a
 * single exon from a transcript in genomic coordinates.
 *
 * NOTE: we're not handling shared exons here
 *
 * @param transcriptId the (unique) identifier of the transcript to which the exon belongs
 * @param region The region (in genomic coordinates) to which the exon maps
 */
case class Exon(exonId: String, transcriptId: String, strand: Boolean, region: ReferenceRegion)
    extends BlockExtractable(strand, region) {
}

/**
 * Coding Sequence annotations, should be a subset of an Exon for a particular Transcript
 * @param transcriptId
 * @param strand
 * @param region
 */
case class CDS(transcriptId: String, strand: Boolean, region: ReferenceRegion)
    extends BlockExtractable(strand, region) {
}

/**
 * UnTranslated Regions
 *
 * @param transcriptId
 * @param strand
 * @param region
 */
case class UTR(transcriptId: String, strand: Boolean, region: ReferenceRegion)
    extends BlockExtractable(strand, region) {
}

object GeneContext {
  implicit def sparkContextToGeneContext(sc: SparkContext): GeneContext = new GeneContext(sc)
}

class GeneContext(sc: SparkContext) {

  // This import has to go here and not at the top level, because there's a
  // conflict with another implicit used in asGenes, below.
  import org.bdgenomics.adam.rdd.ADAMContext._

  def loadGTFGenes(filename: String): RDD[Gene] = {
    val features: RDD[Feature] = sc.adamLoad(filename)
    features.asGenes()
  }
}

object GeneFeatureRDD {
  implicit def convertBaseFeatureRDDToGeneFeatureRDD(rdd: RDD[Feature]): GeneFeatureRDD =
    new GeneFeatureRDD(rdd)

  def strand(str: Strand): Boolean =
    str match {
      case Strand.Forward     => true
      case Strand.Reverse     => false
      case Strand.Independent => true
    }
}

class GeneFeatureRDD(featureRDD: RDD[Feature]) extends Serializable {

  //  /*
  //  TODO(Timothy) this is a hack
  //   */
  //  def feature2Region(f: Feature): ReferenceRegion =
  //    new GTFFeature(f).region

  def asGenes(): RDD[Gene] = {

    /*
    Creating a set of gene models works in four steps:

    1. Key each GTFFeature by its featureType (which should be either 'gene',
       'transcript', or 'exon').

    2. Take all the values of type 'exon', and create Exon objects from them.
       Key this RDD by transcriptId of each exon, and group all exons of the same
       transcript together. Also, do the same for the 'cds' features.

    3. Take all the values of type 'transcript', key them by their transcriptId,
       and join them with the cds and exons from step #2.  This should give each us
       enough information to create each Transcript, which we do, key each Transcript
       by its geneId, and group the transcripts which share a common gene together.

    4. Finally, find each 'gene'-typed GTFFeature, key it by its geneId, and join with
       the transcripts in #3.  Use these joined values to create the final set of
       Gene values.

    The three groupBys and two joins are necessary for creating the two-level hierarchical
    tree-structured Gene models, under the assumption that the rows of the GTF file itself
    aren't ordered.
     */

    // Step #1
    val typePartitioned: RDD[(String, Feature)] =
      featureRDD.keyBy(_.getFeatureType.toString).cache()

    // Step #2
    val exonsByTranscript: RDD[(String, Iterable[Exon])] =
      typePartitioned.filter(_._1 == "exon").flatMap {
        // There really only should be _one_ parent listed in this flatMap, but since
        // getParentIds is modeled as returning a List[], we'll write it this way.
        case ("exon", ftr: Feature) =>
          val ids: Seq[String] = ftr.getParentIds.map(_.toString)
          ids.map(transcriptId => (transcriptId,
            Exon(ftr.getFeatureId.toString, transcriptId, strand(ftr.getStrand), FeatureReferenceMapping.getReferenceRegion(ftr))))
      }.groupByKey()

    val cdsByTranscript: RDD[(String, Iterable[CDS])] =
      typePartitioned.filter(_._1 == "CDS").flatMap {
        case ("CDS", ftr: Feature) =>
          val ids: Seq[String] = ftr.getParentIds.map(_.toString)
          ids.map(transcriptId => (transcriptId,
            CDS(transcriptId, strand(ftr.getStrand), FeatureReferenceMapping.getReferenceRegion(ftr))))
      }.groupByKey()

    // Step #3
    val transcriptsByGene: RDD[(String, Iterable[Transcript])] =
      typePartitioned.filter(_._1 == "transcript").map {
        case ("transcript", ftr: Feature) => (ftr.getFeatureId.toString, ftr)
      }.join(exonsByTranscript)
        .join(cdsByTranscript)
        .flatMap {
          // There really only should be _one_ parent listed in this flatMap, but since
          // getParentIds is modeled as returning a List[], we'll write it this way.
          case (transcriptId: String, ((tgtf: Feature, exons: Iterable[Exon]), cds: Iterable[CDS])) =>
            val geneIds: Seq[String] = tgtf.getParentIds.map(_.toString) // should be length 1
            geneIds.map(geneId => (geneId,
              Transcript(transcriptId, Seq(), geneId, strand(tgtf.getStrand),
                exons, cds, Seq())))
        }.groupByKey()

    // Step #4
    val genes = typePartitioned.filter(_._1 == "gene").map {
      case ("gene", ftr: Feature) => (ftr.toString, ftr)
    }.join(transcriptsByGene).map {
      case (geneId: String, (ggtf: Feature, transcripts: Iterable[Transcript])) =>
        Gene(geneId, Seq(), strand(ggtf.getStrand), transcripts)
    }

    genes
  }
}

object ReferenceUtils {

  def unionReferenceSet(refs: Iterable[ReferenceRegion]): Iterable[ReferenceRegion] = {

    def folder(acc: Seq[ReferenceRegion], tref: ReferenceRegion): Seq[ReferenceRegion] =
      acc match {
        case Seq() => Seq(tref)
        case (first: ReferenceRegion) +: Seq(rest) =>
          if (first.overlaps(tref))
            first.hull(tref) +: Seq(rest)
          else
            tref +: first +: Seq(rest)
      }

    refs.toSeq.sorted.foldLeft(Seq[ReferenceRegion]())(folder)
  }
}

