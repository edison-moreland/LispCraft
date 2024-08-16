package net.devdude.lispcraft.mod;

import io.wispforest.owo.itemgroup.Icon;
import io.wispforest.owo.itemgroup.OwoItemGroup;
import io.wispforest.owo.registration.reflect.BlockEntityRegistryContainer;
import io.wispforest.owo.registration.reflect.BlockRegistryContainer;
import io.wispforest.owo.registration.reflect.FieldRegistrationHandler;
import io.wispforest.owo.registration.reflect.ItemRegistryContainer;
import net.devdude.lispcraft.mod.common.console.ConsoleBlock;
import net.devdude.lispcraft.mod.common.console.ConsoleBlockEntity;
import net.devdude.lispcraft.mod.common.console.ConsoleScreenHandler;
import net.fabricmc.api.ModInitializer;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Mod implements ModInitializer {

    public static final String MOD_ID = "lispcraft";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final OwoItemGroup MOD_GROUP = OwoItemGroup
            .builder(Identifier.of(MOD_ID, "item_group"), () -> Icon.of(Blocks.CONSOLE))
            .build();

    public static Identifier id(String name) {
        return Identifier.of(MOD_ID, name);
    }

    @Override
    public void onInitialize() {
        FieldRegistrationHandler.register(Items.class, MOD_ID, false);
        FieldRegistrationHandler.register(Blocks.class, MOD_ID, false);
        FieldRegistrationHandler.register(BlockEntities.class, MOD_ID, false);
        FieldRegistrationHandler.register(ScreenHandlers.class, MOD_ID, false);
        MOD_GROUP.initialize();
        Network.initialize();
    }

    public static class Items implements ItemRegistryContainer {
    }

    public static class Blocks implements BlockRegistryContainer {
        public static final Block CONSOLE = new ConsoleBlock(AbstractBlock.Settings.create());

        @Override
        public BlockItem createBlockItem(Block block, String identifier) {
            return new BlockItem(block, new Item.Settings().group(Mod.MOD_GROUP));
        }
    }

    public static class BlockEntities implements BlockEntityRegistryContainer {
        public static final BlockEntityType<ConsoleBlockEntity> CONSOLE = BlockEntityType.Builder.create(ConsoleBlockEntity::new, Blocks.CONSOLE).build();
    }

    public static class ScreenHandlers implements ScreenHandlerRegistryContainer {
        public static final ScreenHandlerType<ConsoleScreenHandler> CONSOLE = new ScreenHandlerType<>(ConsoleScreenHandler::new, FeatureFlags.VANILLA_FEATURES);
    }
}
