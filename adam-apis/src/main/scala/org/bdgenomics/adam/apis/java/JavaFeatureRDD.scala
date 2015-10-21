package org.bdgenomics.adam.apis.java

import org.apache.spark.rdd.RDD
import org.bdgenomics.formats.avro.Feature

class JavaFeatureRDD(val rdd: RDD[Feature]) extends Serializable {

}
