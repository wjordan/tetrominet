/*
 * TetromiNET Copyright (C) 2008-2009 Will Jordan.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * See <http://www.gnu.org/licenses/> for details.
 */

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