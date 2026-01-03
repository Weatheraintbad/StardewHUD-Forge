package wb.stardewhud.hud.components;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import wb.stardewhud.StardewHUD;
import wb.stardewhud.hud.HudRenderer;

import java.util.HashMap;
import java.util.Map;

public class FortuneComponent {
    private final HudRenderer hudRenderer;

    // 图标路径
    private static final String FORTUNE_ICON_PATH_TEMPLATE =
            StardewHUD.MOD_ID + ":textures/icons/fortune/%s.png";

    // 默认图标
    private static final ResourceLocation DEFAULT_ICON = ResourceLocation.fromNamespaceAndPath(StardewHUD.MOD_ID, "textures/icons/fortune/default.png");
    private String positiveEffectId = null;
    private String negativeEffectId = null;
    private long lastSyncedDay = -1;
    private boolean hasSyncedData = false;

    private final Map<String, ResourceLocation> iconCache = new HashMap<>();

    public FortuneComponent(HudRenderer hudRenderer) {
        this.hudRenderer = hudRenderer;
    }

    public void render(GuiGraphics context, int x, int y) {
        // 只渲染效果图标，不处理季节
        int iconSize = 14;
        int spacing = 3;

        if (positiveEffectId != null) {
            ResourceLocation icon = getEffectIcon(positiveEffectId);
            context.blit(icon, x, y, 0, 0, iconSize, iconSize, iconSize, iconSize);
        }

        if (negativeEffectId != null) {
            ResourceLocation icon = getEffectIcon(negativeEffectId);
            context.blit(icon, x + iconSize + spacing, y, 0, 0, iconSize, iconSize, iconSize, iconSize);
        }
    }

    public void update() {
        Minecraft client = hudRenderer.getClient();
        if (client == null || client.player == null || client.level == null) {
            return;
        }

        // 处理数据同步
        long currentDay = client.level.getGameTime() / 24000L;
        if (!hasSyncedData || currentDay != lastSyncedDay) {
            // 在没有EverySingleDay模组时，可以设置一些测试数据或留空
            // 或者完全禁用运势显示
            if (StardewHUD.isModLoaded("everysingleday")) {
                // 这里可以添加Forge网络请求
                // 暂时留空，需要另外实现Forge网络通信
            }
        }
    }

    private ResourceLocation getEffectIcon(String effectId) {
        if (effectId == null || effectId.isEmpty()) {
            return DEFAULT_ICON;
        }

        if (iconCache.containsKey(effectId)) {
            return iconCache.get(effectId);
        }

        ResourceLocation icon = loadEffectIcon(effectId);
        iconCache.put(effectId, icon);
        return icon;
    }

    private ResourceLocation loadEffectIcon(String effectId) {
        String iconPath = String.format(FORTUNE_ICON_PATH_TEMPLATE, effectId);
        return ResourceLocation.parse(iconPath);
    }

    private void preloadEffectIcons() {
        if (positiveEffectId != null && !iconCache.containsKey(positiveEffectId)) {
            iconCache.put(positiveEffectId, loadEffectIcon(positiveEffectId));
        }
        if (negativeEffectId != null && !iconCache.containsKey(negativeEffectId)) {
            iconCache.put(negativeEffectId, loadEffectIcon(negativeEffectId));
        }
    }

    public void setEffectsForTesting(String positiveId, String negativeId) {
        this.positiveEffectId = positiveId;
        this.negativeEffectId = negativeId;
        this.hasSyncedData = true;
        this.lastSyncedDay = -1;

        if (positiveId != null) {
            getEffectIcon(positiveId);
        }
        if (negativeId != null) {
            getEffectIcon(negativeId);
        }

        StardewHUD.LOGGER.info("手动设置测试效果: 正面={}, 负面={}", positiveId, negativeId);
    }

    public void reset() {
        positiveEffectId = null;
        negativeEffectId = null;
        lastSyncedDay = -1;
        hasSyncedData = false;
        iconCache.clear();

        StardewHUD.LOGGER.debug("已重置FortuneComponent数据");
    }

    public boolean isValid() {
        return hasSyncedData && (positiveEffectId != null || negativeEffectId != null);
    }

    public String getPositiveEffectId() {
        return positiveEffectId;
    }

    public String getNegativeEffectId() {
        return negativeEffectId;
    }

    public boolean hasEffectsData() {
        return hasSyncedData;
    }

    public long getLastSyncedDay() {
        return lastSyncedDay;
    }
}