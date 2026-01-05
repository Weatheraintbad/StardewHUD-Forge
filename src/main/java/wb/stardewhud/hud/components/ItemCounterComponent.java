package wb.stardewhud.hud.components;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import wb.stardewhud.StardewHUD;
import wb.stardewhud.hud.HudRenderer;

public class ItemCounterComponent {
    private final HudRenderer hudRenderer;
    private String itemId;
    private Item item;
    private int itemCount = 0;
    private int lastSnapTick = -1;

    private final int COUNTER_WIDTH  = HudRenderer.getCounterWidth();
    private final int COUNTER_HEIGHT = HudRenderer.getCounterHeight();

    private static final int ITEM_LEFT_MARGIN      = 9;
    private static final int TEXT_RIGHT_MARGIN     = 8;
    private static final float TEXT_SCALE          = 1.5f;
    private static final int TEXT_COLOR            = 0xFF8B0000;
    private static final int SHADOW_COLOR          = 0xFFFFFFFF;
    private static final boolean ENABLE_SHADOW     = false;
    private static final int ITEM_ICON_SIZE        = 16;
    private static final int ITEM_VERTICAL_OFFSET  = 4;
    private static final int SCALE_COMPENSATION    = 4;

    private static final String COPPER_COIN_ID = "yoscoins:copper_coin";
    private static final String SILVER_COIN_ID = "yoscoins:silver_coin";
    private static final String GOLD_COIN_ID   = "yoscoins:gold_coin";
    private static final String MONEY_POUCH_ID = "yoscoins:money_pouch";
    private static final String KUBEJS_COIN_ID = "kubejs:coin";

    public ItemCounterComponent(HudRenderer hudRenderer, String itemId) {
        this.hudRenderer = hudRenderer;
        this.itemId = itemId;
        parseItemId();
    }

    public void markInventoryChanged() {
        lastSnapTick = -1;
    }

    public void render(GuiGraphics context, int x, int y) {
        String configItemId = StardewHUD.getConfig().counterItemId;
        if (!configItemId.equals(this.itemId)) {
            this.itemId = configItemId;
            parseItemId();
        }

        float alpha = hudRenderer.getConfig().backgroundAlpha;
        context.setColor(1.0f, 1.0f, 1.0f, alpha);
        context.blit(HudRenderer.COUNTER_BG, x, y, 0, 0, COUNTER_WIDTH, COUNTER_HEIGHT, COUNTER_WIDTH, COUNTER_HEIGHT);
        context.setColor(1.0f, 1.0f, 1.0f, 1.0f);

        if (item != null) {
            int itemX = x + ITEM_LEFT_MARGIN;
            int itemY = y + (COUNTER_HEIGHT - ITEM_ICON_SIZE) / 2 + ITEM_VERTICAL_OFFSET;
            ItemStack stack = new ItemStack(item, 1);
            context.renderItem(stack, itemX, itemY);

            Minecraft client = hudRenderer.getClient();
            String countText = String.valueOf(itemCount);
            int textX = calculateScaledRightAlignedPosition(client, countText, x);
            int textY = y + (COUNTER_HEIGHT - 8) / 2 + 3;
            drawScaledTextWithCustomShadow(context, countText, textX, textY, TEXT_SCALE, TEXT_COLOR, SHADOW_COLOR, ENABLE_SHADOW);
        } else {
            Minecraft client = hudRenderer.getClient();
            String text = "?";
            int textWidth = client.font.width(text);
            int textX = x + (COUNTER_WIDTH - textWidth) / 2;
            int textY = y + (COUNTER_HEIGHT - 8) / 2;
            drawScaledTextWithCustomShadow(context, text, textX, textY, TEXT_SCALE, TEXT_COLOR, SHADOW_COLOR, ENABLE_SHADOW);
        }
    }

    private int calculateScaledRightAlignedPosition(Minecraft client, String text, int counterX) {
        int originalWidth = client.font.width(text);
        float scaledWidth = originalWidth * TEXT_SCALE;
        int targetRightEdge = counterX + COUNTER_WIDTH - TEXT_RIGHT_MARGIN;
        int calculatedX = (int) (targetRightEdge - scaledWidth);
        return calculatedX - SCALE_COMPENSATION;
    }

    private void drawScaledTextWithCustomShadow(GuiGraphics context, String text, int x, int y,
                                                float scale, int textColor, int shadowColor, boolean enableShadow) {
        Minecraft client = hudRenderer.getClient();
        context.pose().pushPose();
        context.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        context.pose().translate(x, y, 0);
        context.pose().scale(scale, scale, 1.0f);

        if (enableShadow) {
            int opaqueShadowColor = shadowColor | 0xFF000000;
            context.drawString(client.font, text, 1, 1, opaqueShadowColor, false);
            int opaqueTextColor = textColor | 0xFF000000;
            context.drawString(client.font, text, 0, 0, opaqueTextColor, false);
        } else {
            int opaqueTextColor = textColor | 0xFF000000;
            context.drawString(client.font, text, 0, 0, opaqueTextColor, false);
        }
        context.pose().popPose();
        context.setColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    public void update() {
        String configItemId = StardewHUD.getConfig().counterItemId;
        if (!configItemId.equals(this.itemId)) {
            this.itemId = configItemId;
        }
        parseItemId();
        snapshotItems();
    }

    private void snapshotItems() {
        Minecraft mc = hudRenderer.getClient();
        if (mc.player == null || item == null) return;

        long now = mc.player.tickCount;
        if (lastSnapTick == (int) now) return;
        lastSnapTick = (int) now;

        itemCount = 0;
        String curId = BuiltInRegistries.ITEM.getKey(item).toString();

        if (curId.equals(KUBEJS_COIN_ID)) {
            int physical = countItemInInventory(KUBEJS_COIN_ID) + countItemInPouches(KUBEJS_COIN_ID);
            int electronic = (int) getSDMMoneyReflect(mc.player);   // ← 零依赖反射
            itemCount = physical + electronic;
            StardewHUD.LOGGER.info("[CoinHUD] 实物:{} + 电子:{} = 总计:{}", physical, electronic, itemCount);
            return;
        }

        boolean isCountingCopper = curId.equals(COPPER_COIN_ID);
        if (isCountingCopper) {
            calculateTotalCopperValue(mc);
            return;
        }

        countItemsInInventorySlots(mc.player.getInventory().items);
        countItemsInInventorySlots(mc.player.getInventory().offhand);
        countItemsInInventorySlots(mc.player.getInventory().armor);
        if (isCoinItem()) countItemsInMoneyPouches(mc);
    }

    private long getSDMMoneyReflect(Player player) {
        try {
            Class<?> clazz = Class.forName("net.sixik.sdmshoprework.SDMShopR");
            java.lang.reflect.Method m = clazz.getMethod("getMoney", Player.class);
            return (Long) m.invoke(null, player);
        } catch (Exception e) {
            StardewHUD.LOGGER.debug("[CoinHUD] SDMShop 未安装或方法变动，返回 0: {}", e.toString());
        }
        return 0L;
    }

    private int countItemInInventory(String targetId) {
        int sum = 0;
        sum += countItemIn(hudRenderer.getClient().player.getInventory().items, targetId);
        sum += countItemIn(hudRenderer.getClient().player.getInventory().offhand, targetId);
        sum += countItemIn(hudRenderer.getClient().player.getInventory().armor, targetId);
        return sum;
    }
    private int countItemIn(Iterable<ItemStack> slots, String targetId) {
        int sum = 0;
        for (ItemStack s : slots) {
            if (s.isEmpty()) continue;
            if (targetId.equals(BuiltInRegistries.ITEM.getKey(s.getItem()).toString())) {
                sum += s.getCount();
            }
        }
        return sum;
    }
    private int countItemInPouches(String targetId) {
        Minecraft mc = hudRenderer.getClient();
        int sum = 0;
        for (ItemStack stack : mc.player.getInventory().items) {
            if (!isMoneyPouch(stack)) continue;
            SimpleContainer pouch = readMoneyPouchInventory(stack);
            if (pouch == null) continue;
            for (int i = 0; i < pouch.getContainerSize(); i++) {
                ItemStack in = pouch.getItem(i);
                if (in.isEmpty()) continue;
                if (targetId.equals(BuiltInRegistries.ITEM.getKey(in.getItem()).toString())) {
                    sum += in.getCount();
                }
            }
        }
        for (ItemStack stack : mc.player.getInventory().offhand) {
            if (!isMoneyPouch(stack)) continue;
            SimpleContainer pouch = readMoneyPouchInventory(stack);
            if (pouch == null) continue;
            for (int i = 0; i < pouch.getContainerSize(); i++) {
                ItemStack in = pouch.getItem(i);
                if (in.isEmpty()) continue;
                if (targetId.equals(BuiltInRegistries.ITEM.getKey(in.getItem()).toString())) {
                    sum += in.getCount();
                }
            }
        }
        return sum;
    }

    private void calculateTotalCopperValue(Minecraft mc) {
        int[] counts = new int[3];
        counts = countCoinsInSlots(mc.player.getInventory().items, counts);
        counts = countCoinsInSlots(mc.player.getInventory().offhand, counts);
        counts = countCoinsInSlots(mc.player.getInventory().armor, counts);
        int electronic = (int) getSDMMoneyReflect(mc.player); // ← 官方入口
        itemCount = counts[0] + (9 * counts[1]) + (81 * counts[2]) + electronic;
        StardewHUD.LOGGER.info("[CopperHUD] 铜:{} 银:{} 金:{} 电子:{} → 总铜:{}", counts[0], counts[1], counts[2], electronic, itemCount);
    }

    private int[] countCoinsInSlots(Iterable<ItemStack> slots, int[] currentCounts) {
        int[] counts = new int[]{currentCounts[0], currentCounts[1], currentCounts[2]};
        for (ItemStack stack : slots) {
            if (stack.isEmpty()) continue;
            String itemIdString = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            int count = stack.getCount();
            switch (itemIdString) {
                case COPPER_COIN_ID -> counts[0] += count;
                case SILVER_COIN_ID -> counts[1] += count;
                case GOLD_COIN_ID -> counts[2] += count;
                case KUBEJS_COIN_ID -> counts[0] += count;
                case MONEY_POUCH_ID -> {
                    SimpleContainer pouch = readMoneyPouchInventory(stack);
                    if (pouch != null) {
                        for (int i = 0; i < pouch.getContainerSize(); i++) {
                            ItemStack inside = pouch.getItem(i);
                            if (inside.isEmpty()) continue;
                            String inId = BuiltInRegistries.ITEM.getKey(inside.getItem()).toString();
                            int inCnt = inside.getCount();
                            switch (inId) {
                                case COPPER_COIN_ID -> counts[0] += inCnt;
                                case SILVER_COIN_ID -> counts[1] += inCnt;
                                case GOLD_COIN_ID -> counts[2] += inCnt;
                                case KUBEJS_COIN_ID -> counts[0] += inCnt;
                            }
                        }
                    }
                }
            }
        }
        return counts;
    }

    private void countItemsInInventorySlots(Iterable<ItemStack> slots) {
        for (ItemStack stack : slots) {
            if (!stack.isEmpty() && stack.getItem() == item) {
                itemCount += stack.getCount();
            }
        }
    }

    private void countItemsInMoneyPouches(Minecraft mc) {
        for (ItemStack stack : mc.player.getInventory().items) {
            if (isMoneyPouch(stack)) {
                SimpleContainer pouch = readMoneyPouchInventory(stack);
                if (pouch != null) {
                    for (int i = 0; i < pouch.getContainerSize(); i++) {
                        ItemStack pouchItem = pouch.getItem(i);
                        if (!pouchItem.isEmpty() && pouchItem.getItem() == item) {
                            itemCount += pouchItem.getCount();
                        }
                    }
                }
            }
        }
        for (ItemStack stack : mc.player.getInventory().offhand) {
            if (isMoneyPouch(stack)) {
                SimpleContainer pouch = readMoneyPouchInventory(stack);
                if (pouch != null) {
                    for (int i = 0; i < pouch.getContainerSize(); i++) {
                        ItemStack pouchItem = pouch.getItem(i);
                        if (!pouchItem.isEmpty() && pouchItem.getItem() == item) {
                            itemCount += pouchItem.getCount();
                        }
                    }
                }
            }
        }
    }

    private boolean isCoinItem() {
        String itemIdString = BuiltInRegistries.ITEM.getKey(item).toString();
        return itemIdString.equals(COPPER_COIN_ID) ||
                itemIdString.equals(SILVER_COIN_ID) ||
                itemIdString.equals(GOLD_COIN_ID);
    }

    private boolean isMoneyPouch(ItemStack stack) {
        if (stack.isEmpty()) return false;
        String itemIdString = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        return itemIdString.equals(MONEY_POUCH_ID);
    }

    private SimpleContainer readMoneyPouchInventory(ItemStack moneyPouch) {
        if (moneyPouch.isEmpty()) return null;
        try {
            try {
                Class<?> moneyPouchClass = Class.forName("yoscoins.item.MoneyPouchItem");
                java.lang.reflect.Method readInvMethod = moneyPouchClass.getMethod("readInv", ItemStack.class);
                Object result = readInvMethod.invoke(null, moneyPouch);
                if (result instanceof SimpleContainer) {
                    return (SimpleContainer) result;
                }
            } catch (ClassNotFoundException | NoSuchMethodException e) {
                StardewHUD.LOGGER.debug("钱袋API不可用，尝试NBT方式: {}", e.getMessage());
            }
            return readMoneyPouchFromNBT(moneyPouch);
        } catch (Exception e) {
            StardewHUD.LOGGER.warn("读取钱袋内容失败: {}", e.getMessage());
            return null;
        }
    }

    private SimpleContainer readMoneyPouchFromNBT(ItemStack moneyPouch) {
        if (!moneyPouch.hasTag()) {
            return new SimpleContainer(0);
        }
        try {
            CompoundTag nbt = moneyPouch.getTag();
            if (nbt != null && nbt.contains("Items")) {
                ListTag itemList = nbt.getList("Items", CompoundTag.TAG_COMPOUND);
                SimpleContainer inventory = new SimpleContainer(itemList.size());
                for (int i = 0; i < itemList.size(); i++) {
                    CompoundTag itemNbt = itemList.getCompound(i);
                    ItemStack itemStack = ItemStack.of(itemNbt);
                    inventory.setItem(i, itemStack);
                }
                return inventory;
            }
        } catch (Exception e) {
            StardewHUD.LOGGER.warn("通过NBT读取钱袋失败: {}", e.getMessage());
        }
        return new SimpleContainer(0);
    }

    private void parseItemId() {
        try {
            ResourceLocation id = ResourceLocation.tryParse(itemId);
            if (id != null && BuiltInRegistries.ITEM.containsKey(id)) {
                item = BuiltInRegistries.ITEM.get(id);
            } else {
                item = null;
                StardewHUD.LOGGER.warn("物品ID无效: {}", itemId);
            }
        } catch (Exception e) {
            item = null;
            StardewHUD.LOGGER.error("解析物品ID时出错: {}", itemId, e);
        }
    }

    public void setItemId(String newItemId) {
        StardewHUD.getConfig().counterItemId = newItemId;
        this.itemId = newItemId;
        parseItemId();
        markInventoryChanged();
    }
}