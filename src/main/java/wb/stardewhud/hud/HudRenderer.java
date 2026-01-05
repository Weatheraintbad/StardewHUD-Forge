package wb.stardewhud.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import wb.stardewhud.StardewHUD;
import wb.stardewhud.config.ModConfig;
import wb.stardewhud.hud.components.*;

public class HudRenderer {
    private final ModConfig config;
    private final Minecraft client;

    private final ClockComponent clock;
    private final TimeDisplayComponent timeDisplay;
    private final WeatherComponent weather;
    private final FortuneComponent fortune;
    private final ItemCounterComponent itemCounter;
    private final SeasonComponent season;

    // 纹理标识符
    public static final ResourceLocation CLOCK_BG = ResourceLocation.fromNamespaceAndPath(StardewHUD.MOD_ID, "textures/gui/clock_bg.png");
    public static final ResourceLocation INFO_BG = ResourceLocation.fromNamespaceAndPath(StardewHUD.MOD_ID, "textures/gui/info_bg.png");
    public static final ResourceLocation COUNTER_BG = ResourceLocation.fromNamespaceAndPath(StardewHUD.MOD_ID, "textures/gui/counter_bg.png");

    // HUD尺寸常量（原始尺寸，未缩放）
    private static final int CLOCK_WIDTH = 40;
    private static final int CLOCK_HEIGHT = 65;
    private static final int INFO_WIDTH = 80;
    private static final int INFO_HEIGHT = 65;
    private static final int COUNTER_WIDTH = 100;
    private static final int COUNTER_HEIGHT = 32;
    private static final int COUNTER_TOP_MARGIN = -2;

    // 总宽度和高度（原始尺寸）
    private static final int TOTAL_WIDTH = CLOCK_WIDTH + INFO_WIDTH;
    private static final int TOTAL_HEIGHT = INFO_HEIGHT + COUNTER_TOP_MARGIN + COUNTER_HEIGHT;

    // 80%缩放常量
    private static final float SCALE_80_PERCENT = 0.8f;

    public HudRenderer(ModConfig config) {
        this.config = config;
        this.client = Minecraft.getInstance();

        this.clock = new ClockComponent(this);
        this.timeDisplay = new TimeDisplayComponent(this);
        this.weather = new WeatherComponent(this);
        this.fortune = new FortuneComponent(this);
        this.itemCounter = new ItemCounterComponent(this, config.counterItemId);
        this.season = new SeasonComponent(this);
    }

    public void render(GuiGraphics context, float tickDelta) {
        if (!shouldRender()) return;

        // 计算屏幕尺寸
        int screenWidth = client.getWindow().getGuiScaledWidth();
        int margin = 5; // 边距

        // 计算总缩放比例（80% * config.scale）
        float totalScale = SCALE_80_PERCENT * config.scale;

        // 计算缩放后的总尺寸
        int scaledTotalWidth = (int)(TOTAL_WIDTH * totalScale);
        int scaledTotalHeight = (int)(TOTAL_HEIGHT * totalScale);

        // 计算位置（现在从右侧计算X坐标）
        int x, y;
        if (config.position.x == 0 && config.position.y == 0) {
            // 自动定位到右上角
            x = screenWidth - scaledTotalWidth - margin;
            y = margin;
        } else {
            // 使用配置的位置
            x = screenWidth - config.position.x;
            y = config.position.y;
        }

        // 保存当前变换状态
        context.pose().pushPose();
        context.pose().translate(x, y, 0);
        context.pose().scale(totalScale, totalScale, 1.0f);

        try {
            // 1. 渲染计数器（如果启用）
            if (config.showItemCounter) {
                int counterX = CLOCK_WIDTH + INFO_WIDTH - COUNTER_WIDTH;
                int counterY = INFO_HEIGHT + COUNTER_TOP_MARGIN;
                itemCounter.render(context, counterX, counterY);
            }

            // 2. 渲染时钟组件（如果启用）
            if (config.showClock) {
                clock.render(context, 0, 0, tickDelta);
            }

            // 3. 渲染信息框背景（如果启用了任何信息框组件）
            if (config.showTimeDisplay || config.showWeather || config.showFortune || config.showSeason) {
                // 渲染信息框背景
                renderInfoBackground(context, CLOCK_WIDTH, 0);

                // 计算基础图标位置
                int baseIconX = CLOCK_WIDTH + 15;
                int iconY = 26;
                int iconSpacing = 24;

                // 渲染天气图标
                if (config.showWeather) {
                    weather.render(context, baseIconX, iconY);
                    baseIconX += iconSpacing; // 下一个图标向右偏移
                }

                // 渲染季节或运势图标（二选一，季节优先）
                if (config.showSeason) {
                    // 季节开启时，始终渲染季节图标（覆盖运势位置）
                    season.render(context, baseIconX, iconY);
                } else if (config.showFortune && fortune.hasEffectsData()) {
                    // 只有没开启季节，且运势有数据时，才渲染运势图标
                    fortune.render(context, baseIconX, iconY);
                }

                // 渲染时间显示
                if (config.showTimeDisplay) {
                    timeDisplay.renderGameInfo(context, CLOCK_WIDTH - 1, INFO_WIDTH - 4, 11);
                    timeDisplay.renderTime(context, CLOCK_WIDTH - 3, INFO_WIDTH, 48);
                }
            }


        } finally {
            context.pose().popPose();
        }
    }

    private void renderInfoBackground(GuiGraphics context, int x, int y) {
        context.setColor(1.0f, 1.0f, 1.0f, config.backgroundAlpha);
        context.blit(INFO_BG, x, y, 0, 0, INFO_WIDTH, INFO_HEIGHT, INFO_WIDTH, INFO_HEIGHT);
        context.setColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    public void update() {
        // 更新所有组件的数据
        ClientLevel world = client.level;
        if (world != null) {
            if (config.showClock) clock.update();
            if (config.showTimeDisplay) timeDisplay.update(world);
            if (config.showWeather) weather.update(world);
            if (config.showFortune) fortune.update();
            if (config.showItemCounter) itemCounter.update();
            // 季节始终更新，不受配置影响（可根据需要改为受配置控制）
            season.update();
        }
    }

    public boolean shouldRender() {
        return config.enabled && client.level != null && !client.options.hideGui;
    }

    public ModConfig getConfig() {
        return config;
    }

    public Minecraft getClient() {
        return client;
    }

    // 获取缩放后的HUD尺寸（包含80%缩放）
    public int getHudWidth() {
        return (int)(TOTAL_WIDTH * SCALE_80_PERCENT * config.scale);
    }

    public int getHudHeight() {
        return (int)(TOTAL_HEIGHT * SCALE_80_PERCENT * config.scale);
    }

    // 向其他组件暴露原始尺寸常量（不包含80%缩放）
    public static int getClockWidth() {
        return CLOCK_WIDTH;
    }

    public static int getClockHeight() {
        return CLOCK_HEIGHT;
    }

    public static int getInfoWidth() {
        return INFO_WIDTH;
    }

    public static int getInfoHeight() {
        return INFO_HEIGHT;
    }

    public static int getCounterWidth() {
        return COUNTER_WIDTH;
    }

    public static int getCounterHeight() {
        return COUNTER_HEIGHT;
    }

    public static int getTotalWidth() {
        return TOTAL_WIDTH;
    }

    public static int getTotalHeight() {
        return TOTAL_HEIGHT;
    }

    // 获取总缩放比例（包含80%缩放）
    public float getTotalScale() {
        return SCALE_80_PERCENT * config.scale;
    }
}