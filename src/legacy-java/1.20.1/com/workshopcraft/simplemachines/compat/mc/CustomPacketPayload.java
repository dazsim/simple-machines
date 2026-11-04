package com.workshopcraft.simplemachines.compat.mc;

import net.minecraft.resources.ResourceLocation;

/** 1.20.5+ payload marker; the loader entrypoints send it on the type id channel. */
public interface CustomPacketPayload {
    Type<? extends CustomPacketPayload> type();

    record Type<T extends CustomPacketPayload>(ResourceLocation id) {
    }
}
