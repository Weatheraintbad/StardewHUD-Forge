package wb.stardewhud.hud.components;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import wb.stardewhud.StardewHUD;
import wb.stardewhud.hud.HudRenderer;

import java.lang.reflect.Method;

public class SeasonComponent {
    private final HudRenderer hudRenderer;

    // 季节图标
    private static final ResourceLocation SPRING_ICON = ResourceLocation.fromNamespaceAndPath(StardewHUD.MOD_ID, "textures/icons/fortune/spring.png");
    private static final ResourceLocation SUMMER_ICON = ResourceLocation.fromNamespaceAndPath(StardewHUD.MOD_ID, "textures/icons/fortune/summer.png");
    private static final ResourceLocation AUTUMN_ICON = ResourceLocation.fromNamespaceAndPath(StardewHUD.MOD_ID, "textures/icons/fortune/autumn.png");
    private static final ResourceLocation WINTER_ICON = ResourceLocation.fromNamespaceAndPath(StardewHUD.MOD_ID, "textures/icons/fortune/winter.png");

    private long lastCalculatedDay = -1;
    private ResourceLocation currentSeasonIcon = WINTER_ICON; // 初始化为冬季图标

    // Serene Seasons兼容
    private static Boolean sereneSeasonsLoaded = null;
    private static Method getSeasonStateMethod = null;
    private static final int SUB_SEASON_COUNT = 3; // 每个大季节包含3个子季节(early/mid/late)

    // 季节倒计时状态
    private int lastSubSeasonOrdinal = -1;
    private int remainingDaysInSeason = 0;

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
        int seasonIndex;
        boolean isUsingSereneSeasons = false;

        // 首先尝试使用Serene Seasons API获取季节
        Integer ssSeasonIndex = getSereneSeasonsSeason();
        if (ssSeasonIndex != null) {
            seasonIndex = ssSeasonIndex;
            isUsingSereneSeasons = true;

            // 游戏日变化时，剩余天数减1
            if (dayFromTicks != lastCalculatedDay && lastCalculatedDay != -1) {
                remainingDaysInSeason = Math.max(0, remainingDaysInSeason - 1);

                if (StardewHUD.shouldLog()) {
                    StardewHUD.LOGGER.debug("游戏日变化: {}剩余{}天", getSeasonName(seasonIndex), remainingDaysInSeason);
                }
            }
        } else {
            // 从配置中获取每个季节持续的天数
            int seasonDays = StardewHUD.getConfig().seasonDays;

            // 计算当前季节索引
            if (dayFromTicks == 0) {
                // 第0天特殊处理：冬季
                seasonIndex = 3; // 冬季
            } else {
                // 从第1天开始计算季节
                // 注意：第1天到第28天是春季，所以用(dayFromTicks-1)来计算
                long adjustedDay = dayFromTicks - 1; // 从第1天开始调整为第0天
                seasonIndex = (int)((adjustedDay / seasonDays) % 4);
            }
        }

        // 获取新季节图标
        ResourceLocation newSeasonIcon = getSeasonIcon(seasonIndex);

        // 检测两种情况需要更新：1. 游戏日变化 2. 季节图标变化（比如通过命令强制修改季节）
        if (dayFromTicks != lastCalculatedDay || !newSeasonIcon.equals(currentSeasonIcon)) {
            long oldDay = lastCalculatedDay;
            lastCalculatedDay = dayFromTicks;

            // 只在季节变化时更新
            if (!newSeasonIcon.equals(currentSeasonIcon)) {
                ResourceLocation oldIcon = currentSeasonIcon;
                currentSeasonIcon = newSeasonIcon;

                if (StardewHUD.shouldLog()) {
                    StardewHUD.LOGGER.info("季节图标切换: [{}] 第{}天 -> 第{}天, 图标: {} -> {}",
                            getSeasonName(seasonIndex),
                            oldDay, dayFromTicks,
                            getFileName(oldIcon), getFileName(newSeasonIcon));
                }
            } else {
                if (StardewHUD.shouldLog() && oldDay != -1) {
                    if (isUsingSereneSeasons) {
                        StardewHUD.LOGGER.debug("游戏日变化: 第{}天 -> 第{}天, 季节保持: {} (剩余{}天)",
                                oldDay, dayFromTicks, getSeasonName(seasonIndex), remainingDaysInSeason);
                    } else {
                        StardewHUD.LOGGER.debug("游戏日变化: 第{}天 -> 第{}天, 季节保持: {}",
                                oldDay, dayFromTicks, getSeasonName(seasonIndex));
                    }
                }
            }
        }
    }

    // 尝试通过反射获取Serene Seasons的当前季节
    private Integer getSereneSeasonsSeason() {
        // 检查是否已检测过Serene Seasons
        if (sereneSeasonsLoaded == null) {
            try {
                // 检查是否加载了Serene Seasons，仅获取核心API方法
                Class<?> seasonHelperClass = Class.forName("sereneseasons.api.season.SeasonHelper");
                getSeasonStateMethod = seasonHelperClass.getMethod("getSeasonState", Level.class);
                Class.forName("sereneseasons.api.season.Season");
                sereneSeasonsLoaded = true;

                if (StardewHUD.shouldLog()) {
                    StardewHUD.LOGGER.info("已检测到Serene Seasons模组，将使用其季节系统");
                }
            } catch (Exception e) {
                sereneSeasonsLoaded = false;
                if (StardewHUD.shouldLog()) {
                    StardewHUD.LOGGER.debug("未检测到Serene Seasons模组，使用内置季节计算: {}", e.getMessage());
                }
            }
        }

        // 如果加载了Serene Seasons，尝试获取当前季节
        if (sereneSeasonsLoaded && getSeasonStateMethod != null) {
            try {
                Level level = hudRenderer.getClient().level;
                if (level != null) {
                    // 直接获取最新季节状态，完全依赖官方API返回值，不管是自然变化还是指令修改
                    Object seasonState = getSeasonStateMethod.invoke(null, level);

                    // 普通群系季节获取（简化逻辑，优先保证基础功能）
                    Method getSeasonMethod = seasonState.getClass().getMethod("getSeason");
                    Enum<?> season = (Enum<?>) getSeasonMethod.invoke(seasonState);
                    // Season枚举顺序: SPRING, SUMMER, AUTUMN, WINTER，和我们的索引完全一致
                    int seasonIndex = season.ordinal();

                    // 简单子季节处理
                    try {
                        Method getSubSeasonMethod = seasonState.getClass().getMethod("getSubSeason");
                        Object subSeason = getSubSeasonMethod.invoke(seasonState);
                        int subSeasonOrdinal = ((Enum<?>) subSeason).ordinal();
                        int subSeasonInSeason = subSeasonOrdinal % SUB_SEASON_COUNT;
                        int seasonDays = StardewHUD.getConfig().seasonDays;
                        int daysPerSubSeason = seasonDays / SUB_SEASON_COUNT;

                        if (subSeasonOrdinal != lastSubSeasonOrdinal) {
                            lastSubSeasonOrdinal = subSeasonOrdinal;
                            remainingDaysInSeason = (SUB_SEASON_COUNT - subSeasonInSeason) * daysPerSubSeason;
                        }
                    } catch (Exception e) {
                        // 忽略子季节错误，不影响主季节显示
                    }

                    if (StardewHUD.shouldLog()) {
                        StardewHUD.LOGGER.debug("Serene Seasons API返回当前季节: {} 索引: {}", season.name(), seasonIndex);
                    }

                    return seasonIndex;
                }
            } catch (Exception e) {
                if (StardewHUD.shouldLog()) {
                    StardewHUD.LOGGER.warn("获取Serene Seasons季节失败: {}", e.getMessage());
                }
            }
        }

        return null;
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
        currentSeasonIcon = WINTER_ICON; // 重置为冬季图标
        if (StardewHUD.shouldLog()) {
            StardewHUD.LOGGER.debug("已重置SeasonComponent数据");
        }
    }
}