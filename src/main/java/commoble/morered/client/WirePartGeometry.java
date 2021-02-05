package commoble.morered.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;

import commoble.morered.redwire.WireBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.model.BlockModel;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.IModelTransform;
import net.minecraft.client.renderer.model.IUnbakedModel;
import net.minecraft.client.renderer.model.ItemOverrideList;
import net.minecraft.client.renderer.model.ModelBakery;
import net.minecraft.client.renderer.model.RenderMaterial;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.IResourceManager;
import net.minecraft.util.Direction;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockDisplayReader;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.IModelConfiguration;
import net.minecraftforge.client.model.IModelLoader;
import net.minecraftforge.client.model.data.IDynamicBakedModel;
import net.minecraftforge.client.model.data.IModelData;
import net.minecraftforge.client.model.geometry.IModelGeometry;

public class WirePartGeometry implements IModelGeometry<WirePartGeometry>
{
	private final BlockModel lineModel;
	private final BlockModel edgeModel;
	
	public WirePartGeometry(BlockModel lineModel, BlockModel edgeModel)
	{
		this.lineModel = lineModel;
		this.edgeModel = edgeModel;
	}

	@Override
	public IBakedModel bake(IModelConfiguration owner, ModelBakery bakery, Function<RenderMaterial, TextureAtlasSprite> spriteGetter, IModelTransform modelTransform,
		ItemOverrideList overrides, ResourceLocation modelLocation)
	{
		IBakedModel[] lineModels = new IBakedModel[24];
		IBakedModel[] edgeModels = new IBakedModel[12];
		boolean isSideLit = owner.isSideLit();
		
		// for each combination of attachment face + rotated connection side, bake a connection line model
		for (int side = 0; side < 6; side++)
		{
			for (int subSide = 0; subSide < 4; subSide++)
			{
				int index = side*4 + subSide;
				IModelTransform transform = FaceRotation.getFaceRotation(side, subSide);
				lineModels[index] = this.lineModel.bakeModel(bakery, this.lineModel, spriteGetter, transform, modelLocation, isSideLit);
			}
		}
		
		for (int edge = 0; edge < 12; edge++)
		{
			// the 12 edges are a little weirder to index
			// let's define them in directional precedence
			// down comes first, then up, then the sides
			// the "default" edge with no rotation has to be on the middle sides to ignore the z-axis, we'll use bottom-west
			IModelTransform transform = EdgeRotation.EDGE_ROTATIONS[edge];
			edgeModels[edge] = this.edgeModel.bakeModel(bakery, this.edgeModel, spriteGetter, transform, modelLocation, isSideLit);
		}
		
		return new WirePartModel(owner.useSmoothLighting(), owner.isShadedInGui(), owner.isSideLit(),
			spriteGetter.apply(this.lineModel.resolveTextureName("particle")),
			lineModels,
			edgeModels);
	}

	@Override
	public Collection<RenderMaterial> getTextures(IModelConfiguration owner, Function<ResourceLocation, IUnbakedModel> modelGetter, Set<Pair<String, String>> missingTextureErrors)
	{
        Set<RenderMaterial> textures = new HashSet<>();
        textures.addAll(this.lineModel.getTextures(modelGetter, missingTextureErrors));
        textures.addAll(this.edgeModel.getTextures(modelGetter, missingTextureErrors));
        return textures;
	}
	
	public static class WirePartModel implements IDynamicBakedModel
	{
		private static final List<BakedQuad> NO_QUADS = ImmutableList.of();
		
        private final boolean isAmbientOcclusion;
        private final boolean isGui3d;
        private final boolean isSideLit;
        private final TextureAtlasSprite particle;
        private final IBakedModel[] lineModels;
        private final IBakedModel[] edgeModels;
		
		public WirePartModel(boolean isAmbientOcclusion, boolean isGui3d, boolean isSideLit, TextureAtlasSprite particle, IBakedModel[] lineModels, IBakedModel[] edgeModels)
		{
			this.isAmbientOcclusion = isAmbientOcclusion;
			this.isGui3d = isGui3d;
			this.isSideLit = isSideLit;
			this.particle = particle;
			this.lineModels = lineModels;
			this.edgeModels = edgeModels;
		}

		@Override
		public boolean isAmbientOcclusion()
		{
			return this.isAmbientOcclusion;
		}

		@Override
		public boolean isGui3d()
		{
			return this.isGui3d;
		}

		@Override
		public boolean isSideLit()
		{
			return this.isSideLit;
		}

		@Override
		public boolean isBuiltInRenderer()
		{
			return false;
		}

		@Override
		public TextureAtlasSprite getParticleTexture()
		{
			return this.particle;
		}

		@Override
		public ItemOverrideList getOverrides()
		{
			return ItemOverrideList.EMPTY;
		}

		@Override
		public List<BakedQuad> getQuads(BlockState state, Direction side, Random rand, IModelData extraData)
		{
			WireModelData wireData = extraData.getData(WireModelData.PROPERTY);
			if (wireData == null)
				return NO_QUADS;

			List<BakedQuad> quads = new ArrayList<>();
			int lineStart = 6;
			int lines = 24;
			int edgeStart = lineStart+lines;
			int edges = 12;
			for (int i=0; i<lines; i++)
			{
				if (wireData.test(i+lineStart))
					quads.addAll(this.lineModels[i].getQuads(state, side, rand, extraData));
			}
			for (int i=0; i < edges; i++)
			{
				if (wireData.test(i+edgeStart))
					quads.addAll(this.edgeModels[i].getQuads(state, side, rand, extraData));
			}
			
			return quads;
		}
		
		
	}
	
	// wrapper for replacing the multipart models with (the root models that each specific blockstates have)
	public static class WireBlockModel extends BakedModelWrapper<IBakedModel>
	{

		public WireBlockModel(IBakedModel originalModel)
		{
			super(originalModel);
		}

		@Override
		public IModelData getModelData(IBlockDisplayReader world, BlockPos pos, BlockState state, IModelData tileData)
		{
			Block block = state.getBlock();
			if (block instanceof WireBlock)
			{
				return new WireModelData(((WireBlock)block).getExpandedShapeIndex(state, world, pos));
			}
			else
			{
				return tileData;
			}
		}
		
		
	}

	public static class WirePartLoader implements IModelLoader<WirePartGeometry>
	{
		public static final WirePartLoader INSTANCE = new WirePartLoader();

		@Override
		public void onResourceManagerReload(IResourceManager resourceManager)
		{
			// not needed at the moment, consider using if caches need to be cleared
		}

		@Override
		public WirePartGeometry read(JsonDeserializationContext context, JsonObject modelContents)
		{
            BlockModel lineModel = context.deserialize(JSONUtils.getJsonObject(modelContents, "line"), BlockModel.class);
            BlockModel edgeModel = context.deserialize(JSONUtils.getJsonObject(modelContents, "edge"), BlockModel.class);
            return new WirePartGeometry(lineModel, edgeModel);
		}
		
	}
}
