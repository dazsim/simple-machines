package com.workshopcraft.simplemachines.entangled;

import com.workshopcraft.simplemachines.SimpleMachines;
import com.workshopcraft.simplemachines.assembly.Core;
import com.workshopcraft.simplemachines.assembly.Tier;
import com.workshopcraft.simplemachines.machine.EnergyMenu;
import com.workshopcraft.simplemachines.machine.WorkBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Writes a 6 digit code onto the Link Card in its slot, 500 FE a time. */
public class EntangledProgrammerBlockEntity extends WorkBlockEntity {
    public static final int CAPACITY = 10_000, MAX_IN = 200, PER_WRITE = 500;
    public static final int DATA_COUNT = EnergyMenu.DATA_COUNT + 4;
    public static final int NONE = 0, WROTE = 1, NO_CARD = 2, NO_POWER = 3, NOT_OWNER = 4, NO_CODE = 5;
    private int code = -1;
    private boolean priv;
    private int status = NONE;

    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int id) {
            return switch (id - EnergyMenu.DATA_COUNT) {
                case 0 -> (code + 1) & 0xFFFF;
                case 1 -> (code + 1) >>> 16;
                case 2 -> priv ? 1 : 0;
                case 3 -> status;
                default -> data.get(id);
            };
        }

        @Override
        public void set(int id, int value) {
            if (id < EnergyMenu.DATA_COUNT) data.set(id, value);
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public EntangledProgrammerBlockEntity(BlockPos pos, BlockState state) {
        super(SimpleMachines.MACHINE_ENTITIES.get(Core.PROGRAMMER), pos, state, 1, CAPACITY, MAX_IN, 0);
    }

    @Override
    protected boolean isOutput(int slot) {
        return false;
    }

    @Override
    protected boolean accepts(ServerLevel level, int slot, ItemStack stack) {
        return EntangledLink.programmable(stack);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean energyOut(net.minecraft.core.Direction side) {
        return false;
    }

    @Override
    protected void work(ServerLevel level, BlockPos pos, BlockState state, Tier tier) {
        setLit(level, pos, state, energy.energy >= PER_WRITE);
    }

    public int code() {
        return code;
    }

    public boolean isPrivate() {
        return priv;
    }

    public void setCode(int c) {
        code = Math.clamp(c, 0, EntangledLink.MAX);
        status = NONE;
        setChanged();
    }

    public void random(ServerLevel level, Player player) {
        setCode(EntangledData.get(level.getServer()).freeCode(priv ? player.getUUID().toString() : "public", level.getRandom()));
    }

    public void togglePrivate() {
        priv = !priv;
        status = NONE;
        setChanged();
    }

    public void write(ServerLevel level, Player player) {
        final ItemStack card = items.get(0);
        final EntangledLink old = card.get(SimpleMachines.ENTANGLED_LINK);
        if (code < 0) status = NO_CODE;
        else if (card.isEmpty()) status = NO_CARD;
        else if (old != null && !EntangledLink.allows(old, player)) status = NOT_OWNER;
        else if (energy.energy < PER_WRITE) status = NO_POWER;
        else {
            energy.energy -= PER_WRITE;
            card.set(SimpleMachines.ENTANGLED_LINK, old == null ? new EntangledLink(code, player.getUUID(), priv, "")
                    : old.withCode(code, player.getUUID(), priv));
            status = WROTE;
            level.playSound(null, getBlockPos(), SoundEvents.ENDER_EYE_DEATH, SoundSource.BLOCKS, 0.6F, 1.4F);
        }
        setChanged();
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        code = input.getIntOr("code", -1);
        priv = input.getBooleanOr("private", false);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("code", code);
        output.putBoolean("private", priv);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.simplemachines.entangled_programmer");
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        status = NONE;
        return new EntangledProgrammerMenu(id, inventory, this, menuData, this);
    }
}
