package main

/*import org.apache.spark.mllib.clustering.{KMeans, KMeansModel}
import org.apache.spark.mllib.linalg.Vectors*/
import org.apache.spark.{SparkConf, SparkContext}

object Evaluator {
  def main(args: Array[String]) {
    val conf = new SparkConf().setAppName("Evaluator")
    val sc = new SparkContext(conf)
    
    val records = HadoopRDDGenerator.createUsing(sc, withPath = args(0))

    val totalRawRecords = Parser.parse(records).collect
    
    val rawRecords = totalRawRecords.filter(r => {
      val recordSplitted = r.split('|')
      val kwds = recordSplitted(0)
      kwds.length > 0 && recordSplitted.length == 4
    })
    
    val topKwdsOcc = rawRecords.map(r => {
      val recordSplitted = r.split('|')
      val kwds = recordSplitted(0)
      kwds.split(", ")
    }).toList.flatten.groupBy(identity).mapValues(_.size).filterNot(_._1 matches "(Iran|China)")
    
    val topKwds = topKwdsOcc.toSeq.sortBy(-_._2).take(10)
    println("\nTop Keywords:")
    topKwds.foreach(k => println(k._1 + ": " + k._2))
        
    var recordsByAff = rawRecords.map(r => {
      val recordSplitted = r.split('|')
      val kwds = recordSplitted(0)
      val pubYear = recordSplitted(1)
      val affs = recordSplitted(2)
      val subs = recordSplitted(3)
      affs.split(", ").map(a => {
        /*var aff = a
        if (a.split("-")(0).trim.length == 0) { // no City
          aff = a.replaceAll("-", "").trim
        }*/
        new {
          val keywords = kwds
          val year = pubYear
          val affiliation = a
          val subjects = subs
        }
      })
    }).toList.flatten.groupBy(a => a.affiliation)
    
    recordsByAff.filterNot(a => { val aff = a._1.trim; aff.isEmpty() && aff.length < 2 })
    
    val sortedAffiliations = recordsByAff.toSeq.sortBy(aff => { val affRecords = aff._2; - affRecords.length })
    
    val topAffiliations = sortedAffiliations.take(20)

    topAffiliations.foreach(
        aff => {
          val affiliation = aff._1
          val records = aff._2
          
          println("\nAffiliation: " + affiliation + " | Records: " + records.length)
          
          val keywordsOcc = records.map(m => {
            m.keywords.split(", ")
          }).toList
          .flatten
          .groupBy(identity)
          .mapValues(_.size)
          .filterNot(_._1 matches "(Iran|China|Korea|Germany|India|Japan|Japanese|Italy|Canada|Australia|France|Brazil|Spain|Sweden)")
          
          val keywordsByOcc = keywordsOcc.toSeq.sortBy(-_._2)
          
          val tenthKeywordOcc = keywordsByOcc.take(10).last._2
          val topKeywords = keywordsByOcc.takeWhile(k => {
            val keywordOcc = k._2
            keywordOcc >= tenthKeywordOcc && keywordOcc > 1
          })
          val topKeywordsValues = topKeywords.map(_._1)
          
          println("Top keywords: ")
          topKeywords.foreach(k => {
            val keyword = k._1
            val occurrence = k._2
            println(keyword + ": " + occurrence)
          })
          
          val recordsByYear = records.groupBy(r => r.year)
          val recordsByYearSorted = recordsByYear.toSeq.sortBy(r => {val year = r._1; year})
          recordsByYearSorted.foreach(              
              r => {
                val year = r._1
                val recordsMeta = r._2
                
                val kwdsOcc = recordsMeta.map(m => {
                  m.keywords.split(", ")
                }).toList.flatten.groupBy(identity).mapValues(_.size)
                val kwdsByOcc = kwdsOcc.toSeq.sortBy(-_._2)
                val topKwds = kwdsByOcc.filter(k => topKeywordsValues.contains(k._1))
                
                if (topKwds.length > 0) {
                  println("\tYear: " + year + " | Records: " + recordsMeta.length)
                  
                  println("\t\tKeywords: ")
                  topKwds.foreach(k => {
                    val keyword = k._1
                    val occurrence = k._2
                    println("\t\t\t" + keyword + ": " + occurrence)
                  })
                }
              }
          )
          println("\n-----------------------------------------------------------------------")
        }
    )
    println("totalRawRecords: " + totalRawRecords.length + " rawRecords: " + rawRecords.length)
  }
}