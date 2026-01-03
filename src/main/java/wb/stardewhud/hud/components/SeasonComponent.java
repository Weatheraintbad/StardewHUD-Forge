package wb.stardewhud.hud.components;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import wb.stardewhud.StardewHUD;
import wb.stardewhud.hud.HudRenderer;

public class SeasonComponent {
    private final HudRenderer hudRenderer;

    // 季节图标
    private static final ResourceLocation SPRING_ICON = ResourceLocation.fromNamespaceAndPath(StardewHUD.MOD_ID, "textures/icons/fortune/spring.png");
    private static final ResourceLocation SUMMER_ICON = ResourceLocation.fromNamespaceAndPath(StardewHUD.MOD_ID, "textures/icons/fortune/summer.png");
    private static final ResourceLocation AUTUMN_ICON = ResourceLocation.fromNamespaceAndPath(StardewHUD.MOD_ID, "textures/icons/fortune/autumn.png");
    private static final ResourceLocation WINTER_ICON = ResourceLocation.fromNamespaceAndPath(StardewHUD.MOD_ID, "textures/icons/fortune/winter.png");

    private long lastCalculatedDay = -1;
    private ResourceLocation currentSeasonIcon = SPRING_ICON;

    public SeasonComponent(HudRenderer hudRenderer) {
        this.hudRenderer = hudRenderer;
    }

    public void render(GuiGraphics context, int x, int y) {
        context.blit(currentSeasonIcon, x - 8, y - 2, 0, 0, 41, 17, 41, 17);
    }


    public void update() {
        Minecraft client = hudRenderer.getClient();
        if (client == null || client.player == null || client.level == null) {
            return;
        }

        updateSeasonIcon(client.level.getDayTime());
    }

    // 根据时间切换季节图标
    private void updateSeasonIcon(long timeOfDay) {
        long dayFromTicks = timeOfDay / 24000L;

        if (dayFromTicks != lastCalculatedDay) {
            long oldDay = lastCalculatedDay;
            lastCalculatedDay = dayFromTicks;

            // 计算季节索引（每91天一个季节）
            int seasonIndex = (int)((dayFromTicks / 28) % 4);

            // 获取新季节图标
            ResourceLocation newSeasonIcon = getSeasonIcon(seasonIndex);

            // 只在季节变化时更新
            if (newSeasonIcon != currentSeasonIcon) {
                ResourceLocation oldIcon = currentSeasonIcon;
                currentSeasonIcon = newSeasonIcon;

                StardewHUD.LOGGER.info("季节图标切换: [{}] 第{}天 -> 第{}天, 图标: {} -> {}",
                        getSeasonName(seasonIndex),
                        oldDay, dayFromTicks,
                        getFileName(oldIcon), getFileName(newSeasonIcon));
            } else {
                StardewHUD.LOGGER.debug("游戏日变化: 第{}天 -> 第{}天, 季节保持: {}",
                        oldDay, dayFromTicks, getSeasonName(seasonIndex));
            }
        }
    }

    private ResourceLocation getSeasonIcon(int seasonIndex) {
        switch (seasonIndex) {
            case 0: return SPRING_ICON;
            case 1: return SUMMER_ICON;
            case 2: return AUTUMN_ICON;
            case 3: return WINTER_ICON;
            default: return SPRING_ICON;
        }
    }

    private String getSeasonName(int seasonIndex) {
        switch (seasonIndex) {
            case 0: return "春季";
            case 1: return "夏季";
            case 2: return "秋季";
            case 3: return "冬季";
            default: return "未知";
        }
    }

    private String getFileName(ResourceLocation id) {
        if (id == null) return "null";
        String path = id.getPath();
        int lastSlash = path.lastIndexOf('/');
        return lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
    }

    public void reset() {
        lastCalculatedDay = -1;
        currentSeasonIcon = SPRING_ICON;
        StardewHUD.LOGGER.debug("已重置SeasonComponent数据");
    }
}