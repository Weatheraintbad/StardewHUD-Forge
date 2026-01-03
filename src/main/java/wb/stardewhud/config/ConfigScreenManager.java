package wb.stardewhud.config;

import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;

public class ConfigScreenManager {

    public static void register() {
        // 注册配置界面到Forge
        ModLoadingContext.get().registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(
                        (client, parent) -> new ModConfigScreen(parent)
                )
        );
    }
}