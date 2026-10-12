package com.jbarnes.turtledragonquests.registry;

import java.util.function.Supplier;

import com.jbarnes.turtledragonquests.TDQ;
import com.jbarnes.turtledragonquests.currency.WalletAttachment;
import com.jbarnes.turtledragonquests.quest.QuestProgressAttachment;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class TDQAttachmentTypes {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, TDQ.MOD_ID);

    public static final Supplier<AttachmentType<WalletAttachment>> WALLET = ATTACHMENTS.register("wallet", () ->
            AttachmentType.builder(WalletAttachment::empty)
                    .serialize(WalletAttachment.CODEC)
                    .copyOnDeath()
                    .build());

    public static final Supplier<AttachmentType<QuestProgressAttachment>> QUEST_PROGRESS =
            ATTACHMENTS.register("quest_progress", () ->
                    AttachmentType.builder(QuestProgressAttachment::empty)
                            .serialize(QuestProgressAttachment.CODEC)
                            .copyOnDeath()
                            .build());

    private TDQAttachmentTypes() {}

    public static void register(IEventBus modEventBus) {
        ATTACHMENTS.register(modEventBus);
    }
}
