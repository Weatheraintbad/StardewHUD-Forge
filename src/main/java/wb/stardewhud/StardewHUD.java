package wb.stardewhud;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wb.stardewhud.config.ModConfig;
import wb.stardewhud.config.ConfigScreenManager;
import wb.stardewhud.hud.HudRenderer;

@Mod(StardewHUD.MOD_ID)
public class StardewHUD {
    public static final String MOD_ID = "stardewhud";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static HudRenderer hudRenderer;
    private static ModConfig config;

    public StardewHUD() {
        // 使用新的API注册事件
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::onClientSetup);
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void onClientSetup(final FMLClientSetupEvent event) {
        // 初始化配置
        config = new ModConfig();
        config.load();

        if (shouldLog()) {
            LOGGER.info("StardewHUD 正在初始化...");
        }

        // 检查其他模组是否存在的工具方法
        if (isModLoaded("modmenu")) {
            if (shouldLog()) {
                LOGGER.info("检测到 ModMenu，配置界面将可用");
            }
        } else {
            if (shouldLog()) {
                LOGGER.info("使用原生Forge配置界面");
            }
        }

        // 初始化HUD渲染器
        hudRenderer = new HudRenderer(config);

        // 注册配置界面
        ConfigScreenManager.register();

        if (shouldLog()) {
            LOGGER.info("StardewHUD 初始化完成！");
        }
    }

    @SubscribeEvent
    public void onRenderGui(RenderGuiEvent.Post event) {
        // 渲染实际的HUD
        if (hudRenderer != null && hudRenderer.shouldRender()) {
            hudRenderer.render(event.getGuiGraphics(), event.getPartialTick());
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && hudRenderer != null) {
            hudRenderer.update();
        }
    }

    public static HudRenderer getHudRenderer() {
        return hudRenderer;
    }

    public static ModConfig getConfig() {
        if (config == null) {
            if (shouldLog()) {
                LOGGER.warn("配置还未初始化，正在创建默认配置...");
            }
            config = new ModConfig();
            config.load();
        }
        return config;
    }

    // 检查其他模组是否存在的工具方法
    public static boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    // 检查是否应该输出日志
    public static boolean shouldLog() {
        return config != null && config.enableLogging;
    }
}