package pulpcore.scala

// Don't import Int - Scala will be confused
import pulpcore.animation.Fixed
import pulpcore.animation.Bool
import pulpcore.animation.Color
import pulpcore.animation.Easing
import pulpcore.animation.Tween
import pulpcore.math.CoreMath

/**
  Contains implicit defs. Most code will use "import PulpCore._"
*/
object PulpCore {
  
  // Convert properties to numbers
  implicit def Int2int(v: pulpcore.animation.Int):Int = v.get
  implicit def Fixed2double(v: Fixed):Double = v.get
  implicit def Bool2bool(v: Bool):Boolean = v.get
  implicit def Color2int(v: Color):Int = v.get
  
  // Add methods to existing properties
  implicit def Fixed2RichFixed(prop: Fixed) = new RichFixed(prop)
  implicit def Int2RichInt2(prop: pulpcore.animation.Int) = new RichInt2(prop)
  
  // Convert to Tweens
  implicit def Tuple2RichFixedTween(v: Tuple2[Double, Double]) = new RichFixedTween(
    new Tween(CoreMath.toFixed(v._1), CoreMath.toFixed(v._2), 1000))
  implicit def Tuple2RichIntTween(v: Tuple2[Int, Int]) = new RichIntTween(
    new Tween(v._1, v._2, 1000))
  implicit def Int2RichToIntTween(v: Int) = new RichToIntTween(new Tween(0, v, 0))
  implicit def Double2RichToFixedTween(v: Double) = new RichToFixedTween(
    new Tween(0, CoreMath.toFixed(v), 0))
    
}

class RichFixedTween(val tween: Tween) {
  
  def dur(duration:Int) = new RichFixedTween(new Tween(tween.getFromValue, tween.getToValue, 
    duration, tween.getEasing, tween.getStartDelay))
  
  def ease(easing:Easing) = new RichFixedTween(new Tween(tween.getFromValue, tween.getToValue, 
    tween.getDuration, easing, tween.getStartDelay))
    
  def delay(startDelay:Int) = new RichFixedTween(new Tween(tween.getFromValue, tween.getToValue, 
    tween.getDuration, tween.getEasing, startDelay))
  
}

class RichToFixedTween(val tween: Tween) {
  
  def dur(duration:Int) = new RichToFixedTween(new Tween(tween.getFromValue, tween.getToValue, 
    duration, tween.getEasing, tween.getStartDelay))
  
  def ease(easing:Easing) = new RichToFixedTween(new Tween(tween.getFromValue, tween.getToValue, 
    tween.getDuration, easing, tween.getStartDelay))
    
  def delay(startDelay:Int) = new RichToFixedTween(new Tween(tween.getFromValue, tween.getToValue, 
    tween.getDuration, tween.getEasing, startDelay))
  
}

class RichIntTween(val tween: Tween) {
  
  def dur(duration:Int) = new RichIntTween(new Tween(tween.getFromValue, tween.getToValue, 
    duration, tween.getEasing, tween.getStartDelay))
  
  def ease(easing:Easing) = new RichIntTween(new Tween(tween.getFromValue, tween.getToValue, 
    tween.getDuration, easing, tween.getStartDelay))
    
  def delay(startDelay:Int) = new RichIntTween(new Tween(tween.getFromValue, tween.getToValue, 
    tween.getDuration, tween.getEasing, startDelay))
  
}

class RichToIntTween(val tween: Tween) {
  
  def dur(duration:Int) = new RichToIntTween(new Tween(tween.getFromValue, tween.getToValue, 
    duration, tween.getEasing, tween.getStartDelay))
  
  def ease(easing:Easing) = new RichToIntTween(new Tween(tween.getFromValue, tween.getToValue, 
    tween.getDuration, easing, tween.getStartDelay))
    
  def delay(startDelay:Int) = new RichToIntTween(new Tween(tween.getFromValue, tween.getToValue, 
    tween.getDuration, tween.getEasing, startDelay))
  
}

class RichFixed(val prop: Fixed) extends Ordered[Double] {
  
  def compare(that: Double): Int = { prop.get.compare(that) }
  
  override def equals(that: Any): Boolean = that match { 
    case that: RichFixed => prop.get equals that.prop.get
    case that        => prop.get equals that
  } 
  
  def ->(v: RichFixedTween) {
    prop.setBehavior(v.tween);
  }
  
  def ->(v: RichToFixedTween) {
    val t = v.tween
    prop.animateToFixed(t.getToValue, t.getDuration, t.getEasing, t.getStartDelay)
  }
  
  def ->(v: RichIntTween) {
    val t = v.tween
    prop.animateAsFixed(CoreMath.toFixed(t.getFromValue), CoreMath.toFixed(t.getToValue),
      t.getDuration, t.getEasing, t.getStartDelay)
  }
  
  def ->(v: RichToIntTween) {
    val t = v.tween
    prop.animateToFixed(CoreMath.toFixed(t.getToValue), t.getDuration, t.getEasing, t.getStartDelay)
  }
  
  def +=(v:Double) = {
    prop.set(prop.get + v)
    prop
  }
  def -=(v:Double) = {
    prop.set(prop.get + v)
    prop
  }
  def *=(v:Double) = {
    prop.set(prop.get * v)
    prop
  }
  def /=(v:Double) = {
    prop.set(prop.get / v)
    prop
  }
}

class RichInt2(val prop: pulpcore.animation.Int) extends Ordered[Double] {
  
  def compare(that: Double): Int = { prop.get.toDouble.compare(that) }
  
  override def equals(that: Any): Boolean = that match { 
    case that: RichInt2 => prop.get equals that.prop.get
    case that        => prop.get equals that 
  } 
  
  def ->(v: RichFixedTween) {
    val t = v.tween
    prop.animate(CoreMath.toInt(t.getFromValue), CoreMath.toInt(t.getToValue),
      t.getDuration, t.getEasing, t.getStartDelay)
  }
  
  def ->(v: RichToFixedTween) {
    val t = v.tween
    prop.animateTo(CoreMath.toInt(t.getToValue), t.getDuration, t.getEasing, t.getStartDelay)
  }
  
  def ->(v: RichIntTween) {
    prop.setBehavior(v.tween);
  }
  
  def ->(v: RichToIntTween) {
    val t = v.tween
    prop.animateTo(t.getToValue, t.getDuration, t.getEasing, t.getStartDelay)
  }
  
  def +=(v:Int) = {
    prop.set(prop.get + v)
    prop
  }
  def -=(v:Int) = {
    prop.set(prop.get + v)
    prop
  }
  def *=(v:Int) = {
    prop.set(prop.get * v)
    prop
  }
  def /=(v:Int) = {
    prop.set(prop.get / v)
    prop
  }
}
