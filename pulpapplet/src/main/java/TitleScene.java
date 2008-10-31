import pulpcore.animation.event.SceneChangeEvent;
import pulpcore.image.CoreFont;
import pulpcore.scene.Scene2D;
import pulpcore.sprite.Button;
import pulpcore.sprite.Group;
import pulpcore.sprite.ImageSprite;
import pulpcore.sprite.Label;
import pulpcore.sprite.Sprite;
import pulpcore.Stage;
import HelloWorld.Hello;

public class TitleScene extends Scene2D {
    
    Button playButton;
    Button optionsButton;
    Group componentLayer;
    
    @Override
    public void load() {
        
        Label title = new Label(CoreFont.load("hello.font.png"), "Hello World", 320, 240);
        title.setAnchor(Sprite.CENTER);
        playButton = Button.createLabeledButton("Play", 320, 320);
        playButton.setAnchor(Sprite.CENTER);
        optionsButton = Button.createLabeledButton("Options", 320, 370);
        optionsButton.setAnchor(Sprite.CENTER);
        
        componentLayer = new Group();
        componentLayer.add(playButton);
        componentLayer.add(optionsButton);
        
        add(new ImageSprite("background.png", 0, 0));
        add(title);
        addLayer(componentLayer);
    }
    
    @Override 
    public void update(int elapsedTime) {
        if (optionsButton.isClicked()) {
            // Pushes the scene onto the stack (doesn't unload this Scene)
            Stage.pushScene(new OptionScene());
        }
        else if (playButton.isClicked()) {
            // Animated alternative to Stage.setScene(new HelloWorld());
            componentLayer.alpha.animateTo(0, 300);
            addEvent(new SceneChangeEvent(new Hello(), 300));
        }
    }
}
