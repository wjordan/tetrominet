package HelloWorld

import pulpcore.image.CoreFont
import pulpcore.scene.Scene2D
import pulpcore.sound.Sound
import pulpcore.sprite.ImageSprite
import pulpcore.sprite.Label
import pulpcore.sprite.Sprite
import pulpcore.Input
import pulpcore.Stage

/**
 * @author will
 * @date Oct 12, 2008 9:10:17 PM
 */

class Hello extends Scene2D {
  var label : Label = null

  override def load = {
      add(new ImageSprite("background.png", 0, 0));

      val font: CoreFont = CoreFont.load("hello.font.png");
      label = new Label(font, "Hello There!", 320, 240);
      label.setAnchor(Sprite.CENTER);
      add(label);
        
      val sound: Sound = Sound.load("sound.wav");
      sound.play();
  }

  override def update(elapsedTime: Int) {
      val angle: Double = 0.06 * (Input.getMouseX() - 320);
      val duration: Int = 100;
      label.angle.animateTo(angle, duration);
    if(Input.isPressed(Input.KEY_R)) Stage.replaceScene(new Hello)
  }

  override def unload = {
    super.unload
    println("Unloading!")
  }
}