package org.bdgenomics.adam.apis.java

import org.apache.spark.rdd.RDD
import org.bdgenomics.formats.avro.Fragment

class JavaFragmentRDD(rdd: RDD[Fragment]) extends Serializable {

}
