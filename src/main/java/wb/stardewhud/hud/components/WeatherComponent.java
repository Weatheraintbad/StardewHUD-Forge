package wb.stardewhud.hud.components;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import wb.stardewhud.StardewHUD;
import wb.stardewhud.hud.HudRenderer;

public class WeatherComponent {
    private final HudRenderer hudRenderer;

    // 天气图标纹理
    private static final ResourceLocation WEATHER_SUNNY = ResourceLocation.fromNamespaceAndPath(StardewHUD.MOD_ID, "textures/icons/weather/sunny.png");
    private static final ResourceLocation WEATHER_RAINY = ResourceLocation.fromNamespaceAndPath(StardewHUD.MOD_ID, "textures/icons/weather/rainy.png");
    private static final ResourceLocation WEATHER_THUNDER = ResourceLocation.fromNamespaceAndPath(StardewHUD.MOD_ID, "textures/icons/weather/thunder.png");

    // 当前天气状态
    private WeatherType currentWeather = WeatherType.SUNNY;

    public enum WeatherType {
        SUNNY,
        RAINY,
        THUNDER
    }

    public WeatherComponent(HudRenderer hudRenderer) {
        this.hudRenderer = hudRenderer;
    }

    public void render(GuiGraphics context, int x, int y) {
        // 根据当前天气状态选择图标
        ResourceLocation iconTexture = getWeatherIcon();

        // 渲染天气图标（20x14），使用给定的坐标
        if (iconTexture != null) {
            context.blit(iconTexture, x - 7, y, 0, 0, 21, 13, 21, 13);
        }
    }

    private ResourceLocation getWeatherIcon() {
        switch (currentWeather) {
            case RAINY:
                return WEATHER_RAINY;
            case THUNDER:
                return WEATHER_THUNDER;
            case SUNNY:
            default:
                return WEATHER_SUNNY;
        }
    }

    public void update(Level world) {
        if (world == null) return;

        // 判断当前天气
        if (world.isThundering()) {
            currentWeather = WeatherType.THUNDER;
        } else if (world.isRaining()) {
            currentWeather = WeatherType.RAINY;
        } else {
            currentWeather = WeatherType.SUNNY;
        }
    }
}