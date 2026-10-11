package io.github.sprocketstudios.sprocketdyes.mixin.client;

import io.github.sprocketstudios.sprocketdyes.client.CreativeSections;
import io.github.sprocketstudios.sprocketdyes.client.SectionedCreativeScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Mixin;
//? if >=26.3 {
import io.github.sprocketstudios.sprocketdyes.client.SectionHeaderRenderer;
import java.util.Collection;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//?}

/**
 * Sections the "Colored Blocks" tab (see {@link CreativeSections}): rebuilds the screen's item list when the
 * tab is shown, toggles a section when its header row is clicked, and draws the headers over their rows.
 */
@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeModeInventoryScreenMixin extends AbstractContainerScreen<CreativeModeInventoryScreen.ItemPickerMenu> implements SectionedCreativeScreen {
    private CreativeModeInventoryScreenMixin(CreativeModeInventoryScreen.ItemPickerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    //? if >=26.3 {
    @Shadow private static CreativeModeTab selectedTab;
    @Shadow protected abstract void selectTab(CreativeModeTab tab);
    @Shadow private float scrollOffs;

    private static boolean sprocketDyes$isColoredBlocks(CreativeModeTab tab) {
        return tab == BuiltInRegistries.CREATIVE_MODE_TAB.getValue(CreativeModeTabs.COLORED_BLOCKS);
    }

    /** Re-lays out the list, keeping the same top row where possible. */
    private void sprocketDyes$relayout(boolean keepScroll) {
        var picker = (ItemPickerMenuAccessor) this.menu;
        int row = keepScroll ? picker.sprocketDyes$getRowIndexForScroll(this.scrollOffs) : 0;
        if (sprocketDyes$isColoredBlocks(selectedTab)) {
            CreativeSections.apply(this.menu.items);
        } else {
            CreativeSections.deactivate();
        }
        this.scrollOffs = picker.sprocketDyes$getScrollForRowIndex(row);
        this.menu.scrollTo(this.scrollOffs);
    }

    @Inject(method = "selectTab", at = @At("TAIL"))
    private void sprocketDyes$onSelectTab(CreativeModeTab tab, CallbackInfo ci) {
        if (sprocketDyes$isColoredBlocks(tab) || CreativeSections.isActive()) this.sprocketDyes$relayout(false);
    }

    @Inject(method = "refreshCurrentTabContents", at = @At("TAIL"))
    private void sprocketDyes$onRefresh(Collection<ItemStack> displayList, CallbackInfo ci) {
        if (sprocketDyes$isColoredBlocks(selectedTab)) this.sprocketDyes$relayout(true);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void sprocketDyes$onClick(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        CreativeSections.Section section = this.sprocketDyes$headerUnder(event.x(), event.y());
        if (section == null) return;
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            CreativeSections.toggle(section);
            this.menu.items.clear();
            this.menu.items.addAll(selectedTab.getDisplayItems());
            this.sprocketDyes$relayout(true);
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
        cir.setReturnValue(true);  // header rows are never item slots
    }

    /** The header under the mouse, if it's over a visible header row of the item grid. */
    private CreativeSections.Section sprocketDyes$headerUnder(double mouseX, double mouseY) {
        if (!CreativeSections.isActive() || !sprocketDyes$isColoredBlocks(selectedTab)) return null;
        double gx = mouseX - (this.leftPos + 8), gy = mouseY - (this.topPos + 17);
        if (gx < 0 || gx >= SectionHeaderRenderer.WIDTH || gy < 0 || gy >= 5 * SectionHeaderRenderer.HEIGHT) return null;
        int first = ((ItemPickerMenuAccessor) this.menu).sprocketDyes$getRowIndexForScroll(this.scrollOffs);
        return CreativeSections.headerAt(first + (int) (gy / SectionHeaderRenderer.HEIGHT));
    }

    @Override
    public void sprocketDyes$showColoredBlocks() {
        this.selectTab(BuiltInRegistries.CREATIVE_MODE_TAB.getValue(CreativeModeTabs.COLORED_BLOCKS));
    }

    @Override
    public void sprocketDyes$clickVisibleRow(int visibleRow) {
        double x = this.leftPos + 8 + SectionHeaderRenderer.WIDTH / 2.0;
        double y = this.topPos + 17 + visibleRow * SectionHeaderRenderer.HEIGHT + SectionHeaderRenderer.HEIGHT / 2.0;
        this.mouseClicked(new MouseButtonEvent(x, y, new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0)), false);
    }

    /** Called from AbstractContainerScreenMixin after the slots are drawn (below the carried item and tooltips). */
    @Override
    public void sprocketDyes$drawHeaders(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (!CreativeSections.isActive() || !sprocketDyes$isColoredBlocks(selectedTab)) return;
        int first = ((ItemPickerMenuAccessor) this.menu).sprocketDyes$getRowIndexForScroll(this.scrollOffs);
        CreativeSections.Section hovered = this.sprocketDyes$headerUnder(mouseX, mouseY);
        for (int r = 0; r < 5; r++) {
            CreativeSections.Section section = CreativeSections.headerAt(first + r);
            if (section != null) {
                SectionHeaderRenderer.draw(graphics, this.font, section, this.leftPos + 8, this.topPos + 17 + r * SectionHeaderRenderer.HEIGHT, section == hovered);
            }
        }
    }
    //?}
}
