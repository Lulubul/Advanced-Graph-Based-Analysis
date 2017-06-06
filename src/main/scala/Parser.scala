package main

import org.apache.spark.rdd.RDD
import scala.xml.XML
import scala.xml.Node
import org.apache.hadoop.io.Text 
import scala.collection.mutable.ListBuffer

object Parser {
def parse(records: RDD[(Text, Text)]) = {
  val deHadoopedRecords = records.map(hadoopXML=>hadoopXML._1.toString)
	
  //Shows that multiple lines
  deHadoopedRecords.map(recordString=>{
    val recordXML = XML.loadString(recordString)
    val articleFrontMeta = (recordXML \ "metadata" \ "article" \ "front" \ "article-meta")
    val articleSubjects = (articleFrontMeta  \ "article-categories" \ "subj-group" \ "subject")
    val subjects = articleSubjects.map(s => s.text.filter(_ >= ' ').trim).mkString(", ")
    val articlekwds = (articleFrontMeta  \\ "kwd")
    val keywords = articlekwds.map(s => s.text.filter(_ >= ' ').trim).mkString(", ")
    
    var affNodes = (articleFrontMeta \\ "aff")
    
    val affs = affNodes.map(aff => {
      val contryNodeText = (aff \ "country").text.trim
      var affCity = ""
      var affCountry = ""
      
      if (!contryNodeText.isEmpty()) {
          val affAddr = (aff \ "addr-line").text.trim
          if (!affAddr.isEmpty) {
            affCity = affAddr
            affCountry = contryNodeText
          } else {
            val contryNodeTextSplitted = contryNodeText.split(", ", 2)
            if (contryNodeTextSplitted.length > 1) {
              affCity = contryNodeTextSplitted(0).trim
              affCountry = contryNodeTextSplitted(1).trim
            } else {
              if ((aff \ "institution").text.length > 0) {
                affCity = (aff \ "institution").text.split(", ").last
              } else {
                val affText = aff.text.split(",")
                if (affText.length > 1) {
                  affCity = affText(affText.length - 2).split(" ").filterNot(_.exists(_.isDigit)).mkString(" ").trim
                }
              }
              affCountry = contryNodeTextSplitted(0).trim
            }
          }
      } else {
        val affText = aff.text.split(",")
        if (affText.length > 1) {
          affCity = affText(affText.length - 2).split(" ").filterNot(_.exists(_.isDigit)).mkString(" ").trim
          
          affCountry = affText(affText.length - 1).replaceAll("\n", "").trim.replaceAll("[\\.\\;]$", "");
          if (affCountry.contains(".")) {
            affCountry = affCountry.split("\\.").head
          }
          affCountry = affCountry.split(" ").last.trim
        }
      }
      
      new {val city = affCity; val country = affCountry.replaceAll("[\\(\\)]", "")}
    }).toList
    //val affiliations = affs.filter(a => a.city.length() > 0 && a.country.length() > 0).map(a => a.city + ", " + a.country).mkString(", ")
    val affiliations = affs.filter(a => a.city.length() > 0 && a.country.length() > 0).map(a => a.city + "- " + a.country).distinct.mkString(", ")
    
    val pubDateNodes = (recordXML \ "metadata" \ "article" \ "front" \ "article-meta" \ "pub-date")
    var pubDateNodeFiltered = pubDateNodes.filter(p => pubDateFilter(p \ "@pub-type" toString))
    if (pubDateNodeFiltered.isEmpty) {
      pubDateNodeFiltered = pubDateNodes.filter(p => pubDateFilter(p \ "@date-type" toString))
    }
    
    val pubYear = (pubDateNodeFiltered \ "year").text
    
    keywords + "|" + pubYear + "|" + affiliations + "|" + subjects
  }).filter(r => {
    val recordSplitted = r.split('|')
    val kwds = recordSplitted(0)
    kwds.length > 0 && recordSplitted.length == 4
  })
}

def pubDateFilter(value: String) = {
  value == "collection" || value == "ppub"
}
}