package org.bdgenomics.adam.apis.java

import org.apache.spark.rdd.RDD
import org.bdgenomics.formats.avro.Genotype

class JavaGenotypeRDD(val rdd: RDD[Genotype]) extends Serializable {

}
