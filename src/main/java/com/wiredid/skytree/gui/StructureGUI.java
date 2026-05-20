package com.wiredid.skytree.gui;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.util.ComponentUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class StructureGUI {

        public StructureGUI(SkytreePlugin plugin) {

        }

        public void open(Player player, String machineType) {
                // 6 rows (54 slots) to have enough vertical space for 3-4 block high structures
                Inventory gui = Bukkit.createInventory(null, 54,
                                ComponentUtil.parse("Structure: " + formatName(machineType)));

                // Fill background with Black Stained Glass Pane
                ItemStack bg = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
                for (int i = 0; i < 54; i++) {
                        gui.setItem(i, bg);
                }

                // Visualize Structure based on machine type
                // Center column is index 4, 13, 22, 31, 40, 49
                // We generally build bottom-up or top-down. Let's do Top-Down visuals (Top
                // block at top of GUI)

                switch (machineType.toLowerCase()) {
                        case "sieve":
                                gui.setItem(13, createItem(Material.OAK_FENCE, "§6Oak Fence",
                                                "§7(Right-click this to use)"));
                                gui.setItem(22, createItem(Material.CHEST, "§6Chest",
                                                "§7(Put items to sieve here)"));

                                gui.setItem(45, createItem(Material.BOOK, "§e§lHow to Build:",
                                                "§71. Place §eCHEST§7 on ground",
                                                "§72. Place §eOAK FENCE§7 on top of chest",
                                                "",
                                                "§aHow to Use:",
                                                "§71. Put items (dirt/gravel) in §eCHEST",
                                                "§72. Hold §eMESH§7 in hand",
                                                "§73. Right-click the §eOAK FENCE§7 (top)",
                                                "§74. Get random drops!"));
                                break;

                        case "barrel":
                                gui.setItem(22, createItem(Material.OAK_TRAPDOOR, "§6Trapdoor",
                                                "§7(On top)"));
                                gui.setItem(31, createItem(Material.BARREL, "§6Barrel",
                                                "§7(Base)"));

                                gui.setItem(45, createItem(Material.BOOK, "§e§lHow to Build:",
                                                "§71. Place §eBARREL§7 on ground",
                                                "§72. Place §eTRAPDOOR§7 on top",
                                                "",
                                                "§aHow to Use:",
                                                "§71. Hold §eORGANIC ITEM§7 (leaves, etc)",
                                                "§72. Right-click the §eBARREL",
                                                "§73. Get dirt!",
                                                "",
                                                "§7§o(No energy required!)"));
                                break;

                        case "crucible":
                                gui.setItem(22, createItem(Material.BARREL, "§6Barrel",
                                                "§7(Insert Cobble/Ice)"));
                                gui.setItem(31, createItem(Material.MAGMA_BLOCK, "§6Heat Source",
                                                "§7(Fire, Lava, Magma, Campfire)"));

                                gui.setItem(45, createItem(Material.BOOK, "§e§lHow to Build:",
                                                "§71. Place §eHEAT SOURCE§7 on ground",
                                                "§7   (Fire/Lava/Magma/Campfire)",
                                                "§72. Place §eBARREL§7 on top",
                                                "",
                                                "§aHow to Use:",
                                                "§71. Hold §eCOBBLE/ICE§7 in hand",
                                                "§72. Right-click the §eBARREL",
                                                "§73. Get lava/water bucket!"));
                                break;

                        case "compressor":
                                gui.setItem(22, createItem(Material.PISTON, "§6Piston",
                                                "§7(Click to process)"));
                                gui.setItem(31, createItem(Material.DISPENSER, "§6Dispenser",
                                                "§7(Holds items)"));

                                gui.setItem(45, createItem(Material.BOOK, "§e§lHow to Build:",
                                                "§71. Place §eDISPENSER§7 on ground",
                                                "§72. Place §ePISTON§7 on top",
                                                "",
                                                "§aHow to Use:",
                                                "§71. Put §e9+ ITEMS§7 in dispenser",
                                                "§72. Right-click the §ePISTON§7 (top)",
                                                "§73. Get compressed block!",
                                                "",
                                                "§7Recipes: 9 cobble → compressed cobble",
                                                "§7§o(No energy required!)"));
                                break;

                        case "pulverizer":
                                gui.setItem(22, createItem(Material.IRON_BLOCK, "§6Iron Block",
                                                "§7(Click to process)"));
                                gui.setItem(31, createItem(Material.DISPENSER, "§6Dispenser",
                                                "§7(Holds items)"));

                                gui.setItem(45, createItem(Material.BOOK, "§e§lHow to Build:",
                                                "§71. Place §eDISPENSER§7 on ground",
                                                "§72. Place §eIRON BLOCK§7 on top",
                                                "",
                                                "§aHow to Use:",
                                                "§71. Put §eORES§7 in dispenser",
                                                "§72. Right-click the §eIRON BLOCK§7 (top)",
                                                "§73. Get dust!",
                                                "",
                                                "§7Recipes: Iron Ore → 2x Iron Dust",
                                                "§7§o(No energy required!)"));
                                break;

                        case "furnace_advanced":
                                gui.setItem(13, createItem(Material.IRON_TRAPDOOR, "§6Iron Trapdoor",
                                                "§7(Decoration/Cover)"));
                                gui.setItem(22, createItem(Material.BLAST_FURNACE, "§6Blast Furnace",
                                                "§7(Machine Core)"));

                                gui.setItem(45, createItem(Material.BOOK, "§e§lHow to Build:",
                                                "§71. Place §eBLAST FURNACE§7 on ground",
                                                "§72. Place §eIRON TRAPDOOR§7 on top (Optional)",
                                                "",
                                                "§aHow to Use:",
                                                "§71. Open GUI (Right-click)",
                                                "§72. Add Fuel (Coal/Lava)",
                                                "§73. Add Items to Smelt",
                                                "§74. Smelts at §e10x Speed§7!",
                                                "",
                                                "§7Recipes: Iron Dust → Iron Ingot"));
                                break;

                        case "powered_spawner":
                                gui.setItem(22, createItem(Material.SPAWNER, "§6Spawner",
                                                "§7(Entity Source)"));
                                gui.setItem(31, createItem(Material.REDSTONE_BLOCK, "§6Redstone Block",
                                                "§7(Activation)"));

                                gui.setItem(45, createItem(Material.BOOK, "§e§lHow to Build:",
                                                "§71. Place §eREDSTONE BLOCK§7 on ground",
                                                "§72. Place §eSPAWNER§7 on top",
                                                "",
                                                "§c§lNOTE:",
                                                "§7 Requires mob soul to activate."));
                                break;
                }

                // Back Button
                gui.setItem(49, createItem(Material.ARROW, "§cBack", "§7Return to Guide"));

                player.openInventory(gui);
        }

        private ItemStack createItem(Material material, String name, String... lore) {
                ItemStack item = new ItemStack(material);
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                        meta.displayName(ComponentUtil.parse(name));
                        meta.lore(ComponentUtil.parseList(lore));
                        item.setItemMeta(meta);
                }
                return item;
        }

        private String formatName(String input) {
                String[] words = input.split("_");
                StringBuilder sb = new StringBuilder();
                for (String w : words) {
                        sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1).toLowerCase()).append(" ");
                }
                return sb.toString().trim();
        }
}



