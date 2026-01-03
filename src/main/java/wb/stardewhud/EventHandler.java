package wb.stardewhud;

import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import wb.stardewhud.config.ModConfig;

@Mod.EventBusSubscriber(modid = StardewHUD.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class EventHandler {

    @SubscribeEvent
    public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Pre event) {
        // Forge 1.20.1中效果图层的ID可能有所不同
        if (event.getOverlay().id().getPath().equals("effects")) {
            ModConfig config = StardewHUD.getConfig();

            // 如果HUD启用且隐藏效果选项开启，并且HUD在效果区域，则取消渲染
            if (config.enabled && config.hideVanillaEffects) {
                if (shouldHideVanillaEffects(config)) {
                    event.setCanceled(true);
                }
            }
        }
    }

    private static boolean shouldHideVanillaEffects(ModConfig config) {
        Minecraft client = Minecraft.getInstance();
        int screenWidth = client.getWindow().getGuiScaledWidth();

        // 计算HUD位置
        int hudX, hudY, hudWidth, hudHeight;

        var hudRenderer = StardewHUD.getHudRenderer();
        if (hudRenderer == null) return false;

        hudWidth = hudRenderer.getHudWidth();
        hudHeight = hudRenderer.getHudHeight();

        var pos = config.position;
        if (pos.x == 0 && pos.y == 0) {
            // 自动定位到右上角
            int margin = 10;
            hudX = screenWidth - hudWidth - margin;
            hudY = margin;
        } else {
            // 从右侧计算
            hudX = screenWidth - pos.x;
            hudY = pos.y;
        }

        // 原版效果区域（右上角，大约100x100像素）
        int effectAreaLeft = screenWidth - 100;
        int effectAreaTop = 0;
        int effectAreaRight = screenWidth;
        int effectAreaBottom = 100;

        // 检查是否重叠
        boolean isOverlapping =
                hudX < effectAreaRight &&
                        hudX + hudWidth > effectAreaLeft &&
                        hudY < effectAreaBottom &&
                        hudY + hudHeight > effectAreaTop;

        return isOverlapping;
    }
}