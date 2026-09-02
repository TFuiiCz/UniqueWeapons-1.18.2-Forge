package com.twistedfantasy.uniqueweapons;

import com.mojang.logging.LogUtils;
import com.twistedfantasy.uniqueweapons.init.WeaponTiers;
import com.twistedfantasy.uniqueweapons.items.BladeOfMadness;
import com.twistedfantasy.uniqueweapons.items.MagmaBlade;
import com.twistedfantasy.uniqueweapons.items.ThunderSpear;
import com.twistedfantasy.uniqueweapons.items.ThunderSpearThrown;
import com.twistedfantasy.uniqueweapons.items.ScytheSinner;
import com.twistedfantasy.uniqueweapons.items.ArchmageBook;
import com.twistedfantasy.uniqueweapons.items.AxeOfWrath;
import com.twistedfantasy.uniqueweapons.items.VoidAccelerator;
import com.twistedfantasy.uniqueweapons.items.FallingBook;
import com.twistedfantasy.uniqueweapons.items.MagicianDeceptionBook;
import com.twistedfantasy.uniqueweapons.entity.WrathSummonedSkeleton;
import com.twistedfantasy.uniqueweapons.entity.WrathSummonedZombie;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

@Mod(UniqueWeapons.MODID)
public class UniqueWeapons{
    public static final String MODID = "uniqueweapons";
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = 
    DeferredRegister.create(ForgeRegistries.ENTITIES, MODID);
    public static final RegistryObject<EntityType<ThunderSpearThrown>> CUSTOM_TRIDENT = ENTITY_TYPES.register(
        "thunder_spear",
        () -> EntityType.Builder.<ThunderSpearThrown>of(ThunderSpearThrown::new, MobCategory.MISC)
            .sized(0.5F, 0.5F)
            .clientTrackingRange(4)
            .updateInterval(20)
            .build("thunder_spear")
    );
    
    public static final RegistryObject<EntityType<WrathSummonedZombie>> WRATH_ZOMBIE = 
    ENTITY_TYPES.register("wrath_zombie",
        () -> EntityType.Builder.of(WrathSummonedZombie::new, MobCategory.MONSTER)
            .sized(0.6F, 1.95F)
            .clientTrackingRange(8)
            .build("wrath_zombie"));
    
    public static final RegistryObject<EntityType<WrathSummonedSkeleton>> WRATH_SKELETON = 
        ENTITY_TYPES.register("wrath_skeleton",
            () -> EntityType.Builder.of(WrathSummonedSkeleton::new, MobCategory.MONSTER)
                .sized(0.6F, 1.99F)
                .clientTrackingRange(8)
                .build("wrath_skeleton"));
    
    public static final RegistryObject<Item> AXE_OF_WRATH = ITEMS.register("axe_of_wrath",
        () -> new AxeOfWrath(
            Tiers.NETHERITE,
            13,
            -3.5F,
            new Item.Properties()
                .tab(CreativeModeTab.TAB_COMBAT)
                .fireResistant()
                .rarity(Rarity.EPIC)
        )
    );
    
    public static final RegistryObject<Item> MAGMA_BLADE = ITEMS.register("magma_blade",
        () -> new MagmaBlade(
            Tiers.NETHERITE,
            0,
            -1.7F,
            new Item.Properties()
                .tab(CreativeModeTab.TAB_COMBAT)
                .fireResistant()
                .rarity(Rarity.EPIC)
        )
    );
    public static final RegistryObject<Item> VOID_ACCELERATOR = ITEMS.register("void_accelerator", 
        () -> new VoidAccelerator(
            new Item.Properties()
                .tab(CreativeModeTab.TAB_COMBAT)
                .fireResistant()
                .rarity(Rarity.EPIC)
                .durability(1500)
        ));
    public static final RegistryObject<Item> THUNDER_SPEAR = ITEMS.register("thunder_spear",
        () -> new ThunderSpear(
            new Item.Properties()
                .tab(CreativeModeTab.TAB_COMBAT)
                .fireResistant()
                .durability(2500))
    );
    
    public static final RegistryObject<Item> BLADE_OF_MADNESS = ITEMS.register("blade_of_madness", 
        () -> new BladeOfMadness(
            WeaponTiers.BLADE_OF_MADNESS,
            5,
            -1.7F,
            new Item.Properties()
                .tab(CreativeModeTab.TAB_COMBAT)
                .fireResistant()
                .rarity(Rarity.EPIC)
        ));
    public static final RegistryObject<Item> ARCHMAGEBOOK = ITEMS.register("archmage_book",
    () -> new ArchmageBook(new Item.Properties()));
    public static final RegistryObject<Item> DECEPTIONBOOK = ITEMS.register("deception_book",
    () -> new MagicianDeceptionBook(new Item.Properties()));
    public static final RegistryObject<Item> FALLINGBOOK = ITEMS.register("falling_book",
    () -> new FallingBook(new Item.Properties()));
    
    public static final RegistryObject<Item> SCYTHE_SINNER = ITEMS.register("scythe_sinner", 
        () -> new ScytheSinner(
            Tiers.NETHERITE,
            0,
            -2.4F,
            new Item.Properties()
                .tab(CreativeModeTab.TAB_COMBAT)
                .durability(2500)
                .fireResistant()
                .rarity(Rarity.EPIC)
        ));
    
    public static ResourceLocation rl(String path) {
        return new ResourceLocation(MODID, path);
    }
    
    public UniqueWeapons() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ENTITY_TYPES.register(modEventBus);
        ITEMS.register(modEventBus);
        
        com.twistedfantasy.uniqueweapons.network.PacketHandler.register();
        
        MinecraftForge.EVENT_BUS.register(this);
    }
}