# Estimating RNA editing levels using RNA-seq data and ADAM

In this example, we show how to estimate the levels of RNA editing in a given
`.bam` file. We will use the interactive scala shell.

/Users/laserson/repos/spark/bin/spark-shell \
    --master local[2] \
    --jars /Users/laserson/repos/adam/adam-cli/target/adam-0.12.1-SNAPSHOT.jar \
    --properties-file /Users/laserson/repos/adam/examples/adam-examples.conf

```scala
import org.apache.spark.rdd.RDD
import org.bdgenomics.adam.avro.ADAMRecord
import org.bdgenomics.adam.rdd.ADAMContext._
import org.bdgenomics.adam.rdd.features.ADAMFeaturesContext._
import org.bdgenomics.adam.models.SequenceDictionary
import org.bdgenomics.adam.models.SequenceRecord

val bamFile = "hdfs:///path/to/data.bam"
val siteFile = "hdfs:///path/to/sites.bed"

val bamFile = "/Users/laserson/repos/adam/adam-core/src/test/resources/artificial.sam"
val siteFile = "/Users/laserson/Dropbox/editing/data/Human_AG_all_hg19.bed"

// load editing sites and make into Features
val editingSites = sc.adamBEDFeatureLoad(siteFile)
val editingContigs = SequenceDictionary(editingSites.map(_.contig.getContigName.toString).distinct.collect.map(s => SequenceRecord(s, 0)))


// load reads and filter
val reads: RDD[ADAMRecord] = sc.adamLoad(bamFile)
val seqDict = reads.adamGetSequenceDictionary()
RegionJoin.partitionAndJoin(sc, )

val seqDict = reads.adamGetSequenceDictionary()
val pileups = reads.adamRecords2Pileup().groupBy(p => (p.getContig.getContigName, p.getPosition))

RegionJoin.partitionAndJoin(sc, seqDict, editingSites, pileups)



```