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

import scala.collection.JavaConverters._
import java.io.{File,FileInputStream}
import java.util.Properties
import com.musicg.wave.Wave
import nu.pattern.OpenCV
import org.opencv.core.{Core,Mat,Size,Point,Scalar,CvType}
import org.opencv.imgproc.Imgproc



object AudioFingerprint {

  OpenCV.loadShared()
  System.loadLibrary(Core.NATIVE_LIBRARY_NAME)

  val props = new Properties
  val fileStream = new FileInputStream(new File("config.properties"))
  props.load(fileStream)
  fileStream.close()
  val config = props.asScala.toMap.map { case (k,v) => (k,v.toInt) }
  
  
  /**
   * Calculate spectrogram of a WAV file and get the peaks of it
   * 
   * @param wave decoded WAV file (PCM 16 Bit 44100)
   * @return peaks
   */  
  def calculatePeaks(wave: Wave): Array[Tuple2[Int, Int]] = {
    val waveHeader = wave.getWaveHeader
    val bytePerSample = waveHeader.getBitsPerSample / 8
    
    val spec = wave.getSpectrogram(config("wsize"), config("overlapFactor"))
    // Convert to OpenCV Mat
    val specMat = new Mat(spec.getNumFrames, spec.getNumFrequencyUnit, CvType.CV_32FC1)
    for (frameIdx <- 0 until spec.getNumFrames; freqIdx <- 0 until spec.getNumFrequencyUnit)
      specMat.put(frameIdx, freqIdx, 100.0 * spec.getNormalizedSpectrogramData()(frameIdx)(freqIdx))
    
    val dilated = new Mat()
    val dilation = 2 * config("dilationSize") + 1
    val element = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(dilation, dilation))

    Imgproc.dilate(specMat, dilated, element, new Point(-1,-1), 1)

    for (
      frameIdx <- 0 until specMat.height toArray;  
        freqIdx  <- 0 until specMat.width toArray;
          s = specMat.get(frameIdx, freqIdx)(0);
          d = dilated.get(frameIdx, freqIdx)(0);
      if ((s == d) && (s > config("ampMin")))
    ) yield (frameIdx, freqIdx)
  }


  /**
   * Convert peaks to fingerprints
   * 
   * @param peaks Peaks as calculated by 'calculatePeaks'
   * @return fingerprints as an iterator of Strings
   */  
  def generateFingerprints(peaks: Array[Tuple2[Int, Int]]): Iterator[Tuple2[String, Int]] = {
    for (
      i <- (0 until peaks.length).iterator; 
        j <- (1 until math.min(config("fanValue"), peaks.length - i)).iterator;
          frame1 = peaks(i)._1;   frame2 = peaks(i + j)._1;
          freq1 = peaks(i)._2;    freq2 = peaks(i + j)._2;
          frameDelta = frame2 - frame1;
      if ((frameDelta >= config("minFrameDelta")) && (frameDelta <= config("maxFrameDelta")))
    ) yield (s"${freq1}|${freq2}|${frameDelta}", frame1)
  }


  /**
   * Load WAV file and create fingerprints
   * 
   * @param filename Name of a WAV file
   * @return fingerprints as an iterator of Strings
   */  
  def analyzeFile(filename:String): Iterator[Tuple2[String, Int]]  = {
    println(s"Fingerprinting ${filename}")
    val wave = new Wave(filename)
    val musicgFingerprint = wave.getFingerprint

    generateFingerprints(calculatePeaks(wave))
  }


  /**
   * Main program
   *
   * Command line arguments
   * "-d"                        Dump Databse
   * "-f <list of wav files>"    Add fingerprints of wav files to database
   * "-r <wav chunk>"            Recognize a wav chunk by matching with fingerprints in databse
   * 
   * @param args Command line options
   */  
  def main(args: Array[String]) = {

    val db = com.betaocean.audiofingerprint.Database

    args(0) match {

      // Dump Database
      case "-d" => db.dump

      // Fingerprint one or more files into databse
      case "-f" => {
        args.slice(1, args.length).foreach { filename =>
          val fingerprints = analyzeFile(filename)
          db.insert(fingerprints, filename)
        }
      }

      // Recognize a WAV file which is hopefully a chunk of a sound in the database
      case "-r" => {
        args.slice(1, args.length).foreach { filename =>
          val fingerprints = analyzeFile(filename)
          println(db.findMatches(fingerprints))
        }
      }

      case _ => println("Wrong paramter")
    }
  }  
}
