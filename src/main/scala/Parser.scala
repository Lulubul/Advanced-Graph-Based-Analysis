package main

import org.apache.spark.rdd.RDD
import scala.xml.XML
import scala.xml.Node
import scala.xml.NodeSeq
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
      val keywords = articlekwds.map(s => s.text.filter(_ >= ' ').trim.toLowerCase).mkString(", ")

      val affiliations = getAffiliations(articleFrontMeta)
      
      val pubYear = getPubYear(articleFrontMeta)
      
      keywords + "|" + pubYear + "|" + affiliations + "|" + subjects
    })
  }
  
  def getAffiliations(articleFrontMeta: NodeSeq) = {
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
                  affCity = getAffCity(affText(affText.length - 2))
                }
              }
              affCountry = contryNodeTextSplitted(0).trim
            }
          }
      } else {
        val affText = aff.text.split(",")
        if (affText.length > 1) {
          affCity = getAffCity(affText(affText.length - 2))
          
          affCountry = affText(affText.length - 1).replaceAll("\n", "").trim.replaceAll("[\\.\\;]$", "");
          if (affCountry.contains(".")) {
            affCountry = affCountry.split("\\.").head
          }
          affCountry = affCountry.split(" ").last.trim
        }
      }
      
      new { val city = affCity; val country = affCountry.replaceAll("[\\(\\)]", "") }
    }).toList

    //affs.map(a => a.city + " - " + a.country).distinct.mkString(", ")
    affs.map(a => a.country).distinct.mkString(", ")
  }
  
  def getAffCity(lastButOneAffText: String) = {
    lastButOneAffText.split(" ").filterNot(_.exists(_.isDigit)).mkString(" ").trim
  }
  
  def getPubYear(articleFrontMeta: NodeSeq) = {
    val pubDateNodes = (articleFrontMeta \ "pub-date")
    
    var pubDateNodeFiltered = pubDateNodes.filter(p => pubDateFilter(p \ "@pub-type" toString))
    if (pubDateNodeFiltered.isEmpty) {
      pubDateNodeFiltered = pubDateNodes.filter(p => pubDateFilter(p \ "@date-type" toString))
    }
    if (pubDateNodeFiltered.isEmpty) {
      pubDateNodeFiltered = pubDateNodes.filter(p => (p \ "@pub-type").toString == "epub")
    }
    if (!pubDateNodeFiltered.isEmpty) {
      pubDateNodeFiltered = pubDateNodeFiltered.head
    }
    (pubDateNodeFiltered \ "year").text.replaceAll("\n", "").trim
  }
  
  def pubDateFilter(value: String) = {
    value == "collection" || value == "ppub"
  }
}