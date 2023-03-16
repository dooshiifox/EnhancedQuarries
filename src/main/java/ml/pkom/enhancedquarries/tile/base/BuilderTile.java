package ml.pkom.enhancedquarries.tile.base;

import ml.pkom.enhancedquarries.Items;
import ml.pkom.enhancedquarries.block.base.Builder;
import ml.pkom.enhancedquarries.inventory.DisabledInventory;
import ml.pkom.enhancedquarries.mixin.MachineBaseBlockEntityAccessor;
import ml.pkom.enhancedquarries.util.BlueprintUtil;
import ml.pkom.mcpitanlibarch.api.util.ItemUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.enums.StairShape;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundCategory;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import reborncore.api.blockentity.InventoryProvider;
import reborncore.common.blockentity.MachineBaseBlockEntity;
import reborncore.common.blockentity.SlotConfiguration;
import reborncore.common.powerSystem.PowerAcceptorBlockEntity;
import reborncore.common.util.RebornInventory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BuilderTile extends PowerAcceptorBlockEntity implements InventoryProvider {// implements IInventory {

    // Container
    public RebornInventory<BuilderTile> inventory = new RebornInventory<>(28, "BuilderTile", 64, this);

    public RebornInventory<BuilderTile> getInventory() {
        return inventory;
    }

    public Inventory needInventory = new DisabledInventory(27);

    // TR
    // デフォルトコスト
    private long defaultEnergyCost = 30;

    // ブロック1回設置分に対するエネルギーのコスト
    public long getEnergyCost() {
        return defaultEnergyCost;
    }

    // エネルギーの容量
    public long getBaseMaxPower() {
        return 5000;
    }

    // エネルギーの最大出力(不要なので0)
    public long getBaseMaxOutput() {
        return 0;
    }

    // エネルギーの最大入力
    public long getBaseMaxInput() {
        return 500;
    }

    // エネルギーの生産をするか？→false
    public boolean canProvideEnergy(final Direction direction) { return false; }

    // ----

    // NBT

    public void writeNbt(NbtCompound tag) {
        tag.putDouble("coolTime", coolTime);
        if (pos1 != null) {
            tag.putInt("rangePos1X", getPos1().getX());
            tag.putInt("rangePos1Y", getPos1().getY());
            tag.putInt("rangePos1Z", getPos1().getZ());
        }
        if (pos2 != null) {
            tag.putInt("rangePos2X", getPos2().getX());
            tag.putInt("rangePos2Y", getPos2().getY());
            tag.putInt("rangePos2Z", getPos2().getZ());
        }
        super.writeNbt(tag);
    }

    public void readNbt(NbtCompound tag) {
        super.readNbt(tag);

        if (tag.contains("coolTime")) coolTime = tag.getDouble("coolTime");
        if (tag.contains("rangePos1X")
                && tag.contains("rangePos1Y")
                && tag.contains("rangePos1Z")
                && tag.contains("rangePos2X")
                && tag.contains("rangePos2Y")
                && tag.contains("rangePos2Z")) {
            setPos1(new BlockPos(tag.getInt("rangePos1X"), tag.getInt("rangePos1Y"), tag.getInt("rangePos1Z")));
            setPos2(new BlockPos(tag.getInt("rangePos2X"), tag.getInt("rangePos2Y"), tag.getInt("rangePos2Z")));
        }
    }

    // ----

    private double defaultBasicSpeed = 5;

    // 基準の速度
    public double getBasicSpeed() {
        return defaultBasicSpeed;
    }

    // TR用のTick
    public void TRTick(World world, BlockPos pos, BlockState state, MachineBaseBlockEntity blockEntity) {
        super.tick(world, pos, state, blockEntity);
    }

    public double defaultSettingCoolTime = 300;

    // クールダウンの基準
    public double getSettingCoolTime() {
        return defaultSettingCoolTime;
    }

    public double coolTime = getSettingCoolTime();

    public Map<BlockPos, BlockState> blueprintMap = new LinkedHashMap<>();

    public void tick(World world, BlockPos pos, BlockState state, MachineBaseBlockEntity blockEntity) {
        // 1.--
        super.tick(world, pos, state, blockEntity);
        if (getWorld() == null || getWorld().isClient())
        {
            return;
        }
        // ----
        //BlockState state = getWorld().getBlockState(getPos());
        Builder builder = (Builder) state.getBlock();

        // レッドストーン受信で無効
        if (getWorld().isReceivingRedstonePower(getPos())) {
            if (isActive()) {
                builder.setActive(false, getWorld(), getPos());
            }
            return;
        }
        ItemStack blueprint = inventory.getStack(0);

        if (blueprint.hasNbt() && blueprint.getItem() == Items.BLUEPRINT) {
            if (blueprintMap.isEmpty()) {
                blueprintMap = BlueprintUtil.readNBt(blueprint, getFacing());
                pos1 = pos.add(BlueprintUtil.getMinPos(blueprintMap));
                pos2 = pos.add(BlueprintUtil.getMaxPos(blueprintMap));

                // 必要なアイテム数
                List<ItemStack> needStacks = new ArrayList<>();
                for (BlockState blockState : blueprintMap.values()) {
                    if (blockState.isAir()) continue;
                    Item item = blockState.getBlock().asItem();

                    boolean b = false;
                    for (ItemStack stack : needStacks) {
                        if (stack.getItem() != item) continue;
                        stack.increment(1);
                        b = true;
                    }
                    if (b) continue;
                    needStacks.add(new ItemStack(item, 1));
                }
                int i = 0;
                for (ItemStack stack : needStacks) {
                    if (stack.isEmpty()) continue;
                    needInventory.setStack(i, stack);
                    i++;
                    if (i == needInventory.size()) break;
                }
            }
        } else {
            pos1 = pos2 = null;
            blueprintMap = new LinkedHashMap<>();
            for (int i = 0; i < needInventory.size(); i++) {
                needInventory.setStack(i, ItemStack.EMPTY);
            }
            return;
        }
        if (inventory.isEmpty()) return;

        if (blueprintMap.isEmpty()) return;
        if (getEnergy() > getEuPerTick(getEnergyCost())) {
            // ここに処理を記入
            if (coolTime <= 0) {
                coolTime = getSettingCoolTime();
                if (tryBuilding()) {
                    useEnergy(getEnergyCost());
                }
            }
            coolTimeBonus();
            coolTime = coolTime - getBasicSpeed();
            if (!isActive()) {
                builder.setActive(true, getWorld(), getPos());
            }
        } else if (isActive()) {
            builder.setActive(false, getWorld(), getPos());
        }
    }

    public ItemStack latestGotStack = ItemStack.EMPTY;

    public static boolean isStorageBox(ItemStack stack) {
        return ItemUtil.toID(stack.getItem()).toString().equals("storagebox:storagebox");
    }

    public ItemStack getInventoryStack(Block block) {
        for (ItemStack stack : getInventory().getStacks()) {
            if (stack.isEmpty()) continue;

            latestGotStack = stack;
            if (stack.getItem() instanceof BlockItem && stack.getItem() == block.asItem()) return stack;
            // StorageBox
            if (isStorageBox(stack)) {
                NbtCompound tag = stack.getNbt();
                if (tag.contains("StorageItemData")) {
                    ItemStack itemInBox = ItemStack.fromNbt(tag.getCompound("StorageItemData"));
                    if (itemInBox.getItem() instanceof BlockItem && itemInBox.getItem() == block.asItem()) return itemInBox;
                }
            }
            // ---- StorageBox
        }
        return ItemStack.EMPTY;
    }

    public boolean tryPlacing(BlockPos blockPos, BlockState state) {
        if (getInventoryStack(state.getBlock()).isEmpty()) return false;

        if (getWorld().setBlockState(blockPos, state)) {
            state.getBlock().onPlaced(getWorld(), blockPos, state, null, latestGotStack);
            getWorld().playSound(null, blockPos, state.getSoundGroup().getPlaceSound(), SoundCategory.BLOCKS, 1F, 1F);
            if (isStorageBox(latestGotStack)) {
                NbtCompound tag = latestGotStack.getNbt();
                if (tag.contains("StorageSize")) {
                    int countInBox = tag.getInt("StorageSize");
                    countInBox--;
                    tag.putInt("StorageSize", countInBox);
                    if (countInBox <= 0) {
                        tag.remove("StorageItemData");
                        tag.remove("StorageSize");
                    }
                    latestGotStack.setNbt(tag);
                }
                return true;
            }
            latestGotStack.decrement(1);
            return true;
        }
        return false;

    }

    public boolean tryBuilding() {
        if (getWorld() == null || getWorld().isClient()) return false;
        if (pos1 == null || pos2 == null)
            return false;
        int procX;
        int procY;
        int procZ;
        for (procY = pos1.getY(); procY <= pos2.getY(); procY++) {
            for (procX = pos1.getX(); procX <= pos2.getX(); procX++) {
                for (procZ = pos1.getZ(); procZ <= pos2.getZ(); procZ++) {
                    BlockPos procPos = new BlockPos(procX, procY, procZ);
                    Block procBlock = getWorld().getBlockState(procPos).getBlock();

                    BlockState buildingState = blueprintMap.get(procPos.add(-pos.getX(), -pos.getY(), -pos.getZ()));
                    if (buildingState == null) continue;
                    if (buildingState.getBlock() == Blocks.AIR || procBlock == buildingState.getBlock()) continue;
                    if (procBlock != Blocks.AIR) continue;

                    if (procBlock.asItem() == Items.NORMAL_BUILDER) {
                        continue;
                    }
                    if (tryPlacing(procPos, buildingState)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // クールダウンのエネルギー量による追加ボーナス
    public void coolTimeBonus() {
        if (getBaseMaxPower() / 1.125 < getEnergy()) {
            coolTime = coolTime - getBasicSpeed() * 5;
        }
        if (getBaseMaxPower() / 1.25 < getEnergy()) {
            coolTime = coolTime - getBasicSpeed() * 3;
        }
        if (getBaseMaxPower() / 3 < getEnergy()) {
            coolTime = coolTime - getBasicSpeed() * 2;
        }
        if (getBaseMaxPower() / 5 < getEnergy()) {
            coolTime = coolTime - getBasicSpeed();
        }
        if (getBaseMaxPower() / 7 < getEnergy()) {
            coolTime = coolTime - getBasicSpeed();
        }
        if (getBaseMaxPower() / 10 < getEnergy()) {
            coolTime = coolTime - getBasicSpeed();
        }
        if (getBaseMaxPower() / 12 < getEnergy()) {
            coolTime = coolTime - getBasicSpeed();
        }
        if (getBaseMaxPower() / 15 < getEnergy()) {
            coolTime = coolTime - getBasicSpeed();
        }
        if (getBaseMaxPower() / 16 < getEnergy()) {
            coolTime = coolTime - getBasicSpeed();
        }
        if (getBaseMaxPower() / 20 < getEnergy()) {
            coolTime = coolTime - getBasicSpeed();
        }
        if (getBaseMaxPower() / 30 < getEnergy()) {
            coolTime = coolTime - getBasicSpeed();
        }
        if (getBaseMaxPower() / 40 < getEnergy()) {
            coolTime = coolTime - getBasicSpeed();
        }
    }

    // マーカーによる範囲指定を許可するか？
    public boolean canSetPosByMarker() {
        return false;
    }

    private BlockPos pos1 = null;
    private BlockPos pos2 = null;

    public BlockPos getPos1() {
        return pos1;
    }

    public BlockPos getPos2() {
        return pos2;
    }

    public void setPos1(BlockPos pos1) {
        this.pos1 = pos1;
    }

    public void setPos2(BlockPos pos2) {
        this.pos2 = pos2;
    }

    public BuilderTile(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public void init() {
        int index = 0;
        SlotConfiguration.SlotIO output = new SlotConfiguration.SlotIO(SlotConfiguration.ExtractConfig.OUTPUT);
        SlotConfiguration.SlotIO input = new SlotConfiguration.SlotIO(SlotConfiguration.ExtractConfig.INPUT);
        SlotConfiguration slotConfig = new SlotConfiguration(getInventory());
        for (;index < getInventory().size();index++){
            slotConfig.getSlotDetails(index).updateSlotConfig(new SlotConfiguration.SlotConfig(Direction.NORTH, input, index));
            slotConfig.getSlotDetails(index).updateSlotConfig(new SlotConfiguration.SlotConfig(Direction.SOUTH, input, index));
            slotConfig.getSlotDetails(index).updateSlotConfig(new SlotConfiguration.SlotConfig(Direction.EAST, input, index));
            slotConfig.getSlotDetails(index).updateSlotConfig(new SlotConfiguration.SlotConfig(Direction.WEST, input, index));
            slotConfig.getSlotDetails(index).updateSlotConfig(new SlotConfiguration.SlotConfig(Direction.UP, input, index));
            slotConfig.getSlotDetails(index).updateSlotConfig(new SlotConfiguration.SlotConfig(Direction.DOWN, output, index));
            markDirty();
        }
        ((MachineBaseBlockEntityAccessor) this).setSlotConfiguration(slotConfig);
    }
}