package org.bdgenomics.adam.apis.java

import org.apache.avro.Schema
import org.apache.avro.generic.IndexedRecord
import org.apache.parquet.hadoop.metadata.CompressionCodecName
import org.apache.spark.rdd.RDD
import org.bdgenomics.utils.cli.SaveArgs
import org.bdgenomics.adam.rdd.ADAMContext._

// TODO: How to deal with view bounds on ADAMRDDFunctions?
class JavaADAMRDD[T <: IndexedRecord: Manifest](val rdd: RDD[T]) extends Serializable {

  def adamParquetSave(args: SaveArgs): Unit = {
    rdd.adamParquetSave(args)
  }

  def adamParquetSave(filePath: String,
                      blockSize: Int,
                      pageSize: Int,
                      compressCodec: CompressionCodecName,
                      disableDictionaryEncoding: Boolean,
                      schema: Option[Schema]): Unit = {
    rdd.adamParquetSave(
      filePath, blockSize, pageSize, compressCodec, disableDictionaryEncoding, schema)
  }

  def adamParquetSave(filePath: String): Unit = {
    rdd.adamParquetSave(filePath)
  }
}
