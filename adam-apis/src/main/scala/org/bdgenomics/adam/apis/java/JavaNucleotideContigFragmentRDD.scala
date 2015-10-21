package org.bdgenomics.adam.apis.java

import org.apache.spark.rdd.RDD
import org.bdgenomics.formats.avro.NucleotideContigFragment

class JavaNucleotideContigFragmentRDD(val rdd: RDD[NucleotideContigFragment]) extends Serializable {

}
