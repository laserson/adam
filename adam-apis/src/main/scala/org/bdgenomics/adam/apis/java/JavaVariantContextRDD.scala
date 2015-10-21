package org.bdgenomics.adam.apis.java

import org.apache.spark.rdd.RDD
import org.bdgenomics.adam.models.VariantContext

class JavaVariantContextRDD(val rdd: RDD[VariantContext]) extends Serializable {

}
