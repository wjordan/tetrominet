import pulpcore.Build;
import pulpcore.CoreSystem;
import pulpcore.Stage;

public class LoadingScene extends pulpcore.scene.LoadingScene {
    
    public LoadingScene() {
        super("gamelogic-" + ProjectBuild.VERSION + ".zip" , new TitleScene());
        System.out.println( "Asset : " + "gamelogic-" + ProjectBuild.VERSION + ".zip" );
        CoreSystem.setTalkBackField("app.name", "HelloWorld");
        CoreSystem.setTalkBackField("app.version", ProjectBuild.VERSION);
        
        Stage.setUncaughtExceptionScene(new UncaughtExceptionScene());
        Stage.invokeOnShutdown(new Runnable() {
            public void run() {
                // Shutdown network connections, DB connections, etc. 
            }
        });
    }
    
    @Override
    public void load() {
        
        // Start loading the zip
        super.load();
    }
}
