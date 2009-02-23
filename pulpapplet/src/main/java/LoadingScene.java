/*
 * TetromiNET Copyright (C) 2008-2009 Will Jordan.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * See <http://www.gnu.org/licenses/> for details.
 */

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
