package de.ph1b.audiobook.model;

import android.content.Context;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import de.ph1b.audiobook.utils.L;
import de.ph1b.audiobook.utils.Validate;


public class Book implements Comparable<Book> {

    public static final float MIN_SPEED = 0.5F;
    public static final float MAX_SPEED = 2.0F;
    public static final int LOUDNESS_ENHANCED_MAX = 4000;

    public static final String TAG = Book.class.getSimpleName();
    private static final long ID_UNKNOWN = -1;
    private final HashMap<Short, Short> equalizerLevels = new HashMap<>();
    @NonNull
    private final String root;
    @NonNull
    private final ArrayList<Chapter> chapters;
    @NonNull
    private final Type type;
    @NonNull
    private final String packageName;
    @NonNull
    private final ArrayList<Bookmark> bookmarks = new ArrayList<>();
    @Nullable
    private final String author;
    private long id = ID_UNKNOWN;
    @NonNull
    private String name;
    private int time = 0;
    private float playbackSpeed = 1.0f;
    @NonNull
    private String currentMediaPath;
    private boolean useCoverReplacement = false;
    private int loudnessEnhanced = 0;

    public Book(Book that) {
        new Validate().notNull(that.root, that.name, that.chapters, that.currentMediaPath, that.type,
                equalizerLevels)
                .notEmpty(that.root, that.name, that.currentMediaPath)
                .notEmpty(that.chapters);

        this.id = that.id;
        this.root = that.root;
        ArrayList<Chapter> copyChapters = new ArrayList<>();
        for (Chapter c : that.chapters) {
            copyChapters.add(new Chapter(c));
        }
        this.chapters = copyChapters;
        this.type = Type.valueOf(that.type.name());
        this.packageName = that.packageName;
        for (Bookmark b : that.bookmarks) {
            this.bookmarks.add(new Bookmark(b));
        }
        this.name = that.name;
        this.author = that.author;
        this.time = that.time;
        this.playbackSpeed = that.playbackSpeed;
        this.currentMediaPath = that.currentMediaPath;
        this.useCoverReplacement = that.useCoverReplacement;
        this.equalizerLevels.putAll(that.equalizerLevels);
        this.loudnessEnhanced = that.loudnessEnhanced;
    }

    public Book(@NonNull String root,
                @NonNull String name,
                @Nullable String author,
                @NonNull ArrayList<Chapter> chapters,
                @NonNull
                String currentMediaPath,
                @NonNull Type type,
                @NonNull Context c) {
        new Validate().notNull(root, name, chapters, currentMediaPath, type)
                .notEmpty(root, name)
                .notEmpty(chapters);

        this.root = root;
        this.name = name;
        this.author = author;
        this.chapters = chapters;
        this.type = type;
        this.packageName = c.getPackageName();
        setPosition(0, currentMediaPath);
        this.currentMediaPath = currentMediaPath;
    }

    /**
     * Sets Equalizer band levels. {@link android.media.audiofx.Equalizer#setBandLevel(short, short)}
     *
     * @param band  the band
     * @param level the level
     */
    public void setBandLevel(short band, short level) {
        equalizerLevels.put(band, level);
    }

    /**
     * Gets Equalizer band levels. {@link android.media.audiofx.Equalizer#getBandLevel(short)} (short, short)}
     *
     * @param band the band to get the level from
     * @return the level or -1 if it has not been set.
     */
    public short getBandLevel(short band) {
        Short level = equalizerLevels.get(band);
        return level == null ? -1 : level;
    }

    public HashMap<Short, Short> getEqualizerLevels() {
        return equalizerLevels;
    }

    /**
     * @return how much the loudness should be enhanced.
     * @see android.media.audiofx.LoudnessEnhancer
     */
    public int getLoudnessEnhanced() {
        return loudnessEnhanced;
    }

    /**
     * Sets the loudness enhancing.
     *
     * @param loudnessEnhanced how much the loudness should be enhanced.
     * @see android.media.audiofx.LoudnessEnhancer
     */
    public void setLoudnessEnhanced(int loudnessEnhanced) {
        if (loudnessEnhanced < 0 || loudnessEnhanced > LOUDNESS_ENHANCED_MAX)
            throw new IllegalArgumentException("Loudness must be 0 < l < " + LOUDNESS_ENHANCED_MAX +
                    ", you set=" + loudnessEnhanced);
        L.d(TAG, "Set loudnessEnhanced to=" + loudnessEnhanced);
        this.loudnessEnhanced = loudnessEnhanced;
    }

    @NonNull
    public ArrayList<Bookmark> getBookmarks() {
        return bookmarks;
    }


    @NonNull
    public File getCoverFile() {
        File coverFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() +
                File.separator + "Android" + File.separator + "data" + File.separator + packageName,
                id + ".jpg");
        if (!coverFile.getParentFile().exists()) {
            //noinspection ResultOfMethodCallIgnored
            coverFile.getParentFile().mkdirs();
        }
        return coverFile;
    }

    @NonNull
    public Type getType() {
        return type;
    }

    public void setPosition(int time, @NonNull String currentMediaPath) {
        boolean relativeMediaPathExists = false;
        for (Chapter c : chapters) {
            if (c.getPath().equals(currentMediaPath)) {
                relativeMediaPathExists = true;
            }
        }
        if (!relativeMediaPathExists) {
            throw new IllegalArgumentException("Creating book with name=" + name +
                    " failed because currentMediaPath=" + currentMediaPath +
                    " does not exist in chapters");
        }

        this.time = time;
        this.currentMediaPath = currentMediaPath;
    }

    public boolean isUseCoverReplacement() {
        return useCoverReplacement;
    }

    public void setUseCoverReplacement(boolean useCoverReplacement) {
        this.useCoverReplacement = useCoverReplacement;
    }


    /**
     * @return the author of the book or null if not set.
     */
    @Nullable
    public String getAuthor() {
        return author;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o instanceof Book) {
            Book that = (Book) o;

            if (!(this.root.equals(that.root))) return false;

            if (this.chapters.size() != that.chapters.size()) {
                return false;
            } else {
                for (int i = 0; i < this.chapters.size(); i++) {
                    if (!this.chapters.get(i).equals(that.chapters.get(i))) {
                        return false;
                    }
                }
            }

            return this.type == that.type;

        }
        return false;
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = PRIME + root.hashCode();
        for (Chapter c : chapters) {
            result = PRIME * result + c.hashCode();
        }
        return result;
    }

    @Override
    public String toString() {
        return TAG + "[" +
                "root=" + root +
                ", type=" + type +
                ", id=" + id +
                ", name=" + name +
                ", time=" + time +
                ", playbackSpeed=" + playbackSpeed +
                ", loudnessEnhances=" + loudnessEnhanced +
                ", equalizerLevels=" + equalizerLevels +
                ", currentMediaPath=" + currentMediaPath +
                ", useCoverReplacement=" + useCoverReplacement +
                ", chapters=" + chapters +
                "]";
    }

    @NonNull
    public String getCurrentMediaPath() {
        return currentMediaPath;
    }

    @Nullable
    public Chapter getNextChapter() {
        int currentIndex = chapters.indexOf(getCurrentChapter());
        if (currentIndex < chapters.size() - 1) {
            return chapters.get(currentIndex + 1);
        }
        return null;
    }

    @NonNull
    public Chapter getCurrentChapter() {
        for (Chapter c : chapters) {
            if (c.getPath().equals(currentMediaPath)) {
                return c;
            }
        }
        throw new IllegalArgumentException("getCurrentChapter has no valid id with" +
                " currentMediaPath=" + currentMediaPath);
    }

    @Nullable
    public Chapter getPreviousChapter() {
        int currentIndex = chapters.indexOf(getCurrentChapter());
        if (currentIndex > 0) {
            return chapters.get(currentIndex - 1);
        }
        return null;
    }

    public int getTime() {
        return time;
    }

    @NonNull
    public String getName() {
        return name;
    }

    public void setName(@NonNull String name) {
        new Validate().notNull(name)
                .notEmpty(name);
        this.name = name;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @NonNull
    public ArrayList<Chapter> getChapters() {
        return chapters;
    }

    public float getPlaybackSpeed() {
        return playbackSpeed;
    }

    public void setPlaybackSpeed(float playbackSpeed) {
        if (playbackSpeed < MIN_SPEED || playbackSpeed > MAX_SPEED)
            throw new IllegalArgumentException("Playback Speed is=" + playbackSpeed + " but mus be" +
                    " in range of " + MIN_SPEED + " and " + MAX_SPEED);
        this.playbackSpeed = playbackSpeed;
    }

    @NonNull
    public String getRoot() {
        return root;
    }

    @Override
    public int compareTo(@NonNull Book that) {
        return new NaturalOrderComparator().compare(this.name, that.name);
    }

    public enum Type {
        COLLECTION_FOLDER,
        COLLECTION_FILE,
        SINGLE_FOLDER,
        SINGLE_FILE,
    }
}

