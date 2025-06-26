/*
 * Hello Minecraft! Launcher
 * Copyright (C) 2025 huangyuhui <huanghongxun2008@126.com> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.jackhuang.hmcl.schematic;

import com.github.steveice10.opennbt.NBTIO;
import com.github.steveice10.opennbt.tag.builtin.*;
import javafx.geometry.Point3D;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Objects;
import java.util.zip.GZIPInputStream;

/**
 * @author Glavo
 * @see <a href="https://litemapy.readthedocs.io/en/v0.9.0b0/litematics.html">The Litematic file format</a>
 */
public final class LitematicFile {
    private static final String TAG_VERSION = "Version";
    private static final String TAG_METADATA = "Metadata";
    private static final String TAG_REGIONS = "Regions";
    private static final String TAG_SUB_VERSION = "SubVersion";
    private static final String TAG_MINECRAFT_DATA_VERSION = "MinecraftDataVersion";

    private static final String TAG_PREVIEW_IMAGE = "PreviewImageData";
    private static final String TAG_NAME = "Name";
    private static final String TAG_AUTHOR = "Author";
    private static final String TAG_DESCRIPTION = "Description";
    private static final String TAG_TIME_CREATED = "TimeCreated";
    private static final String TAG_TIME_MODIFIED = "TimeModified";
    private static final String TAG_TOTAL_BLOCKS = "TotalBlocks";
    private static final String TAG_TOTAL_VOLUME = "TotalVolume";
    private static final String TAG_ENCLOSING_SIZE = "EnclosingSize";
    private static final String TAG_SIZE_X = "x";
    private static final String TAG_SIZE_Y = "y";
    private static final String TAG_SIZE_Z = "z";

    private static int getIntValue(Tag tag, int defaultValue) {
        return tag instanceof IntTag ? ((IntTag) tag).getValue() : defaultValue;
    }

    private static @Nullable Instant getInstantValue(Tag tag) {
        return tag instanceof LongTag ? Instant.ofEpochMilli(((LongTag) tag).getValue()) : null;
    }

    private static @Nullable String getStringValue(Tag tag) {
        return tag instanceof StringTag ? ((StringTag) tag).getValue() : null;
    }

    public static LitematicFile load(Path file) throws IOException {
        Objects.requireNonNull(file, "File path cannot be null");

        final CompoundTag root;
        try (InputStream in = new GZIPInputStream(Files.newInputStream(file))) {
            root = (CompoundTag) NBTIO.readTag(in);
        }

        final IntTag versionTag = getRequiredTag(root, TAG_VERSION, IntTag.class, file);
        final CompoundTag metadataTag = getRequiredTag(root, TAG_METADATA, CompoundTag.class, file);

        int regionCount = 0;
        Tag regionsTag = root.get(TAG_REGIONS);
        if (regionsTag instanceof CompoundTag) {
            regionCount = ((CompoundTag) regionsTag).size();
        }

        return new LitematicFile(
                file,
                metadataTag,
                versionTag.getValue(),
                getIntValue(root.get(TAG_SUB_VERSION), 0),
                getIntValue(root.get(TAG_MINECRAFT_DATA_VERSION), 0),
                regionCount
        );
    }

    private static <T extends Tag> T getRequiredTag(CompoundTag parent, String name,
                                                    Class<T> type, Path file) throws IOException {
        Tag tag = parent.get(name);
        if (tag == null) {
            throw new IOException(String.format("Missing required tag '%s' in file: %s", name, file));
        }
        if (!type.isInstance(tag)) {
            throw new IOException(String.format(
                    "Expected %s for '%s' but got %s in file: %s",
                    type.getSimpleName(), name, tag.getClass().getSimpleName(), file));
        }
        return type.cast(tag);
    }

    private final @NotNull Path file;
    private final int version;
    private final int subVersion;
    private final int minecraftDataVersion;
    private final int regionCount;
    private final int[] previewImageData;
    private final String name;
    private final String author;
    private final String description;
    private final Instant timeCreated;
    private final Instant timeModified;
    private final int totalBlocks;
    private final int totalVolume;
    private final Point3D enclosingSize;

    private LitematicFile(
            @NotNull Path file,
            @NotNull CompoundTag metadata,
            int version,
            int subVersion,
            int minecraftDataVersion,
            int regionCount
    ) {
        this.file = file;
        this.version = version;
        this.subVersion = subVersion;
        this.minecraftDataVersion = minecraftDataVersion;
        this.regionCount = regionCount;


        Tag previewTag = metadata.get(TAG_PREVIEW_IMAGE);
        this.previewImageData = (previewTag instanceof IntArrayTag)
                ? ((IntArrayTag) previewTag).getValue() : null;

        this.name = getStringValue(metadata.get(TAG_NAME));
        this.author = getStringValue(metadata.get(TAG_AUTHOR));
        this.description = getStringValue(metadata.get(TAG_DESCRIPTION));
        this.timeCreated = getInstantValue(metadata.get(TAG_TIME_CREATED));
        this.timeModified = getInstantValue(metadata.get(TAG_TIME_MODIFIED));
        this.totalBlocks = getIntValue(metadata.get(TAG_TOTAL_BLOCKS), 0);
        this.totalVolume = getIntValue(metadata.get(TAG_TOTAL_VOLUME), 0);

        Point3D size = null;
        Tag sizeTag = metadata.get(TAG_ENCLOSING_SIZE);
        if (sizeTag instanceof CompoundTag) {
            CompoundTag sizeCompound = (CompoundTag) sizeTag;
            int x = getIntValue(sizeCompound.get(TAG_SIZE_X), -1);
            int y = getIntValue(sizeCompound.get(TAG_SIZE_Y), -1);
            int z = getIntValue(sizeCompound.get(TAG_SIZE_Z), -1);

            if (x >= 0 && y >= 0 && z >= 0) {
                size = new Point3D(x, y, z);
            }
        }
        this.enclosingSize = size;
    }

    public @NotNull Path getFile() {
        return file;
    }

    public int getVersion() {
        return version;
    }

    public int getSubVersion() {
        return subVersion;
    }

    public int getMinecraftDataVersion() {
        return minecraftDataVersion;
    }

    public int[] getPreviewImageData() {
        return previewImageData != null ? previewImageData.clone() : null;
    }

    public String getName() {
        return name;
    }

    public String getAuthor() {
        return author;
    }

    public String getDescription() {
        return description;
    }

    public Instant getTimeCreated() {
        return timeCreated;
    }

    public Instant getTimeModified() {
        return timeModified;
    }

    public int getTotalBlocks() {
        return totalBlocks;
    }

    public int getTotalVolume() {
        return totalVolume;
    }

    public Point3D getEnclosingSize() {
        return enclosingSize;
    }

    public int getRegionCount() {
        return regionCount;
    }
}