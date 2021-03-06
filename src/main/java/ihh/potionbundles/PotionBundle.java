package ihh.potionbundles;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.PotionItem;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionUtils;
import net.minecraft.potion.Potions;
import net.minecraft.stats.Stats;
import net.minecraft.util.NonNullList;
import net.minecraft.util.Util;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class PotionBundle extends PotionItem {
    public static final String USES_KEY = "Uses";

    public PotionBundle(Properties properties) {
        super(properties);
    }

    @Nonnull
    @Override
    public ITextComponent getDisplayName(@Nonnull ItemStack stack) {
        return new TranslationTextComponent("item.potionbundles.potion_bundle", new TranslationTextComponent(PotionUtils.getPotionFromItem(stack).getNamePrefixed(Util.makeTranslationKey("item", Items.POTION.getRegistryName()) + ".effect.")).getString());
    }

    @Override
    public void addInformation(@Nonnull ItemStack stack, @Nullable World world, @Nonnull List<ITextComponent> tooltip, @Nonnull ITooltipFlag flag) {
        super.addInformation(stack, world, tooltip, flag);
        tooltip.add(new TranslationTextComponent("item.potionbundles.potion_bundle.uses", stack.getOrCreateTag().getInt(USES_KEY)));
    }

    @Override
    public double getDurabilityForDisplay(ItemStack stack) {
        if (Config.CLIENT.durabilityBarColor.get() == -1) return 1;
        return stack.getOrCreateTag().getInt(USES_KEY) / 3.0;
    }

    @Override
    public int getRGBDurabilityForDisplay(ItemStack stack) {
        return Config.CLIENT.durabilityBarColor.get();
    }

    @Nonnull
    @Override
    public ItemStack onItemUseFinish(@Nonnull ItemStack stack, @Nonnull World world, @Nonnull LivingEntity entity) {
        if (!stack.hasTag() || !stack.getOrCreateTag().contains(USES_KEY) || PotionUtils.getPotionFromItem(stack) == Potions.EMPTY)
            return stack;
        PlayerEntity player = entity instanceof PlayerEntity ? (PlayerEntity) entity : null;
        if (player instanceof ServerPlayerEntity)
            CriteriaTriggers.CONSUME_ITEM.trigger((ServerPlayerEntity) player, stack);
        if (!world.isRemote) for (EffectInstance effect : PotionUtils.getEffectsFromStack(stack)) {
            if (effect.getPotion().isInstant())
                effect.getPotion().affectEntity(player, player, entity, effect.getAmplifier(), 1);
            else entity.addPotionEffect(new EffectInstance(effect));
        }
        CompoundNBT tag = stack.getOrCreateTag();
        if (player != null) {
            player.addStat(Stats.ITEM_USED.get(this));
            tag.putInt(USES_KEY, tag.getInt(USES_KEY) - 1);
            ItemHandlerHelper.giveItemToPlayer(player, new ItemStack(Items.GLASS_BOTTLE));
        }
        return tag.getInt(USES_KEY) == 0 ? Config.SERVER.returnString.get() ? new ItemStack(Items.STRING) : ItemStack.EMPTY : stack;
    }

    @Override
    public void fillItemGroup(@Nonnull ItemGroup group, @Nonnull NonNullList<ItemStack> items) {
        if (this.isInGroup(group)) for (Potion potion : ForgeRegistries.POTION_TYPES) {
            if (potion == Potions.EMPTY) continue;
            ItemStack stack = PotionUtils.addPotionToItemStack(new ItemStack(this), potion);
            stack.getOrCreateTag().putInt(USES_KEY, 3);
            items.add(stack);
        }
    }
}
