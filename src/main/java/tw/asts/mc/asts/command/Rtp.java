package tw.asts.mc.asts.command;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.BasicCommand;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;
import tw.asts.mc.asts.*;

import java.util.*;

public final class Rtp implements BasicCommand {

    private List<String> disabledWorlds;
    private boolean disabledRadiusFar;
    private int radiusDefault;
    private int radiusFar;
    private int cooldownTime = 10;
    private Map<String, Long> cooldowns = new HashMap<>();

    public Rtp(FileConfiguration config) {
        this.disabledWorlds = config.getStringList("rtp.disable.worlds");
        this.disabledRadiusFar = config.getBoolean("rtp.disable.far");
        this.radiusDefault = config.getInt("rtp.radius.default");
        this.radiusFar = config.getInt("rtp.radius.far");
    }

    @Override
    public void execute(@NotNull CommandSourceStack stack, @NotNull String[] args) {

        if (stack.getExecutor() == null || stack.getExecutor().getType() != EntityType.PLAYER) {
            stack.getSender().sendMessage(text.miniMessageComponent(text.miniMessage(BasicConfig.prefix("隨機傳送") + "只有玩家可以使用此指令！")));
            return;
        }
        else if (disabledWorlds.contains(stack.getExecutor().getWorld().getName())) {
            stack.getSender().sendMessage(text.miniMessageComponent(text.miniMessage(BasicConfig.prefix("隨機傳送") + "此世界禁用了隨機傳送！")));
            return;
        }
        else if (args.length > 1) {
            stack.getSender().sendMessage(text.miniMessageComponent(text.miniMessage(BasicConfig.prefix("隨機傳送") + "請輸入正確的參數！")));
            return;
        }
        int max = radiusDefault;
        int minY = 64;
        if (args.length == 1) {
            String arg = args[0];
            if (arg.equals("cave")) {
                minY = -60;
            }
            else if (arg.equals("far")) {
                if (disabledRadiusFar) {
                    stack.getSender().sendMessage(text.miniMessageComponent(text.miniMessage(BasicConfig.prefix("隨機傳送") + "遠距傳送已被禁用！")));
                    return;
                }
                max = radiusFar;
            }
            else {
                stack.getSender().sendMessage(text.miniMessageComponent(text.miniMessage(BasicConfig.prefix("隨機傳送") + "請輸入正確的參數！")));
                return;
            }
        }
        if (cooldowns.containsKey(stack.getSender().getName()) && cooldowns.get(stack.getSender().getName()) - System.currentTimeMillis() / 1000 < cooldownTime * 1000L) {
            long timeLeft = (cooldowns.get(stack.getSender().getName()) + cooldownTime * 1000L - System.currentTimeMillis()) / 1000;
            stack.getSender().sendMessage(text.miniMessageComponent(text.miniMessage(BasicConfig.prefix("隨機傳送") + "請等待 " + timeLeft + " 秒後再使用此指令！")));
            return;
        }
        cooldowns.put(stack.getSender().getName(), System.currentTimeMillis());
        World world = stack.getExecutor().getWorld();
        if (world.getEnvironment() == World.Environment.NETHER) {
            minY = 32;
        }
        // 固體且不安全的方塊
        List<Material> unsafeBlocks = List.of(Material.CACTUS, Material.COBWEB, Material.MAGMA_BLOCK, Material.SWEET_BERRY_BUSH);
        while (stack.getExecutor().isValid()) {
            int x = (int) (Math.random() * max * 2) - max;
            int z = (int) (Math.random() * max * 2) - max;
            int y = world.getHighestBlockYAt(x, z);
            if (y < minY) {
                continue;
            }
            else if (world.getEnvironment() == World.Environment.NETHER) {
                minY = 32;
                while (y >= minY) {
                    y--;
                    if (world.getBlockAt(x, y + 1, z).getType().isAir() && world.getBlockAt(x, y + 2, z).getType().isAir()) {
                        break;
                    }
                }
                if (y < minY) {
                    continue;
                }
            }
            else if (args.length == 1 && args[0].equals("cave")) {
                y = world.getHighestBlockYAt(x, z);
                while (y >= minY - 1) {
                    y--;
                    if (world.getBlockAt(x, y + 1, z).getType().isAir() && world.getBlockAt(x, y + 2, z).getType().isAir()) {
                        break;
                    }
                }
                if (y < minY) {
                    continue;
                }
            }
            Location blockLoc = new Location(world, x, y, z);
            if (!world.getType(blockLoc).isSolid() && !world.getType(blockLoc).isCompostable()) {
                continue;
            }
            else if (unsafeBlocks.contains(world.getType(blockLoc))) {
                continue;
            }
            Location teleportLoc = new Location(world, x + 0.5, y + 1, z + 0.5);
            stack.getExecutor().sendMessage(text.miniMessageComponent(text.miniMessage(BasicConfig.prefix("隨機傳送") + "正在把您傳送至 " + x + ", " + (y + 1) + ", " + z + "...")));
            stack.getExecutor().teleport(teleportLoc);
            break;
        }
    }
    public @NotNull Collection<String> suggest(@NotNull CommandSourceStack stack, @NotNull String[] args) {
        if (args.length <= 1) {
            List<String> arg1 = List.of("cave", "far");
            if (args.length == 1) {
                return arg1.stream().filter(a -> a.toLowerCase().startsWith(args[0].toLowerCase())).toList();
            }
            return arg1;
        }
        return List.of();
    }
}