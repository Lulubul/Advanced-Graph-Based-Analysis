package main

/*import org.apache.spark.mllib.clustering.{KMeans, KMeansModel}
import org.apache.spark.mllib.linalg.Vectors*/
import org.apache.spark.{SparkConf, SparkContext}

object Evaluator {
  def main(args: Array[String]) {
    val conf = new SparkConf().setAppName("Evaluator")
    val sc = new SparkContext(conf)
    
    val records = HadoopRDDGenerator.createUsing(sc, withPath = args(0))

    val rawRecords = Parser.parse(records).collect
    
    val kwdsOccurrence =  rawRecords.map(r => {
      r.split('|')(0).split(", ")
    }).toList.flatten.groupBy(identity).mapValues(_.size).filterNot(_._1 matches "(Iran|China)" )
    
    /*
    //sort by comparing values
    val topKwds = kwdsOccurrence.toList.sortBy(-_._2).take(50).map(_._1)
    
    val recordsList = rawRecords.map(r => {
      val recordSplitted = r.split('|')
      val kwds = recordSplitted(0).split(", ").filter(k => topKwds.contains(k))
      val pubYear = recordSplitted(1)
      val affs = recordSplitted(2)
      val subs = recordSplitted(3)
      kwds.map(k => new {val kwd = k; val year = pubYear; val affiliations = affs; val subjects = subs})
    }).toList.flatten*/
    
    
    
    val recordsList = rawRecords.map(r => {
      val recordSplitted = r.split('|')
      val pubYear = recordSplitted(1)
      val affs = recordSplitted(2)
      val subs = recordSplitted(3)
      affs.split(", ").map(k => new { val year = pubYear; val affiliation = k; val subjects = subs })
    }).toList.flatten
    
    
    val sortedRecords = recordsList.map(r => {
      val subjects = r.subjects.split(", ")
      subjects.map(k => new { val year = r.year; val affiliation = r.affiliation; val subjects = k })
    }).toList.flatten.sortBy(r => r.year).groupBy(r => r.affiliation)  
    
    
    val top20Affiliations = sortedRecords.toSeq.sortBy(r => - r._2.map(x => x.subjects.size).sum).take(20)
    
	  //val recordsByCategory = rawRecords.groupBy(r => r.category)
  	/*val categories = rawRecords.map(s => s(1)).distinct().zipWithIndex.collectAsMap
  	
  	val parsedData = rawRecords.map(s => Vectors.dense(Array(s(0).toDouble, categories.get(s(1)).get.toDouble))).cache()
  	
  	val numClusters = 2
  	val numIterations = 20
  	val clusters = KMeans.train(parsedData, numClusters, numIterations)
  	
  	val WSSSE = clusters.computeCost(parsedData)
    println("Within Set Sum of Squared Errors = " + WSSSE)
    
    val vectors = parsedData.collect()
    vectors.map(v => println(clusters.predict(v)+" "+v.toString))
  
    sc.stop()*/
  
    top20Affiliations.foreach(r => println(r._1 + "|" + r._2.map(x => x.year + "|" + x.subjects + ": " + x.subjects.size).distinct))
    //recordsList.foreach(r => println(r.year + "|" + r.subjects + "|" + r.affiliations ))
    //rawRecords.foreach(println)
    //println(topKwds)
  }
}