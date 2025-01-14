package commoble.morered.datagen;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import org.apache.commons.lang3.text.WordUtils;

import com.google.common.collect.ImmutableList;

import commoble.morered.HexidecrubrometerBlock;
import commoble.morered.MoreRed;
import commoble.morered.ObjectNames;
import commoble.morered.bitwise_logic.TwoInputBitwiseLogicPlateBlock;
import commoble.morered.datagen.BlockStateFile.Case;
import commoble.morered.datagen.BlockStateFile.Model;
import commoble.morered.datagen.BlockStateFile.Multipart;
import commoble.morered.datagen.BlockStateFile.PropertyValue;
import commoble.morered.datagen.BlockStateFile.Variants;
import commoble.morered.datagen.BlockStateFile.WhenApply;
import commoble.morered.plate_blocks.PlateBlock;
import commoble.morered.plate_blocks.PlateBlockStateProperties;
import commoble.morered.soldering.SolderingRecipe;
import commoble.morered.wires.AbstractWireBlock;
import commoble.morered.wires.ColoredCableBlock;
import commoble.morered.wires.WireCountLootFunction;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.ExplosionCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.crafting.IntersectionIngredient;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.common.data.LanguageProvider;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

@Mod("morered_datagen")
@EventBusSubscriber(modid="morered_datagen", bus=Bus.MOD)
public class MoreRedDataGen
{
	static record DataGenContext(Map<ResourceLocation, BlockStateFile> blockStates,
		Map<ResourceLocation, SimpleModel> models,
		Map<ResourceLocation, WirePartModelDefinition> wirePartModels,
		Map<ResourceLocation, Recipe<?>> recipes,
		Map<ResourceLocation, LootTable> lootTables,
		TagProvider<Block> blockTags,
		TagProvider<Item> itemTags,
		LanguageProvider lang) {}
	
	static final TagKey<Item> SMOOTH_STONE = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "smooth_stone"));
	static final TagKey<Item> SMOOTH_STONE_SLABS = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "slabs/smooth_stone"));
	static final TagKey<Item> SMOOTH_STONE_QUARTER_SLABS = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "quarter_slabs/smooth_stone"));
	static final TagKey<Item> REDSTONE_ALLOY_INGOTS = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "ingots/redstone_alloy"));

	@SuppressWarnings("deprecation")
	@SubscribeEvent
	static void onGatherData(GatherDataEvent event)
	{
		DataGenerator generator = event.getGenerator();
		CompletableFuture<HolderLookup.Provider> holders = event.getLookupProvider();
		PackOutput output = generator.getPackOutput();
		
		Map<ResourceLocation, BlockStateFile> blockStates = new HashMap<>();
		Map<ResourceLocation, SimpleModel> models = new HashMap<>();
		Map<ResourceLocation, WirePartModelDefinition> wirePartModels = new HashMap<>();
		Map<ResourceLocation, Recipe<?>> recipes = new HashMap<>();
		Map<ResourceLocation, LootTable> lootTables = new HashMap<>();
		TagProvider<Block> blockTags = TagProvider.create(event, Registries.BLOCK, holders);
		TagProvider<Item> itemTags = TagProvider.create(event, Registries.ITEM, holders);
		LanguageProvider lang = new LanguageProvider(output, MoreRed.MODID, "en_us")
		{
			@Override
			protected void addTranslations()
			{} // no
		};
		DataGenContext context = new DataGenContext(blockStates, models, wirePartModels, recipes, lootTables, blockTags, itemTags, lang);
		
		Function<String, Block> getBlock = s -> BuiltInRegistries.BLOCK.get(MoreRed.getModRL(s));
		Function<String, Item> getItem = s -> BuiltInRegistries.ITEM.get(MoreRed.getModRL(s));
		String fromSoldering = "%s_from_soldering";
		
		// blocks
		redstonePlateBlock(ObjectNames.AND_2_GATE, "Two Input AND Gate", context, 4,
			"#t#",
			"trt",
			"###");
		redstonePlateBlock(ObjectNames.AND_GATE, "AND Gate", context, 5,
			"#t#",
			"trt",
			"#t#");
		bitwisePlateBlock(ObjectNames.BITWISE_AND_GATE, "Bitwise AND Gate", "and_gate_symbol", context);
		bitwisePlateBlock(ObjectNames.BITWISE_DIODE, "Bitwise Diode", "diode_symbol", context);
		bitwisePlateBlock(ObjectNames.BITWISE_NOT_GATE, "Bitwise NOT Gate", "not_gate_symbol", context);
		bitwisePlateBlock(ObjectNames.BITWISE_OR_GATE, "Bitwise OR Gate", "or_gate_symbol", context);
		bitwisePlateBlock(ObjectNames.BITWISE_XNOR_GATE, "Bitwise XNOR Gate", "xnor_gate_symbol", context);
		bitwisePlateBlock(ObjectNames.BITWISE_XOR_GATE, "Bitwise XOR Gate", "xor_gate_symbol", context);
		postBlock(ObjectNames.BUNDLED_CABLE_POST, "Bundled Cable Post", context)
			.tags(blockTags, MoreRed.Tags.Blocks.BUNDLED_CABLE_POSTS)
			.blockItem()
			.help(helper -> helper.recipe(recipes, RecipeHelpers.shapeless(helper.item(), 1, CraftingBookCategory.REDSTONE, List.of(
					Ingredient.of(MoreRed.Tags.Items.BUNDLED_NETWORK_CABLES),
					Ingredient.of(Tags.Items.INGOTS_IRON))))
				.recipe(recipes, mangle(helper.id(), fromSoldering), new SolderingRecipe(new ItemStack(helper.item()), List.of(
					SizedIngredient.of(MoreRed.Tags.Items.BUNDLED_NETWORK_CABLES,1),
					SizedIngredient.of(Tags.Items.INGOTS_IRON,1)))));
		postBlock(ObjectNames.BUNDLED_CABLE_RELAY_PLATE, "Bundled Cable Relay Plate", context)
			.tags(blockTags, MoreRed.Tags.Blocks.BUNDLED_CABLE_POSTS)
			.blockItem()
			.help(helper -> helper.recipe(recipes, RecipeHelpers.shaped(helper.item(), 1, CraftingBookCategory.REDSTONE, List.of(
					" b ",
					"bFb",
					"###"), Map.of(
					'#', Ingredient.of(SMOOTH_STONE_QUARTER_SLABS),
					'b', Ingredient.of(MoreRed.Tags.Items.BUNDLED_NETWORK_CABLES),
					'F', Ingredient.of(Tags.Items.INGOTS_IRON))))
				.recipe(recipes, mangle(helper.id(), fromSoldering), new SolderingRecipe(new ItemStack(helper.item()), List.of(
					SizedIngredient.of(SMOOTH_STONE_QUARTER_SLABS, 2),
					SizedIngredient.of(Tags.Items.INGOTS_IRON, 1),
					SizedIngredient.of(MoreRed.Tags.Items.BUNDLED_NETWORK_CABLES, 1)))));
		wireBlock(ObjectNames.BUNDLED_NETWORK_CABLE, "Bundled Network Cable", context)
			.blockItem()
			.tags(itemTags,  MoreRed.Tags.Items.BUNDLED_NETWORK_CABLES)
			.help(helper -> helper.recipe(recipes, RecipeHelpers.shaped(helper.item(), 3, CraftingBookCategory.REDSTONE, List.of(
					"#",
					"#",
					"#"), Map.of('#', Ingredient.of(MoreRed.Tags.Items.NETWORK_CABLES)))));
		redstonePlateBlock(ObjectNames.DIODE, "Diode", context, 3,
			"trt",
			"###");
		hexidecrubrometerBlock(context);
		redstonePlateBlock(ObjectNames.LATCH, "Latch", context, 3,
			"#t#",
			"r r",
			"#t#");
		switchedPlateBlock(ObjectNames.MULTIPLEXER, "Multiplexer", context, 1,
			"#r#",
			"rir",
			"#r#");
		redstonePlateBlock(ObjectNames.NAND_2_GATE, "Two Input NAND Gate", context, 3,
			"#r#",
			"trt",
			"###");
		redstonePlateBlock(ObjectNames.NAND_GATE, "NAND Gate", context, 4,
			"#r#",
			"trt",
			"#t#");
		redstonePlateBlock(ObjectNames.NOR_GATE, "NOR Gate", context, 2,
			"#r#",
			"rtr",
			"#r#");
		redstonePlateBlock(ObjectNames.NOT_GATE, "NOT Gate", context, 2,
			"rtr",
			"###");
		redstonePlateBlock(ObjectNames.OR_GATE, "OR Gate", context, 3,
			"#t#",
			"rtr",
			"#r#");
		switchedPlateBlock(ObjectNames.PULSE_GATE, "Pulse Gate", context, 1,
			"rir",
			"###");
		wireBlock(ObjectNames.RED_ALLOY_WIRE, "Red Alloy Wire", context)
			.blockItem()
			.tags(itemTags, MoreRed.Tags.Items.RED_ALLOY_WIRES)
			.help(helper -> helper.recipe(recipes, RecipeHelpers.shaped(helper.item(), 12, CraftingBookCategory.REDSTONE, List.of("###"), Map.of('#', Ingredient.of(REDSTONE_ALLOY_INGOTS)))));
		postBlock(ObjectNames.REDWIRE_POST, "Redwire Post", context)
			.tags(blockTags, MoreRed.Tags.Blocks.REDWIRE_POSTS)
			.blockItem()
			.help(helper -> helper.recipe(recipes, RecipeHelpers.shaped(helper.item(), 1, CraftingBookCategory.REDSTONE, List.of(
					"F",
					"R"), Map.of(
					'R', Ingredient.of(REDSTONE_ALLOY_INGOTS),
					'F', Ingredient.of(Tags.Items.INGOTS_IRON))))
				.recipe(recipes, mangle(helper.id(), fromSoldering), new SolderingRecipe(new ItemStack(helper.item()), List.of(
					SizedIngredient.of(Tags.Items.INGOTS_IRON,1),
					SizedIngredient.of(REDSTONE_ALLOY_INGOTS,1)))));
				
		postBlock(ObjectNames.REDWIRE_POST_PLATE, "Redwire Post Plate", context)
			.tags(blockTags, MoreRed.Tags.Blocks.REDWIRE_POSTS)
			.blockItem()
			.help(helper -> helper.recipe(recipes, RecipeHelpers.shaped(helper.item(), 1, CraftingBookCategory.REDSTONE, List.of(
					" F ",
					" R ",
					"###"), Map.of(
					'#', Ingredient.of(SMOOTH_STONE_QUARTER_SLABS),
					'R', Ingredient.of(REDSTONE_ALLOY_INGOTS),
					'F', Ingredient.of(Tags.Items.INGOTS_IRON))))
				.recipe(recipes, mangle(helper.id(), fromSoldering), new SolderingRecipe(new ItemStack(helper.item()), List.of(
					SizedIngredient.of(SMOOTH_STONE_QUARTER_SLABS,1),
					SizedIngredient.of(REDSTONE_ALLOY_INGOTS,1),
					SizedIngredient.of(Tags.Items.INGOTS_IRON,1)))));
				
		postBlock(ObjectNames.REDWIRE_POST_RELAY_PLATE, "Redwire Post Relay Plate", context)
			.tags(blockTags, MoreRed.Tags.Blocks.REDWIRE_POSTS)
			.blockItem()
			.help(helper -> helper.recipe(recipes, RecipeHelpers.shaped(helper.item(), 1, CraftingBookCategory.REDSTONE, List.of(
					" F ",
					"rRr",
					"###"), Map.of(
					'#', Ingredient.of(SMOOTH_STONE_QUARTER_SLABS),
					'r', Ingredient.of(Tags.Items.DUSTS_REDSTONE),
					'R', Ingredient.of(REDSTONE_ALLOY_INGOTS),
					'F', Ingredient.of(Tags.Items.INGOTS_IRON))))
				.recipe(recipes, mangle(helper.id(), fromSoldering), new SolderingRecipe(new ItemStack(helper.item()), List.of(
					SizedIngredient.of(SMOOTH_STONE_QUARTER_SLABS,1),
					SizedIngredient.of(Tags.Items.DUSTS_REDSTONE,1),
					SizedIngredient.of(REDSTONE_ALLOY_INGOTS,1),
					SizedIngredient.of(Tags.Items.INGOTS_IRON,1)))));
		simpleBlock(ObjectNames.SOLDERING_TABLE, "Soldering Table", context)
			.simpleBlockItem(models)
			.help(helper -> helper.recipe(recipes, RecipeHelpers.shaped(helper.item(), 1, CraftingBookCategory.REDSTONE, List.of(
				"sss",
				"#b#",
				"#b#"), Map.of(
				'#', Ingredient.of(Items.RED_NETHER_BRICKS),
				's', Ingredient.of(SMOOTH_STONE_QUARTER_SLABS),
				'b', Ingredient.of(Tags.Items.RODS_BLAZE)))));
		plateBlock(ObjectNames.STONE_PLATE, "Stone Plate", context)
			.tags(blockTags, BlockTags.MINEABLE_WITH_PICKAXE)
			.blockItem()
			.tags(itemTags, SMOOTH_STONE_QUARTER_SLABS)
			.help(helper -> helper.recipe(recipes, RecipeHelpers.shaped(helper.item(), 12, CraftingBookCategory.BUILDING,
					List.of("###"),
					Map.of('#', Ingredient.of(SMOOTH_STONE_SLABS))))
				.recipe(recipes, mangle(helper.id(), "%s_from_smooth_stone_slab_stonecutting"),
					new StonecutterRecipe("", Ingredient.of(SMOOTH_STONE_SLABS), new ItemStack(helper.item(), 4)))
				.recipe(recipes, mangle(helper.id(), "%s_from_smooth_stone_stonecutting"),
					new StonecutterRecipe("", Ingredient.of(SMOOTH_STONE), new ItemStack(helper.item(), 8))));				
		redstonePlateBlock(ObjectNames.XNOR_GATE, "XNOR Gate", context, 4,
		    "#t#",
		    "ttt",
		    "###");
		redstonePlateBlock(ObjectNames.XOR_GATE, "XOR Gate", context, 4,
		    "#r#",
		    "ttt",
		    "###");
		
		// other items
		spool(ObjectNames.BUNDLED_CABLE_SPOOL, "Bundled Cable Spool", context, MoreRed.Tags.Items.BUNDLED_NETWORK_CABLES);
		spool(ObjectNames.REDWIRE_SPOOL, "Redwire Spool", context, MoreRed.Tags.Items.RED_ALLOY_WIRES);
		simpleItem(ObjectNames.RED_ALLOY_INGOT, "Red Alloy Ingot", context)
			.tags(itemTags, REDSTONE_ALLOY_INGOTS);
		
		// data for vanilla things
		recipes.put(MoreRed.getModRL("smooth_stone_slab_from_stone_plate"), RecipeHelpers.shaped(Items.SMOOTH_STONE_SLAB, 1, CraftingBookCategory.BUILDING, List.of(
				"###"), Map.of(
				'#', Ingredient.of(MoreRed.get().stonePlateBlock.get()))));
		
		itemTags.tag(SMOOTH_STONE)
			.add(Items.SMOOTH_STONE.builtInRegistryHolder().key());
		itemTags.tag(SMOOTH_STONE_SLABS)
			.add(Items.SMOOTH_STONE_SLAB.builtInRegistryHolder().key());
		itemTags.tag(MoreRed.Tags.Items.RED_ALLOYABLE_INGOTS)
			.addTag(Tags.Items.INGOTS_COPPER)
			.addTag(Tags.Items.INGOTS_IRON);
		
		// tags in tags
		itemTags.tag(MoreRed.Tags.Items.NETWORK_CABLES)
			.addTag(MoreRed.Tags.Items.BUNDLED_NETWORK_CABLES)
			.addTag(MoreRed.Tags.Items.COLORED_NETWORK_CABLES);
		
		// misc. translations
		lang.add("itemGroup.morered", "More Red");
		lang.add("emi.category.morered.soldering", "Soldering");
		lang.add("gui.morered.category.soldering", "Soldering");
		

		// do stuff that has files for each color
		for (int i=0; i<16; i++)
		{
			final DeferredHolder<Block, ColoredCableBlock> blockHolder = MoreRed.get().networkCableBlocks[i];
			final ResourceLocation blockId = blockHolder.getId();
			final String modid = blockId.getNamespace();
			final String blockPath = blockId.getPath();
			final ColoredCableBlock block = blockHolder.get();
			final Item item = block.asItem();
			
			String blockName = WordUtils.capitalize(blockPath.replace("_", " ")); 
			
			wireBlock(ObjectNames.NETWORK_CABLES_BY_COLOR[i], blockName, context);
			
			ResourceLocation blockTexture = mangle(blockId, "block/%s");
			
			// generate block models
			// generate the simple models for the block
			List<String> blockModelTypes = ImmutableList.of("edge","elbow","line","node");
			blockModelTypes.forEach(modelType->
				context.models.put(MoreRed.getModRL(String.format("block/%s_%s", blockPath, modelType)),
					SimpleModel.createWithoutRenderType(MoreRed.getModRL(String.format("block/colored_network_cable_%s_template", modelType)))
						.addTexture("wire",blockTexture)));

			// generate item models
			models.put(
				ResourceLocation.fromNamespaceAndPath(blockId.getNamespace(), String.format("item/%s", blockId.getPath())),
				SimpleModel.createWithoutRenderType(ResourceLocation.fromNamespaceAndPath(modid, "item/colored_network_cable_template"))
					.addTexture("wire", blockTexture));	

			// generate recipe
			recipes.put(blockId,
				RecipeHelpers.shaped(item, 8, CraftingBookCategory.REDSTONE,
					List.of("www", "w#w", "www"),
					Map.<Character,Ingredient>of(
						'w', Ingredient.of(MoreRed.Tags.Items.RED_ALLOY_WIRES),
						'#', IntersectionIngredient.of(
							Ingredient.of(ItemTags.WOOL),
							Ingredient.of(TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "dyed/" + DyeColor.values()[i].getName())))))));
			
			// tags
			itemTags
				.getOrCreateRawBuilder(MoreRed.Tags.Items.COLORED_NETWORK_CABLES)
				.addElement(blockId);
			itemTags.getOrCreateRawBuilder(TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "dyed/" + DyeColor.values()[i].getName())))
				.addElement(blockId);
		}

		generator.addProvider(event.includeClient(), JsonDataProvider.create(output, generator, PackOutput.Target.RESOURCE_PACK, "blockstates", BlockStateFile.CODEC, blockStates));
		generator.addProvider(event.includeClient(), JsonDataProvider.create(output, generator, PackOutput.Target.RESOURCE_PACK, "models", SimpleModel.CODEC, models));
		generator.addProvider(event.includeClient(), JsonDataProvider.named(output, generator, PackOutput.Target.RESOURCE_PACK, "models", WirePartModelDefinition.CODEC, wirePartModels, "morered wire models"));
		generator.addProvider(event.includeClient(), lang);

		generator.addProvider(event.includeServer(), JsonDataProvider.create(output, generator, PackOutput.Target.DATA_PACK, "loot_table", LootTable.DIRECT_CODEC, lootTables));
		generator.addProvider(event.includeServer(), JsonDataProvider.create(output, generator, PackOutput.Target.DATA_PACK, "recipe", Recipe.CODEC, recipes));
		generator.addProvider(event.includeServer(), blockTags);
		generator.addProvider(event.includeServer(), itemTags);
	}
	
	static BlockDataHelper plateBlock(String blockPath, String name, DataGenContext context)
	{
		ResourceLocation blockId = MoreRed.getModRL(blockPath);
		Block block = BuiltInRegistries.BLOCK.get(blockId);
		ResourceLocation model = mangle(blockId, "block/%s");
		ResourceLocation modelAlt = mangle(blockId, "block/%s_alt");
		
		var variantBuilder = Variants.builder();
		for (Direction dir : Direction.values())
		{
			for (int rotationIndex = 0; rotationIndex < 4; rotationIndex++)
			{
				ResourceLocation stateModel =
					dir.getAxis() == Direction.Axis.Y || rotationIndex % 2 == 0
					? model
					: modelAlt;
				int x = dir == Direction.DOWN ? 0
					: dir == Direction.UP ? 180
					: 270 - 90*rotationIndex;
				// don't look too closely at the magic numbers
				int y = switch(dir) {
					case DOWN -> 90 * rotationIndex;
					case UP -> new int[] {180,90,0,270}[rotationIndex];
					case NORTH -> new int[] {0,270,180,270}[rotationIndex];
					case SOUTH -> new int[] {180,90,0,90}[rotationIndex];
					case WEST -> new int[] {270,180,90,180}[rotationIndex];
					case EAST -> new int[] {90,0,270,0}[rotationIndex];
				};
				variantBuilder.addVariant(List.of(
						PropertyValue.create(PlateBlock.ATTACHMENT_DIRECTION, dir),
						PropertyValue.create(PlateBlock.ROTATION, rotationIndex)),
					Model.create(stateModel, BlockModelRotation.by(x, y)));
			}
		}
		BlockStateFile blockState = BlockStateFile.variants(variantBuilder);
		LootTable lootTable = simpleLoot(block);
		var blockHelper = BlockDataHelper.create(block, context.blockStates, blockState, context.lootTables, lootTable).localize(context.lang, name);
		blockHelper.simpleBlockItem(context.models);
		return blockHelper;
	}
	
	static BlockDataHelper redstonePlateBlock(String blockPath, String name, DataGenContext context, int redstone, String... recipePattern)
	{
		var blockHelper = plateBlock(blockPath, name, context);
		blockHelper.blockItem().help(helper -> plateRecipes(helper, context, redstone, recipePattern));
		return blockHelper;
	}
	
	static BlockDataHelper bitwisePlateBlock(String blockPath, String name, String symbolTexture, DataGenContext context)
	{
		ResourceLocation blockId = MoreRed.getModRL(blockPath);
		Block block = BuiltInRegistries.BLOCK.get(blockId);
		ResourceLocation parent = block instanceof TwoInputBitwiseLogicPlateBlock
			? MoreRed.getModRL("block/two_input_bitwise_logic_plate_template")
			: MoreRed.getModRL("block/single_input_bitwise_logic_plate_template");
		ResourceLocation symbolLocation = MoreRed.getModRL("block/" + symbolTexture);
		
		context.models.put(mangle(blockId, "block/%s"), SimpleModel.createWithoutRenderType(parent).addTexture("symbol", symbolLocation));
		context.models.put(mangle(blockId, "block/%s_alt"), SimpleModel.createWithoutRenderType(mangle(parent, "%s_alt")).addTexture("symbol", symbolLocation));
		BlockDataHelper helper = plateBlock(blockPath, name, context);
		helper.blockItem().help(h -> h.recipe(context.recipes, mangle(h.id(), "%s_from_soldering"), new SolderingRecipe(new ItemStack(h.item()), List.of(
			SizedIngredient.of(SMOOTH_STONE_QUARTER_SLABS, 2),
			SizedIngredient.of(Tags.Items.GEMS_QUARTZ, 1),
			SizedIngredient.of(Tags.Items.DUSTS_REDSTONE, 1),
			SizedIngredient.of(MoreRed.Tags.Items.BUNDLED_NETWORK_CABLES, 1)))));
		return helper;
	}
	
	static BlockDataHelper postBlock(String blockPath, String name, DataGenContext context)
	{
		ResourceLocation blockId = MoreRed.getModRL(blockPath);
		Block block = BuiltInRegistries.BLOCK.get(blockId);
		ResourceLocation blockModel = blockModel(blockId);
		var variants = Variants.builder();
		int[] xs = {0,180,270,90,90,90};
		int[] ys = {0,0,0,0,90,270};
		for (Direction dir : Direction.values())
		{
			int x = xs[dir.ordinal()];
			int y = ys[dir.ordinal()];
			variants.addVariant(PropertyValue.create(BlockStateProperties.FACING, dir),
				Model.create(blockModel, BlockModelRotation.by(x, y)));
		}
		BlockStateFile blockState = BlockStateFile.variants(variants);
		BlockDataHelper blockHelper = BlockDataHelper.create(block, context.blockStates, blockState, context.lootTables, simpleLoot(block))
			.localize(context.lang, name);
		blockHelper.simpleBlockItem(context.models);
		return blockHelper;
	}
	
	static LootTable simpleLoot(Block block)
	{
		return LootTable.lootTable()
			.withPool(LootPool.lootPool()
				.add(LootItem.lootTableItem(block))
				.when(ExplosionCondition.survivesExplosion()))
			.build();
	}
	
	static ResourceLocation mangle(ResourceLocation id, String pathFormatter)
	{
		return ResourceLocation.fromNamespaceAndPath(id.getNamespace(), String.format(pathFormatter, id.getPath()));
	}
	
	@SuppressWarnings("deprecation")
	static ResourceLocation blockId(Block block)
	{
		return block.builtInRegistryHolder().key().location();
	}
	
	static ResourceLocation blockModel(ResourceLocation blockId)
	{
		return mangle(blockId, "block/%s");
	}
	
	static BlockDataHelper hexidecrubrometerBlock(DataGenContext context)
	{
		Block block = MoreRed.get().hexidecrubrometerBlock.get();
		ResourceLocation blockId = MoreRed.get().hexidecrubrometerBlock.getId();
		var variants = Variants.builder();
		for (int power=0; power<16; power++)
		{
			ResourceLocation modelId = mangle(blockModel(blockId), "%s_" + String.valueOf(power));
			SimpleModel simpleModel = SimpleModel.createWithoutRenderType(MoreRed.getModRL("block/hexidecrubrometer_template"))
				.addTexture("display", modelId);
			context.models.put(modelId, simpleModel);
			for (Direction facing : HexidecrubrometerBlock.ROTATION.getPossibleValues())
			{
				int y = switch(facing) {
					case EAST -> 90;
					case SOUTH -> 180;
					case WEST -> 270;
					default -> 0;
				};
				for (AttachFace face : AttachFace.values())
				{
					int x = switch(face) {
						case CEILING -> 90;
						case FLOOR -> 270;
						case WALL -> 0;
					};
					
					variants.addVariant(List.of(
						PropertyValue.create(HexidecrubrometerBlock.READING_FACE, face),
						PropertyValue.create(HexidecrubrometerBlock.ROTATION, facing),
						PropertyValue.create(HexidecrubrometerBlock.POWER, power)),
						Model.create(modelId, BlockModelRotation.by(x, y)));
				}
			}
		}
		
		var helper = BlockDataHelper.create(block, context.blockStates, BlockStateFile.variants(variants), context.lootTables, simpleLoot(block));
		
		helper.blockItem(context.models, SimpleModel.createWithoutRenderType(mangle(blockId, "block/%s_0")))
			.recipe(context.recipes, mangle(blockId, "%s_from_soldering"), new SolderingRecipe(new ItemStack(block.asItem()), List.of(
				SizedIngredient.of(SMOOTH_STONE_QUARTER_SLABS, 8),
				SizedIngredient.of(Tags.Items.DUSTS_REDSTONE, 9),
				SizedIngredient.of(Tags.Items.GEMS_QUARTZ, 4))));
		helper.localize(context.lang, "Hexidecrubrometer");
		
		return helper;
	}
	
	static BlockDataHelper switchedPlateBlock(String blockPath, String blockName, DataGenContext context, int redstone, String... recipePattern)
	{
		ResourceLocation blockId = MoreRed.getModRL(blockPath);
		Block block = BuiltInRegistries.BLOCK.get(blockId);
		ResourceLocation model = mangle(blockId, "block/%s");
		ResourceLocation modelAlt = mangle(blockId, "block/%s_alt");
		ResourceLocation switchedModel = mangle(blockId, "block/%s_switched");
		ResourceLocation switchedModelAlt = mangle(blockId, "block/%s_switched_alt");
		
		var variantBuilder = Variants.builder();
		for (Direction dir : Direction.values())
		{
			for (int rotationIndex = 0; rotationIndex < 4; rotationIndex++)
			{
				ResourceLocation stateModel =
					dir.getAxis() == Direction.Axis.Y || rotationIndex % 2 == 0
					? model
					: modelAlt;
				ResourceLocation stateModelSwitched = 
					dir.getAxis() == Direction.Axis.Y || rotationIndex % 2 == 0
					? switchedModel
					: switchedModelAlt;
				
				int x = dir == Direction.DOWN ? 0
					: dir == Direction.UP ? 180
					: 270 - 90*rotationIndex;
				// don't look too closely at the magic numbers
				int y = switch(dir) {
					case DOWN -> 90 * rotationIndex;
					case UP -> new int[] {180,90,0,270}[rotationIndex];
					case NORTH -> new int[] {0,270,180,270}[rotationIndex];
					case SOUTH -> new int[] {180,90,0,90}[rotationIndex];
					case WEST -> new int[] {270,180,90,180}[rotationIndex];
					case EAST -> new int[] {90,0,270,0}[rotationIndex];
				};
				variantBuilder.addVariant(List.of(
						PropertyValue.create(PlateBlock.ATTACHMENT_DIRECTION, dir),
						PropertyValue.create(PlateBlock.ROTATION, rotationIndex),
						PropertyValue.create(PlateBlockStateProperties.INPUT_B, false)),
					Model.create(stateModel, BlockModelRotation.by(x, y)));
				variantBuilder.addVariant(List.of(
					PropertyValue.create(PlateBlock.ATTACHMENT_DIRECTION, dir),
					PropertyValue.create(PlateBlock.ROTATION, rotationIndex),
					PropertyValue.create(PlateBlockStateProperties.INPUT_B, true)),
				Model.create(stateModelSwitched, BlockModelRotation.by(x, y)));
			}
		}
		BlockStateFile blockState = BlockStateFile.variants(variantBuilder);
		
		var blockHelper = BlockDataHelper.create(block, context.blockStates, blockState, context.lootTables, simpleLoot(block))
			.localize(context.lang, blockName);
		blockHelper.simpleBlockItem(context.models).help(helper -> switchedPlateRecipes(helper, context, redstone, recipePattern));
		return blockHelper;
	}
	
	static BlockDataHelper wireBlock(String blockPath, String blockName, DataGenContext context)
	{
		ResourceLocation blockId = MoreRed.getModRL(blockPath);
		Block block = BuiltInRegistries.BLOCK.get(blockId);
		

		// generate blockstate json
		ResourceLocation partsModel = mangle(blockId, "block/%s_parts");
		ResourceLocation nodeModel = mangle(blockId, "block/%s_node");
		ResourceLocation elbowModel = mangle(blockId, "block/%s_elbow");
		BlockStateFile blockState = BlockStateFile.multipart(Multipart.builder()
			.addWhenApply(WhenApply.always(Model.create(partsModel)))
			.addWhenApply(WhenApply.when(
				Case.create(AbstractWireBlock.DOWN, true),
				Model.create(nodeModel)))
			.addWhenApply(WhenApply.when(
				Case.create(AbstractWireBlock.UP, true),
				Model.create(nodeModel, BlockModelRotation.X180_Y0)))
			.addWhenApply(WhenApply.when(
				Case.create(AbstractWireBlock.SOUTH, true),
				Model.create(nodeModel, BlockModelRotation.X90_Y0)))
			.addWhenApply(WhenApply.when(
				Case.create(AbstractWireBlock.WEST, true),
				Model.create(nodeModel, BlockModelRotation.X90_Y90)))
			.addWhenApply(WhenApply.when(
				Case.create(AbstractWireBlock.NORTH, true),
				Model.create(nodeModel, BlockModelRotation.X90_Y180)))
			.addWhenApply(WhenApply.when(
				Case.create(AbstractWireBlock.EAST, true),
				Model.create(nodeModel, BlockModelRotation.X90_Y270)))
			.addWhenApply(WhenApply.when(
				Case.builder()
					.addCondition(AbstractWireBlock.DOWN, true)
					.addCondition(AbstractWireBlock.WEST, true),
				Model.create(elbowModel)))
			.addWhenApply(WhenApply.when(
				Case.builder()
					.addCondition(AbstractWireBlock.DOWN, true)
					.addCondition(AbstractWireBlock.NORTH, true),
				Model.create(elbowModel, BlockModelRotation.X0_Y90)))
			.addWhenApply(WhenApply.when(
				Case.builder()
					.addCondition(AbstractWireBlock.DOWN, true)
					.addCondition(AbstractWireBlock.EAST, true),
				Model.create(elbowModel, BlockModelRotation.X0_Y180)))
			.addWhenApply(WhenApply.when(
				Case.builder()
					.addCondition(AbstractWireBlock.DOWN, true)
					.addCondition(AbstractWireBlock.SOUTH, true),
				Model.create(elbowModel, BlockModelRotation.X0_Y270)))
			.addWhenApply(WhenApply.when(
				Case.builder()
					.addCondition(AbstractWireBlock.SOUTH, true)
					.addCondition(AbstractWireBlock.WEST, true),
				Model.create(elbowModel, BlockModelRotation.X90_Y0)))
			.addWhenApply(WhenApply.when(
				Case.builder()
					.addCondition(AbstractWireBlock.WEST, true)
					.addCondition(AbstractWireBlock.NORTH, true),
				Model.create(elbowModel, BlockModelRotation.X90_Y90)))
			.addWhenApply(WhenApply.when(
				Case.builder()
					.addCondition(AbstractWireBlock.NORTH, true)
					.addCondition(AbstractWireBlock.EAST, true),
				Model.create(elbowModel, BlockModelRotation.X90_Y180)))
			.addWhenApply(WhenApply.when(
				Case.builder()
					.addCondition(AbstractWireBlock.EAST, true)
					.addCondition(AbstractWireBlock.SOUTH, true),
				Model.create(elbowModel, BlockModelRotation.X90_Y270)))
			.addWhenApply(WhenApply.when(
				Case.builder()
					.addCondition(AbstractWireBlock.UP, true)
					.addCondition(AbstractWireBlock.WEST, true),
				Model.create(elbowModel, BlockModelRotation.X180_Y0)))
			.addWhenApply(WhenApply.when(
				Case.builder()
					.addCondition(AbstractWireBlock.UP, true)
					.addCondition(AbstractWireBlock.NORTH, true),
				Model.create(elbowModel, BlockModelRotation.X180_Y90)))
			.addWhenApply(WhenApply.when(
				Case.builder()
					.addCondition(AbstractWireBlock.UP, true)
					.addCondition(AbstractWireBlock.EAST, true),
				Model.create(elbowModel, BlockModelRotation.X180_Y180)))
			.addWhenApply(WhenApply.when(
				Case.builder()
					.addCondition(AbstractWireBlock.UP, true)
					.addCondition(AbstractWireBlock.SOUTH, true),
				Model.create(elbowModel, BlockModelRotation.X180_Y270))));
		
		LootTable lootTable = LootTable.lootTable()
			.withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1))
				.add(LootItem.lootTableItem(block)
					.apply(() -> WireCountLootFunction.INSTANCE))
				.when(ExplosionCondition.survivesExplosion()))
			.build();
		
		BlockDataHelper helper = BlockDataHelper.create(block, context.blockStates, blockState, context.lootTables, lootTable);
				
		// generate the parts model for the block
		context.wirePartModels.put(
			MoreRed.getModRL(String.format("block/%s_parts", blockPath)),
			new WirePartModelDefinition(
				SimpleModel.createWithoutRenderType(MoreRed.getModRL(String.format("block/%s_line", blockPath))),
				SimpleModel.createWithoutRenderType(MoreRed.getModRL(String.format("block/%s_edge", blockPath)))));
		
		
		return helper.localize(context.lang, blockName);
	}
	
	static BlockDataHelper simpleBlock(String blockPath, String blockName, DataGenContext context)
	{
		ResourceLocation blockId = MoreRed.getModRL(blockPath);
		Block block = BuiltInRegistries.BLOCK.get(blockId);
		context.lang.add(block, blockName);
		return BlockDataHelper.create(block,
			context.blockStates, BlockStateFile.variants(Variants.always(Model.create(mangle(blockId, "block/%s")))),
			context.lootTables, simpleLoot(block));
	}
	
	static ItemDataHelper plateRecipes(ItemDataHelper helper, DataGenContext context, int redstone, String... pattern)
	{
		Map<Character, Ingredient> patternKey = new HashMap<>();
		for (String line : pattern)
		{
			if (line.contains("#"))
			{
				patternKey.put('#', Ingredient.of(SMOOTH_STONE_QUARTER_SLABS));
			}
			if (line.contains("r"))
			{
				patternKey.put('r', Ingredient.of(Tags.Items.DUSTS_REDSTONE));
			}
			if (line.contains("t"))
			{
				patternKey.put('t', Ingredient.of(Items.REDSTONE_TORCH));
			}
		}
		helper.recipe(context.recipes, mangle(helper.id(), "%s_from_soldering"), 
			new SolderingRecipe(new ItemStack(helper.item()), List.of(
				new SizedIngredient(Ingredient.of(SMOOTH_STONE_QUARTER_SLABS), 1),
				new SizedIngredient(Ingredient.of(Tags.Items.DUSTS_REDSTONE), redstone))))
		.recipe(context.recipes, RecipeHelpers.shaped(helper.item(), 1, CraftingBookCategory.REDSTONE, List.of(pattern), patternKey));
		
		return helper;
	}
	
	static ItemDataHelper switchedPlateRecipes(ItemDataHelper helper, DataGenContext context, int redstone, String... recipePattern)
	{
		helper.recipe(context.recipes, mangle(helper.id(), "%s_from_soldering"), 
			new SolderingRecipe(new ItemStack(helper.item()), List.of(
				SizedIngredient.of(SMOOTH_STONE_QUARTER_SLABS, 1),
				SizedIngredient.of(Tags.Items.DUSTS_REDSTONE, redstone),
				SizedIngredient.of(Tags.Items.INGOTS_IRON, 1))))
		.recipe(context.recipes, RecipeHelpers.shaped(helper.item(), 1, CraftingBookCategory.REDSTONE, List.of(recipePattern), Map.of(
		    '#', Ingredient.of(SMOOTH_STONE_QUARTER_SLABS),
		    'r', Ingredient.of(Tags.Items.DUSTS_REDSTONE),
		    'i', Ingredient.of(Items.REDSTONE_TORCH))));
		
		return helper;
	}
	
	static ItemDataHelper spool(String itemPath, String name, DataGenContext context, TagKey<Item> wireIngredientTag)
	{
		ResourceLocation itemId = MoreRed.getModRL(itemPath);
		Item item = BuiltInRegistries.ITEM.get(itemId);
		context.lang.add(item, name);
		return ItemDataHelper.create(item, context.models, SimpleModel.createWithoutRenderType(ResourceLocation.withDefaultNamespace("item/handheld"))
				.addTexture("layer0", ResourceLocation.withDefaultNamespace("item/stick"))
				.addTexture("layer1", mangle(itemId, "item/%s")))
		.help(helper -> helper.recipe(context.recipes, RecipeHelpers.shaped(helper.item(), 1, CraftingBookCategory.REDSTONE, List.of(
		    "rfs",
		    "frf",
		    "sfr"), Map.of(
		    's', Ingredient.of(Tags.Items.RODS_WOODEN),
		    'f', Ingredient.of(Tags.Items.INGOTS_IRON),
		    'r', Ingredient.of(wireIngredientTag)))));
	}
	
	static ItemDataHelper simpleItem(String itemPath, String name, DataGenContext context)
	{
		ResourceLocation itemId = MoreRed.getModRL(itemPath);
		Item item = BuiltInRegistries.ITEM.get(itemId);
		context.lang.add(item, name);
		return ItemDataHelper.create(item, context.models, SimpleModel.createWithoutRenderType(ResourceLocation.withDefaultNamespace("item/generated"))
			.addTexture("layer0", mangle(itemId, "item/%s")));
	}
}
