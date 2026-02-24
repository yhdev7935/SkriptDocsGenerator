package fr.skylyxx.docsgenerator.commands;

import ch.njol.skript.Skript;
import ch.njol.skript.SkriptAddon;
import ch.njol.util.Pair;
import fr.skylyxx.docsgenerator.SkriptDocsGenerator;
import fr.skylyxx.docsgenerator.utils.DocBuilder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class SkriptDocsGeneratorCommand implements CommandExecutor, TabCompleter {

    private final SkriptDocsGenerator skriptDocsGenerator;
    private final AddonDocGenerator addonDocGenerator;

    @FunctionalInterface
    interface AddonDocGenerator {
        int generate(Pair<String, SkriptAddon> addon) throws Exception;
    }

    public SkriptDocsGeneratorCommand() {
        this(SkriptDocsGenerator.getPlugin(SkriptDocsGenerator.class), DocBuilder::generateAddonDoc);
    }

    SkriptDocsGeneratorCommand(SkriptDocsGenerator skriptDocsGenerator) {
        this(skriptDocsGenerator, DocBuilder::generateAddonDoc);
    }

    SkriptDocsGeneratorCommand(SkriptDocsGenerator skriptDocsGenerator, AddonDocGenerator addonDocGenerator) {
        this.skriptDocsGenerator = skriptDocsGenerator;
        this.addonDocGenerator = addonDocGenerator;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (Skript.isAcceptRegistrations()) {
            sender.sendMessage(skriptDocsGenerator.getColored("&aSkriptDocsGenerator &6» &cSkript hasn't finished addon registration !"));
            return true;
        }
        List<SkriptAddon> addons = new ArrayList<>(Skript.getAddons());
        addons.add(Skript.getAddonInstance());
        if (args.length < 1) {
            for (SkriptAddon addon : addons) {
                try {
                    int syntaxes = addonDocGenerator.generate(new Pair<>(addon.source().getName(), addon));
                    sender.sendMessage(skriptDocsGenerator.getColored("&aSkriptDocsGenerator &6» &aDocumentation generated for &2" + addon.getName() + " &a(&e" + syntaxes + " syntaxes&a)"));
                } catch (Exception e) {
                    sender.sendMessage(skriptDocsGenerator.getColored("&aSkriptDocsGenerator &6» &cAn error has occurred while generating documentation for &4" + addon.getName() + "&c: &4" + e.getMessage()));
                }
            }
        } else {
            SkriptAddon addon = null;
            for (SkriptAddon skriptAddon : addons) {
                if (skriptAddon.getName().equalsIgnoreCase(args[0]))
                    addon = skriptAddon;
            }
            if (addon == null) {
                sender.sendMessage(skriptDocsGenerator.getColored("&aSkriptDocsGenerator &6» &cNo addon with name &4" + args[0] + " &cwas found !"));
            } else {
                try {
                    int syntaxes = addonDocGenerator.generate(new Pair<>(addon.source().getName(), addon));
                    sender.sendMessage(skriptDocsGenerator.getColored("&aSkriptDocsGenerator &6» &aDocumentation generated for &2" + addon.getName() + " &a(&e" + syntaxes + " syntaxes&a)"));
                } catch (Exception e) {
                    sender.sendMessage(skriptDocsGenerator.getColored("&aSkriptDocsGenerator &6» &cAn error has occurred while generating documentation for &4" + addon.getName() + "&c: &4" + e.getMessage()));
                }
            }
        }
        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> list = new ArrayList<>();
        for (SkriptAddon addon : Skript.getAddons()) {
            list.add(addon.getName());
        }
        return list;
    }
}
