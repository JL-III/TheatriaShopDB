package com.playtheatria.shopdb.updater;

import com.Acrobot.Breeze.Utils.BlockUtil;
import com.Acrobot.Breeze.Utils.InventoryUtil;
import com.Acrobot.ChestShop.Signs.ChestShopSign;
import com.Acrobot.ChestShop.Utils.uBlock;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.Sign;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

import static com.Acrobot.ChestShop.Utils.uBlock.SHOP_FACES;

public class ChestShopUtil {
    public static boolean chestIsFull(ItemStack item, Inventory chestShopInventory) {
        return !InventoryUtil.fits(item, chestShopInventory);
    }

    public static List<Sign> findConnectedShopSigns(InventoryHolder chestShopInventoryHolder) {
        List<Sign> result = new ArrayList<>();

        if (chestShopInventoryHolder instanceof DoubleChest) {
            BlockState leftChestSide = (BlockState) ((DoubleChest) chestShopInventoryHolder).getLeftSide();
            BlockState rightChestSide = (BlockState) ((DoubleChest) chestShopInventoryHolder).getRightSide();

            if (leftChestSide == null || rightChestSide == null) {
                return result;
            }

            Block leftChest = leftChestSide.getBlock();
            Block rightChest = rightChestSide.getBlock();

            if (ChestShopSign.isShopBlock(leftChest)) {
                result.addAll(findConnectedShopSigns(leftChest));
            }

            if (ChestShopSign.isShopBlock(rightChest)) {
                result.addAll(findConnectedShopSigns(rightChest));
            }
        } else if (chestShopInventoryHolder instanceof BlockState) {
            Block chestBlock = ((BlockState) chestShopInventoryHolder).getBlock();

            if (ChestShopSign.isShopBlock(chestBlock)) {
                result.addAll(findConnectedShopSigns(chestBlock));
            }
        }

        return result;
    }

    public static List<Sign> findConnectedShopSigns(Block chestBlock) {
        List<Sign> result = new ArrayList<>();

        for (BlockFace bf : SHOP_FACES) {
            Block faceBlock = chestBlock.getRelative(bf);

            if (!BlockUtil.isSign(faceBlock)) {
                continue;
            }

            Sign sign = (Sign) faceBlock.getState();

            Container signContainer = uBlock.findConnectedContainer(sign);
            if (signContainer == null || !chestBlock.equals(signContainer.getBlock())) {
                continue;
            }

            if (ChestShopSign.isValid(sign)) {
                result.add(sign);
            }
        }

        return result;
    }
}
