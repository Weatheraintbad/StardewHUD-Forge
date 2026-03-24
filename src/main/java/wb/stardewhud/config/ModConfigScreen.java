package wb.stardewhud.config;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import wb.stardewhud.StardewHUD;

import java.util.ArrayList;
import java.util.List;

public class ModConfigScreen extends Screen {
    private final Screen parent;
    private ModConfig config;

    // 标签页相关
    private List<AbstractConfigPage> pages = new ArrayList<>();
    private AbstractConfigPage currentPage;
    private List<Button> tabButtons = new ArrayList<>();

    // 底部按钮
    private Button saveButton;
    private Button cancelButton;
    private Button resetButton;

    // 布局常量
    private static final int TAB_HEIGHT = 25;
    private static final int TAB_WIDTH = 80;
    private static final int TAB_SPACING = 5;
    private static final int PAGE_START_Y = 70;
    private static final int PAGE_END_Y_OFFSET = 40;

    public ModConfigScreen(Screen parent) {
        super(Component.translatable("config.stardewhud.title"));
        this.parent = parent;
        this.config = StardewHUD.getConfig();
    }

    @Override
    protected void init() {
        super.init();

        // 清除所有旧的组件
        this.clearWidgets();
        tabButtons.clear();

        // 创建标签页
        pages.clear();
        pages.add(new GeneralPage());
        pages.add(new DisplayPage());
        pages.add(new EffectsPage());

        // 创建标签按钮
        int totalTabsWidth = pages.size() * TAB_WIDTH + (pages.size() - 1) * TAB_SPACING;
        int tabStartX = (this.width - totalTabsWidth) / 2;

        for (int i = 0; i < pages.size(); i++) {
            final int pageIndex = i;
            Button tabButton = Button.builder(pages.get(i).getTitle(), button -> switchPage(pageIndex))
                    .bounds(tabStartX + i * (TAB_WIDTH + TAB_SPACING), 40, TAB_WIDTH, TAB_HEIGHT)
                    .build();
            this.addRenderableWidget(tabButton);
            tabButtons.add(tabButton);
        }

        // 默认显示第一个页面
        if (currentPage == null) {
            switchPage(0);
        } else {
            currentPage.init();
        }

        // 创建底部按钮
        int buttonY = this.height - 30;
        int centerX = this.width / 2;

        saveButton = Button.builder(Component.translatable("gui.save"), button -> saveConfig())
                .bounds(centerX - 155, buttonY, 100, 20).build();
        this.addRenderableWidget(saveButton);

        resetButton = Button.builder(Component.translatable("config.stardewhud.reset"), button -> resetToDefaults())
                .bounds(centerX - 50, buttonY, 100, 20).build();
        this.addRenderableWidget(resetButton);

        cancelButton = Button.builder(Component.translatable("gui.cancel"), button -> onClose())
                .bounds(centerX + 55, buttonY, 100, 20).build();
        this.addRenderableWidget(cancelButton);
    }

    private void switchPage(int pageIndex) {
        if (pageIndex >= 0 && pageIndex < pages.size()) {
            // 移除当前页面的组件
            if (currentPage != null) {
                currentPage.clearWidgets();
            }

            // 切换到新页面
            currentPage = pages.get(pageIndex);
            currentPage.init();

            // 更新标签按钮状态
            for (int i = 0; i < tabButtons.size(); i++) {
                Button tab = tabButtons.get(i);
                if (i == pageIndex) {
                    tab.active = false; // 当前页面按钮禁用
                } else {
                    tab.active = true; // 其他页面按钮启用
                }
            }
        }
    }

    // ========== 抽象页面基类 ==========
    private abstract class AbstractConfigPage {
        protected List<AbstractWidget> widgets = new ArrayList<>();
        protected boolean initialized = false;

        abstract Component getTitle();
        abstract void init();

        protected void clearWidgets() {
            for (AbstractWidget widget : widgets) {
                ModConfigScreen.this.removeWidget(widget);
            }
            widgets.clear();
        }

        protected void addWidget(AbstractWidget widget) {
            widgets.add(widget);
            ModConfigScreen.this.addRenderableWidget(widget);
        }

        protected void applyChanges() {
            // 子类重写此方法来应用配置更改
        }

        protected void resetToDefaults() {
            // 子类重写此方法来重置为默认值
        }

        abstract void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick);
    }

    // ========== 通用设置页面 ==========
    private class GeneralPage extends AbstractConfigPage {
        private Checkbox enabledCheckbox;
        private EditBox counterItemIdField;
        private EditBox scaleField;
        private EditBox backgroundAlphaField;
        private EditBox posXField;
        private EditBox posYField;
        private EditBox seasonDaysField; // 新增：季节天数输入框

        @Override
        Component getTitle() {
            return Component.translatable("category.stardewhud.general");
        }

        @Override
        void init() {
            int centerX = ModConfigScreen.this.width / 2;
            int startY = PAGE_START_Y;
            int spacing = 25;

            int leftLabelX = centerX - 170; // 左侧标签位置
            int fieldX = centerX - 40;    // 输入框位置

            // 启用HUD
            enabledCheckbox = new Checkbox(
                    fieldX, startY, 200, 20,
                    Component.translatable("config.stardewhud.enabled"),
                    config.enabled, true
            );
            addWidget(enabledCheckbox);

            // 计数器物品ID
            int fieldY = startY + spacing;
            counterItemIdField = new EditBox(
                    ModConfigScreen.this.font,
                    fieldX, fieldY, 200, 20,
                    Component.empty()
            );
            counterItemIdField.setValue(config.counterItemId);
            counterItemIdField.setMaxLength(100);
            addWidget(counterItemIdField);

            // HUD缩放大小
            fieldY += spacing;
            scaleField = new EditBox(
                    ModConfigScreen.this.font,
                    fieldX, fieldY, 200, 20,
                    Component.empty()
            );
            scaleField.setValue(String.valueOf(config.scale * 100));
            scaleField.setMaxLength(10);
            addWidget(scaleField);

            // HUD背景透明度
            fieldY += spacing;
            backgroundAlphaField = new EditBox(
                    ModConfigScreen.this.font,
                    fieldX, fieldY, 200, 20,
                    Component.empty()
            );
            backgroundAlphaField.setValue(String.valueOf(config.backgroundAlpha));
            backgroundAlphaField.setMaxLength(10);
            addWidget(backgroundAlphaField);

            // HUD位置X
            fieldY += spacing;
            posXField = new EditBox(
                    ModConfigScreen.this.font,
                    fieldX, fieldY, 95, 20,
                    Component.empty()
            );
            posXField.setValue(String.valueOf(config.position.x));
            posXField.setMaxLength(4);
            addWidget(posXField);

            // HUD位置Y
            posYField = new EditBox(
                    ModConfigScreen.this.font,
                    fieldX + 105, fieldY, 95, 20,
                    Component.empty()
            );
            posYField.setValue(String.valueOf(config.position.y));
            posYField.setMaxLength(4);
            addWidget(posYField);

            // 季节天数配置
            fieldY += spacing;
            seasonDaysField = new EditBox(
                    ModConfigScreen.this.font,
                    fieldX, fieldY, 200, 20,
                    Component.empty()
            );
            seasonDaysField.setValue(String.valueOf(config.seasonDays));
            seasonDaysField.setMaxLength(4);
            addWidget(seasonDaysField);

            initialized = true;
        }

        @Override
        protected void applyChanges() {
            if (!initialized) return;

            config.enabled = enabledCheckbox.selected();
            config.counterItemId = counterItemIdField.getValue();

            try {
                float scalePercent = Float.parseFloat(scaleField.getValue());
                config.scale = Math.max(10f, Math.min(scalePercent, 500f)) / 100f;
            } catch (NumberFormatException e) {
                config.scale = 1.0f;
            }

            try {
                config.backgroundAlpha = Float.parseFloat(backgroundAlphaField.getValue());
                config.backgroundAlpha = Math.max(0.0f, Math.min(config.backgroundAlpha, 1.0f));
            } catch (NumberFormatException e) {
                config.backgroundAlpha = 1.0f;
            }

            try {
                config.position.x = Integer.parseInt(posXField.getValue());
            } catch (NumberFormatException e) {
                config.position.x = 0;
            }

            try {
                config.position.y = Integer.parseInt(posYField.getValue());
            } catch (NumberFormatException e) {
                config.position.y = 0;
            }

            // 应用季节天数配置
            try {
                int seasonDays = Integer.parseInt(seasonDaysField.getValue());
                config.seasonDays = Math.max(1, Math.min(seasonDays, 1000)); // 限制在1-1000天
            } catch (NumberFormatException e) {
                config.seasonDays = 28; // 默认值
            }
        }

        @Override
        protected void resetToDefaults() {
            if (!initialized) return;

            ModConfig defaults = new ModConfig();
            config.enabled = defaults.enabled;
            config.counterItemId = defaults.counterItemId;
            config.scale = defaults.scale;
            config.backgroundAlpha = defaults.backgroundAlpha;
            config.position.x = defaults.position.x;
            config.position.y = defaults.position.y;
            config.seasonDays = defaults.seasonDays;

            enabledCheckbox.onPress();
            counterItemIdField.setValue(defaults.counterItemId);
            scaleField.setValue(String.valueOf(defaults.scale * 100));
            backgroundAlphaField.setValue(String.valueOf(defaults.backgroundAlpha));
            posXField.setValue(String.valueOf(defaults.position.x));
            posYField.setValue(String.valueOf(defaults.position.y));
            seasonDaysField.setValue(String.valueOf(defaults.seasonDays));
        }

        @Override
        void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            int centerX = ModConfigScreen.this.width / 2;
            int startY = PAGE_START_Y;
            int spacing = 25;

            int leftLabelX = centerX - 170; // 左侧标签位置
            int fieldX = centerX - 40;    // 输入框位置

            // 渲染输入框左侧标签 - 使用翻译键
            int fieldY = startY + spacing;
            guiGraphics.drawString(
                    ModConfigScreen.this.font,
                    Component.translatable("stardewhud.label.counterItemId"),
                    leftLabelX + 20, fieldY + 5, 0xFFFFFF, false
            );

            fieldY += spacing;
            guiGraphics.drawString(
                    ModConfigScreen.this.font,
                    Component.translatable("stardewhud.label.scale"),
                    leftLabelX + 20, fieldY + 5, 0xFFFFFF, false
            );

            fieldY += spacing;
            guiGraphics.drawString(
                    ModConfigScreen.this.font,
                    Component.translatable("stardewhud.label.backgroundAlpha"),
                    leftLabelX + 20, fieldY + 5, 0xFFFFFF, false
            );

            fieldY += spacing;
            guiGraphics.drawString(
                    ModConfigScreen.this.font,
                    Component.translatable("stardewhud.label.positionX"),
                    leftLabelX + 20, fieldY + 5, 0xFFFFFF, false
            );

            guiGraphics.drawString(
                    ModConfigScreen.this.font,
                    Component.translatable("stardewhud.label.positionY"),
                    leftLabelX + 70, fieldY + 5, 0xFFFFFF, false
            );

            fieldY += spacing;
            guiGraphics.drawString(
                    ModConfigScreen.this.font,
                    Component.translatable("stardewhud.label.seasonDays"),
                    leftLabelX + 20, fieldY + 5, 0xFFFFFF, false
            );
        }
    }

    // ========== 显示设置页面 ==========
    private class DisplayPage extends AbstractConfigPage {
        private Checkbox showClockCheckbox;
        private Checkbox showWeatherCheckbox;
        private Checkbox showTimeDisplayCheckbox;
        private Checkbox showFortuneCheckbox;
        private Checkbox showSeasonCheckbox;
        private Checkbox showItemCounterCheckbox;
        private Checkbox enableLoggingCheckbox;

        @Override
        Component getTitle() {
            return Component.translatable("category.stardewhud.visibility");
        }

        @Override
        void init() {
            int centerX = ModConfigScreen.this.width / 2;
            int startY = PAGE_START_Y;
            int spacing = 22;

            // 左列复选框
            int leftX = centerX - 120;
            int y = startY;

            showClockCheckbox = new Checkbox(
                    leftX, y, 110, 20,
                    Component.translatable("config.stardewhud.showClock"),
                    config.showClock, true
            );
            addWidget(showClockCheckbox);
            y += spacing;

            showWeatherCheckbox = new Checkbox(
                    leftX, y, 110, 20,
                    Component.translatable("config.stardewhud.showWeather"),
                    config.showWeather, true
            );
            addWidget(showWeatherCheckbox);
            y += spacing;

            showTimeDisplayCheckbox = new Checkbox(
                    leftX, y, 110, 20,
                    Component.translatable("config.stardewhud.showTimeDisplay"),
                    config.showTimeDisplay, true
            );
            addWidget(showTimeDisplayCheckbox);

            // 右列复选框
            int rightX = centerX + 10;
            y = startY;

            showFortuneCheckbox = new Checkbox(
                    rightX, y, 110, 20,
                    Component.translatable("config.stardewhud.showFortune"),
                    config.showFortune, true
            );
            addWidget(showFortuneCheckbox);
            y += spacing;

            showSeasonCheckbox = new Checkbox(
                    rightX, y, 110, 20,
                    Component.translatable("config.stardewhud.showSeason"),
                    config.showSeason, true
            );
            addWidget(showSeasonCheckbox);
            y += spacing;

            showItemCounterCheckbox = new Checkbox(
                    rightX, y, 110, 20,
                    Component.translatable("config.stardewhud.showItemCounter"),
                    config.showItemCounter, true
            );
            addWidget(showItemCounterCheckbox);
            y += spacing;

            // 日志输出开关
            enableLoggingCheckbox = new Checkbox(
                    leftX, y, 200, 20,
                    Component.translatable("config.stardewhud.enableLogging"),
                    config.enableLogging, true
            );
            addWidget(enableLoggingCheckbox);

            initialized = true;
        }

        @Override
        protected void applyChanges() {
            if (!initialized) return;

            config.showClock = showClockCheckbox.selected();
            config.showWeather = showWeatherCheckbox.selected();
            config.showTimeDisplay = showTimeDisplayCheckbox.selected();
            config.showFortune = showFortuneCheckbox.selected();
            config.showSeason = showSeasonCheckbox.selected();
            config.showItemCounter = showItemCounterCheckbox.selected();
            config.enableLogging = enableLoggingCheckbox.selected();
        }

        @Override
        protected void resetToDefaults() {
            if (!initialized) return;

            ModConfig defaults = new ModConfig();
            config.showClock = defaults.showClock;
            config.showWeather = defaults.showWeather;
            config.showTimeDisplay = defaults.showTimeDisplay;
            config.showFortune = defaults.showFortune;
            config.showSeason = defaults.showSeason;
            config.showItemCounter = defaults.showItemCounter;
            config.enableLogging = defaults.enableLogging;

            showClockCheckbox.onPress();
            showWeatherCheckbox.onPress();
            showTimeDisplayCheckbox.onPress();
            showFortuneCheckbox.onPress();
            showSeasonCheckbox.onPress();
            showItemCounterCheckbox.onPress();
            if (enableLoggingCheckbox.selected() != defaults.enableLogging) {
                enableLoggingCheckbox.onPress();
            }
        }

        @Override
        void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            int centerX = ModConfigScreen.this.width / 2;
        }
    }

    // ========== 效果设置页面 ==========
    private class EffectsPage extends AbstractConfigPage {
        private Checkbox hideVanillaEffectsCheckbox;

        @Override
        Component getTitle() {
            return Component.translatable("category.stardewhud.effects");
        }

        @Override
        void init() {
            int centerX = ModConfigScreen.this.width / 2;
            int startY = PAGE_START_Y;

            // 隐藏原版效果
            hideVanillaEffectsCheckbox = new Checkbox(
                    centerX - 60, startY, 200, 20,
                    Component.translatable("config.stardewhud.hideVanillaEffects"),
                    config.hideVanillaEffects, true
            );
            addWidget(hideVanillaEffectsCheckbox);

            initialized = true;
        }

        @Override
        protected void applyChanges() {
            if (!initialized) return;

            config.hideVanillaEffects = hideVanillaEffectsCheckbox.selected();
        }

        @Override
        protected void resetToDefaults() {
            if (!initialized) return;

            ModConfig defaults = new ModConfig();
            config.hideVanillaEffects = defaults.hideVanillaEffects;
            hideVanillaEffectsCheckbox.onPress();
        }

        @Override
        void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            int centerX = ModConfigScreen.this.width / 2;
            int startY = PAGE_START_Y;

            // 渲染说明
            int descY = startY + 30;
            guiGraphics.drawString(
                    ModConfigScreen.this.font,
                    Component.literal("• ").copy().append(Component.translatable("tip.stardewhud.effects")),
                    centerX - 120, descY, 0xAAAAAA, false
            );
        }
    }

    // ========== 主界面方法 ==========
    private void saveConfig() {
        // 应用当前页面的更改
        if (currentPage != null) {
            currentPage.applyChanges();
        }

        // 保存到文件
        config.save();

        // 显示保存成功的消息
        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.displayClientMessage(
                    Component.translatable("stardewhud.message.config_saved"),
                    false
            );
        }

        // 返回上一界面
        onClose();
    }

    private void resetToDefaults() {
        // 重置当前页面的配置
        if (currentPage != null) {
            currentPage.resetToDefaults();
        }

        // 重新初始化当前页面以更新显示
        if (currentPage != null) {
            currentPage.clearWidgets();
            currentPage.init();
        }

        // 显示重置消息
        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.displayClientMessage(
                    Component.translatable("stardewhud.message.config_reset"),
                    false
            );
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 渲染背景
        this.renderBackground(guiGraphics);

        // 渲染主标题
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 10, 0xFFFFFF);

        // 渲染当前页面
        if (currentPage != null) {
            currentPage.render(guiGraphics, mouseX, mouseY, partialTick);
        }

        // 渲染所有组件
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // 渲染页面指示器
        if (currentPage != null) {
            int centerX = this.width / 2;
            int indicatorY = this.height - 50;

            // 查找当前页面索引
            int currentIndex = pages.indexOf(currentPage);
            if (currentIndex >= 0) {
                String pageText = "Page " + (currentIndex + 1) + " / " + pages.size();
                guiGraphics.drawCenteredString(this.font,
                        Component.literal(pageText),
                        centerX, indicatorY + 5, 0x888888);
            }
        }
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(parent);
        }
    }
}