/**
 * Copyright (C) 2016 Bernhard Walter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.betaocean.audiofingerprint

import java.io.File
import org.apache.commons.io.FileUtils
import com.owlike.genson.{ScalaGenson, GensonBuilder, ScalaBundle}
import java.nio.charset.StandardCharsets
import scala.collection.mutable


case class Entry(offset:Int, sid: Int) { override def toString() = s"(${sid} -> ${offset})" }
case class DB(fingerprints: mutable.Map[String, List[Entry]], var sounds:Array[String])

object Database {
  
  private val dataStr = FileUtils.readFileToString(new File("db.json"), StandardCharsets.UTF_8)
  val genson = new GensonBuilder().withBundle(ScalaBundle()).useIndentation(true).create()
  
  // Load database from JSON file
  val data = genson.fromJson[DB](dataStr)
  var keySet = data.fingerprints.keySet

  /**
   * Dump database
   */  
  def dump = {
    println("\nFINGERPRINTS:")
    data.fingerprints.foreach { case (k,v) => println(s"${k} => ${v.mkString(", ")}") }

    println("\nSOUNDS:")
    for (i <- 0 until data.sounds.length) println(s"${i}: ${data.sounds(i)}")
  }


  /**
   * Save database as JSON file
   */  
  def save = {
    FileUtils.writeStringToFile(new File("db.json"), genson.toJson(data), StandardCharsets.UTF_8, false)
  }

  
  /**
   * Insert a fingerprint into the database
   * 
   * @param fingerprints Fingerprints as an iterator
   */  
  def insert(fingerprints: Iterator[Tuple2[String, Int]], filename: String) = {
    data.sounds = data.sounds :+ filename
    val sid = data.sounds.length - 1

    fingerprints.foreach { fingerprint => 
      if (data.fingerprints.get(fingerprint._1) == None) {
        data.fingerprints(fingerprint._1) = List[Entry]()
      }
      data.fingerprints(fingerprint._1) = data.fingerprints(fingerprint._1) :+ Entry(fingerprint._2, sid)
    }
    save
    keySet = data.fingerprints.keySet
  }


  /**
   * Insert a fingerprint into the database
   * 
   * @param fingerprints Fingerprints of a chunk of music as an iterator
   * @return Tuple of ("filename of sound", "occurences") or ("None", 0)
   */  
  def findMatches(fingerprints: Iterator[Tuple2[String, Int]]): Tuple2[String, Double] = {
    val fingerprintMap = fingerprints.toList.groupBy(_._1).mapValues(_.map(_._2))
    val keys = fingerprintMap.keySet
    val commonKeys = keySet.intersect(keys).toList
    
    val sidDists = commonKeys.flatMap {key =>
      for (
        entry <- data.fingerprints(key); 
        offset <- fingerprintMap(key)
      ) yield ((entry.sid, entry.offset - offset), 1)   // List of distances enhanced by 1 for counting ...
    }
    val distRanking = sidDists.groupBy(_._1)                          // ... grouped by (sid, dist) ...
                              .mapValues(_.map(_._2).reduce(_ + _))   // ... and counted

    if (distRanking.isEmpty)
      ("None", 0) 
    else {
      val result = distRanking.maxBy(_._2)              // this is the song with the highest count for a dist
      (data.sounds(result._1._1), 1.0 * result._2 / sidDists.length)
    }
  }
}


 