package com.hzldm.createnavigationterminal.menu;

import com.hzldm.createnavigationterminal.blockentity.NavigationTerminalBlockEntity;
import com.hzldm.createnavigationterminal.item.WaypointCardItem;
import com.hzldm.createnavigationterminal.registry.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Navigation terminal menu.
 *
 * Carries one route-card slot plus the player inventory slots.
 * Server side binds to the BlockEntity container; client side only receives a
 * BlockPos, so it uses a stub container to keep both slot lists identical.
 */
public class NavigationTerminalMenu extends AbstractContainerMenu {

    private static final double MAX_INTERACT_DISTANCE_SQR = 64.0;

    /** Card slot position inside the GUI (shared with the Screen). */
    public static final int CARD_SLOT_X = 6;
    public static final int CARD_SLOT_Y = 36;

    /** Player inventory origin inside the GUI. */
    public static final int PLAYER_INV_Y = 194;

    private final BlockPos terminalPos;

    /** Client-side placeholder so both sides register the same slot count. */
    private final SimpleContainer clientStub = new SimpleContainer(1);

    public NavigationTerminalMenu(int id, Inventory inventory, BlockPos pos) {
        super(ModMenuTypes.NAVIGATION_TERMINAL.get(), id);
        this.terminalPos = pos;
        this.addSlot(makeCardSlot(clientStub));
        addPlayerSlots(inventory);
    }

    public NavigationTerminalMenu(int id, Inventory inventory,
                                  NavigationTerminalBlockEntity blockEntity) {
        super(ModMenuTypes.NAVIGATION_TERMINAL.get(), id);
        this.terminalPos = blockEntity.getBlockPos();
        this.addSlot(makeCardSlot(blockEntity.getCardSlot()));
        addPlayerSlots(inventory);
    }

    private static Slot makeCardSlot(Container container) {
        return new Slot(container, 0, CARD_SLOT_X, CARD_SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof WaypointCardItem;
            }
        };
    }

    /**
     * Register the player inventory.
     *
     * These slots are mandatory even when not visually emphasised: without them the
     * client can send a click for a slot index the server does not have, which
     * disconnects the player with a network protocol error.
     */
    private void addPlayerSlots(Inventory inventory) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory,
                        col + row * 9 + 9,
                        59 + col * 18,
                        PLAYER_INV_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventory, col, 59 + col * 18, PLAYER_INV_Y + 58));
        }
    }

    public BlockPos getTerminalPos() {
        return terminalPos;
    }

    @Nullable
    public NavigationTerminalBlockEntity terminal(Player player) {
        return player.level().getBlockEntity(terminalPos)
                instanceof NavigationTerminalBlockEntity be ? be : null;
    }

    @Override
    public boolean stillValid(Player player) {
        if (player.level().getBlockEntity(terminalPos) == null) {
            return false;
        }
        return player.distanceToSqr(
                terminalPos.getX() + 0.5,
                terminalPos.getY() + 0.5,
                terminalPos.getZ() + 0.5) <= MAX_INTERACT_DISTANCE_SQR;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= this.slots.size()) {
            return ItemStack.EMPTY;
        }
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack inSlot = slot.getItem();
        ItemStack copy = inSlot.copy();

        if (index == 0) {
            if (!this.moveItemStackTo(inSlot, 1, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (!(inSlot.getItem() instanceof WaypointCardItem)) {
                return ItemStack.EMPTY;
            }
            if (!this.moveItemStackTo(inSlot, 0, 1, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (inSlot.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return copy;
    }
}
