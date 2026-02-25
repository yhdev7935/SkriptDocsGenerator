package fr.skylyxx.docsgenerator.utils;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.doc.*;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.registrations.Classes;
import fr.skylyxx.docsgenerator.SkriptDocsGenerator;
import fr.skylyxx.docsgenerator.types.DocumentationElement;
import fr.skylyxx.docsgenerator.types.EventDocumentationElement;
import fr.skylyxx.docsgenerator.types.JsonDocOutput;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.addon.SkriptAddon;
import org.skriptlang.skript.bukkit.registration.BukkitSyntaxInfos;
import org.skriptlang.skript.docs.Origin;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class DocBuilder {

    private static final SkriptDocsGenerator skriptDocsGenerator = SkriptDocsGenerator.getPlugin(SkriptDocsGenerator.class);

    @Nullable
    public static DocumentationElement generateElementDoc(SyntaxInfo<?> syntaxInfo) throws Exception {
        Class<?> clazz = syntaxInfo.type();
        if (clazz.isAnnotationPresent(NoDoc.class))
            return null;
        DocumentationElement documentationElement = new DocumentationElement();

        if (clazz.isAnnotationPresent(DocumentationId.class))
            documentationElement.setId(clazz.getAnnotation(DocumentationId.class).value());
        else
            documentationElement.setId(clazz.getSimpleName());

        if (clazz.isAnnotationPresent(Name.class))
            documentationElement.setName(clazz.getAnnotation(Name.class).value());
        else
            documentationElement.setName(clazz.getSimpleName());

        if (clazz.isAnnotationPresent(Description.class))
            documentationElement.setDescription(clazz.getAnnotation(Description.class).value());

        documentationElement.setPatterns(syntaxInfo.patterns().toArray(new String[0]));

        if (clazz.isAnnotationPresent(Examples.class))
            documentationElement.setExamples(clazz.getAnnotation(Examples.class).value());
        if (clazz.isAnnotationPresent(Since.class)) {
            documentationElement.setSince(clazz.getAnnotation(Since.class).value());
        } else {
            SkriptAddon addon = getAddon(syntaxInfo);
            if (addon == null) {
                throw new Exception("No addon found for syntax " + syntaxInfo.type().getName());
            } else {
                documentationElement.setSince(new String[]{getAddonVersion(addon)});
            }
        }
        if (clazz.isAnnotationPresent(RequiredPlugins.class)) {
            documentationElement.setRequiredPlugins(clazz.getAnnotation(RequiredPlugins.class).value());
        }

        return documentationElement;
    }

    public static EventDocumentationElement generateEventDoc(BukkitSyntaxInfos.Event<?> eventInfo) throws Exception {
        SkriptAddon addon = getAddon(eventInfo);
        if (addon == null)
            throw new Exception("No addon found for event " + eventInfo.type().getName());
        String className = eventInfo.type().getSimpleName();

        boolean cancellable = true;
        for (Class<? extends Event> clazz : eventInfo.events()) {
            if(!Cancellable.class.isAssignableFrom(clazz)) {
                cancellable = false;
                break;
            }
        }

        EventDocumentationElement eventDocumentationElement = new EventDocumentationElement()
                .setId(eventInfo.documentationId() == null ? className : eventInfo.documentationId())
                .setName(eventInfo.name())
                .setDescription(toNullableArray(eventInfo.description()))
                .setPatterns(eventInfo.patterns().toArray(new String[0]))
                .setExamples(toNullableArray(eventInfo.examples()))
                .setSince(eventInfo.since().isEmpty() ? new String[]{getAddonVersion(addon)} : eventInfo.since().toArray(new String[0]))
                .setRequiredPlugins(toNullableArray(eventInfo.requiredPlugins()))
                .setCancellable(cancellable);
        return eventDocumentationElement;
    }

    public static DocumentationElement generateClassInfoDoc(ClassInfo<?> classInfo) throws Exception {
        SkriptAddon addon = getAddon(classInfo);
        if (addon == null)
            throw new Exception("No addon found for classinfo" + classInfo.getCodeName());
        DocumentationElement documentationElement = new DocumentationElement()
                .setId(getID(classInfo))
                .setName(classInfo.getDocName())
                .setDescription(classInfo.getDescription())
                .setPatterns(new String[]{classInfo.getCodeName()})
                .setExamples(classInfo.getExamples())
                .setSince(new String[]{classInfo.getSince() == null ? getAddonVersion(addon) : classInfo.getSince()});

        return documentationElement;
    }

    private static String getID(ClassInfo<?> classInfo) {
        String result = null;
        try {
            final Method method =  classInfo.getClass().getMethod("getDocumentationId");
            method.setAccessible(true);
            result = (String) method.invoke(classInfo);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e1) {
            try {
                final Method method = classInfo.getClass().getMethod("getDocumentationID");
                method.setAccessible(true);
                result = (String) method.invoke(classInfo);
            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e2) {
                e2.printStackTrace();
            }
        }
        return result != null ? result : classInfo.getCodeName();
    }

    public static int generateAddonDoc(String mainClass, SkriptAddon skriptAddon) throws Exception {
        List<DocumentationElement> effects = new ArrayList<>();
        for (SyntaxInfo<? extends Effect> effect : Skript.instance().syntaxRegistry().syntaxes(SyntaxRegistry.EFFECT)) {
            SkriptAddon addon = getAddon(effect);
            if (addon == null)
                continue;
            if (addon.name().equals(skriptAddon.name())) {
                DocumentationElement documentationElement = generateElementDoc(effect);
                if (documentationElement == null)
                    continue;
                effects.add(documentationElement);
            }
        }

        List<DocumentationElement> expressions = new ArrayList<>();
        for (SyntaxInfo.Expression<?, ?> expression : Skript.instance().syntaxRegistry().syntaxes(SyntaxRegistry.EXPRESSION)) {
            SkriptAddon addon = getAddon(expression);
            if (addon == null)
                continue;
            if (addon.name().equals(skriptAddon.name())) {
                DocumentationElement documentationElement = generateElementDoc(expression);
                if (documentationElement == null)
                    continue;
                expressions.add(documentationElement);
            }
        }

        List<DocumentationElement> sections = new ArrayList<>();
        for (SyntaxInfo<? extends ch.njol.skript.lang.Section> section : Skript.instance().syntaxRegistry().syntaxes(SyntaxRegistry.SECTION)) {
            SkriptAddon addon = getAddon(section);
            if (addon == null)
                continue;
            if (addon.name().equals(skriptAddon.name())) {
                DocumentationElement documentationElement = generateElementDoc(section);
                if (documentationElement == null)
                    continue;
                sections.add(documentationElement);
            }
        }

        List<DocumentationElement> conditions = new ArrayList<>();
        for (SyntaxInfo<? extends Condition> condition : Skript.instance().syntaxRegistry().syntaxes(SyntaxRegistry.CONDITION)) {
            SkriptAddon addon = getAddon(condition);
            if (addon == null)
                continue;
            if (addon.name().equals(skriptAddon.name())) {
                DocumentationElement documentationElement = generateElementDoc(condition);
                if (documentationElement == null)
                    continue;
                conditions.add(documentationElement);
            }
        }

        List<EventDocumentationElement> events = new ArrayList<>();
        for (BukkitSyntaxInfos.Event<?> event : Skript.instance().syntaxRegistry().syntaxes(BukkitSyntaxInfos.Event.KEY)) {
            SkriptAddon addon = getAddon(event);
            if (addon == null)
                continue;
            if (addon.name().equals(skriptAddon.name())) {
                EventDocumentationElement eventDocumentationElement = generateEventDoc(event);
                events.add(eventDocumentationElement);
            }
        }

        List<DocumentationElement> types = new ArrayList<>();
        for (ClassInfo<?> type : Classes.getClassInfos()) {
            SkriptAddon addon = getAddon(type);
            if (addon == null)
                continue;
            if (addon.name().equals(skriptAddon.name())) {
                DocumentationElement documentationElement = generateClassInfoDoc(type);
                types.add(documentationElement);
            }
        }

        JsonDocOutput jsonDocOutput = new JsonDocOutput(effects, expressions, conditions, events, types, sections);
        String json = skriptDocsGenerator.getGson().toJson(jsonDocOutput);
        File file = new File(skriptDocsGenerator.getDataFolder() + File.separator + skriptAddon.name() + ".json");
        try (FileWriter fileWriter = new FileWriter(file)) {
            fileWriter.write(json);
            fileWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return effects.size() + expressions.size() + conditions.size() + events.size() + types.size() + sections.size();
    }

    @Nullable
    public static SkriptAddon getAddon(SyntaxInfo<?> syntaxInfo) {
        Origin origin = syntaxInfo.origin();
        if (origin instanceof Origin.AddonOrigin addonOrigin) {
            return addonOrigin.addon();
        }
        return getAddon(syntaxInfo.type());
    }

    @Nullable
    public static SkriptAddon getAddon(BukkitSyntaxInfos.Event<?> eventInfo) {
        return getAddon((SyntaxInfo<?>) eventInfo);
    }

    @Nullable
    public static SkriptAddon getAddon(ClassInfo<?> classInfo) {
        if (classInfo.getParser() != null)
            return getAddon(classInfo.getParser().getClass());
        if (classInfo.getSerializer() != null)
            return getAddon(classInfo.getSerializer().getClass());
        if (classInfo.getChanger() != null)
            return getAddon(classInfo.getChanger().getClass());
        return null;
    }

    @Nullable
    public static SkriptAddon getAddon(Class<?> clazz) {
        return getAddon(clazz.getName());
    }

    @Nullable
    public static SkriptAddon getAddon(String clazzName) {
        org.skriptlang.skript.Skript skript = Skript.instance();
        if (clazzName.startsWith("ch.njol.skript") || clazzName.startsWith("org.skriptlang.skript"))
            return skript;
        for (SkriptAddon addon : skript.addons()) {
            String packageName = addon.source().getPackageName();
            if (!packageName.isEmpty() && clazzName.startsWith(packageName)) {
                return addon;
            }
        }
        return null;
    }

    private static String getAddonVersion(SkriptAddon addon) {
        try {
            return JavaPlugin.getProvidingPlugin(addon.source()).getDescription().getVersion();
        } catch (IllegalArgumentException exception) {
            return "unknown";
        }
    }

    @Nullable
    private static String[] toNullableArray(Collection<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.toArray(new String[0]);
    }

}
