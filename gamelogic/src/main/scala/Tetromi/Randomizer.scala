/*
 * TetromiNET Copyright (C) 2008-2009 Will Jordan.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * See <http://www.gnu.org/licenses/> for details.
 */

package Tetromi

/**
 * Implements various different random piece selection algorithms.
 * @author will
 * @date Oct 8, 2008 2:44:07 AM
 */
object Randomizer extends Enumeration { val Memoryless, Bag, BagPlusOne, DoubleBag, StrictHistory, History6 = Value
  def apply(randType: Value, seed: Long): Randomizer = {
    randType match {
      case Memoryless => new MemorylessRandomizer(seed)
      case Bag => new BagRandomizer(seed)
      case BagPlusOne => new BagPlusOneRandomizer(seed)
    }
  }
}

trait Randomizer {
  var pieceList = PieceType.elements.slice(0,PieceType.Empty.id).toList
  def apply = getNextPiece
  def getNextPiece : PieceType.Value
}

abstract class RandRandomizer(seed: Long) extends Randomizer {
  val rand = new Random(seed)
  def randomPieceType = PieceType.apply(rand.nextInt(pieceList.length))
}

class BagRandomizer (seed: Long) extends RandRandomizer(seed) {
  var currentPiece = 0
  def newBag = pieceList.sort((a,b)=>rand.nextBoolean)

  var bag = newBag
  def getNextPiece = {
    if(bag.isEmpty) bag = newBag
    val nextPiece = bag.head
    bag -= nextPiece
    nextPiece
  }
}

class MemorylessRandomizer(seed: Long) extends RandRandomizer(seed) {
  def getNextPiece = randomPieceType
}

class BagPlusOneRandomizer(seed: Long) extends BagRandomizer(seed) {
  pieceList = randomPieceType :: PieceType.elements.slice(0,PieceType.Empty.id).toList
}