import pulpcore.Build;
import pulpcore.CoreSystem;
import pulpcore.Stage;
import HelloWorld.HelloWorld;

public class LoadingScene extends pulpcore.scene.LoadingScene {
    
    public LoadingScene() {
        super("pulpapplet-" + ProjectBuild.VERSION + ".zip" , new HelloWorld());
        System.out.println( "Asset : " + "pulpapplet-" + ProjectBuild.VERSION + ".zip" );
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
