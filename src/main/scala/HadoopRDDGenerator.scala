package main

import org.apache.spark.SparkContext
import org.apache.hadoop.streaming.StreamXmlRecordReader
import org.apache.hadoop.streaming.StreamInputFormat
import org.apache.hadoop.mapred.JobConf
import org.apache.hadoop.mapred.FileInputFormat
import org.apache.hadoop.io.Text 

object HadoopRDDGenerator{
  def createUsing(sc: SparkContext, withPath: String) = {
    val jobConf = new JobConf()
    jobConf.set("stream.recordreader.class", "org.apache.hadoop.streaming.StreamXmlRecordReader")
    jobConf.set("stream.recordreader.begin", "<record>")
    jobConf.set("stream.recordreader.end", "</record>")
    FileInputFormat.addInputPaths(jobConf, withPath) 

    sc.hadoopRDD(jobConf, classOf[StreamInputFormat], classOf[Text], classOf[Text])
  }
}