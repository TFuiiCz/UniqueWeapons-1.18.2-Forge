package com.twistedfantasy.uniqueweapons.network;

import com.twistedfantasy.uniqueweapons.UniqueWeapons;
import com.twistedfantasy.uniqueweapons.items.ArchmageBook;
import com.twistedfantasy.uniqueweapons.items.AxeOfWrath;
import com.twistedfantasy.uniqueweapons.items.VoidAccelerator;
import com.twistedfantasy.uniqueweapons.items.MagicianDeceptionBook;
import com.twistedfantasy.uniqueweapons.items.FallingBook;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Supplier;

public class PacketHandler {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
        new net.minecraft.resources.ResourceLocation(UniqueWeapons.MODID, "main"),
        () -> PROTOCOL_VERSION,
        PROTOCOL_VERSION::equals,
        PROTOCOL_VERSION::equals
    );
    
    private static int id = 0;
    
    public static void register() {
        INSTANCE.registerMessage(id++, SummonPacket.class,
            SummonPacket::encode,
            SummonPacket::decode,
            SummonPacket::handle);
        
        INSTANCE.registerMessage(id++, SwapModePacket.class,
            SwapModePacket::encode,
            SwapModePacket::decode,
            SwapModePacket::handle);
        INSTANCE.registerMessage(id++, SwapArchmageBookModePacket.class,
            SwapArchmageBookModePacket::encode,
            SwapArchmageBookModePacket::decode,
            SwapArchmageBookModePacket::handle);
        INSTANCE.registerMessage(id++, SwapMagicianBookModePacket.class,
            SwapMagicianBookModePacket::encode,
            SwapMagicianBookModePacket::decode,
            SwapMagicianBookModePacket::handle);
            
        INSTANCE.registerMessage(id++, OpenAbilityMenuPacket.class,
            OpenAbilityMenuPacket::encode,
            OpenAbilityMenuPacket::decode,
            OpenAbilityMenuPacket::handle);
            
        INSTANCE.registerMessage(id++, SelectAbilityPacket.class,
            SelectAbilityPacket::encode,
            SelectAbilityPacket::decode,
            SelectAbilityPacket::handle);
    }
    
    public static class SummonPacket {
        public SummonPacket() {}
        
        public SummonPacket(FriendlyByteBuf buf) {}
        
        public void encode(FriendlyByteBuf buf) {}
        
        public static SummonPacket decode(FriendlyByteBuf buf) {
            return new SummonPacket();
        }
        
        public void handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player != null) {
                    ItemStack mainHand = player.getMainHandItem();
                    ItemStack offHand = player.getOffhandItem();
                    if (mainHand.getItem() instanceof AxeOfWrath axe) {
                        axe.activateSummonAbility(player, player.level, mainHand);
                    } else if (offHand.getItem() instanceof AxeOfWrath axe) {
                        axe.activateSummonAbility(player, player.level, offHand);
                    }
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }
    
    public static class SwapModePacket {
        public SwapModePacket() {}
        
        public SwapModePacket(FriendlyByteBuf buf) {}
        
        public void encode(FriendlyByteBuf buf) {}
        
        public static SwapModePacket decode(FriendlyByteBuf buf) {
            return new SwapModePacket();
        }
        
        public void handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player != null) {
                    ItemStack mainHand = player.getMainHandItem();
                    if (mainHand.getItem() instanceof VoidAccelerator voidAccelerator) {
                        voidAccelerator.cycleMode(mainHand, player);
                    }
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }
    
    public static class SwapArchmageBookModePacket {
        public SwapArchmageBookModePacket() {}
        
        public SwapArchmageBookModePacket(FriendlyByteBuf buf) {}
        
        public void encode(FriendlyByteBuf buf) {}
        
        public static SwapArchmageBookModePacket decode(FriendlyByteBuf buf) {
            return new SwapArchmageBookModePacket();
        }
        
        public void handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player != null) {
                    ItemStack mainHand = player.getMainHandItem();
                    if (mainHand.getItem() instanceof ArchmageBook grimoire) {
                        grimoire.cycleMode(mainHand, player);
                    }
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }
    
    public static class SwapMagicianBookModePacket {
        public SwapMagicianBookModePacket() {}
        
        public SwapMagicianBookModePacket(FriendlyByteBuf buf) {}
        
        public void encode(FriendlyByteBuf buf) {}
        
        public static SwapMagicianBookModePacket decode(FriendlyByteBuf buf) {
            return new SwapMagicianBookModePacket();
        }
        
        public void handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player != null) {
                    ItemStack mainHand = player.getMainHandItem();
                    if (mainHand.getItem() instanceof MagicianDeceptionBook book) {
                        book.cycleMode(mainHand, player);
                    }
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }
    
    public static class OpenAbilityMenuPacket {
        public OpenAbilityMenuPacket() {}
        
        public OpenAbilityMenuPacket(FriendlyByteBuf buf) {}
        
        public void encode(FriendlyByteBuf buf) {}
        
        public static OpenAbilityMenuPacket decode(FriendlyByteBuf buf) {
            return new OpenAbilityMenuPacket();
        }
        
        public void handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player != null) {
                    ItemStack mainHand = player.getMainHandItem();
                    ItemStack offHand = player.getOffhandItem();
                    
                    if (mainHand.getItem() instanceof FallingBook book) {
                        book.openAbilityMenu(player, mainHand);
                    } else if (offHand.getItem() instanceof FallingBook book) {
                        book.openAbilityMenu(player, offHand);
                    }
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }
    
    public static class SelectAbilityPacket {
        private final int abilityId;
        
        public SelectAbilityPacket(int abilityId) {
            this.abilityId = abilityId;
        }
        
        public SelectAbilityPacket(FriendlyByteBuf buf) {
            this.abilityId = buf.readInt();
        }
        
        public void encode(FriendlyByteBuf buf) {
            buf.writeInt(abilityId);
        }
        
        public static SelectAbilityPacket decode(FriendlyByteBuf buf) {
            return new SelectAbilityPacket(buf.readInt());
        }
        
        public void handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player != null) {
                    ItemStack mainHand = player.getMainHandItem();
                    ItemStack offHand = player.getOffhandItem();
                    
                    if (mainHand.getItem() instanceof FallingBook book) {
                        book.selectAbility(mainHand, player, abilityId);
                    } else if (offHand.getItem() instanceof FallingBook book) {
                        book.selectAbility(offHand, player, abilityId);
                    }
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }
}